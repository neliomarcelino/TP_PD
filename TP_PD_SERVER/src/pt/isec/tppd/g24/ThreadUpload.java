
import java.io.*;
import java.net.*;

public class ThreadUpload extends Thread{
    public static final int MAX_SIZE = 5120;
    private ServerSocket socket;
    private String fileName;

    ThreadUpload(ServerSocket socket, String fileName){
        this.socket = socket;
        this.fileName = fileName;
    }

    @Override
    public void run(){
        File localDirectory;
        Socket socketToClient;
        OutputStream out;
        String requestedCanonicalFilePath = null;
        FileInputStream requestedFileInputStream;

        byte []fileChunk = new byte[MAX_SIZE];
        int nbytes;

        try{
            localDirectory = new File(System.getProperty("user.dir"));

            while(true){

                requestedFileInputStream=null;

                socketToClient = socket.accept();

                try{
                    socketToClient.setSoTimeout(10000); //10 sec

                    out = socketToClient.getOutputStream();

                    requestedCanonicalFilePath = new File(localDirectory+File.separator+fileName).getCanonicalPath();

                    if(!requestedCanonicalFilePath.startsWith(localDirectory.getCanonicalPath()+File.separator)){
                        System.out.println("Nao e' permitido aceder ao ficheiro " + requestedCanonicalFilePath + "!");
                        System.out.println("A directoria de base nao corresponde a " + localDirectory.getCanonicalPath()+"!");
                        continue;
                    }

                    requestedFileInputStream = new FileInputStream(requestedCanonicalFilePath);

                    System.out.println("Ficheiro " + requestedCanonicalFilePath + " aberto para leitura.");

                    while((nbytes = requestedFileInputStream.read(fileChunk)) > 0){
                        out.write(fileChunk, 0, nbytes);
                    }

                    System.out.println("Upload concluida");

                }catch(SocketException e){
                    System.out.println("Ocorreu uma excepcao ao nivel do socket TCP de ligacao ao cliente:\n\t"+e);
                }catch(FileNotFoundException e){
                    System.out.println("Ocorreu a excepcao {" + e + "} ao tentar abrir o ficheiro " + requestedCanonicalFilePath + "!");
                }catch(IOException e){
                    System.out.println("Ocorreu uma excepcao de E/S durante o atendimento ao cliente actual: \n\t" + e);
                }finally{
                    try{
                        if(requestedFileInputStream!=null){
                            requestedFileInputStream.close();
                        }
                        socketToClient.close();
                    }catch(IOException e){}
                }
            }
        }catch(NumberFormatException e){
            System.out.println("O porto de escuta deve ser um inteiro positivo:\n\t"+e);
        }catch(SocketException e){
            System.out.println("Ocorreu uma excepcao ao nivel do socket TCP de escuta:\n\t"+e);
        }catch(IOException e){
            System.out.println("Ocorreu uma excepcao de E/S: \n\t" + e);
        }finally{
            if(socket != null){
                try{
                    socket.close();
                }catch(IOException e){}
            }
        }
    }
}