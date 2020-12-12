package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

public class InfoSincronizacao implements Serializable{
    public static final long serialVersionUID = 10L;
	
	private ArrayList<String> users, mensagens, canais;
	private ArrayList<Timestamp> tusers, tmensagens, tcanais;	

    public InfoSincronizacao(ArrayList<String> users, ArrayList<String> mensagens, ArrayList<String> canais, ArrayList<Timestamp> tusers, ArrayList<Timestamp> tmensagens, ArrayList<Timestamp> tcanais) {
		this.users = users;
		this.mensagens = mensagens;
		this.canais = canais;
		this.tusers = tusers;
		this.tmensagens = tmensagens;
		this.tcanais = tcanais;
	}

    public ArrayList<String> getListUser(){
		return users;
	}
	
	public ArrayList<String> getListMensagens(){
		return mensagens;
	}
	
	public ArrayList<String> getListCanais(){
		return canais;
	}
	
	public ArrayList<Timestamp> getListTimeUser(){
		return tusers;
	}
	
	public ArrayList<Timestamp> getListTimeMensagens(){
		return tmensagens;
	}
	
	public ArrayList<Timestamp> getListTimeCanais(){
		return tcanais;
	}
}