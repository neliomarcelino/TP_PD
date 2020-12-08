package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
   public static final int MAX_SIZE = 5120;
   
   public static void main(String[] args) {
      if (args.length != 2) {
         System.out.println("Sintaxe: java Cliente serverAddress serverUdpPort");
         return;
      }
      
      DatagramSocket socketUdp = null;
      DatagramPacket packet = null;
      Socket socketTcp = null;
      
      ByteArrayOutputStream bOut;
      ByteArrayInputStream bIn;
      ObjectInputStream in;
      ObjectOutputStream out;
      ObjectInputStream tcp_in;
      ObjectOutputStream tcp_out;
      String ServerRequest = "LIGACAO SERVER";
      boolean conexao = false;
      List<InfoServer> lista; // lista[0] = serverAddr | lista[1] = serverPortUdp | lista[2] = serverPortTcp
      InfoServer inicial;
      MsgServer resposta;
      String canal = "";
      Msg msgEnvio;
      String teclado;
      ServerSocket socket = null;
      
      int op = 0;
      User user = new User();
      Boolean conf;
      
      BufferedReader inTeclado = new BufferedReader(new InputStreamReader(System.in));
      String EXIT = "EXIT";
      ThreadMsg t = null;
      ThreadUpload tUpload = null;
      String[] splitStr = null;
      File f;
      
      try {
         inicial = new InfoServer(args[0], Integer.parseInt(args[1]), - 1);
         socketUdp = new DatagramSocket();
         
         do {
            bOut = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bOut);
            
            out.writeUnshared(ServerRequest);
            out.flush();
            
            packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
            
            System.out.println("A enviar pedido de conexao para o servidor: <" + inicial.getAddr() + ":" + inicial.getPortUdp() + ">");
            
            socketUdp.send(packet);
            socketUdp.setSoTimeout(30000); // 30 sec
            packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
            
            socketUdp.receive(packet);
            
            bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            in = new ObjectInputStream(bIn);
            
            resposta = (MsgServer) in.readObject();
            
            System.out.println("Recebi resposta.");
            
            conexao = resposta.getPodeLigar();
            lista = resposta.getAddrString();
            inicial = lista.get(0);
         } while (! conexao);
         
         do {
            System.out.println("1 - Auntenticacao");
            System.out.println("2 - Cria conta");
            op = Integer.parseInt(inTeclado.readLine());
         } while (op < 1 || op > 2);
         
         switch (op) {
            case 1:
               System.out.println("Autenticacao");
               
               do {
                  System.out.print("Username: ");
                  user.setUsername(inTeclado.readLine());
                  System.out.print("Password: ");
                  user.setPassword(inTeclado.readLine());
                  
                  String login = "LOGIN" + ":" + user.getUsername() + ":" + user.getPassword();
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  out.writeUnshared(login);
                  out.flush();
                  
                  packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
                  
                  socketUdp.send(packet);
                  socketUdp.setSoTimeout(5000); // 5 sec
                  
                  packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                  socketUdp.receive(packet);
                  bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                  in = new ObjectInputStream(bIn);
                  String res = ((String) in.readObject());
                  if (res.equalsIgnoreCase("OK")) {
                     conf = true;
                  } else if (res.equalsIgnoreCase("NOT OK")) {
                     conf = false;
                     System.out.println("Login invalido");
                  } else {
                     conf = false;
                     System.out.println("Erro!");
                  }
               } while (! conf);
               break;
            
            case 2:
               System.out.println("Cria conta");
               do {
                  System.out.print("Nome: ");
                  user.setName(inTeclado.readLine());
                  System.out.print("Username: ");
                  user.setUsername(inTeclado.readLine());
                  System.out.print("Password: ");
                  user.setPassword(inTeclado.readLine());
                  
                  String registo = "REGISTA" + ":" + user.getUsername() + ":" + user.getName() + ":" + user.getPassword();
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  out.writeUnshared(registo);
                  out.flush();
                  
                  packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
                  
                  socketUdp.send(packet);
                  socketUdp.setSoTimeout(5000); // 5 sec
                  
                  packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                  socketUdp.receive(packet);
                  bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                  in = new ObjectInputStream(bIn);
                  
                  String res = (String) in.readObject();
                  if (res.equalsIgnoreCase("OK"))
                     conf = true;
                  else if (res.equalsIgnoreCase("USERNAME IN USE")) {
                     conf = false;
                     System.out.println("Erro ao criar a conta. Username ja utilizado.");
                  } else {
                     System.out.println("Erro!");
                     conf = false;
                  }
               } while (! conf);
               break;
            
            default:
               break;
         }
         
         socketTcp = new Socket(inicial.getAddr(), inicial.getPortTcp());
         
         t = new ThreadMsg(socketTcp, lista, ServerRequest, socketUdp);
         t.start();
         
         while (true) {
            System.out.print("> ");
            teclado = inTeclado.readLine();
            
            if (teclado.equalsIgnoreCase(EXIT)) {
               break;
            }
            
            if (teclado.contains("/fich")) {
               splitStr = teclado.trim().split("\\s+");
               if (splitStr.length != 2) {
                  System.out.println("Erro no numero de argumentos");
                  continue;
               }
			   if (canal.equalsIgnoreCase("")) {
                  System.out.println("Nao está em nenhum canal!");
                  continue;
               }
               f = new File(System.getProperty("user.dir") + File.separator + splitStr[1]);
               if (! f.isFile()) {
                  System.out.println("Ficheiro nao esta na directoria:" + System.getProperty("user.dir"));
                  continue;
               }
               socket = new ServerSocket(0);
               msgEnvio = new Msg(user.getUsername(), teclado + " " + socket.getLocalPort(), canal);
               
            } else if(teclado.contains("/get_fich")){
                    splitStr = teclado.trim().split("\\s+");
                    if(splitStr.length != 2){
                        System.out.println("Erro no numero de argumentos");
                        continue;
                    }
					if (canal.equalsIgnoreCase("")) {
						System.out.println("Nao está em nenhum canal!");
						continue;
					}
                    msgEnvio = new Msg(user.getUsername(), teclado, canal);
            }else if (teclado.contains("/canal")) {
               splitStr = teclado.trim().split("\\s");
               if (splitStr.length != 3) {
                  System.out.println("Erro nos argumentos");
                  continue;
               }
               String nome = splitStr[1];
               String password = splitStr[2];
               String changeChannel = "CHANGE CHANNEL" + ":" + nome + ":" + password;
               tcp_out = new ObjectOutputStream(socketTcp.getOutputStream());
               tcp_out.writeObject(changeChannel);

               tcp_in = new ObjectInputStream(socketTcp.getInputStream());
               String res = (String) tcp_in.readObject();
               if (res.equalsIgnoreCase("OK")) {
                  canal = splitStr[1];
                  System.out.println("Conectado ao canal '" + canal + "'");
               } else if(res.equalsIgnoreCase("INVALID PASSWORD")) {
                  System.out.println("Password invalida!");
               } else if (res.equalsIgnoreCase("NOT OK")) {
                  System.out.println("Erro!");
               }
               continue;
               
            } else if (teclado.contains("/createchannel")) {
               splitStr = teclado.trim().split("\\s");

               conf = true;
               String nome, descricao, password;
               System.out.println("Criar canal");
               System.out.print("Nome do canal: ");
               nome = inTeclado.readLine();
               if(nome.contains(":")) {
                  System.out.println("Nao e possivel criar canais com o simbolo ':'");
                  continue;
               }
               System.out.print("Descricao: ");
               descricao = inTeclado.readLine();
               System.out.print("Password: ");
               password = inTeclado.readLine();
               
               String createChannel = "CREATE CHANNEL" + ":" + nome + ":" + descricao + ":" + password + ":" + user.getUsername();
               
               bOut = new ByteArrayOutputStream();
               out = new ObjectOutputStream(bOut);
               
               out.writeUnshared(createChannel);
               out.flush();
               
               packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
               
               socketUdp.send(packet);
               socketUdp.setSoTimeout(5000); // 5 sec
               
               packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
               socketUdp.receive(packet);
               bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
               in = new ObjectInputStream(bIn);
               
               String res = (String) in.readObject();
               if (res.equalsIgnoreCase("OK")) {
                  canal = nome;
                  System.out.println("Canal '" + canal + "' criado com sucesso!");
               } else if (res.equalsIgnoreCase("NAME IN USE")) {
                  System.out.println("Nao foi possivel criar o canal. Ja existe um canal com nome '" + splitStr[1] + "'");
               } else if (res.equalsIgnoreCase("NOT OK")) {
                  System.out.println("Erro! Canal ou password errada");
               }
               continue;
            }
            else if(teclado.contains("/editchannel")) {
               splitStr = teclado.trim().split("\\s");
               if (splitStr.length != 2) {
                  System.out.println("Erro nos argumentos");
                  continue;
               }
               
               String nome = splitStr[1];
               String editChannel = "EDIT CHANNEL" + ":" + nome + ":" + user.getUsername();
   
               bOut = new ByteArrayOutputStream();
               out = new ObjectOutputStream(bOut);
   
               out.writeUnshared(editChannel);
               out.flush();
   
               packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
   
               socketUdp.send(packet);
               socketUdp.setSoTimeout(5000); // 5 sec
   
               packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
               socketUdp.receive(packet);
               bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
               in = new ObjectInputStream(bIn);
   
               String res = (String) in.readObject();
               if(res.equalsIgnoreCase("NOT ADMIN")) {
                  System.out.println("Apenas o admin pode editar os canais");
                  continue;
               }else if (res.equalsIgnoreCase("NOT OK")) {
                  System.out.println("ERRO!");
                  continue;
               }
   
               System.out.println("Edita canal " + nome + ":");
               System.out.println("Descricao: ");
               String descricao = inTeclado.readLine();
               System.out.println("Password: ");
               String password = inTeclado.readLine();
               System.out.println("Admin: ");
               String admin = inTeclado.readLine();
               editChannel = "EDIT CHANNEL" + ":" + nome + ":" + descricao + ":" + password + ":" + admin;
   
               bOut = new ByteArrayOutputStream();
               out = new ObjectOutputStream(bOut);
   
               out.writeUnshared(editChannel);
               out.flush();
   
               packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
   
               socketUdp.send(packet);
               socketUdp.setSoTimeout(5000); // 5 sec
   
               packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
               socketUdp.receive(packet);
               bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
               in = new ObjectInputStream(bIn);
   
               res = (String) in.readObject();
               if (res.equalsIgnoreCase("OK")) {
                  System.out.println("Canal '" + nome + "' editado com sucesso!");
               } else if(res.equalsIgnoreCase("ADMIN NOT EXISTS")) {
                  System.out.println("Admin inserido nao existe");
               } else if (res.equalsIgnoreCase("NOT OK")) {
                  System.out.println("Erro! Nao foi possivel editar o canal");
               }
   
               continue;
            }
            else if(teclado.contains("/delchannel")) {
               splitStr = teclado.trim().split("\\s");
               
               String nome = splitStr[1];
               if(nome.contains(":")) {
                  System.out.println("Nao existem canais com o simbolo ':'");
                  continue;
               }
   
               String deleteChannel = "DELETE CHANNEL" + ":" + nome + ":" + user.getUsername();
   
               bOut = new ByteArrayOutputStream();
               out = new ObjectOutputStream(bOut);
   
               out.writeUnshared(deleteChannel);
               out.flush();
   
               packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
   
               socketUdp.send(packet);
               socketUdp.setSoTimeout(5000); // 5 sec
   
               packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
               socketUdp.receive(packet);
               bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
               in = new ObjectInputStream(bIn);
               
               String res = (String) in.readObject();
               if (res.equalsIgnoreCase("OK")) {
                  System.out.println("Canal '" + nome + "' eliminado com sucesso!");
               } else if (res.equalsIgnoreCase("NOT OK")) {
                  System.out.println("Erro!");
               }
               continue;
            }
            else if (teclado.contains("/listchannels")) {
               bOut = new ByteArrayOutputStream();
               out = new ObjectOutputStream(bOut);
   
               out.writeUnshared("LIST CHANNELS");
               out.flush();
   
               packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
   
               socketUdp.send(packet);
               socketUdp.setSoTimeout(5000); // 5 sec
   
               packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
               socketUdp.receive(packet);
               bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
               in = new ObjectInputStream(bIn);
               
               String res = (String) in.readObject();
               if (res.equalsIgnoreCase("NOT OK")) {
                  System.out.println("Nao foi possivel listar os canais!");
               } else if (res.equalsIgnoreCase("NO CHANNELS")) {
                  System.out.println("Nao existem canais criados.");
               } else {
                  splitStr = (res.trim().split(":"));
                  System.out.println("Lista dos canais:");
                  System.out.format("  %15s\t\t%15s\n", "Nome", "Admin");
                  for(int i = 0, j = 1; i < splitStr.length; j++) {
                     System.out.format("%d %15s\t\t%15s\n", j, splitStr[i++], splitStr[i++]);
                  }
               }
               continue;
            }
            else if (teclado.contains("/listusers")){
               bOut = new ByteArrayOutputStream();
               out = new ObjectOutputStream(bOut);
   
               out.writeUnshared("LIST USERS");
               out.flush();
   
               packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
   
               socketUdp.send(packet);
               socketUdp.setSoTimeout(5000); // 5 sec
   
               packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
               socketUdp.receive(packet);
               bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
               in = new ObjectInputStream(bIn);
   
               String res = (String) in.readObject();
               if (res.equalsIgnoreCase("NOT OK")) {
                  System.out.println("Nao foi possivel listar os canais!");
               } else {
                  splitStr = (res.trim().split(":"));
                  System.out.println("  Lista dos utilizadores:");
                  
                  for(int i = 0, j = 1; i < splitStr.length; j++) {
                     System.out.format("%d %15s\t\t%15s", j, splitStr[i++], splitStr[i++]);
                  }
               }
               continue;
            }
            else if (teclado.contains("/help")) {
               System.out.println("\n HELP:" +
                                          "\n\n/fich [caminho do ficheiro]" +
                                          "\n\tEnvia ficheiro" +
                                          "\n\n/canal [nome do canal] [password]" +
                                          "\n\tTroca de canal" +
                                          "\n\n/pm [username]" +
                                          "\n\tEnvia mensagem privada para [username]" +
                                          "\n\n/listchannels" +
                                          "\n\tLista todos os canais existentes" +
                                          "\n\n/createchannel" +
                                          "\n\tCria canal de chat" +
                                          "\n\n/editchannel [nome do canal]" +
                                          "\n\tEdita canal (necessita de ser administrador)" +
                                          "\n\n/delchannel [nome do canal]" +
                                          "\n\tElimina canal (necessita de ser administrador)" +
                                          "\n\n/listusers" +
                                          "\n\tLista todos os utilizadores" +
                                          "\n\n");
               continue;
            } else {
               msgEnvio = new Msg(user.getUsername(), teclado, canal);
            }
            synchronized (socketTcp) {
               try {
                  out = new ObjectOutputStream(socketTcp.getOutputStream());
               } catch (SocketException e) {
                  if (t.isAlive()) {
                     socketTcp = t.getSocketTcp();
                     out = new ObjectOutputStream(socketTcp.getOutputStream());
                  } else {
                     break;
                  }
               }
            }
            out.writeObject(msgEnvio);
            out.flush();
            
            if (teclado.contains("/fich")) {
               (tUpload = new ThreadUpload(socket, splitStr[1])).start();
            }
         }
      } catch (SocketTimeoutException e) {
         System.out.println("Nao recebi nenhuma resposta (Servidor down?)\n\t" + e);
      } catch (ClassNotFoundException | UnknownHostException e) {
         System.out.println("Destino desconhecido:\n\t" + e);
      } catch (NumberFormatException e) {
         System.out.println("O porto do servidor deve ser um inteiro positivo.");
      } catch (SocketException e) {
         System.out.println("Ocorreu um erro ao nivel do socket:\n\t" + e);
      } catch (IOException e) {
         System.out.println("Ocorreu um erro no acesso ao socket:\n\t" + e);
      } finally {
         if (t != null) {
            t.terminate();
         }
         try {
            if (socketUdp != null)
               socketUdp.close();
            if (socketTcp != null)
               socketTcp.close();
         } catch (IOException e) {
         }
      }
   }
}