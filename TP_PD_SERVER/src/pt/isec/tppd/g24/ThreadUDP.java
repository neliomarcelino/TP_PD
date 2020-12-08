package pt.isec.tppd.g24;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

public class ThreadUDP extends Thread {
   protected boolean running;
   private List<InfoServer> listaServers;
   public static final int BUFSIZE = 10000;
   private InfoServer esteServer;
   private DatagramSocket socket;
   private Statement stmt;
   
   public ThreadUDP(InfoServer esteServer, List<InfoServer> listaServers, DatagramSocket socket, Statement stmt) {
      this.esteServer = esteServer;
      this.listaServers = listaServers;
      running = true;
      this.socket = socket;
      this.stmt = stmt;
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
      
      try {
         System.out.println("UDP Thread iniciado...");
         while (running) {
            receivePacket = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);
            socket.receive(receivePacket);
            
            bIn = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
            in = new ObjectInputStream(bIn);
            
            obj = in.readObject();
            
            if (obj instanceof InfoServer) {
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
                  ServerSocket socketfich = new ServerSocket(0);
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
                     ResultSet rs = stmt.executeQuery("SELECT USERNAME FROM USERS;");
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
                        if (stmt.executeUpdate("INSERT INTO USERS VALUES ('" + username + "', '" + name + "', '" + password + "');") >= 1)
                           out.writeUnshared("OK");
                        System.out.println("Registo de utilizador ('" + username + "', '" + name + "', '" + password + "') efetuado com sucesso!");
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
                     ResultSet rs = stmt.executeQuery("SELECT USERNAME, PASSWORD FROM USERS;");
                     boolean conf = false;
                     while (rs.next()) {
                        if (rs.getString("USERNAME").equalsIgnoreCase(nome)) {
                           if (rs.getString("PASSWORD").equalsIgnoreCase(password)) {
                              conf = true;
                           }
                           break;
                        }
                     }
                     
                     if (conf) {
                        out.writeUnshared("OK");
                        System.out.println("Utilizador '" + nome + "' efetuou login com a password: '" + password + "' com sucesso!");
                     } else {
                        System.out.println("Utilizador '" + nome + "' efetuou login com a password: '" + password + "' sem sucesso");
                        out.writeUnshared("NOT OK");
                     }
                  }
                  
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
                  
               } else if (receivedMsg.contains("CREATE CHANNEL")) {
                  String[] splitStr = receivedMsg.trim().split(":");
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  if (splitStr.length != 5) {
                     out.writeUnshared("NOT OK");
                  } else {
                     String nome = splitStr[1];
                     String descricao = splitStr[2];
                     String password = splitStr[3];
                     String admin = splitStr[4];
                     
                     ResultSet rs = stmt.executeQuery("SELECT nome FROM canais;");
                     boolean conf = false;
                     while (rs.next()) {
                        if (rs.getString("NOME").equalsIgnoreCase(nome)) {
                           conf = true;
                           break;
                        }
                     }
                     if (conf) {
                        out.writeUnshared("NAME IN USE");
                     } else {
                        if (stmt.executeUpdate("INSERT INTO canais VALUES ('" + nome + "', '" + descricao + "', '" + password + "', '" + admin + "');") >= 1) {
                           out.writeUnshared("OK");
                           System.out.println("Utilizador '" + admin + "' criou o canal '" + nome + "' com sucesso!");
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
                  
               }
               else if (receivedMsg.contains("EDIT CHANNEL")) {
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
                  // Edita canal
                  else if (splitStr.length == 5) {
                     String nome = splitStr[1];
                     String descricao = splitStr[2];
                     String password = splitStr[3];
                     String admin = splitStr[4];
                     
                     ResultSet rs = stmt.executeQuery("SELECT username FROM users;");
                     boolean conf = false;
                     while (rs.next()) {
                        if (rs.getString("USERNAME").equalsIgnoreCase(admin)) {
                           conf = true;
                           break;
                        }
                     }
                     
                     if (conf) {
                        if (stmt.executeUpdate("UPDATE canais " +
                                                       "SET descricao = '" + descricao + "', " +
                                                       "password = '" + password + "', " +
                                                       "admin = '" + admin + "' " +
                                                       "WHERE upper(nome) = upper('" + nome + "');") >= 1) {
                           out.writeUnshared("OK");
                        }
                     } else {
                        out.writeUnshared("ADMIN NOT EXISTS");
                     }
                  } else {
                     out.writeUnshared("NOT OK");
                  }
                  
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
               } else if (receivedMsg.contains("DELETE CHANNEL")) {
                  String[] splitStr = receivedMsg.trim().split(":");
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  if (splitStr.length != 3) {
                     out.writeUnshared("NOT OK");
                  } else {
                     String nome = splitStr[1];
                     String admin = splitStr[2];
                     
                     ResultSet rs = stmt.executeQuery("SELECT nome, admin FROM canais;");
                     boolean conf = false;
                     while (rs.next()) {
                        if (rs.getString("NOME").equalsIgnoreCase(nome)) {
                           if (rs.getString("ADMIN").equalsIgnoreCase(admin)) {
                              conf = true;
                           }
                           break;
                        }
                     }
                     if (conf) {
                        if (stmt.executeUpdate("DELETE FROM canais WHERE UPPER(NOME) = UPPER('" + nome + "');") >= 1) {
                           out.writeUnshared("OK");
                           System.out.println("User '" + admin + "' eliminou canal '" + nome + "'");
                        } else {
                           out.writeUnshared("NOT OK");
                        }
                     } else {
                        out.writeUnshared("NOT OK");
                     }
                  }
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
                  
               }  else if (receivedMsg.contains("LIST CHANNELS")) {
                  ResultSet rs = stmt.executeQuery("SELECT nome, admin FROM canais;");
                  boolean conf = false;
                  StringBuilder channels = new StringBuilder();
                  while (rs.next()) {
                     conf = true;
                     channels.append(rs.getString("NOME"));
                     channels.append(":");
                     channels.append(rs.getString("ADMIN"));
                     channels.append(":");
                  }
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  if (channels.length() > 0) {
                     channels.setLength(channels.length() - 1);
                     out.writeUnshared(channels.toString());
                  }
                  
                  if (! conf) {
                     out.writeUnshared("NO CHANNELS");
                  }
                  
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
                  
               } else if (receivedMsg.contains("LIST USERS")) {
                  ResultSet rs = stmt.executeQuery("SELECT username, name FROM users;");
                  boolean conf = false;
                  StringBuilder channels = new StringBuilder();
                  while (rs.next()) {
                     conf = true;
                     channels.append(rs.getString("username"));
                     channels.append(":");
                     channels.append(rs.getString("name"));
                     channels.append(":");
                  }
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  if (channels.length() > 0) {
                     channels.setLength(channels.length() - 1);
                     out.writeUnshared(channels.toString());
                  }
   
                  if (! conf) {
                     out.writeUnshared("NOT OK");
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