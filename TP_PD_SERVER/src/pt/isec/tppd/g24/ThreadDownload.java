package pt.isec.tppd.g24.servidor;
import pt.isec.tppd.g24.*;


import java.io.*;
import java.net.*;

public class ThreadDownload extends Thread{
    public static final int MAX_SIZE = 5120;
    private String serverAddr;
    private int serverPort;
	private String canal;

    private String fileName;

    ThreadDownload(String serverAddress, int serverPort, String fileName, String canal){
        this.serverAddr = serverAddress;
        this.serverPort = serverPort;
        this.fileName = fileName;
		this.canal = canal;
    }

    @Override
    public void run(){
        File localDirectory;
        String localFilePath = null;
        DatagramSocket socket = null;
		DatagramPacket packet, enviaPacket;
		InetAddress addr;
        InputStream in;
		String envia = "PRONTO";
        FileOutputStream localFileOutputStream = null;
        //int contador = 0;
        ObjectOutputStream out;

        localDirectory = new File(System.getProperty("user.dir") + File.separator + canal);
		if (!localDirectory.exists()){
			localDirectory.mkdir();
		}
		
        try{

            try{

                localFilePath = localDirectory.getCanonicalPath()+File.separator+fileName;
                localFileOutputStream = new FileOutputStream(localFilePath);

                System.out.println("Ficheiro " + localFilePath + " criado.");

            }catch(IOException e){

                if(localFilePath == null){
                    System.out.println("Ocorreu a excepcao {" + e +"} ao obter o caminho canonico para o ficheiro local!");
                }else{
                    System.out.println("Ocorreu a excepcao {" + e +"} ao tentar criar o ficheiro " + localFilePath + "!");
                }

                return;
            }

            try{
				addr = InetAddress.getByName(serverAddr);
                socket = new DatagramSocket();
                socket.setSoTimeout(10000);

                packet = new DatagramPacket(envia.getBytes(), envia.length(), addr, serverPort);
                socket.send(packet);
                
                //System.out.println(socket.getReceiveBufferSize());
                
                do{
                    
                    packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                    socket.receive(packet);
                    
                    if(packet.getPort() == serverPort && packet.getAddress().equals(addr)){
                        localFileOutputStream.write(packet.getData(), 0, packet.getLength());
						
						enviaPacket = new DatagramPacket("OK".getBytes(), "OK".length(), addr, serverPort);
						socket.send(enviaPacket);
                    }
                    
                }while(packet.getLength() > 0);
                
                System.out.println("Transferencia concluida.");
               
            }catch(UnknownHostException e){
                System.out.println("Destino desconhecido:\n\t"+e);
            }catch(NumberFormatException e){
                System.out.println("O porto do servidor deve ser um inteiro positivo:\n\t"+e);
            }catch(SocketTimeoutException e){
                System.out.println("Nao foi recebida qualquer bloco adicional, podendo a transferencia estar incompleta:\n\t"+e);
            }catch(SocketException e){
                System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t"+e);
            }catch(IOException e){
                System.out.println("Ocorreu um erro no acesso ao socket ou ao ficheiro local " + localFilePath +":\n\t"+e);
            }
        }finally{
                if(socket != null){
                    socket.close();
                }
            if(localFileOutputStream != null){
                try{
                    localFileOutputStream.close();
                }catch(IOException e){}
            }

        }
    }
}