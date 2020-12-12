package pt.isec.tppd.g24;

import java.io.*;
import java.net.*;

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

					
					while (true){
						socket.setSoTimeout(5000);
						try{
							receivePacket = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
							socket.receive(receivePacket);
							resposta = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

							if(resposta.equalsIgnoreCase("ERRO")){
								socket.send(packet);
							}
							break;
						}catch (SocketTimeoutException e){}
					}


					
                    /*
                    * Este programa nao considera o facto do protocolo UDP oferecer um servico do tipo "best effort".
                    * Sendo assim, e' possivel que ocorra perda de datagramas/blocos por erro e esgotamento de buffers.
                    *
                    * Por exemplo, ponha a correr o servidor e os clientes na mesma maquina, transfira ficheiros de
                    * grande dimensao (e.g., fotagrafias com elevada resolucao) e verifique o resultado.
                    * Uma solucao seria a propria aplicacao incluir um mecanismo de controlo de erros e de fluxo(e.g.,
                    * do tipo "stop and wait" com mensagens de confirmacao enviadas pelo cliente a cada bloco recebido,
                    * numeracao dos blocos e verificacao da sequencia, timeout do lado servidor e verificacao da origem
                    * das confirmacoes dado que o UDP nao e' orientado a ligacao).
                    *
                    * No entanto, este esforco adicional nao se justifica. Neste caso, e' preferivel optar pelo protocolo
                    * TCP.
                    *
                    * O pedaco de codigo seguinte que se encontra em comentario permite, em parte, lidar com a situacao de
                    * transferencia de ficheiros de grande dimensao acima referida. No entanto, esta abordagem nao
                    * e' uma solcao aceitavel. E' apenas um remendo que da' mais tempo ao cliente para processar um bloco
                    * antes que o proximo seja enviado pelo servidor, diminuindo, deste modo, a possibilidade de esgotamento
                    * de buffers e consequente perda de datagramas.
                    *
                    */

                    /*try {
                    Thread.sleep(5);
                    } catch (InterruptedException ex) {}
                  */

                }while(nbytes > 0);     

                System.out.println("Transferencia concluida");
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