package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class ThreadTCP extends Thread {
    public static final int MAX_SIZE = 5120;
    private Socket socketClient;
    private InetAddress group;
    private int portMulti;
    protected boolean running;
    private InfoServer esteServer;
	private List<Socket> listaDeClientes;
	private Statement stmt;

    ThreadTCP(Socket socketClient, InetAddress group, int portMulti, InfoServer esteServer, List<Socket> listaDeClientes, Statement stmt) {
        this.socketClient = socketClient;
        this.portMulti = portMulti;
        this.group = group;
        this.esteServer = esteServer;
        running = true;
        this.listaDeClientes = listaDeClientes;
        this.stmt = stmt;
    }

    @Override
    public void run() {
		ServerSocket socket;
        ObjectInputStream in;
        ObjectOutputStream out, tcpOut;
        Object obj;
        ByteArrayOutputStream bOut;
        Msg mensagem;
        String cli_req;
        DatagramPacket packet = null;
        DatagramSocket socketUdp = null;
		ThreadDownload t = null;
		File f;
        try {
            while(running) {
                System.out.println("ThreadTCP BIMP");
                out = new ObjectOutputStream(socketClient.getOutputStream());
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
                    }
                    else if(mensagem.getConteudo().contains("/get_fich")){
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
                } else if(obj instanceof String) {
                    cli_req = (String)obj;
                    if (cli_req.contains("CHANGE CHANNEL")) {
                        System.out.println("Changing channel");
                        String[] splitStr = cli_req.trim().split(":");
        
                        if (splitStr.length != 3) {
                            out.writeObject("NOT OK");
                        } else {
                            String nome = splitStr[1];
                            String password = splitStr[2];
                            ResultSet rs = stmt.executeQuery("SELECT nome, password FROM canais;");
                            boolean conf = false;
                            while (rs.next()) {
                                if (rs.getString("NOME").equalsIgnoreCase(nome)) {
                                    if (rs.getString("PASSWORD").equalsIgnoreCase(password)) {
                                        conf = true;
                                    }
                                    break;
                                }
                            }
                            if (conf) {
                                out.writeObject("OK");
                            } else {
                                out.writeObject("INVALID PASSWORD");
                            }
                        }
                    }
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void terminate(){ running = false; }
}