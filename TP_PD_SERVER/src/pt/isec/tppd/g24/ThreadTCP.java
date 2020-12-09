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
        String cli_req, fileName;
        DatagramPacket packet = null;
        DatagramSocket socketUdp = null;
		ThreadDownload t = null;
		File f;
		int i;
        try {
            while(running) {
                in = new ObjectInputStream(socketClient.getInputStream());
                obj = in.readObject();

                if (obj instanceof Msg) {
                    mensagem = (Msg) obj;
					
					// Tratamento de ficheiros
                    if(mensagem.getConteudo().contains("/fich")){
                        String[] splitStr = mensagem.getConteudo().trim().split("\\s+");
						String[] splitFilename = splitStr[1].trim().split("\\.");
						fileName = splitStr[1];
						f = new File(System.getProperty("user.dir")+ File.separator + mensagem.getCanal() + File.separator + fileName);
						i = 1;
						while(f.isFile()){
							fileName = splitFilename[0] + "(" + i + ")";
							for(int j = 1; j < splitFilename.length; j++){
								fileName += splitFilename[j];
							}
							f = new File(System.getProperty("user.dir")+ File.separator + mensagem.getCanal() + File.separator + fileName);
							i++;
						}
                        (t = new ThreadDownload(socketClient.getInetAddress().getHostAddress(), Integer.parseInt(splitStr[2]), fileName, mensagem.getCanal())).start();
                        mensagem = new Msg(mensagem.getUsername(), splitStr[0]+" "+fileName + " "+ esteServer, mensagem.getCanal());
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
                        
                    } else if (mensagem.getConteudo().contains("CHANGE CHANNEL")) {
                        String[] splitStr = mensagem.getConteudo().trim().split(":");
                        StringBuilder str = new StringBuilder();
                        str.append("CHANGE CHANNEL").append(":");
                        
                        out = new ObjectOutputStream(socketClient.getOutputStream());
                        if (splitStr.length != 3) {
                            str.append("NOT OK");
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
                                str.append("OK").append(":").append(nome);
                            } else {
                                str.append("INVALID PASSWORD");
                            }
                        }
                        out.writeUnshared(str.toString());
                        out.flush();
                        continue;
                    } else if (mensagem.getConteudo().contains("CREATE CHANNEL")) {
                        String[] splitStr = mensagem.getConteudo().trim().split(":");
                        StringBuilder str = new StringBuilder();
                        str.append("CREATE CHANNEL").append(":");
    
                        out = new ObjectOutputStream(socketClient.getOutputStream());
                        if (splitStr.length != 4) {
                            str.append("NOT OK");
                        } else {
                            String nome = splitStr[1];
                            String descricao = splitStr[2];
                            String password = splitStr[3];
                            String admin = mensagem.getUsername();
                            
                            ResultSet rs = stmt.executeQuery("SELECT nome FROM canais;");
                            boolean conf = false;
                            while (rs.next()) {
                                if (rs.getString("NOME").equalsIgnoreCase(nome)) {
                                    conf = true;
                                    break;
                                }
                            }
                            if (conf) {
                                str.append("NAME IN USE").append(":").append(nome);
                            } else {
                                if (stmt.executeUpdate("INSERT INTO canais VALUES ('" + nome + "', '" + descricao + "', '" + password + "', '" + admin + "');") >= 1) {
                                    str.append("OK").append(":").append(nome);
                                    System.out.println("Utilizador '" + admin + "' criou o canal '" + nome + "' com sucesso!");
                                } else {
                                    str.append("NOT OK");
                                }
                            }
                        }
                        out.writeUnshared(str.toString());
                        out.flush();
                        continue;
                        
                    } else if (mensagem.getConteudo().contains("EDIT CHANNEL")) {
                        String[] splitStr = mensagem.getConteudo().trim().split(":");
                        StringBuilder str = new StringBuilder();
                        str.append("EDIT CHANNEL").append(":");
                        out = new ObjectOutputStream(socketClient.getOutputStream());
    
                        if (splitStr.length == 5) {
                            String nome = splitStr[1];
                            String descricao = splitStr[2];
                            String password = splitStr[3];
                            String admin = splitStr[4];
        
                            ResultSet rs = stmt.executeQuery("SELECT username FROM users;");
                            boolean conf = false;
                            while (rs.next()) {
                                if (rs.getString("USERNAME").equalsIgnoreCase(admin)) {
                                    conf = true;
                                    break;
                                }
                            }
        
                            if (conf) {
                                if (stmt.executeUpdate("UPDATE canais " +
                                                               "SET descricao = '" + descricao + "', " +
                                                               "password = '" + password + "', " +
                                                               "admin = '" + admin + "' " +
                                                               "WHERE upper(nome) = upper('" + nome + "');") >= 1) {
                                    str.append("OK").append(":").append(nome);
                                }
                            } else {
                                str.append("ADMIN NOT EXISTS").append(":").append(admin);
                            }
                        } else {
                            str.append("NOT OK");
                        }
                        out.writeUnshared(str.toString());
                        out.flush();
                        continue;
                        
                    } else if (mensagem.getConteudo().contains("DELETE CHANNEL")) {
                        String[] splitStr = mensagem.getConteudo().trim().split(":");
                        StringBuilder str = new StringBuilder();
                        str.append("DELETE CHANNEL").append(":");
                        out = new ObjectOutputStream(socketClient.getOutputStream());
    
                        if (splitStr.length != 2) {
                            str.append("NOT OK");
                        } else {
                            String nome = splitStr[1];
                            String user = mensagem.getUsername();
    
                            // Confirma se e o admin que esta a apagar o canal
                            ResultSet rs = stmt.executeQuery("SELECT nome, admin FROM canais;");
                            boolean conf_admin = false;
                            boolean conf_name = false;
                            while (rs.next()) {
                                if (rs.getString("NOME").equalsIgnoreCase(nome)) {
                                    conf_name = true;
                                    if (rs.getString("ADMIN").equalsIgnoreCase(user)) {
                                        conf_admin = true;
                                    }
                                    break;
                                }
                            }
                            if(conf_admin && conf_name) {
                                if (stmt.executeUpdate("DELETE FROM canais WHERE UPPER(NOME) = UPPER('" + nome + "');") >= 1) {
                                    str.append("OK").append(":").append(nome);
                                    System.out.println("User '" + user + "' eliminou canal '" + nome + "'");
                                } else {
                                    str.append("NOT OK");
                                }
                            }
                            if (!conf_admin) {
                                str.append("NOT ADMIN").append(":").append(user);
                            }
                            if(!conf_name) {
                                str.append("SERVER UNKNOWN").append(":").append(nome);
                            }
                        }
                        out.writeUnshared(str.toString());
                        out.flush();
                        continue;
    
                    } else if(mensagem.getConteudo().contains("LIST CHANNELS")) {
                        StringBuilder channels = new StringBuilder();
                        channels.append("LIST CHANNELS").append(":");
                        out = new ObjectOutputStream(socketClient.getOutputStream());

                        ResultSet rs = stmt.executeQuery("SELECT nome, admin FROM canais;");
                        boolean conf = false;
                        
                        while (rs.next()) {
                            conf = true;
                            channels.append(rs.getString("NOME"));
                            channels.append(":");
                            channels.append(rs.getString("ADMIN"));
                            channels.append(":");
                        }
                        if (channels.length() > 0) {
                            channels.setLength(channels.length() - 1);
                        }
    
                        if (! conf) {
                            channels.append("NO CHANNELS");
                        }
                        out.writeUnshared(channels.toString());
                        out.flush();
                        continue;
                        
                    } else if (mensagem.getConteudo().contains("LIST USERS")) {
                        StringBuilder users = new StringBuilder();
                        users.append("LIST USERS").append(":");
                        out = new ObjectOutputStream(socketClient.getOutputStream());
                        
                        ResultSet rs = stmt.executeQuery("SELECT username, name FROM users;");
                        boolean conf = false;
                        
                        while (rs.next()) {
                            conf = true;
                            users.append(rs.getString("username"));
                            users.append(":");
                            users.append(rs.getString("name"));
                            users.append(":");
                        }
                        if (users.length() > 0) {
                            users.setLength(users.length() - 1);
                        }
    
                        if (! conf) {
                            users.append("NOT OK");
                        }
                        out.writeUnshared(users.toString());
                        out.flush();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void terminate(){ running = false; }
}