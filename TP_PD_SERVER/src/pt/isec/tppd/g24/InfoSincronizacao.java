package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

public class InfoSincronizacao implements Serializable{
    public static final long serialVersionUID = 10L;
	
	private ArrayList<String> comandos;
	private ArrayList<Timestamp> tcomandos;	

    public InfoSincronizacao(ArrayList<String> comandos, ArrayList<Timestamp> tcomandos) {
		this.comandos = comandos;
		this.tcomandos = tcomandos;
	}

    public ArrayList<String> getListComandos(){
		return comandos;
	}
	
	public ArrayList<Timestamp> getListTimeComandos(){
		return tcomandos;
	}
}