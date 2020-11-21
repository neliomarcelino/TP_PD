package pt.isec.tppd.g24;


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

public class ThreadUDP extends Thread{
    protected boolean running;
    private List<InfoServer> listaServers;
    public static final int BUFSIZE = 10000;
    private InfoServer esteServer;
    private DatagramSocket socket;

    public ThreadUDP(InfoServer esteServer, List<InfoServer> listaServers, DatagramSocket socket) {
        this.esteServer = esteServer;
        this.listaServers = listaServers;
        running = true;
        this.socket = socket;
    }

    @Override
    public void run(){
        String ServerRequest = "LIGACAO SERVER";
        DatagramPacket receivePacket;
        ByteArrayOutputStream bOut;
        ByteArrayInputStream bIn;
        ObjectOutputStream out;
        ObjectInputStream in;

        String receivedMsg;
        MsgServer msgEnviar = null;
        Object obj;
        InfoServer regisServer;
        int carga;
        boolean menosCarga;

        try {
            System.out.println("UDP Thread iniciado...");
            while (running) {
                receivePacket = new DatagramPacket(new byte[BUFSIZE],BUFSIZE);
                socket.receive(receivePacket);

                bIn = new ByteArrayInputStream(receivePacket.getData(), 0,receivePacket.getLength());
                in = new ObjectInputStream(bIn);

                obj = in.readObject();


                if(obj instanceof InfoServer){
                    regisServer = (InfoServer) obj;
                    if (regisServer.getPortUdp() == esteServer.getPortUdp() && regisServer.getPortTcp() == esteServer.getPortTcp() && regisServer.getAddr().equalsIgnoreCase(esteServer.getAddr()))
                        continue;
                    synchronized (listaServers) {
                        listaServers.add(regisServer);
                    }
                    System.out.println("Recebi o servidor " + regisServer.getAddr() + ":" + regisServer.getPortUdp() + ":" + regisServer.getPortTcp() + " Clientes:" + regisServer.getNClientes());
                }else if(obj instanceof String) {
                    receivedMsg = (String) obj;
                    if (!receivedMsg.equalsIgnoreCase(ServerRequest))
                        continue;
                    carga = 0;
                    menosCarga = false;
                    synchronized (listaServers) {
                        for(InfoServer p : listaServers)
                            carga += p.getNClientes();

                        for(InfoServer p : listaServers) {
                            if (p.getNClientes() / (carga + 0.0) < 0.5)
                                menosCarga = true;
                        }
                        if (esteServer.getNClientes() / (carga + 0.0) < 0.5)
                            menosCarga = false;

                        if(menosCarga){
                            Collections.sort(listaServers);
                            msgEnviar = new MsgServer(false, listaServers);
                        }else{
                            for(InfoServer p : listaServers){
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
            System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t"+e);
        } catch (IOException e) {
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t"+e);
        } catch (ClassNotFoundException e) {
            System.out.println("Mensagem recebida de tipo inesperado! " + e);
        }
    }
    public void terminate(){ running = false; }
}