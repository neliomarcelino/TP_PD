package pt.isec.tppd.g24;

import java.rmi.*;
import java.io.*;
import java.sql.*;

public interface ServidorInterface extends Remote{
    public String regista(String name, String username, String password, String foto, UserInterface inter) throws RemoteException;
	public void addListener(UserInterface listener, String username) throws RemoteException, IOException;
	public void removeListener (UserInterface listener) throws RemoteException;
	public void novaNotificacao(Msg mensagem) throws RemoteException, IOException, SQLException;
	public void novaNotificacao(String notificacao) throws RemoteException, IOException;
}
