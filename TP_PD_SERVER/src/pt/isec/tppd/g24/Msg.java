package pt.isec.tppd.g24;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Msg implements Serializable {
    public static final long serialVersionUID = 10L;
    protected String username;
    protected String conteudo;
    protected Calendar tempo;
    protected String destinatario;

    public Msg(String username, String conteudo, String destinatario){
        this.username = username;
        this.conteudo = conteudo;
        tempo = GregorianCalendar.getInstance();
        this.destinatario = destinatario;
    }

    public String getUsername() {return username;}
    public String getConteudo() {return conteudo;}
    public Calendar getTempo() {return tempo;}
    public String getdestinatario() {return destinatario;}
}