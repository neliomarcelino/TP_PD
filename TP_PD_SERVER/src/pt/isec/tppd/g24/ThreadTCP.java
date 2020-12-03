package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.util.*;

public class ThreadTCP extends Thread {
    public static final int MAX_SIZE = 5120;
    private Socket socketClient;
    private InetAddress group;
    private int portMulti;
    protected boolean running;
    private InfoServer esteServer;
	private List<Socket> listaDeClientes;

    ThreadTCP(Socket socketClient, InetAddress group, int portMulti, InfoServer esteServer, List<Socket> listaDeClientes) {
        this.socketClient = socketClient;
        this.portMulti = portMulti;
        this.group = group;
        this.esteServer = esteServer;
        running = true;
        this.listaDeClientes = listaDeClientes;
    }

    @Override
    public void run() {
		ServerSocket socket;
        ObjectInputStream in;
        ObjectOutputStream out, tcpOut;
        Object obj;
        ByteArrayOutputStream bOut;
        Msg mensagem;
        DatagramPacket packet = null;
        DatagramSocket socketUdp = null;
		ThreadDownload t = null;
		File f;
        try {
            while(running) {
                in = new ObjectInputStream(socketClient.getInputStream());
                obj = in.readObject();

                if (obj instanceof Msg) {
                    mensagem = (Msg) obj;
					
					// Tratamento de ficheiros
                    if(mensagem.getConteudo().contains("/fich")){
                        String[] splitStr = mensagem.getConteudo().trim().split("\\s+");
                        (t = new ThreadDownload(socketClient.getInetAddress().getHostAddress(), Integer.parseInt(splitStr[2]), splitStr[1], mensagem.getCanal())).start();
                        mensagem = new Msg(mensagem.getUsername(), splitStr[0]+" "+splitStr[1] + " "+ esteServer, mensagem.getCanal());
						t.join();
                    }else if(mensagem.getConteudo().contains("/get_fich")){
                        String[] splitStr = mensagem.getConteudo().trim().split("\\s+");
                        f = new File(System.getProperty("user.dir")+ File.separator + mensagem.getCanal() + File.separator + splitStr[1]);
                        if(!f.isFile()){
                            System.out.println("Ficheiro nao esta na directoria:" + System.getProperty("user.dir"));
                            out = new ObjectOutputStream(socketClient.getOutputStream());
                            out.writeUnshared("/get_fich Erro");
                            out.flush();
                            continue;
                        }
                        socket = new ServerSocket(0);
                        out = new ObjectOutputStream(socketClient.getOutputStream());
                        out.writeUnshared(mensagem.getConteudo() + " " + socket.getLocalPort());
                        out.flush();
                        (new ThreadUpload(socket, splitStr[1], mensagem.getCanal())).start();
                        continue;
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
        } catch (SocketException | EOFException e) {
            synchronized (listaDeClientes){
                listaDeClientes.remove(socketClient);
            }
            esteServer.decNClientes();
            try {
                Main.enviaEsteServer(esteServer, group, portMulti);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            System.out.println("Menos um cliente ligado!");
        }catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void terminate(){ running = false; }
}