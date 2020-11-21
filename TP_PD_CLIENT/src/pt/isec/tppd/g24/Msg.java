package pt.isec.tppd.g24;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Msg implements Serializable {
    public static final long serialVersionUID = 10L;
    protected String username;
    protected String conteudo;
    protected Calendar tempo;

    public Msg(String username, String conteudo){
        this.username = username;
        this.conteudo = conteudo;
        tempo = GregorianCalendar.getInstance();
    }

    public String getUsername() {return username;}
    public String getConteudo() {return conteudo;}
    public Calendar getTempo() {return tempo;}
}
