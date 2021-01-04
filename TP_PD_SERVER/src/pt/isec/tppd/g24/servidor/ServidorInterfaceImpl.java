package pt.isec.tppd.g24.servidor;

import pt.isec.tppd.g24.*;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class ServidorInterfaceImpl  extends UnicastRemoteObject implements ServidorInterface{
	private Statement stmt;
	private InetAddress group;
	private int portMulti;
	private InfoServer esteServer;
	//private List<SocketUser> listaDeClientes;
	private List<UserRmi> userList; 

    //public ServidorInterfaceImpl(InfoServer esteServer,Statement stmt, InetAddress group, int portMulti, List<SocketUser> listaDeClientes) throws RemoteException{
	public ServidorInterfaceImpl(InfoServer esteServer,Statement stmt, InetAddress group, int portMulti) throws RemoteException{
		this.esteServer = esteServer;
		this.stmt = stmt;
		this.group = group;
		this.portMulti = portMulti;
		//this.listaDeClientes = listaDeClientes;
		this.userList = new ArrayList<UserRmi>();
	}
	
	public synchronized void addListener (UserInterface listener, String username) throws RemoteException, IOException{
		System.out.println ("Adding listener -" + listener);
		userList.add(new UserRmi(listener, username));
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bOut);
		out.writeUnshared("AUTENTICACAO:" + username);
		out.flush();
		DatagramPacket packetMulti = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
		DatagramSocket socket = new DatagramSocket();
		socket.send(packetMulti);
	}
	
	public synchronized void removeListener (UserInterface listener) throws RemoteException{
		//System.out.println ("Removing listener -" + listener);
		for(UserRmi u : userList){
			if(u.getInterface().equals(listener)){
				userList.remove(u);
				System.out.println ("Removing listener -" + listener);
				break;
			}
		}
	}
	
	@Override
	public String regista(String name, String username, String password, String foto) throws RemoteException{
		try {
		//Verifica se algum parametro é null
		if(name == null || username == null || password == null|| foto == null)
			return "NOT OK";
		
		//Verifica se o username ja esta em uso
        ResultSet rs = stmt.executeQuery("SELECT USERNAME FROM UTILIZADORES;");
                     
        while (rs.next()) {
            if (rs.getString("USERNAME").equalsIgnoreCase(username)) {
				System.out.println("Registo efetuado sem sucesso! Username '" + username + "' ja em uso");
				return "USERNAME IN USE";
            }
        }
		
		//Adiciona na database e manda por multicast udp a informaçao
		if (stmt.executeUpdate("INSERT INTO UTILIZADORES (USERNAME, NOME, PASSWORD, IMAGEM, CANAL) VALUES ('" + username + "', '" + name + "', '" + password + "', '" + foto + "', '" + "NO_CHANNEL" +"');")>= 1){
			rs = stmt.executeQuery("SELECT MAX(timestamp) as timestamp FROM UTILIZADORES;");
			if(rs.next()){
				Timestamp trata = rs.getTimestamp("timestamp");
			}
			ByteArrayOutputStream bOut;
			ObjectOutputStream out;
			bOut = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bOut);
			String comando = "INSERT INTO UTILIZADORES (USERNAME, NOME, PASSWORD, CANAL, IMAGEM) VALUES ('" + username + "', '" + name + "', '" + password + "', '" + "NO_CHANNEL" + "', '" + foto +"');";
			stmt.executeUpdate("INSERT INTO MODIFICACOES (COMANDO) VALUES (\"" + comando + "\");");
			out.writeUnshared("REGISTO:"+username+":"+name+":"+password+":"+esteServer);
			out.flush();
			DatagramPacket packetMulti = new DatagramPacket(bOut.toByteArray(), bOut.size(), group, portMulti);
			DatagramSocket socket = new DatagramSocket();
			socket.send(packetMulti);
			return "OK";
        }
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "NOT OK";
	}
	
	@Override
	public void novaNotificacao(Msg mensagem) throws RemoteException, IOException, SQLException {
		//System.out.println("RMI: " + mensagem.getConteudo());
		String[] splitStr = mensagem.getConteudo().trim().split(":"); 
		String notificacao = mensagem.getUsername() + ": " + mensagem.getConteudo();
		ResultSet rs = null;
		for (UserRmi p : userList) {
			if(splitStr[0].equalsIgnoreCase("PRIVATE MESSAGE") && p.getUsername().equalsIgnoreCase(mensagem.getdestinatario())){
				try {
					p.getInterface().notificacao(notificacao);
				}catch (RemoteException e){ //UNABLE TO CONTACT LISTENER
					removeListener(p.getInterface());
				}
				continue;
			}
			if(p.getUsername().equalsIgnoreCase("Autonoma")){
				try {
					p.getInterface().notificacao(notificacao);
				}catch (RemoteException e){ //UNABLE TO CONTACT LISTENER
					removeListener(p.getInterface());
				}
				continue;
			}
			rs = stmt.executeQuery("SELECT canal FROM utilizadores where username = '" + p.getUsername() + "';");
			String canal = "";
			if(rs !=null && rs.next())	
				canal = rs.getString("canal");
			if(mensagem.getdestinatario().equalsIgnoreCase(canal)){									
				try {
					p.getInterface().notificacao(notificacao);
				}catch (RemoteException e){ //UNABLE TO CONTACT LISTENER
					removeListener(p.getInterface());
				}
			}
        }                
	}
	
	@Override
	public void novaNotificacao(String notificacao) throws RemoteException, IOException, SQLException {
		//System.out.println("RMI: " + notificacao);
		for (UserRmi p : userList){		
			try {
				p.getInterface().notificacao(notificacao);
			}catch (RemoteException e){ //UNABLE TO CONTACT LISTENER
				removeListener(p.getInterface());
			}
        }                
	}
}