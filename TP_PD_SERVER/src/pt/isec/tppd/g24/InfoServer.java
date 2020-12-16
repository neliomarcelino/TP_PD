package pt.isec.tppd.g24;

import java.io.Serializable;

public class InfoServer implements Serializable, Comparable<InfoServer> {
    public static final long serialVersionUID = 10L;

    protected String addr;
    protected int portUdp, portTcp, nClientes;

    public InfoServer(String addr, int portUdp, int portTcp) {
        this.addr = addr;
        this.portUdp = portUdp;
        this.portTcp = portTcp;
        this.nClientes = 0;
    }

    public String getAddr() { return addr; }
    public int getPortUdp() { return portUdp; }
    public int getPortTcp() { return portTcp; }
    public int getNClientes() { return nClientes; }
    public void addNClientes() { nClientes++; }
    public void decNClientes() { nClientes--; }
    public void setnClientes(int nClientes) {this.nClientes = nClientes;}

    @Override
    public int compareTo(InfoServer o) {return this.nClientes - o.getNClientes();}
	@Override
    public String toString(){ return addr + " " + portUdp + " " + portTcp; }
}