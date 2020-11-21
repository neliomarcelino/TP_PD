package pt.isec.tppd.g24;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ThreadMsg extends Thread{
    private Socket socketTcp;
    protected boolean running;

    ThreadMsg(Socket socketTcp){
        this.socketTcp = socketTcp;
        running = true;
    }

    @Override
    public void run(){
        ObjectInputStream in;
        Object obj;
        Msg msg;

        if(socketTcp == null || !running){
            return;
        }

        while(running){
            try{
                in= new ObjectInputStream(socketTcp.getInputStream());
                obj = in.readObject();

                if(obj instanceof Msg){
                    msg = (Msg)obj;

                    System.out.println();
                    System.out.println(msg.getUsername() + ": " + msg.getConteudo());
                }
                System.out.print("> ");
            }catch (SocketException e) {
                synchronized (socketTcp) {
                    int i = 0;
                    ByteArrayOutputStream bOut;
                    ObjectOutputStream out;
                    DatagramPacket packet = null;
                    ByteArrayInputStream bIn;
                    ObjectInputStream iin;
                    MsgServer resposta;
                    boolean conexao = false;
                    do {
                        try {
                            bOut = new ByteArrayOutputStream();
                            out = new ObjectOutputStream(bOut);
                            out.writeUnshared(serverRequest);
                            out.flush();

                            packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(lista.get(i).getAddr()), lista.get(i).getPortUdp());

                            socketUdp.send(packet);
                            socketUdp.setSoTimeout(5000); // 5 sec
                            packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                            socketUdp.receive(packet);

                            bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                            iin = new ObjectInputStream(bIn);

                            resposta = (MsgServer) iin.readObject();

                            conexao = resposta.getPodeLigar();
                            lista = resposta.getAddrString();
                            i = 0;
                        } catch (SocketTimeoutException socketTimeoutException) {
                            i++;
                            if (i == lista.size()) {
                                System.out.println();
                                System.out.println("Conexao com os servidores perdida.");
                                System.out.flush();
                                System.out.println();
                                System.exit(-1);
                            }
                        } catch (IOException ioException) {
                        } catch (ClassNotFoundException classNotFoundException) {
                        }
                    } while (!conexao);
                    try {
                        socketTcp = new Socket(lista.get(0).getAddr(), lista.get(0).getPortTcp());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.out.println("ThreadMsg!" + e);
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                System.out.println();
                System.out.println("Mensagem recebida de tipo inesperado! " + e);
            }
        }
    }

    public void terminate(){ running = false; }

}
