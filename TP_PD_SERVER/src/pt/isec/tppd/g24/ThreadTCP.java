package pt.isec.tppd.g24;

import pt.isec.tppd.g24.ui.terminal.SocketUser;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class ThreadTCP extends Thread {
   public static final int MAX_SIZE = 5120;
   private SocketUser socketClient;
   private InetAddress group;
   private int portMulti;
   protected boolean running;
   private InfoServer esteServer;
   private List<SocketUser> listaDeClientes;
   private Statement stmt;
   
   ThreadTCP(SocketUser socketClient, InetAddress group, int portMulti, InfoServer esteServer, List<SocketUser> listaDeClientes, Statement stmt) {
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
      Socket socket = socketClient.getSocket();
      DatagramSocket datagramSocket;
      ObjectInputStream in;
      ObjectOutputStream out, udpOut;
      Object obj;
      ByteArrayOutputStream bOut;
      Msg mensagem;
      String cli_req, fileName;
      DatagramPacket packet = null;
      DatagramSocket socketUdp = null;
      ThreadDownload t = null;
      String comando = "";
      File f;
      int i;
      try {
         while (running) {
            in = new ObjectInputStream(socket.getInputStream());
            obj = in.readObject();
            
            if (obj instanceof Msg) {
               mensagem = (Msg) obj;
               
               // Tratamento de ficheiros
               if (mensagem.getConteudo().contains("/fich")) {
                  String[] splitStr = mensagem.getConteudo().trim().split("\\s+");
                  String[] splitFilename = splitStr[1].trim().split("\\.");
                  fileName = splitStr[1];
                  f = new File(System.getProperty("user.dir") + File.separator + mensagem.getdestinatario() + File.separator + fileName);
                  i = 1;
                  while (f.isFile()) {
                     fileName = splitFilename[0] + "(" + i + ")";
                     for (int j = 1; j < splitFilename.length; j++) {
                        fileName += splitFilename[j];
                     }
                     f = new File(System.getProperty("user.dir") + File.separator + mensagem.getdestinatario() + File.separator + fileName);
                     i++;
                  }
                  (t = new ThreadDownload(socket.getInetAddress().getHostAddress(), Integer.parseInt(splitStr[2]), fileName, mensagem.getdestinatario())).start();
                  mensagem = new Msg(mensagem.getUsername(), splitStr[0] + " " + fileName + " " + esteServer, mensagem.getdestinatario());
                  t.join();
               } else if (mensagem.getConteudo().contains("/get_fich")) {
                  String[] splitStr = mensagem.getConteudo().trim().split("\\s+");
                  f = new File(System.getProperty("user.dir") + File.separator + mensagem.getdestinatario() + File.separator + splitStr[1]);
                  if (! f.isFile()) {
                     System.out.println("Ficheiro nao esta na directoria:" + System.getProperty("user.dir"));
                     out = new ObjectOutputStream(socket.getOutputStream());
                     out.writeUnshared("/get_fich Erro");
                     out.flush();
                     continue;
                  }
                  datagramSocket = new DatagramSocket();
                  out = new ObjectOutputStream(socket.getOutputStream());
                  out.writeUnshared(mensagem.getConteudo() + " " + datagramSocket.getLocalPort());
                  out.flush();
                  (new ThreadUpload(datagramSocket, splitStr[1], mensagem.getdestinatario())).start();
                  continue;
                  
               } else if (mensagem.getConteudo().contains("GET CANAL")) {
                  String canal = "NOT OK";
                  out = new ObjectOutputStream(socket.getOutputStream());
                  socketClient.setUsername(mensagem.getUsername());
                  
                  ResultSet rs = stmt.executeQuery("SELECT username, canal FROM utilizadores;");
                  while (rs.next()) {
                     if (rs.getString("username").equalsIgnoreCase(mensagem.getUsername())) {
                        canal = rs.getString("CANAL");
                        break;
                     }
                  }
                  out.writeUnshared("GET CANAL:" + canal);
                  out.flush();
                  continue;
                  
               } else if(mensagem.getConteudo().contains("PRIVATE MESSAGE")) {
                  String[] splitStr = mensagem.getConteudo().trim().split(":");
                  StringBuilder pm = new StringBuilder();
                  pm.append("PRIVATE MESSAGE").append(":");
                  if(splitStr.length < 3){
					  pm.append("NOT OK");
					  out = new ObjectOutputStream(socket.getOutputStream());
					  out.writeUnshared(pm.toString());
					  out.flush();
					  continue;
                  } else {
                     String destinatario = splitStr[1];
                     ResultSet rs = stmt.executeQuery("SELECT username FROM utilizadores;");
                     boolean conf = false;
                     while (rs.next()) {
                        if (rs.getString("username").equalsIgnoreCase(destinatario)) {
                           conf = true;
                           break;
                        }
                     }
                     if(!conf) {
                        pm.append("USER UNKNOWN");
						out = new ObjectOutputStream(socket.getOutputStream());
						out.writeUnshared(pm.toString());
						out.flush();
						continue;
                     } else {
						mensagem = new Msg(mensagem.getUsername(), "PRIVATE MESSAGE:" + splitStr[2], destinatario);
                     }
                  }
               } else if (mensagem.getConteudo().contains("CHANGE CHANNEL")) {
                  String[] splitStr = mensagem.getConteudo().trim().split(":");
                  StringBuilder str = new StringBuilder();
                  str.append("CHANGE CHANNEL").append(":");
                  
                  out = new ObjectOutputStream(socket.getOutputStream());
                  if (splitStr.length != 4) {
                     str.append("NOT OK");
                  } else {
                     String nome = splitStr[1];
                     String password = splitStr[2];
                     String user = splitStr[3];
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
                        comando = "UPDATE UTILIZADORES SET canal='" + nome + "' WHERE UPPER(username)=UPPER('" + user + "');";
                        stmt.executeUpdate(comando);
                        stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
                        socketUdp = new DatagramSocket();
                        bOut = new ByteArrayOutputStream();
                        udpOut = new ObjectOutputStream(bOut);
                        
                        udpOut.writeUnshared("CHANGE CHANNEL:" + nome + ":" + user + ":" + esteServer);
                        udpOut.flush();
                        
                        packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
                        socketUdp.send(packet);
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
                  
                  out = new ObjectOutputStream(socket.getOutputStream());
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
                        if (stmt.executeUpdate("INSERT INTO canais (NOME, DESCRICAO, PASSWORD, ADMIN) VALUES ('" + nome + "', '" + descricao + "', '" + password + "', '" + admin + "');") >= 1) {
                           
                           rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM canais;");
                           Timestamp trata = null;
                           if (rs.next()) {
                              trata = rs.getTimestamp("timestamp");
                           }
                           
                           comando = "INSERT INTO canais (NOME, DESCRICAO, PASSWORD, ADMIN, TIMESTAMP) VALUES ('" + nome + "', '" + descricao + "', '" + password + "', '" + admin + "', '" + trata + "');";
                           stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
                           socketUdp = new DatagramSocket();
                           bOut = new ByteArrayOutputStream();
                           udpOut = new ObjectOutputStream(bOut);
                           
                           udpOut.writeUnshared("NEW CANAL:" + nome + ":" + descricao + ":" + password + ":" + admin + ":" + esteServer);
                           udpOut.flush();
                           
                           packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
                           socketUdp.send(packet);
                           
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
                  out = new ObjectOutputStream(socket.getOutputStream());
                  
                  if (splitStr.length == 5) {
                     String nome = splitStr[1];
                     String descricao = splitStr[2];
                     String password = splitStr[3];
                     String admin = splitStr[4];
                     
                     ResultSet rs = stmt.executeQuery("SELECT username FROM UTILIZADORES;");
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
                           comando = "UPDATE canais " +
                                   "SET descricao = '" + descricao + "', " +
                                   "password = '" + password + "', " +
                                   "admin = '" + admin + "' " +
                                   "WHERE upper(nome) = upper('" + nome + "');";
                           stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
                           socketUdp = new DatagramSocket();
                           bOut = new ByteArrayOutputStream();
                           udpOut = new ObjectOutputStream(bOut);
                           
                           udpOut.writeUnshared("EDIT CANAL:" + descricao + ":" + password + ":" + admin + ":" + nome + ":" + esteServer);
                           udpOut.flush();
                           
                           packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
                           socketUdp.send(packet);
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
                  out = new ObjectOutputStream(socket.getOutputStream());
                  
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
                     if (conf_admin && conf_name) {
                        if (stmt.executeUpdate("DELETE FROM canais WHERE UPPER(NOME) = UPPER('" + nome + "');") >= 1) {
                           comando = "DELETE FROM canais WHERE UPPER(NOME) = UPPER('" + nome + "');";
                           stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
                           socketUdp = new DatagramSocket();
                           bOut = new ByteArrayOutputStream();
                           udpOut = new ObjectOutputStream(bOut);
                           
                           udpOut.writeUnshared("DELETE CANAL:" + nome + ":" + user + ":" + esteServer);
                           udpOut.flush();
                           
                           packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
                           socketUdp.send(packet);
                           str.append("OK").append(":").append(nome);
                           System.out.println("User '" + user + "' eliminou canal '" + nome + "'");
                        } else {
                           str.append("NOT OK");
                        }
                     }
                     if (! conf_admin) {
                        str.append("NOT ADMIN").append(":").append(user);
                     }
                     if (! conf_name) {
                        str.append("SERVER UNKNOWN").append(":").append(nome);
                     }
                  }
                  out.writeUnshared(str.toString());
                  out.flush();
                  continue;
                  
               } else if (mensagem.getConteudo().contains("LIST")) {
                  String[] splitStr = mensagem.getConteudo().trim().split(":");
                  String listWhat = splitStr[1];
                  String fields = splitStr[2];
                  boolean conf = false;
                  ResultSet rs;
                  
                  out = new ObjectOutputStream(socket.getOutputStream());
                  
                  if (listWhat.equalsIgnoreCase("channels")) {
                     HashMap<String, Integer> num_users = new HashMap<>();
                     HashMap<String, Integer> num_msg = new HashMap<>();
                     HashMap<String, Integer> num_fich = new HashMap<>();
                     boolean requires_nome = false;
                     boolean requires_descricao = false;
                     boolean requires_admin = false;
                     boolean requires_num_utilizadores = false;
                     boolean requires_num_mensagens = false;
                     boolean requires_num_ficheiros = false;
                     StringBuilder channels_fields = new StringBuilder();
                     StringBuilder channels_values = new StringBuilder();
                     channels_fields.append("LIST").append(":").append("CHANNELS").append(":");
                     
                     if (fields.equalsIgnoreCase("DEFAULT")) {
                        rs = stmt.executeQuery("SELECT DISTINCT nome, descricao, admin FROM canais;");
                        channels_fields.append("nome,descricao,admin:");
                        while (rs.next()) {
                           conf = true;
                           channels_values.append(rs.getString("NOME")).append(",");
                           channels_values.append(rs.getString("DESCRICAO")).append(",");
                           channels_values.append(rs.getString("ADMIN")).append(",");
                        }
                     }
                     // Custom search
                     else if (fields.split(",").length >= 1) {
                        StringBuilder sql = new StringBuilder();
                        sql.append("SELECT ").append("nome, ");
                        if (fields.contains("nome")) {
                           channels_fields.append("nome,");
                           requires_nome = true;
                        }
                        if (fields.contains("descricao")) {
                           sql.append("descricao, ");
                           channels_fields.append("descricao,");
                           requires_descricao = true;
                        }
                        if (fields.contains("admin")) {
                           sql.append("admin, ");
                           channels_fields.append("admin,");
                           requires_admin = true;
                        }
                        
                        rs = stmt.executeQuery("SELECT DISTINCT nome FROM canais");
                        while (rs.next()) {
                           String n = rs.getString("nome");
                           num_fich.put(n, 0);
                           num_msg.put(n, 0);
                           num_users.put(n, 0);
                        }
                        
                        if (fields.contains("num_utilizadores")) {
                           rs = stmt.executeQuery("SELECT count(username) n_users, canal FROM utilizadores, canais WHERE UPPER(canal) = UPPER(canais.nome) GROUP BY canal;");
                           while (rs.next()) {
                              requires_num_utilizadores = true;
                              num_users.replace(rs.getString("canal"), rs.getInt("n_users"));
                           }
                           if (requires_num_utilizadores) {
                              channels_fields.append("num_utilizadores,");
                           }
                        }
                        if (fields.contains("num_mensagens")) {
                           rs = stmt.executeQuery("SELECT count(destinatario) n_mensagens, destinatario FROM mensagens, canais WHERE UPPER(destinatario) = UPPER(canais.nome) GROUP BY destinatario;");
                           while (rs.next()) {
                              requires_num_mensagens = true;
                              num_msg.replace(rs.getString("destinatario"), rs.getInt("n_mensagens"));
                           }
                           if (requires_num_mensagens) {
                              channels_fields.append("num_mensagens,");
                           }
                        }
                        if (fields.contains("num_ficheiros")) {
                           rs = stmt.executeQuery("SELECT DISTINCT count(destinatario) n_ficheiros, destinatario FROM mensagens, canais WHERE UPPER(destinatario) = UPPER(canais.nome) AND UPPER(conteudo) like '/fich %' GROUP BY destinatario;");
                           while (rs.next()) {
                              requires_num_ficheiros = true;
                              num_fich.replace(rs.getString("destinatario"), rs.getInt("n_ficheiros"));
                           }
                           if (requires_num_ficheiros) {
                              channels_fields.append("num_ficheiros,");
                           }
                        }
                        channels_fields.setLength(channels_fields.length() - 1);
                        sql.setLength(sql.length() - 2);
                        sql.append(" FROM canais;");
                        channels_values = new StringBuilder();
                        String channel_name;
                        
                        rs = stmt.executeQuery(sql.toString());
                        while (rs.next()) {
                           conf = true;
                           channel_name = rs.getString("nome");
                           if (requires_nome)
                              channels_values.append(channel_name).append(",");
                           
                           if (requires_descricao)
                              channels_values.append(rs.getString("descricao")).append(",");
                           
                           if (requires_admin)
                              channels_values.append(rs.getString("admin")).append(",");
                           
                           if (requires_num_utilizadores && num_users.containsKey(channel_name))
                              channels_values.append(num_users.get(channel_name)).append(",");
                           
                           if (requires_num_mensagens && num_msg.containsKey(channel_name))
                              channels_values.append(num_msg.get(channel_name)).append(",");
                           
                           if (requires_num_ficheiros && num_fich.containsKey(channel_name))
                              channels_values.append(num_fich.get(channel_name)).append(",");
                        }
                        
                     } else {
                        channels_fields = new StringBuilder();
                        channels_fields.append("LIST:NOT OK");
                     }
                     if (! conf) {
                        channels_fields = new StringBuilder();
                        channels_fields.append("LIST:CHANNELS:NO CHANNELS");
                     }
                     out.writeUnshared(channels_fields.toString());
                     out.flush();
                     channels_values.append("FINISH");
                     
                     for (String str : channels_values.toString().trim().split(",")) {
                        out.writeUnshared(str);
                        out.flush();
                     }
                     
                  } else if (listWhat.equalsIgnoreCase("users")) {
                     StringBuilder users = new StringBuilder();
                     boolean requires_nome = false;
                     boolean requires_username = false;
                     boolean requires_canal = false;
                     users.append("LIST").append(":").append("USERS").append(":");
                     
                     if (fields.equalsIgnoreCase("DEFAULT")) {
                        rs = stmt.executeQuery("SELECT DISTINCT nome, username FROM utilizadores;");
                        users.append("nome,username:");
                        while (rs.next()) {
                           conf = true;
                           users.append(rs.getString("NOME")).append(",");
                           users.append(rs.getString("USERNAME")).append(",");
                        }
                     }
                     // Custom search
                     else if (fields.split(",").length >= 1) {
                        StringBuilder sql = new StringBuilder();
                        sql.append("SELECT ").append("nome, ");
                        if (fields.contains("nome")) {
                           users.append("nome,");
                           requires_nome = true;
                        }
                        if (fields.contains("username")) {
                           sql.append("username, ");
                           users.append("username,");
                           requires_username = true;
                        }
                        if (fields.contains("canal")) {
                           sql.append("canal, ");
                           users.append("canal,");
                           requires_canal = true;
                        }
                        sql.setLength(sql.length() - 2);
                        users.setLength(users.length() - 1);
                        sql.append(" FROM utilizadores;");
                        users.append(":");
                        String user_name;
                        
                        rs = stmt.executeQuery(sql.toString());
                        while (rs.next()) {
                           conf = true;
                           user_name = rs.getString("nome");
                           if (requires_nome)
                              users.append(user_name).append(",");
                           if (requires_username)
                              users.append(rs.getString("username")).append(",");
                           if (requires_canal)
                              users.append(rs.getString("canal")).append(",");
                        }
                        users.setLength(users.length() - 1);
                        
                     } else {
                        users = new StringBuilder();
                        users.append("LIST:NOT OK");
                     }
                     if (! conf) {
                        users = new StringBuilder();
                        users.append("LIST:CHANNELS:NO CHANNELS");
                     }
                     out.writeUnshared(users.toString());
                     
                  } else if (listWhat.equalsIgnoreCase("messages")) {
                     StringBuilder messages = new StringBuilder();
                     boolean requires_n_mensagens = false;
                     boolean requires_remetente = false;
                     boolean requires_destinatario = false;
                     String n_mensagens = "10";   // DEFAULT
                     
                     messages.append("LIST").append(":").append("MESSAGES").append(":");
                     messages.append("remetente,conteudo,destinatario:");
                     if (fields.equalsIgnoreCase("DEFAULT")) {
                        rs = stmt.executeQuery("SELECT DISTINCT remetente, conteudo, destinatario FROM mensagens WHERE UPPER(remetente) = UPPER('" + mensagem.getUsername() + "') OR UPPER(destinatario) = UPPER('" + mensagem.getUsername() + "') ORDER BY timestamp DESC LIMIT " + n_mensagens + ";");
                        
                        while (rs.next()) {
                           conf = true;
                           messages.append(rs.getString("remetente")).append(",");
                           messages.append(rs.getString("conteudo")).append(",");
                           messages.append(rs.getString("destinatario")).append(",");
                        }
                        messages.setLength(messages.length() - 1);
                     }
                     // Custom search
                     else if (fields.split(",").length >= 1) {
                        String[] field = fields.split(",");
                        StringBuilder sql = new StringBuilder();
                        boolean appliedWhere = false;
                        sql.append("SELECT DISTINCT remetente, conteudo, destinatario FROM mensagens ");
                        
                        // Aplica os filtros
                        for (String str : field) {
                           if (str.contains("n_mensagens=")) {
                              requires_n_mensagens = true;
                              n_mensagens = (str.split("="))[1];
                              
                           } else if (str.contains("remetente=")) {
                              if (! appliedWhere) {
                                 sql.append("WHERE ");
                                 appliedWhere = true;
                              }
                              requires_remetente = true;
                              String remetente = (str.split("="))[1];
                              sql.append("UPPER(remetente)=UPPER('").append(remetente).append("')").append("AND ");
                              
                           } else if (str.contains("destinatario")) {
                              if (! appliedWhere) {
                                 sql.append("WHERE ");
                                 appliedWhere = true;
                              }
                              requires_destinatario = true;
                              String destinatario = (str.split("="))[1];
                              sql.append("UPPER(destinatario)=UPPER('").append(destinatario).append("')").append("AND ");
                           }
                        }
                        if (requires_destinatario || requires_remetente) {
                           sql.setLength(sql.length() - ("AND ").length());
                        }
                        sql.append("ORDER BY timestamp DESC ");
                        if (requires_n_mensagens) {
                           sql.append("LIMIT ").append(n_mensagens);
                        }
                        sql.append(";");
                        rs = stmt.executeQuery(sql.toString());
                        while (rs.next()) {
                           conf = true;
                           messages.append(rs.getString("remetente")).append(",");
                           messages.append(rs.getString("conteudo")).append(",");
                           messages.append(rs.getString("destinatario")).append(",");
                        }
                        messages.setLength(messages.length() - 1);
                        
                     } else {
                        messages = new StringBuilder();
                        messages.append("LIST:NOT OK");
                     }
                     if (! conf) {
                        messages = new StringBuilder();
                        messages.append("LIST:MESSAGES:NO MESSAGES FOUND");
                     }
                     out.writeUnshared(messages.toString());
                  }
                  out.flush();
                  continue;
               }
               
               socketUdp = new DatagramSocket();
               bOut = new ByteArrayOutputStream();
               udpOut = new ObjectOutputStream(bOut);
               
               udpOut.writeUnshared(mensagem);
               udpOut.flush();
               
               packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
               socketUdp.send(packet);
            }
         }
      } catch (SocketException | EOFException e) {
         synchronized (listaDeClientes) {
            listaDeClientes.remove(socketClient);
         }
         esteServer.decNClientes();
         try {
            Main.enviaEsteServer(esteServer, group, portMulti);
         } catch (IOException ioException) {
            ioException.printStackTrace();
         }
         System.out.println("Menos um cliente ligado!");
      } catch (IOException e) {
         e.printStackTrace();
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      } catch (InterruptedException e) {
         e.printStackTrace();
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }
   
   public void terminate() {
      running = false;
   }
}