package pt.isec.tppd.g24;

import java.rmi.*;
import java.io.*;

public interface UserInterface extends Remote{
    public void notificacao(String conteudo) throws RemoteException;
}
