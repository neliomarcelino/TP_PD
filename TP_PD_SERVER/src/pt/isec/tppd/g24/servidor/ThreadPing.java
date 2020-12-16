package pt.isec.tppd.g24.servidor;
import pt.isec.tppd.g24.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ThreadPing extends Thread {
    public static int MAX_SIZE = 1000000;
    protected boolean running;
    private InetAddress group;
    private List<InfoServer> listaServers;
    private int portMulti;
    private int segundos;
    private DatagramSocket socketUdp;
    private InfoServer esteServer;

    public ThreadPing(int segundos, InetAddress group, int portMulti, List<InfoServer> listaServers, DatagramSocket socketUdp, InfoServer esteServer){
        this.segundos = segundos;
        running = true;
        this.group = group;
        this.portMulti = portMulti;
        this.listaServers = listaServers;
        this.socketUdp = socketUdp;
        this.esteServer = esteServer;
    }

    @Override
    public void run() {
        ByteArrayOutputStream bOut;
        ByteArrayInputStream bIn;
        ObjectOutputStream out;
        String ping = "PING";
        DatagramPacket packet = null;

        try {
            while(running){
                TimeUnit.SECONDS.sleep(segundos);
                bOut = new ByteArrayOutputStream();
                out = new ObjectOutputStream(bOut);
                out.writeUnshared(ping);
                out.flush();

                packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
                synchronized (listaServers){
                    listaServers.clear();
                    listaServers.add(esteServer);
                }
                //System.out.println("Za warudo!");
                System.out.println("A verificar servidores... (lista de servidores limpa)");
                socketUdp.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void terminate(){ running = false; }
}
