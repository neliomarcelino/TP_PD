package pt.isec.tppd.g24;

import java.io.Serializable;
import java.util.List;

public class MsgServer implements Serializable {
    public static final long serialVersionUID = 10L;
    protected boolean podeLigar;
    protected List<InfoServer> listaServers;

    public MsgServer(boolean podeLigar, List<InfoServer> listaServers){
        this.podeLigar = podeLigar;
        this.listaServers = listaServers;
    }

    public boolean getPodeLigar(){ return podeLigar; }
    public List<InfoServer> getAddrString(){ return listaServers; }
}