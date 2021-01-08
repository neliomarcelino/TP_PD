package pt.isec.tppd.g24;

import java.io.Serializable;

public class InfoServer implements Serializable, Comparable<InfoServer> {
    public static final long serialVersionUID = 10L;

    protected String addr;
    protected int portUdp, portTcp, nClientes;
	protected String serverName;

    public InfoServer(String addr, int portUdp, int portTcp, String serverName) {
        this.addr = addr;
        this.portUdp = portUdp;
        this.portTcp = portTcp;
        this.nClientes = 0;
		this.serverName = serverName;
    }

    public String getAddr() { return addr; }
    public int getPortUdp() { return portUdp; }
    public int getPortTcp() { return portTcp; }
    public int getNClientes() { return nClientes; }
    public void addNClientes() { nClientes++; }
    public void decNClientes() { nClientes--; }
    public void setnClientes(int nClientes) {this.nClientes = nClientes;}
	public String getServerName() { return serverName; }

    @Override
    public int compareTo(InfoServer o) {return this.nClientes - o.getNClientes();}
	@Override
    public String toString(){ return addr + " " + portUdp + " " + portTcp; }
}