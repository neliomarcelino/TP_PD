package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class ThreadUDP extends Thread {
   protected boolean running;
   private List<InfoServer> listaServers;
   public static final int BUFSIZE = 10000;
   private InfoServer esteServer;
   private DatagramSocket socket;
   private Statement stmt;
   private InetAddress group;
   private int portMulti;
   
   public ThreadUDP(InfoServer esteServer, List<InfoServer> listaServers, DatagramSocket socket, Statement stmt, InetAddress group, int portMulti) {
      this.esteServer = esteServer;
      this.listaServers = listaServers;
      running = true;
      this.socket = socket;
      this.stmt = stmt;
	  this.group = group;
	  this.portMulti = portMulti;
   }
   
   @Override
   public void run() {
      String ServerRequest = "LIGACAO SERVER";
      DatagramPacket receivePacket;
      ByteArrayOutputStream bOut;
      ByteArrayInputStream bIn;
      ObjectOutputStream out;
      ObjectInputStream in;
      
      String receivedMsg, fich;
      MsgServer msgEnviar = null;
      Object obj;
      InfoServer regisServer;
      int carga;
      boolean menosCarga;
      ThreadUpload t;
      Sincronizacao sync;
      try {
         System.out.println("UDP Thread iniciado...");
         while (running) {
            receivePacket = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);
            socket.receive(receivePacket);
            
            bIn = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
            in = new ObjectInputStream(bIn);
            
            obj = in.readObject();
			
			if (obj instanceof Sincronizacao) {
			   ArrayList<String> users = new ArrayList<String>(), mensagens = new ArrayList<String>(), canais = new ArrayList<String>();
               sync = (Sincronizacao) obj;
			   ResultSet rs = null;
				
			   String nome,password,username,canal,imagem,remetente,conteudo,destinatario,descricao,admin;
			   ArrayList<Timestamp> tusers = new ArrayList<Timestamp>(), tmensagens = new ArrayList<Timestamp>(), tcanais = new ArrayList<Timestamp>();
			   Timestamp timestamp;
			   if(sync.getUsers()!=null)
					rs = stmt.executeQuery("SELECT * FROM UTILIZADORES WHERE TIMESTAMP > '" + sync.getUsers() + "';");
			   else
					rs = stmt.executeQuery("SELECT * FROM UTILIZADORES;");
			   if(rs != null)
			   while(rs.next()){
					username = rs.getString("username");
					nome = rs.getString("nome");
					password = rs.getString("password");
					canal = rs.getString("canal");
					timestamp = rs.getTimestamp("timestamp");
				   
				    users.add(username+":"+nome+":"+password+":"+canal);
					tusers.add(timestamp);
			   }
			   if(sync.getMensagens()!=null)
			        rs = stmt.executeQuery("SELECT * FROM MENSAGENS WHERE TIMESTAMP > '" + sync.getMensagens() + "';");
				else
					rs = stmt.executeQuery("SELECT * FROM MENSAGENS;");
			   if(rs != null)
			   while(rs.next()){
					remetente = rs.getString("remetente");
					conteudo = rs.getString("conteudo");
					destinatario = rs.getString("destinatario");
					timestamp = rs.getTimestamp("timestamp");
				   
				    mensagens.add(remetente+":"+conteudo+":"+destinatario+":");
					tmensagens.add(timestamp);
			   }
			   if(sync.getCanais()!=null)
			        rs = stmt.executeQuery("SELECT * FROM CANAIS WHERE TIMESTAMP > '" + sync.getCanais() + "';");
				else
					rs = stmt.executeQuery("SELECT * FROM CANAIS;");
			   if(rs != null)
			   while(rs.next()){
					nome = rs.getString("nome");
					descricao = rs.getString("descricao");
					password = rs.getString("password");
					timestamp = rs.getTimestamp("timestamp");
					admin = rs.getString("admin");
				   
				    canais.add(nome+":"+descricao+":"+password+":"+admin);
					tcanais.add(timestamp);
			   }
			   
			   bOut = new ByteArrayOutputStream();
               out = new ObjectOutputStream(bOut);
			   
			   InfoSincronizacao envia = new InfoSincronizacao(users, mensagens, canais, tusers, tmensagens, tcanais);
			   
			   out.writeUnshared(envia);
			   out.flush();
			   
			   receivePacket.setData(bOut.toByteArray());
               receivePacket.setLength(bOut.size());
                 
               socket.send(receivePacket);
            } else if (obj instanceof InfoServer) {
               regisServer = (InfoServer) obj;
               if (regisServer.getPortUdp() == esteServer.getPortUdp() && regisServer.getPortTcp() == esteServer.getPortTcp() && regisServer.getAddr().equalsIgnoreCase(esteServer.getAddr()))
                  continue;
               synchronized (listaServers) {
                  listaServers.add(regisServer);
               }
               System.out.println("Recebi o servidor " + regisServer.getAddr() + ":" + regisServer.getPortUdp() + ":" + regisServer.getPortTcp() + " Clientes:" + regisServer.getNClientes());
            } else if (obj instanceof String) {
               receivedMsg = (String) obj;
               if (receivedMsg.contains("/fich")) {
                  String[] splitStr = receivedMsg.trim().split("\\s+");
                  DatagramSocket socketfich = new DatagramSocket();
				  (t = new ThreadUpload(socketfich, splitStr[1], splitStr[2])).start();
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  out.writeUnshared(socketfich.getLocalPort());
                  out.flush();
                  receivePacket.setData(bOut.toByteArray());
                  receivePacket.setLength(bOut.size());
                  socket.send(receivePacket);
                  continue;
                  
               } else if (receivedMsg.contains("REGISTA")) {
                  String[] splitStr = receivedMsg.trim().split(":");
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  if (splitStr.length != 4) {
                     out.writeUnshared("NOT OK");
                  } else {
                     String username = splitStr[1];
                     String name = splitStr[2];
                     String password = splitStr[3];
                     ResultSet rs = stmt.executeQuery("SELECT USERNAME FROM UTILIZADORES;");
                     boolean usernameInUse = false;
                     
                     while (rs.next()) {
                        if (rs.getString("USERNAME").equalsIgnoreCase(username)) {
                           usernameInUse = true;
                        }
                     }
                     if (usernameInUse) {
                        out.writeUnshared("USERNAME IN USE");
                        System.out.println("Registo efetuado sem sucesso! Username '" + username + "' ja em uso");
                     } else {
						if (stmt.executeUpdate("INSERT INTO UTILIZADORES (USERNAME, NOME, PASSWORD, CANAL) VALUES ('" + username + "', '" + name + "', '" + password + "', '" + "NO_CHANNEL" + "');")>= 1){
						   out.writeUnshared("REGISTO:"+username+":"+name+":"+password+":"+esteServer);
						   out.flush();
						   DatagramPacket packetMulti = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
						   socket.send(packetMulti);
						
						   bOut = new ByteArrayOutputStream();
						   out = new ObjectOutputStream(bOut);
						   out.writeUnshared("OK");
                        } else {
                           out.writeUnshared("NOT OK");
                        }
                     }
                  }
                  
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
                  
               } else if (receivedMsg.contains("LOGIN")) {
                  String[] splitStr = receivedMsg.split(":");
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  if (splitStr.length != 3) {
                     out.writeUnshared("NOT OK");
                  } else {
                     String nome = splitStr[1];
                     String password = splitStr[2];
                     String canal = "";
                     ResultSet rs = stmt.executeQuery("SELECT USERNAME, PASSWORD, CANAL FROM UTILIZADORES;");
                     boolean conf_user = false;
                     boolean conf_pass = false;
                     while (rs.next()) {
                        if (rs.getString("USERNAME").equalsIgnoreCase(nome)) {
                           conf_user = true;
                           if (rs.getString("PASSWORD").equalsIgnoreCase(password)) {
                              conf_pass = true;
                              canal = rs.getString("CANAL");
                           }
                           break;
                        }
                     }
                     
                     if (conf_pass && conf_user) {
                        out.writeUnshared("OK:" + canal);
                        System.out.println("Utilizador '" + nome + "' efetuou login com a password: '" + password + "' com sucesso!");
                     } else {
                        if(!conf_user) {
                           System.out.println("Utilizador '" + nome + "' efetuou login com a password: '" + password + "' sem sucesso [username desconhecido]");
                           out.writeUnshared("NOT OK:WRONG USERNAME");
                        }else if(!conf_pass) {
                           System.out.println("Utilizador '" + nome + "' efetuou login com a password: '" + password + "' sem sucesso [password errada]");
                           out.writeUnshared("NOT OK:WRONG PASS");
                        }else{
                           System.out.println("Erro no login desconhecido!");
                           out.writeUnshared("NOT OK");
                        }
                     }
                  }
                  
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
                  
               } else if (receivedMsg.contains("EDIT CHANNEL")) {
                  String[] splitStr = receivedMsg.trim().split(":");
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  // confirma se pode editar canal
                  if (splitStr.length == 3) {
                     String nome = splitStr[1];
                     String user = splitStr[2];
                     
                     ResultSet rs = stmt.executeQuery("SELECT nome, admin FROM canais;");
                     int conf = 0;
                     while (rs.next()) {
                        if (rs.getString("NOME").equalsIgnoreCase(nome)) {
                           conf = 1;
                           if (rs.getString("ADMIN").equalsIgnoreCase(user)) {
                              conf = 2;
                           }
                           break;
                        }
                     }
                     if (conf == 2) {
                        out.writeUnshared("OK");
                        System.out.println("User '" + user + "' esta a editar o canal '" + nome + "'");
                     } else if (conf == 1) {
                        out.writeUnshared("NOT ADMIN");
                     } else {
                        out.writeUnshared("NOT OK");
                     }
                  }
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
                  
               }
               if (! receivedMsg.equalsIgnoreCase(ServerRequest))
                  continue;
               carga = 0;
               menosCarga = false;
               synchronized (listaServers) {
                  for (InfoServer p : listaServers)
                     carga += p.getNClientes();
                  
                  for (InfoServer p : listaServers) {
                     if (p.getNClientes() / (carga + 0.0) < 0.5)
                        menosCarga = true;
                  }
                  if (esteServer.getNClientes() / (carga + 0.0) < 0.5)
                     menosCarga = false;
                  
                  if (menosCarga) {
                     Collections.sort(listaServers);
                     msgEnviar = new MsgServer(false, listaServers);
                  } else {
                     for (InfoServer p : listaServers) {
                        if (p.getPortTcp() == esteServer.getPortTcp() && p.getPortUdp() == esteServer.getPortUdp() && p.getAddr().equals(esteServer.getAddr()) && p.getNClientes() == esteServer.getNClientes()) {
                           int index = listaServers.indexOf(p);
                           if (index != 0) {
                              InfoServer aux = listaServers.get(0);
                              listaServers.set(0, p);
                              listaServers.set(index, aux);
                           }
                           break;
                        }
                     }
                     msgEnviar = new MsgServer(true, listaServers);
                  }
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  out.writeUnshared(msgEnviar);
                  out.flush();
               }
               receivePacket.setData(bOut.toByteArray());
               receivePacket.setLength(bOut.size());
               socket.send(receivePacket);
            }
         }
      } catch (SocketException e) {
         e.printStackTrace();
         //System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t" + e);
      } catch (IOException e) {
         e.printStackTrace();
         //System.out.println("Ocorreu um erro no acesso ao socket:\n\t" + e);
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
         //System.out.println("Mensagem recebida de tipo inesperado! " + e);
      } catch (SQLException e) {
         e.printStackTrace();
         //System.out.println("Ocorreu um erro no SQL:\n\t" + e);
      }
   }
   
   public void terminate() {
      running = false;
   }
}