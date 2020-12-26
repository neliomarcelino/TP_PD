package pt.isec.tppd.g24;

import pt.isec.tppd.g24.cliente.*;

import java.io.*;
import java.net.*;
import java.util.List;
import java.rmi.*;

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
      String ServerRequest = "LIGACAO SERVER";
      boolean conexao = false, cria = false;
      List<InfoServer> lista; // lista[0] = serverAddr | lista[1] = serverPortUdp | lista[2] = serverPortTcp
      InfoServer inicial;
      MsgServer resposta;
      String canal = "";
      Msg msgEnvio;
      String teclado;
      DatagramSocket socket = null;
	  ServidorInterface servidorRmi;
      
      int op = 0;
      User user = new User();
      Boolean conf = false;
      
      BufferedReader inTeclado = new BufferedReader(new InputStreamReader(System.in));
      String EXIT = "EXIT";
      ThreadMsg t = null;
      ThreadUpload tUpload = null;
      String[] splitStr = null;
      File f;
      
      try {
         inicial = new InfoServer(args[0], Integer.parseInt(args[1]), - 1, "");
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
		 
		 //-----------------------------------------------------------RMI----------------------------------------------------------------
		 try{
			servidorRmi = (ServidorInterface) Naming.lookup("rmi://" + inicial.getAddr() + "/" + inicial.getServerName());
		 } catch (NotBoundException e){
			 System.out.println("Registry nao esta up");
			 return;
		 }
		 
		 //-----------------------------------------------------------RMI----------------------------------------------------------------
         
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
                  String[] spliStr = res.split(":");
                  if (spliStr.length > 1) {
                     if (spliStr[0].equalsIgnoreCase("OK")) {
                        conf = true;
                        canal = spliStr[1];
                     } else if (spliStr[0].equalsIgnoreCase("NOT OK")) {
                        conf = false;
                        if (spliStr[1].equalsIgnoreCase("WRONG USERNAME"))
                           System.out.println("Username invalido");
                        else if (spliStr[1].equalsIgnoreCase("WRONG PASS"))
                           System.out.println("Password invalida");
                        else
                           System.out.println("Erro desconhecido");
                     } else {
                        conf = false;
                        System.out.println("Erro desconhecido!");
                     }
                  } else {
                     conf = false;
                     System.out.println("Erro desconhecido!");
                  }
                  
               } while (! conf);
               break;
            
            case 2:
               System.out.println("Cria conta");
			   cria = true;
               do {
                  System.out.print("Nome: ");
                  user.setName(inTeclado.readLine());
                  if (user.getName().contains("=") || user.getName().contains(":")) {
                     System.out.println("Nome do utilizador tem caracteres invalidos!");
                     continue;
                  }
                  System.out.print("Username: ");
                  user.setUsername(inTeclado.readLine());
                  if (user.getUsername().contains("=") || user.getUsername().contains(":")) {
                     System.out.println("Username tem caracteres invalidos!");
                     continue;
                  }
                  System.out.print("Password: ");
                  user.setPassword(inTeclado.readLine());
                  if (user.getPassword().contains("=") || user.getPassword().contains(":")) {
                     System.out.println("Password tem caracteres invalidos!");
                     continue;
                  }
				  System.out.print("Foto: ");
                  user.setFoto(inTeclado.readLine());
                  if (user.getFoto().contains("=") || user.getFoto().contains(":")) {
                     System.out.println("Password tem caracteres invalidos!");
                     continue;
                  }
				  f = new File(System.getProperty("user.dir") + File.separator + user.getFoto());
				  if (!f.isFile()) {
					System.out.println("'"+user.getFoto()+"'Ficheiro nao esta na directoria:" + System.getProperty("user.dir"));
					continue;
				  }
				  
				  //Pede ao registry para registar
				  String res = servidorRmi.regista(user.getName(), user.getUsername(), user.getPassword(), user.getFoto());
				  
				  /*
                  String registo = "REGISTA" + ":" + user.getUsername() + ":" + user.getName() + ":" + user.getPassword()+ ":" + user.getFoto();
                  
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
				  System.out.println(res);
				  */
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
		 UserInterfaceImpl userRmi = new UserInterfaceImpl();
		 servidorRmi.addListener(userRmi, user.getUsername());
		 
		 if(cria){
			 socket = new DatagramSocket();
			 bOut = new ByteArrayOutputStream();
			 out = new ObjectOutputStream(bOut);
			 String imagem = "/foto " + user.getFoto()+ " "+ socket.getLocalPort();
			 out.writeUnshared(imagem);
			 out.flush();
			 
			 packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());
			 socket.send(packet);
			 (tUpload = new ThreadUpload(socket, user.getFoto())).start();
		 }
		 
         socketTcp = new Socket(inicial.getAddr(), inicial.getPortTcp());
         msgEnvio = new Msg(user.getUsername(), "GET CANAL", canal);
         out = new ObjectOutputStream(socketTcp.getOutputStream());
         out.writeObject(msgEnvio);
         out.flush();
         
         t = new ThreadMsg(socketTcp, lista, ServerRequest, socketUdp, canal);
         t.start();
         
         while (true) {
            System.out.print("> ");
            teclado = inTeclado.readLine();
            canal = t.setChannel();
            
            if (teclado.equalsIgnoreCase(EXIT)) {
               break;
            }
			//----------------------------------------------------------------RMI----------------------------------------------------------------
            if (teclado.startsWith("/enviaServer")) {
               splitStr = teclado.trim().split("\\s+");
               if (splitStr.length < 2) {
                  System.out.println("Erro no numero de argumentos");
                  continue;
               }
			   
               msgEnvio = new Msg(user.getUsername(), teclado.substring(13), canal);
			   servidorRmi.envia(msgEnvio);
			   continue;
            }else
			//----------------------------------------------------------------RMI----------------------------------------------------------------
            if (teclado.startsWith("/fich")) {
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
               socket = new DatagramSocket();
               msgEnvio = new Msg(user.getUsername(), teclado + " " + socket.getLocalPort(), canal);
            } else if (teclado.startsWith("/get_fich")) {
               splitStr = teclado.trim().split("\\s+");
               if (splitStr.length != 2) {
                  System.out.println("Erro no numero de argumentos");
                  continue;
               }
               if (canal.equalsIgnoreCase("")) {
                  System.out.println("Nao está em nenhum canal!");
                  continue;
               }
               msgEnvio = new Msg(user.getUsername(), teclado, canal);
            } else if (teclado.contains("/pm")) {
               splitStr = teclado.trim().split("\\s");
               
               if (splitStr.length >= 3) {
				   if(splitStr[2].startsWith("/fich")){
					   if (splitStr.length != 4) {
						System.out.println("Erro no numero de argumentos");
						continue;
					   }
					   f = new File(System.getProperty("user.dir") + File.separator + splitStr[3]);
					   if (!f.isFile()) {
							System.out.println("Ficheiro nao esta na directoria:" + System.getProperty("user.dir"));
							continue;
					   }
				   }
				   if(splitStr[2].startsWith("/get_fich")){
					   if (splitStr.length != 4) {
						System.out.println("Erro no numero de argumentos");
						continue;
					   }
				   }
				   if(splitStr[1] == user.getUsername())
					   continue;
                  StringBuilder pm = new StringBuilder();
                  pm.append("PRIVATE MESSAGE").append(":").append(splitStr[1]).append(":");
                  for (int i = 2; i < splitStr.length; i++) {
                     pm.append(splitStr[i]).append(" ");
                  }
                  msgEnvio = new Msg(user.getUsername(), pm.toString(), canal);
               } else {
                  System.out.println("Erro nos argumentos");
               }
            } else if (teclado.contains("/canal")) {
               splitStr = teclado.trim().split("\\s");
               if (splitStr.length != 3) {
                  System.out.println("Erro nos argumentos");
                  continue;
               }
               String nome = splitStr[1];
               String password = splitStr[2];
               String changeChannel = "CHANGE CHANNEL" + ":" + nome + ":" + password + ":" + user.getUsername();
               msgEnvio = new Msg(user.getUsername(), changeChannel, canal);
               
            } else if (teclado.contains("/estecanal")) {
               if (canal.equalsIgnoreCase(""))
                  System.out.println("Nao esta em nenhum canal");
               else
                  System.out.println("Canal atual: " + canal);
               continue;
            } else if (teclado.contains("/criacanal")) {
               splitStr = teclado.trim().split("\\s");
               
               String nome, descricao, password;
               System.out.println("Criar canal");
               System.out.print("Nome do canal: ");
               nome = inTeclado.readLine();
               if (nome.contains(":")) {
                  System.out.println("Nao e possivel criar canais com o simbolo ':'");
                  continue;
               }
               System.out.print("Descricao: ");
               descricao = inTeclado.readLine();
               System.out.print("Password: ");
               password = inTeclado.readLine();
               
               String createChannel = "CREATE CHANNEL" + ":" + nome + ":" + descricao + ":" + password;
               
               msgEnvio = new Msg(user.getUsername(), createChannel, canal);
               
            } else if (teclado.contains("/editacanal")) {
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
               if (res.equalsIgnoreCase("NOT ADMIN")) {
                  System.out.println("Apenas o admin pode editar os canais");
                  continue;
               } else if (res.equalsIgnoreCase("NOT OK")) {
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
               
               msgEnvio = new Msg(user.getUsername(), editChannel, canal);
               
            } else if (teclado.contains("/eliminacanal")) {
               splitStr = teclado.trim().split("\\s");
               
               String nome = splitStr[1];
               if (nome.contains(":")) {
                  System.out.println("Erro! Nao existe canal com o nome '" + nome + "'");
                  continue;
               }
               
               String deleteChannel = "DELETE CHANNEL" + ":" + nome;
               
               msgEnvio = new Msg(user.getUsername(), deleteChannel, canal);
               
            } else if (teclado.contains("/listar")) {
               splitStr = teclado.trim().split("\\s");
               
               if (splitStr.length == 1) {
                  System.out.println("Modo de uso: /listar [canais|utilizadores|mensagens] [campos](opcional)");
                  System.out.println("\tCampos dos canais: nome, descricao, admin, num_utilizadores, num_mensagens, num_ficheiros");
                  System.out.println("\tCampos dos utilizadores: nome, username, canal");
                  System.out.println("\tCampos das mensagens: n_mensagens=[num mensagens a listar], remetente=[nome do utilizador], destinatario=[nome do utilizador|nome do canal]");
                  continue;
               }
               
               if (! splitStr[1].contains("canais") && ! splitStr[1].contains("utilizadores") && ! splitStr[1].contains("mensagens")) {
                  System.out.println("Nao e possivel listar " + splitStr[1]);
                  System.out.println("Modo de uso: /listar [canais|utilizadores|mensagens] [campos](opcional)");
                  continue;
               }
               
               conf = true;
               if (splitStr[1].contains("canais")) {
                  if (splitStr.length > 2) {
                     for (int i = 2; i < splitStr.length; i++) {
                        if (! splitStr[i].contains("nome") && ! splitStr[i].contains("descricao") && ! splitStr[i].contains("admin") && ! splitStr[i].contains("num_utilizadores") && ! splitStr[i].contains("num_mensagens") && ! splitStr[i].contains("num_ficheiros")) {
                           System.out.println(splitStr[i] + " nao e um field valido");
                           System.out.println("Modo de uso: /listar [canais|utilizadores|mensagens] [campos](opcional)");
                           System.out.println("\tCampos dos canais: nome, descricao, admin, num_utilizadores, num_mensagens, num_ficheiros");
                           conf = false;
                           break;
                        }
                     }
                     if (! conf) {
                        continue;
                     }
                  }
                  conf = false;
               } else if (splitStr[1].contains("utilizadores")) {
                  if (splitStr.length > 2) {
                     for (int i = 2; i < splitStr.length; i++) {
                        if (! splitStr[i].contains("nome") && ! splitStr[i].contains("username") && ! splitStr[i].contains("canal")) {
                           System.out.println(splitStr[i] + " nao e um campo valido");
                           System.out.println("Modo de uso: /listar [canais|utilizadores|mensagens] [campos](opcional)");
                           System.out.println("\tCampos dos utilizadores: nome, username, canal");
                           conf = false;
                           break;
                        }
                     }
                     if (! conf) {
                        continue;
                     }
                  }
                  conf = false;
               } else if (splitStr[1].contains("mensagens")) {
                  if (splitStr.length > 2) {
                     for (int i = 2; i < splitStr.length; i++) {
                        if (! splitStr[i].contains("n_mensagens=") && ! splitStr[i].contains("remetente=") && ! splitStr[i].contains("destinatario=")) {
                           System.out.println(splitStr[i] + " nao e um campo valido");
                           System.out.println("Modo de uso: /listar [canais|utilizadores|mensagens] [campos](opcional)");
                           System.out.println("\tCampos das mensagens: n_mensagens=[num mensagens a listar], remetente=[nome do utilizador], destinatario=[nome do utilizador|nome do canal]");
                           conf = false;
                           break;
                        }
                     }
                     if (! conf) {
                        continue;
                     }
                  }
                  conf = false;
               }
               
               StringBuilder strBuild = new StringBuilder();
               strBuild.append("LIST").append(":");
               
               String listWhat = splitStr[1];
               
               if (listWhat.equalsIgnoreCase("canais")) {
                  strBuild.append("CHANNELS").append(":");
                  
                  if (splitStr.length > 2) {
                     for (int i = 2; i < splitStr.length; i++) {
                        strBuild.append(splitStr[i]).append(",");
                     }
                     strBuild.setLength(strBuild.length() - 1);
                  } else {
                     strBuild.append("DEFAULT");
                  }
                  
               } else if (listWhat.equalsIgnoreCase("utilizadores")) {
                  strBuild.append("USERS").append(":");
                  
                  if (splitStr.length > 2) {
                     for (int i = 2; i < splitStr.length; i++) {
                        strBuild.append(splitStr[i]).append(",");
                     }
                     strBuild.setLength(strBuild.length() - 1);
                  } else {
                     strBuild.append("DEFAULT");
                  }
                  
               } else if (listWhat.equalsIgnoreCase("mensagens")) {
                  strBuild.append("MESSAGES").append(":");
                  
                  if (splitStr.length > 2) {
                     for (int i = 2; i < splitStr.length; i++) {
                        strBuild.append(splitStr[i]).append(",");
                     }
                     strBuild.setLength(strBuild.length() - 1);
                  } else {
                     strBuild.append("DEFAULT");
                  }
               } else {
                  System.out.println("Erro nos argumentos");
                  System.out.println("Modo de uso: /listar [canais|utilizadores|mensagens] [campos](opcional)");
                  System.out.println("\tCampos dos canais: nome, descricao, admin, num_utilizadores, num_mensagens, num_ficheiros");
                  System.out.println("\tCampos dos utilizadores: nome, username, canal");
                  System.out.println("\tCampos das mensagens: n_mensagens=[num mensagens a listar], remetente=[nome do utilizador], destinatario=[nome do utilizador|nome do canal]");
                  continue;
               }
               
               msgEnvio = new Msg(user.getUsername(), strBuild.toString(), canal);
               
            } else if (teclado.contains("/help")) {
               System.out.println("\n HELP:" +
                                          "\n\n/fich [caminho do ficheiro]" +
                                          "\n\tEnvia ficheiro" +
                                          "\n\n/estecanal" +
                                          "\n\tMostra o canal atual" +
                                          "\n\n/canal [channel name] [password]" +
                                          "\n\tTroca de canal" +
                                          "\n\n/pm [username] [mensagem]" +
                                          "\n\tEnvia mensagem privada para [username]" +
                                          "\n\n/listar [canais|utilizadores|mensagens] [campos]" +
                                          "\n\tLista canais, utilizadores ou mensagens" +
                                          "\n\n/criacanal" +
                                          "\n\tCria canal de chat" +
                                          "\n\n/editacanal [nome do canal]" +
                                          "\n\tEdita canal (necessita de ser administrador)" +
                                          "\n\n/eliminacanal [nome do canal]" +
                                          "\n\tElimina canal (necessita de ser administrador)" +
                                          "\n\n");
               continue;
            } else if (canal.equalsIgnoreCase("")) {
               System.out.println("Nao está em nenhum canal!");
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
            
            if (teclado.startsWith("/fich")) {
               (tUpload = new ThreadUpload(socket, splitStr[1])).start();
            }
         }
      } catch (SocketTimeoutException e) {
         System.out.println("Nao recebi nenhuma resposta (Servidor down?)\n\t" + e);
      } catch (ClassNotFoundException e) {
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