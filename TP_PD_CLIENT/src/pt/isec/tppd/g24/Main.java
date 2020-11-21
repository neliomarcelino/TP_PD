package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;
import java.util.List;

public class Main {
    public static final int MAX_SIZE = 100000;

    public static void main(String[] args)
    {
        if(args.length != 2){
            System.out.println("Sintaxe: java Cliente serverAddress serverUdpPort");
            return;
        }

        DatagramSocket socketUdp = null;
        DatagramPacket packet = null;
        Socket socketTcp = null;

        ByteArrayOutputStream bOut;
        ByteArrayInputStream bIn;
        ObjectInputStream in;
        ObjectOutputStream out;
        String ServerRequest = "LIGACAO SERVER";
        boolean conexao = false;
        List<InfoServer> lista; // lista[0] = serverAddr | lista[1] = serverPortUdp | lista[2] = serverPortTcp
        InfoServer inicial;
        MsgServer resposta;
        Msg msgEnvio;
        String teclado;
        BufferedReader inTeclado = new BufferedReader(new InputStreamReader(System.in));
        String EXIT = "EXIT";

        ThreadMsg t = null;

        try{
            inicial = new InfoServer(args[0], Integer.parseInt(args[1]),-1);
            socketUdp = new DatagramSocket();

            do {
                bOut = new ByteArrayOutputStream();
                out = new ObjectOutputStream(bOut);

                out.writeUnshared(ServerRequest);
                out.flush();


                packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), InetAddress.getByName(inicial.getAddr()), inicial.getPortUdp());

                System.out.println("A enviar pedido de conexao para o servidor: <" + inicial.getAddr() + ":" + inicial.getPortUdp() + ">");

                socketUdp.send(packet);
				socketUdp.setSoTimeout(5000); // 5 sec
                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);

                socketUdp.receive(packet);

                bIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                in = new ObjectInputStream(bIn);

                resposta = (MsgServer) in.readObject();

                System.out.println("Recebi resposta.");

                conexao = resposta.getPodeLigar();
                lista = resposta.getAddrString();
                inicial = lista.get(0);
            } while (!conexao);

            socketTcp = new Socket(inicial.getAddr(), inicial.getPortTcp());

            t = new ThreadMsg(socketTcp);
            t.start();

            while(true){
                System.out.print("> ");
                teclado = inTeclado.readLine();

                if(teclado.equalsIgnoreCase(EXIT)){
                    break;
                }

                msgEnvio = new Msg("Joao", teclado);

                out = new ObjectOutputStream(socketTcp.getOutputStream());
                out.writeObject(msgEnvio);
                out.flush();
            }
        }catch (SocketTimeoutException e) {
            System.out.println("Nao recebi nenhuma resposta (Servidor down?)\n\t"+e);
        }catch(ClassNotFoundException | UnknownHostException e){
            System.out.println("Destino desconhecido:\n\t"+e);
        }catch(NumberFormatException e){
            System.out.println("O porto do servidor deve ser um inteiro positivo.");
        }catch(SocketException e){
            System.out.println("Ocorreu um erro ao nivel do socket:\n\t"+e);
        }catch(IOException e){
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t"+e);
        }finally{
            if(t != null){
                t.terminate();
            }
            try{
                if(socketUdp != null)
                    socketUdp.close();
                if(socketTcp != null)
                    socketTcp.close();
            }catch(IOException e){}
        }
    }

}
