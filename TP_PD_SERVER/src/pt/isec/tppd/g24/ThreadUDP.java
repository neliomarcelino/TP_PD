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
                  (t = new ThreadUpload(socketfich, splitStr[1])).start();
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  out.writeUnshared(socketfich.getLocalPort());
                  out.flush();
                  receivePacket.setData(bOut.toByteArray());
                  receivePacket.setLength(bOut.size());
                  socket.send(receivePacket);
                  continue;
                  
               } else if (receivedMsg.contains("REGISTA")) {
                  String[] user = receivedMsg.trim().split(":", 4);
                  System.out.println("A registar: " + user[1] + ", " + user[2] + ", " + user[3]);
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  ResultSet rs = stmt.executeQuery("SELECT USERNAME FROM USERS;");
                  boolean usernameInUse = false;
                  
                  while (rs.next()) {
                     if (rs.getString("USERNAME").equalsIgnoreCase(user[1])) {
                        usernameInUse = true;
                     }
                  }
                  if(usernameInUse) {
                     out.writeUnshared("USERNAME IN USE");
                     System.out.println("Registo efetuado sem sucesso! Username '" + user[1] + "' ja em uso");
                  } else {
                     if (stmt.executeUpdate("INSERT INTO USERS VALUES ('" + user[1] + "', '" + user[2] + "', '" + user[3] + "');") >= 1)
                        out.writeUnshared("OK");
                     System.out.println("Registo efetuado com sucesso!");
                  }
                  out.flush();
                  DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(),
                                                             receivePacket.getAddress(), receivePacket.getPort());
                  DatagramSocket socket = new DatagramSocket();
                  socket.send(packet);
                  
               } else if (receivedMsg.contains("LOGIN")) {
                  String[] user = receivedMsg.split(":", 3);
                  System.out.println("Utilizador " + user[1] + " a fazer login com password: " + user[2]);
                  
                  bOut = new ByteArrayOutputStream();
                  out = new ObjectOutputStream(bOut);
                  
                  ResultSet rs = stmt.executeQuery("SELECT USERNAME, PASSWORD FROM USERS;");
                  boolean conf = false;
                  while (rs.next()) {
                     if (rs.getString("USERNAME").equalsIgnoreCase(user[1]) && rs.getString("PASSWORD").equalsIgnoreCase(user[2])) {
                        conf = true;
                        break;
                     } else {
                        conf = false;
                     }
                  }
                  
                  if(conf) {
                     out.writeUnshared("OK");
                     System.out.println("Login efetuado com sucesso");
                  } else {
                     System.out.println("Login sem sucesso. username ou password incorreto");
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