package pt.isec.tppd.g24;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.sql.Statement;
import java.util.List;

public class ThreadMulticast extends Thread {
    public static int MAX_SIZE = 1000000;
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    protected MulticastSocket s;
    protected boolean running;
    private List<InfoServer> listaServers;
    private InfoServer esteServer;
    private Statement stmt;
    private List<Socket> listaDeClientes;

    public ThreadMulticast(MulticastSocket s, List<InfoServer> listaServers, InfoServer esteServer, Statement stmt, List<Socket> listaDeClientes){
        this.s = s;
        running = true;
        this.listaServers = listaServers;
        this.esteServer = esteServer;
        this.stmt = stmt;
        this.listaDeClientes = listaDeClientes;
    }

    public void terminate(){ running = false; }

    @Override
    public void run()
    {
        ObjectInputStream in;
        Object obj;
        DatagramPacket pkt;
        Msg msg;
        InfoServer infoServer;
        ByteArrayOutputStream buff;
        ObjectOutputStream out;
        String sql = null;
        boolean cria;
        int i = 2;

        if(s == null || !running){ return; }

        try{
            while(running){
                cria = true;
                pkt = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                s.receive(pkt);

                try{
                    in = new ObjectInputStream(new ByteArrayInputStream(pkt.getData(), 0, pkt.getLength()));
                    obj = in.readObject();
                    in.close();

                    if(obj instanceof Msg){

                        msg = (Msg)obj;

                        System.out.println(msg.getUsername() + ":" + msg.getConteudo());

                        //Guardar msg na Database
                        sql = "INSERT INTO MENSAGENS (username, conteudo)" +
                                "VALUES ('" + msg.getUsername() + "','" + msg.getConteudo() + "');";;

                        if(stmt.executeUpdate(sql)<1){
                            System.out.println("Entry insertion failed");
                        }
                        //Enviar aos clientes a msg

                        synchronized (listaDeClientes) {
                            for (Socket p : listaDeClientes) {
                                out = new ObjectOutputStream(p.getOutputStream());
                                out.writeUnshared(msg);
                                out.flush();
                            }
                        }

                    }else if(obj instanceof InfoServer){

                        infoServer = (InfoServer)obj;
                        synchronized (listaServers) {
                            for(InfoServer p: listaServers){
                                if(p.getAddr().equals(infoServer.getAddr()) && p.getPortUdp() == infoServer.getPortUdp() && p.getPortTcp() == infoServer.getPortTcp()){
                                    p.setnClientes(infoServer.getNClientes());
                                    System.out.println("Servidor: <" + p.getAddr() + ":" + p.getPortUdp()+ ":" + p.getPortTcp() + "> tem " + p.getNClientes() + " cliente(s).");
                                    cria = false;
                                    break;
                                }
                            }
                            if(!cria)
                                continue;
                            System.out.println("Recebi novo servidor " + infoServer.getAddr() + ":" + infoServer.getPortUdp() + ":" + infoServer.getPortTcp() + " Clientes:" + infoServer.getNClientes());
                            listaServers.add(infoServer);
                            buff = new ByteArrayOutputStream();
                            out = new ObjectOutputStream(buff);
                            out.writeUnshared(esteServer);
                            out.flush();
                            out.close();

                            pkt = new DatagramPacket(buff.toByteArray(), buff.size(), InetAddress.getByName(infoServer.getAddr()), infoServer.getPortUdp());
                            s.send(pkt);
                        }
                    }else if(obj instanceof String){
                        if(((String) obj).equalsIgnoreCase("PING")){
                            buff = new ByteArrayOutputStream();
                            out = new ObjectOutputStream(buff);
                            out.writeUnshared(esteServer);
                            out.flush();
                            out.close();

                            pkt.setData(buff.toByteArray());
                            pkt.setLength(buff.size());
                            //System.out.println(pkt.getAddress().getHostAddress() + ":" + pkt.getPort());
                            s.send(pkt);
                        }
                    }
                }catch(ClassNotFoundException e){
                    System.out.println();
                    System.out.println("Mensagem recebida de tipo inesperado! " + e);
                    continue;
                }catch(IOException e){
                    System.out.println();
                    System.out.println("Impossibilidade de aceder ao conteudo da mensagem recebida! " + e);
                    continue;
                }catch(Exception e){
                    System.out.println();
                    System.out.println("Excepcao: " + e);
                }
            }
        }catch(IOException e){
            if(running){
                System.out.println(e);
            }
            if(!s.isClosed()){
                s.close();
            }
        }
    }
}