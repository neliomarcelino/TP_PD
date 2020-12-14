package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.util.*;

public class ThreadMsg extends Thread {
   private Socket socketTcp;
   protected boolean running;
   private List<InfoServer> lista;
   private String serverRequest;
   private DatagramSocket socketUdp;
   public static final int MAX_SIZE = 10000;
   public String canal;
   
   ThreadMsg(Socket socketTcp, List<InfoServer> lista, String serverRequest, DatagramSocket socketUdp, String canal) {
      this.socketTcp = socketTcp;
      running = true;
      this.lista = lista;
      this.serverRequest = serverRequest;
      this.socketUdp = socketUdp;
      this.canal = canal;
   }
   
   @Override
   public void run() {
      ObjectInputStream in;
      Object obj;
      Msg msg;
      ThreadDownload t = null;
      String resp;
      File f;
	  DatagramSocket socketUp;
      int j;
      if (socketTcp == null || ! running) {
         return;
      }
      
      while (running) {
         try {
            in = new ObjectInputStream(socketTcp.getInputStream());
            obj = in.readUnshared();
            
            if (obj instanceof Msg) {
               msg = (Msg) obj;
               
               System.out.println();
               System.out.println(msg.getUsername() + ": " + msg.getConteudo());
               
            } else if (obj instanceof String) {
               resp = (String) obj;
               if (resp.startsWith("/get_fich")) {
                  String[] splitStr = resp.trim().split("\\s+");
                  if (splitStr[1].equalsIgnoreCase("Erro")) {
                     System.out.println("Ficheiro nao existe no servidor/canal.");
                     continue;
                  }
                  String fileName = splitStr[1];
                  String[] splitFilename = splitStr[1].trim().split("\\.");
                  f = new File(System.getProperty("user.dir") + File.separator + fileName);
                  j = 1;
                  while (f.isFile()) {
                     fileName = splitFilename[0] + "(" + j + ")";
                     for (int k = 1; k < splitFilename.length; k++) {
                        fileName += "." + splitFilename[k];
                     }
                     f = new File(System.getProperty("user.dir") + File.separator + fileName);
                     j++;
                  }
                  (t = new ThreadDownload(socketTcp.getInetAddress().getHostAddress(), Integer.parseInt(splitStr[2]), fileName)).start();
                  continue;
                  
               } else if(resp.contains("GET CANAL")) {
                  String[] splitStr = resp.trim().split(":");
                  if(splitStr.length == 2){
                     if(!splitStr[1].equalsIgnoreCase("NOT OK")){
                        canal = splitStr[1];
                     } else {
                        System.out.println("Nao foi possivel receber o canal");
                     }
                  } else {
                     System.out.println("Nao foi possivel receber o canal");
                  }
                  continue;
               } else if (resp.contains("PRIVATE MESSAGE")) {
                  String[] splitStr = resp.trim().split(":");
                  if(splitStr[1].equalsIgnoreCase("NOT OK")) {
                     System.out.println("Nao foi possivel enviar a mensagem");
                  } else if(splitStr[1].equalsIgnoreCase("USER UNKNOWN")) {
                     System.out.println("Utilizador destino nao existe");
                  } else if(splitStr[1].equalsIgnoreCase("/pm_fich")) {
					  System.out.println(splitStr[2]);
					  socketUp = new DatagramSocket();
					  ByteArrayOutputStream bOut = new ByteArrayOutputStream();
					  ObjectOutputStream out = new ObjectOutputStream(bOut);
                      String ficheiro = "/pm_fich " + splitStr[2] + " " + splitStr[3] + " " + socketUp.getLocalPort();
					  out.writeUnshared(ficheiro);
					  out.flush();
         
					  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(lista.get(0).getAddr()), lista.get(0).getPortUdp());
					  socketUdp.send(packet);
		              (new ThreadUpload(socketUp, splitStr[2])).start();
                  } else if (splitStr[1].equalsIgnoreCase("/pm_get_fich")) {
						if(splitStr[2].equals("Erro"))
							continue;
						String fileName = splitStr[2];
						String[] splitFilename = splitStr[2].trim().split("\\.");
						f = new File(System.getProperty("user.dir") + File.separator + fileName);
						int o = 1;
						while (f.isFile()) {
							fileName = splitFilename[0] + "(" + o + ")";
							for (int k = 1; k < splitFilename.length; k++) {
								fileName += "." + splitFilename[k];
							}	
							f = new File(System.getProperty("user.dir") + File.separator + fileName);
							o++;
						}
						(t = new ThreadDownload(socketTcp.getInetAddress().getHostAddress(), Integer.parseInt(splitStr[3]), fileName)).start();
				  }else {
                     String remetente = splitStr[1];
                     String conteudo = splitStr[2];
                     System.out.println("Private message from " + remetente + ": " + conteudo);
                  }
                  continue;
               }
               else if (resp.contains("CHANGE CHANNEL")) {
                  String[] splitStr = resp.trim().split(":");
                  String status_code = splitStr[1];
                  
                  if (status_code.equalsIgnoreCase("OK")) {
                     canal = splitStr[2];
                     System.out.println("Entraste no canal '" + splitStr[2] + "'");
                  } else if (status_code.equalsIgnoreCase("NOT OK")) {
                     System.out.println("Erro desconhecido!");
                  }
                  continue;
                  
               } else if (resp.contains("CREATE CHANNEL")) {
                  String[] splitStr = resp.trim().split(":");
                  String status_code = splitStr[1];
                  
                  if (status_code.equalsIgnoreCase("OK")) {
                     System.out.println("Canal '" + splitStr[2] + "' criado com sucesso!");
                  } else if (status_code.equalsIgnoreCase("NAME IN USE")) {
                     System.out.println("Nao foi possivel criar o canal. Ja existe um canal com nome '" + splitStr[2] + "'");
                  } else if (status_code.equalsIgnoreCase("NOT OK")) {
                     System.out.println("Erro desconhecido!");
                  }
                  continue;
                  
               } else if (resp.contains("EDIT CHANNEL")) {
                  String[] splitStr = resp.trim().split(":");
                  String status_code = splitStr[1];
                  
                  if (status_code.equalsIgnoreCase("OK")) {
                     System.out.println("Canal '" + splitStr[2] + "' editado com sucesso!");
                  } else if (status_code.equalsIgnoreCase("ADMIN NOT EXISTS")) {
                     System.out.println("Erro! Admin '" + splitStr[2] + "' nao existe no sistema");
                  } else if (status_code.equalsIgnoreCase("NOT OK")) {
                     System.out.println("Erro! Nao foi possivel editar o canal");
                  }
                  continue;
                  
               } else if (resp.contains("DELETE CHANNEL")) {
                  String[] splitStr = resp.trim().split(":");
                  String status_code = splitStr[1];
                  
                  if (status_code.equalsIgnoreCase("OK")) {
                     System.out.println("Canal '" + splitStr[2] + "' eliminado com sucesso!");
                  } else if (status_code.equalsIgnoreCase("NOT ADMIN")) {
                     System.out.println("Erro! Apenas o admin pode eliminar o canal!");
                  } else if (status_code.equalsIgnoreCase("SERVER UNKNOWN")) {
                     System.out.println("Erro! Nao existe canal com o nome '" + splitStr[2] + "'");
                  } else if (status_code.equalsIgnoreCase("NOT OK")) {
                     System.out.println("Erro! Nao foi possivel eliminar o canal");
                  }
                  continue;
                  
               } else if (resp.contains("LIST")) {
                  String[] splitStr = resp.split(":");
                  String listWhat = splitStr[1];
                  String status_code = splitStr[2];
                  int k = 1;
                  
                  if (listWhat.equalsIgnoreCase("CHANNELS")) {
                     if (status_code.equalsIgnoreCase("NO CHANNELS")) {
                        System.out.println("Nao existem canais criados.");
                     } else {
                        String[] fields = splitStr[2].split(",");
                        
                        StringBuilder values = new StringBuilder();
                        String s = "";
                        do{
                           s = (String) in.readUnshared();
                           if(s.equalsIgnoreCase("FINISH"))
                              break;
                           values.append(s).append(",");
                        }while(!s.equalsIgnoreCase("FINISH"));
                        
                        values.setLength(values.length() - 1);
                        String[] field_values = values.toString().split(",");
                        
                        System.out.println("Lista dos canais:");
                        for (int i = 0; i < field_values.length; ) {
                           System.out.println("\nCanal " + k++);
                           for (j = 0; j < fields.length; j++) {
                              System.out.println("\t" + fields[j] + ": " + field_values[i++]);
                           }
                        }
                     }
                  } else if (listWhat.equalsIgnoreCase("USERS")) {
                     if(status_code.equalsIgnoreCase("NOT OK")){
                        System.out.println("Nao foi possivel lista os utilizadores.");
                     }else{
                        String[] fields = splitStr[2].split(",");
                        String[] field_values = splitStr[3].split(",");
   
                        System.out.println("Lista dos users:");
                        for (int i = 0; i < field_values.length; ) {
                           System.out.println("\nUser " + k++);
                           for (j = 0; j < fields.length; j++) {
                              System.out.println("\t" + fields[j] + ": " + field_values[i++]);
                           }
                        }
                     }
                  } else if (listWhat.equalsIgnoreCase("MESSAGES")) {
                     if (status_code.equalsIgnoreCase("NO MESSAGES FOUND")) {
                        System.out.println("Nao foram encontradas mensagens.");
                     } else {
                        String[] fields = splitStr[2].split(",");
                        String[] field_values = splitStr[3].split(",");
      
                        System.out.println("Mensagens:");
                        for (int i = 0; i < field_values.length; ) {
                           System.out.println("\nMensagem " + k++);
                           for (j = 0; j < fields.length; j++) {
                              System.out.println("\t" + fields[j] + ": " + field_values[i++]);
                           }
                        }
                     }
                  }
                  continue;
               }
            }
            System.out.print("> ");
         } catch (SocketException e) {
            synchronized (socketTcp) {
               int i = 0;
               ByteArrayOutputStream bOut;
               ObjectOutputStream out;
               DatagramPacket packet = null;
               ByteArrayInputStream bIn;
               ObjectInputStream iin;
               MsgServer resposta;
               boolean conexao = false;
               do {
                  try {
                     bOut = new ByteArrayOutputStream();
                     out = new ObjectOutputStream(bOut);
                     out.writeUnshared(serverRequest);
                     out.flush();
                     
                     packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(lista.get(i).getAddr()), lista.get(i).getPortUdp());
                     
                     socketUdp.send(packet);
                     socketUdp.setSoTimeout(5000); // 5 sec
                     packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                     socketUdp.receive(packet);
                     
                     bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                     iin = new ObjectInputStream(bIn);
                     
                     resposta = (MsgServer) iin.readObject();
                     
                     conexao = resposta.getPodeLigar();
                     lista = resposta.getAddrString();
                     i = 0;
                  } catch (SocketTimeoutException socketTimeoutException) {
                     i++;
                     if (i == lista.size()) {
                        System.out.println();
                        System.out.println("Conexao com os servidores perdida.");
                        System.out.flush();
                        System.out.println();
                        System.exit(- 1);
                     }
                  } catch (IOException ioException) {
                  } catch (ClassNotFoundException classNotFoundException) {
                  }
               } while (! conexao);
               try {
                  socketTcp = new Socket(lista.get(0).getAddr(), lista.get(0).getPortTcp());
               } catch (IOException ioException) {
                  ioException.printStackTrace();
               }
            }
         } catch (IOException e) {
            System.out.println("ThreadMsg!" + e);
            System.out.println(e);
         } catch (ClassNotFoundException e) {
            System.out.println();
            System.out.println("Mensagem recebida de tipo inesperado! " + e);
         }
      }
   }
   
   public void terminate() {
      running = false;
   }
   
   public Socket getSocketTcp() {
      return socketTcp;
   }
   
   public synchronized String setChannel() {
      return canal;
   }
}