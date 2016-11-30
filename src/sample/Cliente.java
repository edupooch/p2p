package sample;


import com.google.gson.Gson;
import sample.modelo.Quadro;

import java.io.FileReader;
import java.io.IOException;

import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by edupooch on 29/11/2016.
 */
public class Cliente {
    private static final int PORTA_PADRAO = 54321;

    public static void main(String[] args)
            throws UnknownHostException, IOException {
        new Thread(() -> {
            try {
                criaServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Scanner scannerIps = new Scanner(new FileReader("lista_ips.txt"));
        String ip = scannerIps.nextLine();

        Socket cliente = new Socket(ip, PORTA_PADRAO);
        System.out.println("O Cliente se conectou ao servidor!");

        Scanner teclado = new Scanner(System.in);
        PrintStream saida = new PrintStream(cliente.getOutputStream());

        while (teclado.hasNextLine()) {
            String msg = teclado.nextLine();
            Quadro quadro = new Quadro(msg,"", new String[]{"",""});
            Gson gson = new Gson();
            String stringJson = gson.toJson(quadro);
            saida.println(stringJson);
        }

        saida.close();
        teclado.close();
        cliente.close();
    }

    private static void criaServer() throws IOException {
        ServerSocket servidor = new ServerSocket(12345);
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
                        System.out.println("Recebeu " + msg);
                        Gson gson = new Gson();
                        Quadro quadro = gson.fromJson(msg, Quadro.class);
                        System.out.println("Resposta: " + quadro.getTipo());
                        switch (quadro.getTipo()) {

                            case Quadro.RESPOSTA_LISTA:
                                System.out.println(Arrays.toString(quadro.getDados()));
                               break;

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
}

