package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

public class Sincronizacao implements Serializable{
    public static final long serialVersionUID = 10L;

    private Timestamp users, mensagens, canais;

    public Sincronizacao(Timestamp users, Timestamp mensagens, Timestamp canais) {
        this.users = users;
		this.mensagens = mensagens;
		this.canais = canais;
    }

    public Timestamp getUsers(){return users;}
	public Timestamp getMensagens(){return mensagens;}
	public Timestamp getCanais(){return canais;}
}