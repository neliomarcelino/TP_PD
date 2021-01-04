package pt.isec.tppd.g24.autonoma;

import pt.isec.tppd.g24.*;

import java.io.*;
import java.rmi.registry.*;
import java.rmi.*;
import java.net.*;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args){
        if(args.length < 1){
            System.out.println("Erro no numero de argumentos");
            return;
        }
        int op;
        Registry[] registry = new Registry[args.length];
        for(int i = 0; i < args.length ; i++){
            try {
                registry[i] = LocateRegistry.getRegistry(args[0]); // Find registry (on default port 1099)
            }catch (RemoteException e){
                System.out.println("Erro a localizar registy ["+ i +"].");
                return;
            }
        }
        ArrayList <String> boundNames = new ArrayList<>();
        for(int i = 0; i < registry.length ; i++){
            try {
                for(int j = 0; j < registry[i].list().length; j++){
                    if(registry[i].list()[j].contains("servidor"))
                        boundNames.add(args[i]+"/"+registry[i].list()[j]);
                }
            }catch (RemoteException e){
                System.out.println("Registy no argumento ["+ i +"] nao esta ligado.");
                return;
            }
        }
        BufferedReader inTeclado = new BufferedReader(new InputStreamReader(System.in));
        String menu = "Utilizar servidor:\n";
        for (int i = 0; i < boundNames.size() ; i++){
            menu += i + " - " + boundNames.get(i) + "\n";
        }
        menu += "Opcao: ";
        System.out.print(menu);
        while(true){
            try {
                op = Integer.parseInt(inTeclado.readLine());
            }catch (IOException e){
                System.out.println("Nao é um numero");
                continue;
            }
            if(op < 0 || op >= boundNames.size()){
                System.out.println("Numero escolhido nao esta na lista");
                continue;
            }else break;
        }

        ServidorInterface servidorRmi;
        try{
            servidorRmi = (ServidorInterface) Naming.lookup("rmi://" + boundNames.get(op));
        } catch (NotBoundException | MalformedURLException | RemoteException e){
            e.printStackTrace();
            System.out.println("Erro a tentar ligar ao servidor: " + boundNames.get(op));
            return;
        }
        UserInterfaceImpl userRmi = null;
        try {
            userRmi = new UserInterfaceImpl();
        } catch (RemoteException e) {
            System.out.println("Erro a tentar criar UserInterfaceImpl!");
            return;
        }
        String username = "Autonoma";
        try {
            servidorRmi.addListener(userRmi, username);
        } catch (IOException e) {
            System.out.println("Erro a tentar adicionar um listener!");
            return;
        }

        System.out.println("Entrou no servidor: " + boundNames.get(op));
        String teclado, canal = "NO_CHANNEL";
        while(true){
            System.out.print("> ");
            try {
                teclado = inTeclado.readLine();
            } catch (IOException e) {
                System.out.println("Erro ao ler teclado.");
                continue;
            }

            if (teclado.startsWith("/enviaServer")) {
                String[] splitStr = teclado.trim().split("\\s+");
                if (splitStr.length < 2) {
                    System.out.println("Erro no numero de argumentos");
                    continue;
                }

                try {
                    servidorRmi.novaNotificacao(username + ": " + teclado.substring(13));
                } catch (IOException e) {
                    System.out.println("Erro no envio da mensagem");
                }
                continue;
            }else if (teclado.startsWith("/regista")) {
                String nome, rUsername, password, foto;
                try {
                    do {
                        System.out.println("Nome: ");
                        nome = inTeclado.readLine();
                        if (nome.contains("=") || nome.contains(":"))
                            System.out.println("Nome do utilizador tem caracteres invalidos!");
                    } while (nome.contains("=") || nome.contains(":"));
                    do {
                        System.out.println("Username: ");
                        rUsername = inTeclado.readLine();
                        if (rUsername.contains("=") || rUsername.contains(":"))
                            System.out.println("Username tem caracteres invalidos!");
                    } while (rUsername.contains("=") || rUsername.contains(":"));
                    do {
                        System.out.println("Password: ");
                        password = inTeclado.readLine();
                        if (password.contains("=") || password.contains(":"))
                            System.out.println("Password tem caracteres invalidos!");
                    } while (password.contains("=") || password.contains(":"));
                    do {
                        System.out.println("Foto: ");
                        foto = inTeclado.readLine();
                        if (foto.contains("=") || foto.contains(":"))
                            System.out.println("Foto tem caracteres invalidos!");
                    } while (foto.contains("=") || foto.contains(":"));
                    File f = new File(System.getProperty("user.dir") + File.separator + foto);
                    if (!f.isFile()) {
                        System.out.println("'"+foto+"'Ficheiro nao esta na directoria:" + System.getProperty("user.dir"));
                        continue;
                    }
                    String res = servidorRmi.regista(nome, rUsername, password, foto);
                    if (res.equalsIgnoreCase("USERNAME IN USE")) {
                        System.out.println("Erro ao criar a conta. Username ja utilizado.");
                    }else {
                        System.out.println("Erro no registo de um utilizador!");
                    }
                }catch (IOException e){
                    System.out.println("Erro no registo de um utilizador.");
                }
                continue;
            }else if (teclado.startsWith("/servidores")) {
                String servidores = "Servidores:\n";
                for (int i = 0; i < boundNames.size() ; i++){
                    servidores += i + " - " + boundNames.get(i) + "\n";
                }
                System.out.println(servidores);
                continue;
            }else if (teclado.startsWith("/muda")) {
                String[] splitStr = teclado.trim().split("\\s+");
                if (splitStr.length < 2) {
                    System.out.println("Erro no numero de argumentos");
                    continue;
                }

                int serv = Integer.parseInt(splitStr[1]);
                if(serv < 0 || serv >= boundNames.size()){
                    System.out.println("Numero escolhido nao esta na lista");
                    continue;
                }
                if(serv == op){
                    System.out.println("Ja esta no servidor escolhido.");
                    continue;
                }
                try {
                    servidorRmi.removeListener(userRmi);
                    try{
                        servidorRmi = (ServidorInterface) Naming.lookup("rmi://" + boundNames.get(serv));
                    } catch (NotBoundException | MalformedURLException | RemoteException e){
                        System.out.println("Erro a tentar ligar ao servidor: " + boundNames.get(serv));
                        return;
                    }
                    servidorRmi.addListener(userRmi, username);
					op = serv;
                } catch (IOException e) {
                    System.out.println("Erro a tentar adicionar/remover listener do servidor!");
                    return;
                }
                continue;
            }else if (teclado.startsWith("/help")) {
                System.out.println("\n HELP:" +
                        "\n\n/regista" +
                        "\n\tRegista um Utilizador" +
                        "\n\n/enviaServer" +
                        "\n\tEnvia uma mensagem para todos no mesmo servidor" +
                        "\n\n/servidores" +
                        "\n\tMostra todos os servidores disponiveis" +
                        "\n\n/muda [escolha em numero]" +
                        "\n\tMuda de servidor" +
                        "\n\n");
                continue;
            }
        }
    }
}