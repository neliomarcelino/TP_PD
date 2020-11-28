package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;

public class ThreadTcp extends Thread {
    public static final int MAX_SIZE = 5120;
    private Socket socketClient;
    private InetAddress group;
    private int portMulti;
    protected boolean running;
    private InfoServer esteServer;

    ThreadTcp(Socket socketClient, InetAddress group, int portMulti, InfoServer esteServer) {
        this.socketClient = socketClient;
        this.portMulti = portMulti;
        this.group = group;
        this.esteServer = esteServer;
        running = true;
    }

    @Override
    public void run() {
        ObjectInputStream in;
        ObjectOutputStream out, tcpOut;
        Object obj;
        ByteArrayOutputStream bOut;
        Msg mensagem;
        DatagramPacket packet = null;
        DatagramSocket socketUdp = null;
		ThreadDownload t = null;
        try {
            while(running) {
                in = new ObjectInputStream(socketClient.getInputStream());
                obj = in.readObject();

                if (obj instanceof Msg) {
                    mensagem = (Msg) obj;
					
					// Tratamento de ficheiros
                    if(mensagem.getConteudo().contains("/fich")){
                        String[] splitStr = mensagem.getConteudo().trim().split("\\s+");
                        (t = new ThreadDownload(socketClient.getInetAddress().getHostAddress(), Integer.parseInt(splitStr[2]), splitStr[1])).start();
                        mensagem = new Msg(mensagem.getUsername(), splitStr[0]+" "+splitStr[1] + " "+ esteServer);
						t.join();
                    }

                    socketUdp = new DatagramSocket();
                    bOut = new ByteArrayOutputStream();
                    out = new ObjectOutputStream(bOut);

                    out.writeUnshared(mensagem);
                    out.flush();

                    packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
                    socketUdp.send(packet);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void terminate(){ running = false; }
}