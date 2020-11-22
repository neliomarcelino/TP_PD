package pt.isec.tppd.g24;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ThreadTCP extends Thread {
    private Socket socketClient;
    private InetAddress group;
    private int portMulti;
    protected boolean running;

    ThreadTCP(Socket socketClient, InetAddress group, int portMulti) {
        this.socketClient = socketClient;
        this.portMulti = portMulti;
        this.group = group;
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
                        mensagem = new Msg(mensagem.getUsername(), splitStr[0]+" "+splitStr[1]);
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
        }
    }

    public void terminate(){ running = false; }
}