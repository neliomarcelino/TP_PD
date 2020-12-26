package pt.isec.tppd.g24.cliente;

import pt.isec.tppd.g24.*;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class UserInterfaceImpl  extends UnicastRemoteObject implements UserInterface{
	
	public UserInterfaceImpl() throws RemoteException{}
	
	@Override
	public void notificacao(Msg mensagem){
		System.out.println();
        System.out.println(mensagem.getUsername() + ": " + mensagem.getConteudo());
		System.out.print(">");
	}
}