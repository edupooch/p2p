package sample;

import sample.controle.Controller;

import java.io.IOException;

public class Main{

    public static void main(String[] args) {
        Controller controller = new Controller();
        try {
            controller.iniciar();
        } catch (Exception e) {
            System.out.println("Ocorreu um problema. Reiniciando programa...");
            main(null);
        }
    }

}
