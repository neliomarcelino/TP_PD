package pt.isec.tppd.g24;

import pt.isec.tppd.g24.servidor.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
   static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
   public static final int BUFSIZE = 10000;
   
   public static void enviaEsteServer(InfoServer esteServer, InetAddress group, int portMulti) throws IOException {
        ByteArrayOutputStream buff;
        ObjectOutputStream out;
        DatagramSocket socket;
        DatagramPacket dgram;

        socket = new DatagramSocket();
        buff = new ByteArrayOutputStream();
        out = new ObjectOutputStream(buff);
        out.writeUnshared(esteServer);
        out.flush(); out.close();
        dgram = new DatagramPacket(buff.toByteArray(), buff.size(), group, portMulti);
        socket.send(dgram);
    }
   
   public static void main(String[] args) {
      if (args.length != 3) {
         System.out.println("Sintaxe: java Servidor portUdp portTcp serverName");
         return;
      }
      
      List<InfoServer> listaServers = new ArrayList<>();
      InfoServer esteServer = null;
      int portUdp, portTcp, portMulti = 5432;
      ThreadUDP tUdp;
      ThreadMulticast tMulti;
      ThreadPing tPing;
      InetAddress group;
      MulticastSocket socketMulti = null;
      ServerSocket socketTcp = null;
      SocketUser socketToClient;
      List<SocketUser> listaDeClientes = new ArrayList<>();
      String addr, servername = args[2], sql;
      DatagramSocket socketUdp = null;
      Connection conn = null;
      Statement stmt = null;
      int i = 2;
	  String verifica = "", canal = "";
      
      try {
            /*
            String localDirectory = System.getProperty("user.dir");
            System.out.println(localDirectory);
            */
         
         //DATABASE
         Class.forName(JDBC_DRIVER);
         
         String dbUrl = "jdbc:mysql://" + servername;
         try {
            conn = DriverManager.getConnection(dbUrl, "nelio", "carreira");
            stmt = conn.createStatement();
         } catch (SQLException e) {
            e.printStackTrace();
            return;
         }
         
         stmt.executeUpdate("CREATE TABLE IF NOT EXISTS utilizadores (" +
                                    " username VARCHAR(255) NOT NULL UNIQUE, " +
                                    " nome VARCHAR(255) NOT NULL, " +
                                    " password VARCHAR(255) NOT NULL, " +
                                    " canal VARCHAR(255), " +
                                    " imagem VARCHAR(255), " +
                                    " timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                    " PRIMARY KEY ( username ))");
         
         stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mensagens (" +
                                    " remetente VARCHAR(255)  NOT NULL, " +
                                    " conteudo VARCHAR(255)  NOT NULL, " +
                                    " destinatario VARCHAR(255) NOT NULL, " +
                                    " timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP );");
         
         stmt.executeUpdate("CREATE TABLE IF NOT EXISTS canais (" +
                                    " nome VARCHAR(255)  NOT NULL UNIQUE, " +
                                    " descricao VARCHAR(255)  NOT NULL, " +
                                    " password VARCHAR(255)  NOT NULL, " +
                                    " timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                    " admin VARCHAR(255)  NOT NULL);");
									
		 stmt.executeUpdate("CREATE TABLE IF NOT EXISTS modificacoes (" +
                                    " comando VARCHAR(255) NOT NULL, " +
                                    " timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP );");
         
         System.out.println("Using database '" + servername + "' on '" + dbUrl + "'");
		 group = InetAddress.getByName("230.30.30.30");
		 
		 try {
			 socketUdp = new DatagramSocket();
			 
			 ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			 ObjectOutputStream out = new ObjectOutputStream(bOut);
				   
			 out.writeUnshared("PING");
			 out.flush();
			 
			 DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
			 socketUdp.send(packet);
			 
			 socketUdp.setSoTimeout(3000); // 3sec
			 packet = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);
			 socketUdp.receive(packet);
			 
			 socketUdp.setSoTimeout(0);
			 
			 ByteArrayInputStream bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
			 ObjectInputStream in = new ObjectInputStream(bIn);
				
			 Object obj = in.readObject();
			 
			 InfoServer syncServer = (InfoServer) obj;
			 
			 Timestamp comando = null, mensagens = null;
			 ResultSet rs;
			 
			 rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM modificacoes;");
			 
			 if(rs.next()){
				 comando = rs.getTimestamp("timestamp");
			 }
			 
			 rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM mensagens;");
			 
			 if(rs.next()){
				 mensagens = rs.getTimestamp("timestamp");
			 }
			 
			 Sincronizacao sincronizar = new Sincronizacao(comando);
			 
			 bOut = new ByteArrayOutputStream();
			 out = new ObjectOutputStream(bOut);
				   
			 out.writeUnshared(sincronizar);
			 out.flush();
			 
			 packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(syncServer.getAddr()), syncServer.getPortUdp());
			 socketUdp.send(packet);
			 
			 packet = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);
			 socketUdp.receive(packet);
			 
			 bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
			 in = new ObjectInputStream(bIn);
				
			 obj = in.readObject();
			 
			 InfoSincronizacao resultado = (InfoSincronizacao) obj; 
			 
			 ArrayList<String> listComandos = resultado.getListComandos(); 
			 ArrayList<Timestamp> timestampComandos = resultado.getListTimeComandos(); 
			 
			 for(int j = 0; j < listComandos.size(); j++){
				 stmt.executeUpdate(listComandos.get(j));
				 stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO, TIMESTAMP) VALUES (\"" + listComandos.get(j) + "\", '" + timestampComandos.get(j) +"');");
			 }
			 
			 rs = stmt.executeQuery("SELECT * from mensagens where timestamp > '"+mensagens+"';");
			 while(rs.next()){
				 verifica = rs.getString("conteudo");
				 canal = rs.getString("destinatario");
				 String[] splitConteudo = verifica.split("\\s+");
				 if(splitConteudo[0].equalsIgnoreCase("/fich")){
					bOut = new ByteArrayOutputStream();
					out = new ObjectOutputStream(bOut);
					out.writeUnshared(splitConteudo[0] + " " + splitConteudo[1] + " " + canal);
					out.flush();
						 
					packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(syncServer.getAddr()), syncServer.getPortUdp());
					socketUdp.send(packet);
						  
					packet = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);
					socketUdp.receive(packet);
					bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					in = new ObjectInputStream(bIn);
					int filePort = (int) in.readObject();
							
					if(filePort == -1){
						continue;
					}
					(new ThreadDownload(syncServer.getAddr(), filePort, splitConteudo[1], canal)).start();
				 }
			 }
		 } catch (SocketTimeoutException socketTimeoutException) {}
         portUdp = Integer.parseInt(args[0]);
         portTcp = Integer.parseInt(args[1]);
		 socketUdp = new DatagramSocket(portUdp);
         socketTcp = new ServerSocket(portTcp);
         socketMulti = new MulticastSocket(portMulti);
         try {
            socketMulti.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName("lo")));
         } catch (SocketException | NullPointerException | UnknownHostException | SecurityException ex) {
            socketMulti.setNetworkInterface(NetworkInterface.getByName("lo")); //e.g., eth0, wlan0, en0, lo
         }
         socketMulti.joinGroup(group);
         
         addr = InetAddress.getLocalHost().getHostAddress();
         if (addr.equals("0.0.0.0"))
            addr = "127.0.0.1";
         esteServer = new InfoServer(addr, portUdp, portTcp);
         (tUdp = new ThreadUDP(esteServer, listaServers, socketUdp, stmt, group, portMulti)).start();
         (tMulti = new ThreadMulticast(socketMulti, listaServers, esteServer, stmt, listaDeClientes)).start();
         (tPing = new ThreadPing(30, group, portMulti, listaServers, socketUdp, esteServer)).start();
         
         enviaEsteServer(esteServer, group, portMulti);
         
         while (true) {
            socketToClient = new SocketUser();
            socketToClient.setSocket(socketTcp.accept());
            esteServer.addNClientes();
            enviaEsteServer(esteServer, group, portMulti);
            
            synchronized (listaDeClientes) {
               listaDeClientes.add(socketToClient);
            }
            new ThreadTCP(socketToClient, group, portMulti, esteServer, listaDeClientes, stmt).start();
         }
      } catch (NumberFormatException e) {
         System.out.println("O porto de escuta deve ser um inteiro positivo.");
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (SocketException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      } catch (SQLException throwables) {
         throwables.printStackTrace();
      }
   }
}