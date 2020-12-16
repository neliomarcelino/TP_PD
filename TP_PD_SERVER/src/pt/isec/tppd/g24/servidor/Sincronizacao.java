package pt.isec.tppd.g24.servidor;

import java.io.*;
import java.sql.*;

public class Sincronizacao implements Serializable{
    public static final long serialVersionUID = 10L;

    private Timestamp comando;

    public Sincronizacao(Timestamp comando) {
        this.comando = comando;
    }

    public Timestamp getComando(){return comando;}
}