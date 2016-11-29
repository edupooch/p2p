package sample;


import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by edupooch on 29/11/2016.
 */
public class Server {

    private final static String DIR_COMPARTILHADO = "C:\\Users\\edupooch\\redes\\";

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(12345);
        System.out.println("Porta 12345 aberta!");

        while (true) {
            Socket cliente = servidor.accept();
            new Thread(() -> {
                System.out.println("Nova conexão com o Cliente " +
                        cliente.getInetAddress().getHostAddress()
                );

                Scanner s = null;
                try {
                    s = new Scanner(cliente.getInputStream());

                    while (s.hasNextLine()) {
                        String msg = s.nextLine();
                        System.out.println(msg);

                        if (msg.contains("ls")){
                            retornaLista();
                        }
                    }

                    s.close();
                    servidor.close();
                    cliente.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        }
    }

    private static void retornaLista(){
        File folder = new File(DIR_COMPARTILHADO);
        File[] listOfFiles = folder.listFiles();

        assert listOfFiles != null;
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                System.out.println("File " + listOfFile.getName());
            }
        }
    }

}