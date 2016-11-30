package sample;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sample.modelo.Quadro;

import java.io.IOException;

import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by edupooch on 29/11/2016.
 */
public class Cliente {
    private static final int PORTA_PADRAO = 54321;

    public static void main(String[] args)
            throws UnknownHostException, IOException {
        Socket cliente = new Socket("127.0.0.1", PORTA_PADRAO);
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
}

