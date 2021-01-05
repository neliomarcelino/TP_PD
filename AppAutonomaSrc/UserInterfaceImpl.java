package pt.isec.tppd.g24.autonoma;

import pt.isec.tppd.g24.*;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class UserInterfaceImpl  extends UnicastRemoteObject implements UserInterface{
	
	public static final int MAX_CHUNCK_SIZE = 10000; //bytes

    public UserInterfaceImpl() throws RemoteException{}

    @Override
    public void notificacao(String conteudo){
        System.out.println();
        System.out.println(conteudo);
        System.out.print(">");
    }
	
	protected FileInputStream getRequestedFileInputStream(String fileName) throws IOException
    {
        String requestedCanonicalFilePath;
		File localDirectory = new File(System.getProperty("user.dir"));
        fileName = fileName.trim();

        /**
         * Verifica se o ficheiro solicitado existe e encontra-se por baixo da localDirectory.
         */

        requestedCanonicalFilePath = new File(localDirectory+File.separator+fileName).getCanonicalPath();

        if(!requestedCanonicalFilePath.startsWith(localDirectory.getCanonicalPath()+File.separator)){
            System.out.println("Nao e' permitido aceder ao ficheiro " + requestedCanonicalFilePath + "!");
            System.out.println("A directoria de base nao corresponde a " + localDirectory.getCanonicalPath()+"!");
            throw new IOException(fileName);
        }

        /**
         * Abre o ficheiro solicitado para leitura.
         */
        return new FileInputStream(requestedCanonicalFilePath);

    }
	
	@Override
    public byte [] getFileChunk(String fileName, long offset) throws RemoteException, IOException
    {
        String requestedCanonicalFilePath = null;
        FileInputStream requestedFileInputStream = null;
        byte [] fileChunk = new byte[MAX_CHUNCK_SIZE];
        int nbytes;

        fileName = fileName.trim();
        //System.out.println("Recebido pedido para: " + fileName);

        try{
			
            requestedFileInputStream = getRequestedFileInputStream(fileName);

            /**
             * Obtem um bloco de bytes do ficheiro, omitindo os primeiros offset bytes.
             */
            requestedFileInputStream.skip(offset);
            nbytes = requestedFileInputStream.read(fileChunk);

            if(nbytes == -1){//EOF
                return null;
            }

            /**
             * Se fileChunk nao esta' totalmente preenchido (MAX_CHUNCK_SIZE), recorre-se
             * a um array auxiliar com tamanho correspondente ao numero de bytes efectivamente lidos.
             */
            if(nbytes < fileChunk.length){
                byte [] aux = new byte[nbytes];
                System.arraycopy(fileChunk, 0, aux, 0, nbytes);
                return aux;
            }
            return fileChunk;
        }catch(IOException e){
            System.out.println("Ocorreu a excepcao de E/S: \n\t" + e);
            throw new IOException(fileName, e.getCause());
        }finally{
            if(requestedFileInputStream != null){
                try {
                    requestedFileInputStream.close();
                } catch (IOException e) {}
            }

        }

    }
}
