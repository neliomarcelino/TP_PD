package pt.isec.tppd.g24.servidor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ThreadUpload extends Thread{
    public static final int MAX_SIZE = 5120;
    private DatagramSocket socket;
    private String fileName, canal;

    ThreadUpload(DatagramSocket socket, String fileName, String canal){
        this.socket = socket;
        this.fileName = fileName;
		this.canal = canal;
    }

    @Override
    public void run(){
        File localDirectory;
		DatagramPacket packet, receivePacket;
        String requestedCanonicalFilePath = null;
        FileInputStream requestedFileInputStream = null;
		String resposta;
		int tentativas,tentativasMax = 5;
		

        byte []fileChunk = new byte[MAX_SIZE];
        int nbytes;

        try{
            localDirectory = new File(System.getProperty("user.dir") + File.separator + canal);
			requestedFileInputStream=null;

			socket.setSoTimeout(10000); // 10sec
            packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);                    
            socket.receive(packet);

			resposta = new String(packet.getData(), 0, packet.getLength()).trim();

			if(resposta.equalsIgnoreCase("PRONTO")){
				
				requestedCanonicalFilePath = new File(localDirectory+File.separator+fileName).getCanonicalPath();

                if(!requestedCanonicalFilePath.startsWith(localDirectory.getCanonicalPath()+File.separator)){
                    System.out.println("Nao e' permitido aceder ao ficheiro " + requestedCanonicalFilePath + "!");
                    System.out.println("A directoria de base nao corresponde a " + localDirectory.getCanonicalPath()+"!");
                    return;
                }
				
				requestedFileInputStream = new FileInputStream(requestedCanonicalFilePath);
                System.out.println("Ficheiro " + requestedCanonicalFilePath + " aberto para leitura.");
				
				do{
                    nbytes = requestedFileInputStream.read(fileChunk);

                    if(nbytes == -1){//EOF
                        nbytes = 0;
                    }

                    packet.setData(fileChunk, 0, nbytes);
                    packet.setLength(nbytes);

                    socket.send(packet);   

					tentativas = 0;
					while (tentativas <= tentativasMax){
						socket.setSoTimeout(5000);
						try{
							receivePacket = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
							socket.receive(receivePacket);
							resposta = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

							if(resposta.equalsIgnoreCase("ERRO")){
								socket.send(packet);
								tentativas++;
							}
							break;
						}catch (SocketTimeoutException e){tentativas++;}
					}
					if(tentativas > tentativasMax)
						break;
                }while(nbytes > 0);     

				if(tentativas > tentativasMax)
					System.out.println("Transferencia sem sucesso!");
				else
					System.out.println("Transferencia concluida!");
			}
        }catch(NumberFormatException e){
            System.out.println("O porto de escuta deve ser um inteiro positivo:\n\t"+e);
        }catch(SocketException e){
            System.out.println("Ocorreu uma excepcao ao nivel do socket TCP de escuta:\n\t"+e);
        }catch(IOException e){
            System.out.println("Ocorreu uma excepcao de E/S: \n\t" + e);
        }finally{
            if(socket != null){
                socket.close();
            }
			if(requestedFileInputStream != null){
				try{
					requestedFileInputStream.close();
				}catch (IOException e){}
			}
        }
    }
}