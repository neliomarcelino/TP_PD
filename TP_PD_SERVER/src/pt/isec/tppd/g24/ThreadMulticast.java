package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.sql.Statement;
import java.util.List;
import java.sql.*;

public class ThreadMulticast extends Thread {
    public static int MAX_SIZE = 10000;
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

                        if(msg.getConteudo().contains("/fich")) {
                            String[] splitStr = msg.getConteudo().trim().split("\\s+");
                            if(!(esteServer.getAddr().equals(splitStr[2]) && esteServer.getPortUdp() == Integer.parseInt(splitStr[3]) && esteServer.getPortTcp() == Integer.parseInt(splitStr[4]))){
                                socketFich = new DatagramSocket();
                                buff = new ByteArrayOutputStream();
                                out = new ObjectOutputStream(buff);
                                out.writeUnshared("/fich " + splitStr[1] + " " + msg.getCanal());
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
									
                                (t = new ThreadDownload(splitStr[2], filePort, splitStr[1], msg.getCanal())).start();
                            }
							msg = new Msg(msg.getUsername(), splitStr[0] + " " + splitStr[1], msg.getCanal());
                        }

                        System.out.println(msg.getUsername() + ":" + msg.getConteudo());

                        //Guardar msg na Database

                        if(stmt.executeUpdate("INSERT INTO MENSAGENS (remetente, conteudo, destinatario) " + "VALUES ('" + msg.getUsername() + "' ,'" + msg.getConteudo() +"' ,'"+ msg.getCanal()+ "' );")<1){
                            System.out.println("Entry insertion failed");
							continue;
                        }
						
						rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM mensagens;");
						Timestamp trata = null;
						if(rs.next()){
							trata = rs.getTimestamp("timestamp");
						}
						sql = "INSERT INTO MENSAGENS (remetente, conteudo, destinatario, timestamp) " + "VALUES ('" + msg.getUsername() + "' ,'" + msg.getConteudo() +"' ,'"+ msg.getCanal()+"' ,'"+ trata+ "' );";
                        //Enviar aos clientes a msg
						stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + sql + "\");");
						
                        synchronized (listaDeClientes) {
                            if (listaDeClientes.size() != 0) {
                                for (Socket p : listaDeClientes) {
                                    out = new ObjectOutputStream(p.getOutputStream());
                                    out.writeUnshared(msg);
                                    out.flush();
                                }
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
