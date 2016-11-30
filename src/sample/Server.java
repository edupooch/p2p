package sample;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sample.modelo.Quadro;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by edupooch on 29/11/2016.
 */
public class Server {

    private final static String DIR_COMPARTILHADO = "C:\\Users\\edupooch\\redes\\";
    private static final int PORTA_PADRAO = 54321;

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(PORTA_PADRAO);
        System.out.println("Porta 12345 aberta!");

        while (true) {
            Socket cliente = servidor.accept();
            new Thread(() -> {
                String ip = cliente.getInetAddress().getHostAddress();
                System.out.println("Nova conexão com o Cliente " + ip);

                Scanner s = null;
                try {
                    s = new Scanner(cliente.getInputStream());

                    while (s.hasNextLine()) {
                        String msg = s.nextLine();
                        Gson gson = new Gson();
                        Quadro quadro = gson.fromJson(msg, Quadro.class);

                        switch (quadro.getTipo()){

                            case Quadro.PEDIDO_LISTA:
                                retornaLista(ip);

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

    private static void retornaLista(String ip) throws IOException {
        File folder = new File(DIR_COMPARTILHADO);
        File[] listOfFiles = folder.listFiles();

        assert listOfFiles != null;
        String[] strList = new String[listOfFiles.length];
        for (int i = 0; i < listOfFiles.length; i++) {
            File listOfFile = listOfFiles[i];
            if (listOfFile.isFile()) {
                strList[i] = listOfFile.getName();
            }
        }

        Quadro quadroLista = new Quadro(Quadro.RESPOSTA_LISTA,"",strList);
        Gson gson = new Gson();
        String strJson = gson.toJson(quadroLista);

        Socket socketResposta = new Socket(ip, PORTA_PADRAO);
        PrintStream saida = new PrintStream(socketResposta.getOutputStream());
        saida.print(strJson);

    }

}