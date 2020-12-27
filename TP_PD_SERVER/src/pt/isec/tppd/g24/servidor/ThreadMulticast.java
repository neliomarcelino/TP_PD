package pt.isec.tppd.g24.servidor;

import pt.isec.tppd.g24.*;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

public class ThreadMulticast extends Thread {
    public static int MAX_SIZE = 10000;
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    protected MulticastSocket s;
    protected boolean running;
    private List<InfoServer> listaServers;
    private InfoServer esteServer;
    private Statement stmt;
    private List<SocketUser> listaDeClientes;
	private ServidorInterfaceImpl servidorRmi;

    public ThreadMulticast(MulticastSocket s, List<InfoServer> listaServers, InfoServer esteServer, Statement stmt, List<SocketUser> listaDeClientes, ServidorInterfaceImpl servidorRmi){
        this.s = s;
        running = true;
        this.listaServers = listaServers;
        this.esteServer = esteServer;
        this.stmt = stmt;
        this.listaDeClientes = listaDeClientes;
		this.servidorRmi = servidorRmi;
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
        String sql = null, comando = "";
        boolean cria;
        int i = 2;
        DatagramSocket socketFich;
        ThreadDownload t;
		ResultSet rs = null;

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

                        if(msg.getConteudo().startsWith("/fich")) {
                            String[] splitStr = msg.getConteudo().trim().split("\\s+");
                            if(!(esteServer.getAddr().equals(splitStr[2]) && esteServer.getPortUdp() == Integer.parseInt(splitStr[3]) && esteServer.getPortTcp() == Integer.parseInt(splitStr[4]))){
                                socketFich = new DatagramSocket();
                                buff = new ByteArrayOutputStream();
                                out = new ObjectOutputStream(buff);
                                out.writeUnshared("/fich " + splitStr[1] + " " + msg.getdestinatario());
                                out.flush();

                                pkt = new DatagramPacket(buff.toByteArray(), buff.size(), InetAddress.getByName(splitStr[2]), Integer.parseInt(splitStr[3]));
                                socketFich.send(pkt);

                                pkt = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                                socketFich.receive(pkt);
                                in = new ObjectInputStream(new ByteArrayInputStream(pkt.getData(), 0, pkt.getLength()));
								int filePort = (int) in.readObject();
								
								if(filePort == -1){
									System.out.println("Servidor nao tem o ficheiro: " + splitStr[1]);
									continue;
								}
									
                                (t = new ThreadDownload(splitStr[2], filePort, splitStr[1], msg.getdestinatario())).start();
                            }
							msg = new Msg(msg.getUsername(), splitStr[0] + " " + splitStr[1], msg.getdestinatario());
                        }

                        System.out.println(msg.getUsername() + ":" + msg.getConteudo());
						String[] splitStr = msg.getConteudo().trim().split(":");


                        //Guardar msg na Database

                        if(stmt.executeUpdate("INSERT INTO MENSAGENS (remetente, conteudo, destinatario) " + "VALUES ('" + msg.getUsername() + "' ,'" + msg.getConteudo() +"' ,'"+ msg.getdestinatario()+ "' );")<1){
                            System.out.println("Entry insertion failed");
							continue;
                        }
						
						rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM mensagens;");
						Timestamp trata = null;
						if(rs.next()){
							trata = rs.getTimestamp("timestamp");
						}
						sql = "INSERT INTO MENSAGENS (remetente, conteudo, destinatario, timestamp) " + "VALUES ('" + msg.getUsername() + "' ,'" + msg.getConteudo() +"' ,'"+ msg.getdestinatario()+"' ,'"+ trata+ "' );";
                        //Enviar aos clientes a msg
						stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + sql + "\");");
						
						servidorRmi.novaNotificacao(msg);
						
