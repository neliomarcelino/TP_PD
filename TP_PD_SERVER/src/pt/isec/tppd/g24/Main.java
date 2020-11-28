package pt.isec.tppd.g24;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class Main {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    public static void enviaEsteServer(InfoServer esteServer, MulticastSocket socketMulti, InetAddress group, int portMulti) throws IOException {
        ByteArrayOutputStream buff;
        ObjectOutputStream out;
        DatagramPacket dgram;

        buff = new ByteArrayOutputStream();
        out = new ObjectOutputStream(buff);
        out.writeUnshared(esteServer);
        out.flush(); out.close();
        dgram = new DatagramPacket(buff.toByteArray(), buff.size(), group, portMulti);
        socketMulti.send(dgram);
    }

    public static void main(String[] args) {
        if(args.length != 3){
            System.out.println("Sintaxe: java Servidor portUdp portTcp sqlDbAddress");
            return;
        }

        List<InfoServer> listaServers = new ArrayList<>();
        InfoServer esteServer = null;
        int portUdp, portTcp, portMulti = 5432;
        ThreadUdp tUdp;
        ThreadMulticast tMulti;
        ThreadPing tPing;
        InetAddress group;
        MulticastSocket socketMulti = null;
        ServerSocket socketTcp = null;
        Socket socketToClient;
        List<Socket> listaDeClientes = new ArrayList<>();
        String addr, dbName = "Servidor1", sql;
        DatagramSocket socketUdp = null;
        Connection conn = null;
        Statement stmt = null;
        int i = 2;


        try{
            /*
            String localDirectory = System.getProperty("user.dir");
            System.out.println(localDirectory);
            */

            //DATABASE
            Class.forName(JDBC_DRIVER);

            String dbUrl = "jdbc:mysql://" + args[2];
            conn = DriverManager.getConnection(dbUrl, "root", "6677");
            stmt = conn.createStatement();

            sql = "CREATE TABLE IF NOT EXISTS USERS" +
                    "(id INT NOT NULL AUTO_INCREMENT, " +
                    " first VARCHAR(255), " +
                    " last VARCHAR(255), " +
                    " age INTEGER, " +
                    " PRIMARY KEY ( id ))";

            stmt.executeUpdate(sql);

            sql = "CREATE TABLE IF NOT EXISTS MENSAGENS (" +
                    " username TEXT NOT NULL, " +
                    " conteudo TEXT not NULL, " +
                    " timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP );";

            stmt.executeUpdate(sql);

            group = InetAddress.getByName("230.30.30.30");
            portUdp = Integer.parseInt(args[0]);
            portTcp = Integer.parseInt(args[1]);
            socketUdp = new DatagramSocket(portUdp);
            socketTcp = new ServerSocket(portTcp);
            socketMulti = new MulticastSocket(portMulti);
            try{
                socketMulti.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName("lo")));
            }catch (SocketException | NullPointerException | UnknownHostException | SecurityException ex){
                socketMulti.setNetworkInterface(NetworkInterface.getByName("lo")); //e.g., eth0, wlan0, en0, lo
            }
            socketMulti.joinGroup(group);

            addr = InetAddress.getLocalHost().getHostAddress();
            if(addr.equals("0.0.0.0"))
                addr = "127.0.0.1";
            esteServer = new InfoServer(addr, portUdp, portTcp);
            (tUdp = new ThreadUdp(esteServer, listaServers, socketUdp)).start();
            (tMulti = new ThreadMulticast(socketMulti, listaServers, esteServer, stmt, listaDeClientes)).start();
            (tPing = new ThreadPing(30, group, portMulti, listaServers, socketUdp, esteServer)).start();

            enviaEsteServer(esteServer, socketMulti, group, portMulti);

            while(true){
                socketToClient = socketTcp.accept();
                esteServer.addNClientes();
                enviaEsteServer(esteServer, socketMulti, group, portMulti);

                synchronized (listaDeClientes) {
                    listaDeClientes.add(socketToClient);
                }
                new ThreadTcp(socketToClient, group, portMulti, esteServer).start();
            }
        }catch(NumberFormatException e){
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