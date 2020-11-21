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
            } catch (IOException e) {
                System.out.println();
                System.out.println("Impossibilidade de aceder ao conteudo da mensagem recebida! " + e);
            } catch (ClassNotFoundException e) {
                System.out.println();
                System.out.println("Mensagem recebida de tipo inesperado! " + e);
            }
        }
    }

    public void terminate(){ running = false; }

}