						/*
                        synchronized (listaDeClientes) {
                            if (listaDeClientes.size() != 0) {
                                for (SocketUser p : listaDeClientes) {
									if(splitStr[0].equalsIgnoreCase("PRIVATE MESSAGE") && p.getUsername().equalsIgnoreCase(msg.getdestinatario())){
										out = new ObjectOutputStream(p.getSocket().getOutputStream());
										out.writeUnshared(msg);
										out.flush();
										continue;
									}
									rs = stmt.executeQuery("SELECT canal FROM utilizadores where username = '" + p.getUsername() + "';");
									String canal = "";
									if(rs !=null && rs.next())	
										canal = rs.getString("canal");
									if(msg.getdestinatario().equalsIgnoreCase(canal)){									
										out = new ObjectOutputStream(p.getSocket().getOutputStream());
										out.writeUnshared(msg);
										out.flush();
									}
                                }
                            }
                        }
						*/
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
						if(((String) obj).startsWith("AUTENTICACAO")){
							String[] splitStr = ((String) obj).trim().split(":");
							servidorRmi.novaNotificacao("Utilizador (" + splitStr[1] + ") entrou.");
                        }else
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
                        }else if(((String) obj).contains("REGISTO")){
							String[] splitStr = ((String) obj).trim().split(":");
							String[] splitServer = splitStr[4].trim().split("\\s+");
							if(!(esteServer.getAddr().equals(splitServer[0]) && esteServer.getPortUdp() == Integer.parseInt(splitServer[1]) && esteServer.getPortTcp() == Integer.parseInt(splitServer[2]))){
								stmt.executeUpdate("INSERT INTO UTILIZADORES (USERNAME, NOME, PASSWORD) VALUES ('" + splitStr[1] + "', '" + splitStr[2] + "', '" + splitStr[3] + "');");
								
								rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM UTILIZADORES;");
								Timestamp trata = null;
								if(rs.next()){
									trata = rs.getTimestamp("timestamp");
								}
								
								comando = "INSERT INTO UTILIZADORES (USERNAME, NOME, PASSWORD, TIMESTAMP) VALUES ('" + splitStr[1] + "', '" + splitStr[2] + "', '" + splitStr[3] + "', '" + trata + "');";
								stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
								System.out.println("Registo de utilizador ('" + splitStr[1] + "', '" + splitStr[2] + "', '" + splitStr[3] + "') efetuado com sucesso!");
							}
						}else if(((String) obj).contains("NEW CANAL")){
							String[] splitStr = ((String) obj).trim().split(":");
							String[] splitServer = splitStr[5].trim().split("\\s+");
							if(!(esteServer.getAddr().equals(splitServer[0]) && esteServer.getPortUdp() == Integer.parseInt(splitServer[1]) && esteServer.getPortTcp() == Integer.parseInt(splitServer[2]))){
								stmt.executeUpdate("INSERT INTO canais (NOME, DESCRICAO, PASSWORD, ADMIN) VALUES ('" + splitStr[1] + "', '" + splitStr[2] + "', '" + splitStr[3] + "', '" + splitStr[4] + "');");
								
								rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM canais;");
								Timestamp trata = null;
								if(rs.next()){
									trata = rs.getTimestamp("timestamp");
								}
								comando = "INSERT INTO canais (NOME, DESCRICAO, PASSWORD, ADMIN) VALUES ('" + splitStr[1] + "', '" + splitStr[2] + "', '" + splitStr[3] + "', '" + splitStr[4] + "', '" + trata + "');";
								stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
								System.out.println("Utilizador '" + splitStr[4] + "' criou o canal '" + splitStr[1] + "' com sucesso!");
							}
						}else if(((String) obj).contains("EDIT CANAL")){
							String[] splitStr = ((String) obj).trim().split(":");
							String[] splitServer = splitStr[5].trim().split("\\s+");
							if(!(esteServer.getAddr().equals(splitServer[0]) && esteServer.getPortUdp() == Integer.parseInt(splitServer[1]) && esteServer.getPortTcp() == Integer.parseInt(splitServer[2]))){
								comando = "UPDATE canais " +
                                                       "SET descricao = '" + splitStr[1] + "', " +
                                                       "password = '" + splitStr[2] + "', " +
                                                       "admin = '" + splitStr[3] + "' " +
                                                       "WHERE upper(nome) = upper('" + splitStr[4] + "');";
								stmt.executeUpdate(comando);
								stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
							}
						}else if(((String) obj).contains("DELETE CANAL")){
							String[] splitStr = ((String) obj).trim().split(":");
							String[] splitServer = splitStr[3].trim().split("\\s+");
							if(!(esteServer.getAddr().equals(splitServer[0]) && esteServer.getPortUdp() == Integer.parseInt(splitServer[1]) && esteServer.getPortTcp() == Integer.parseInt(splitServer[2]))){
								comando = "DELETE FROM canais WHERE UPPER(NOME) = UPPER('" + splitStr[1] + "');";
								stmt.executeUpdate(comando);
								stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
								System.out.println("User '" + splitStr[2] + "' eliminou canal '" + splitStr[1] + "'");
							}
						}else if(((String) obj).contains("CHANGE CHANNEL")){
							String[] splitStr = ((String) obj).trim().split(":");
							String[] splitServer = splitStr[3].trim().split("\\s+");
							if(!(esteServer.getAddr().equals(splitServer[0]) && esteServer.getPortUdp() == Integer.parseInt(splitServer[1]) && esteServer.getPortTcp() == Integer.parseInt(splitServer[2]))){
								comando = "UPDATE UTILIZADORES SET canal='" + splitStr[1] + "' WHERE UPPER(username)=UPPER('" + splitStr[2] + "');";
								stmt.executeUpdate(comando);
								stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
							}
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
