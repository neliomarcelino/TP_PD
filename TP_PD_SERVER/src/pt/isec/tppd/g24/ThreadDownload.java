package pt.isec.tppd.g24;


import java.io.*;
import java.net.*;

public class ThreadDownload extends Thread{
    public static final int MAX_SIZE = 5120;
    private String serverAddr;
    private int serverPort;

    private String fileName;

    ThreadDownload(String serverAddress, int serverPort, String fileName){
        this.serverAddr = serverAddress;
        this.serverPort = serverPort;
        this.fileName = fileName;
    }

    @Override
    public void run(){
        File localDirectory;
        String localFilePath = null;
        Socket socket = null;
        InputStream in;
        FileOutputStream localFileOutputStream = null;
        //int contador = 0;
        ObjectOutputStream out;

        localDirectory = new File(System.getProperty("user.dir"));
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
                socket = new Socket(serverAddr, serverPort);
                socket.setSoTimeout(5000);

                byte []buffer = new byte[MAX_SIZE];
                in = socket.getInputStream();
                int nbytes;

                while((nbytes = in.read(buffer)) > 0)
                    localFileOutputStream.write(buffer, 0, nbytes);

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
            try{
                if(socket != null){
                    socket.close();
                }
            }catch(IOException e){}
            if(localFileOutputStream != null){
                try{
                    localFileOutputStream.close();
                }catch(IOException e){}
            }

        }
    }
}