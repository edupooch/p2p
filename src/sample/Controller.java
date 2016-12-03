package sample;

import com.google.gson.Gson;
import sample.modelo.Quadro;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Controller {

    private final static String DIR_COMPARTILHADO = "src\\dir";
    private static final int PORTA_PADRAO = 54321;
    private static final int N_TENTATIVAS_ENVIO = 15;

    void teste() {
        Path path = Paths.get(DIR_COMPARTILHADO + "\\1.txt");
        try {
            byte[] data = Files.readAllBytes(path);
            String s = Base64.getEncoder().encodeToString(data);
            byte[] decode = Base64.getDecoder().decode(s);
            Path path2 = Paths.get(DIR_COMPARTILHADO + "\\2.txt");

            Files.write(path2, decode);
            System.out.println(s);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void iniciar() throws IOException {
        servidor(); //escuta
        cliente(); //pergunta
    }

    private void servidor() throws IOException {
        ServerSocket servidor = new ServerSocket(PORTA_PADRAO);
        System.out.println("Porta " + PORTA_PADRAO + " aberta!");

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
                        System.out.println(msg);

                        Gson gson = new Gson();
                        Quadro quadro = gson.fromJson(msg, Quadro.class);

                        switch (quadro.getTipo()) {

                            case Quadro.RESPOSTA_LISTA:
                                comparaLista(quadro.getDados(), ip);
                                break;
                            case Quadro.PEDIDO_LISTA:
                                System.out.println("Pedido Lista");
                                enviaLista(ip);
                                break;
                            case Quadro.PEDIDO_ARQUIVO:
                                enviaArquivos(quadro.getDados(), ip);
                                break;
                            case Quadro.RESPOSTA_ARQUIVO:
                                recebeArquivo(quadro.getDados());
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

    private void recebeArquivo(String[] dados) {
        for (int i = 0; i < dados.length; i = i + 2) {
            String nomeArquivo = dados[i];
            System.out.print("Arquivo " + nomeArquivo + " recebido!");
            Path path = Paths.get(DIR_COMPARTILHADO + "\\" + nomeArquivo);
            try {
                byte[] bytesArquivo = Base64.getDecoder().decode(dados[i+1]);
                Files.write(path, bytesArquivo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enviaArquivos(String[] listaArquivos, String ipDestino) {
        for (String nomeArquivo : listaArquivos) {
            Path path = Paths.get(DIR_COMPARTILHADO + "\\" + nomeArquivo);
            try {
                byte[] dados = Files.readAllBytes(path);
                String strDados = Base64.getEncoder().encodeToString(dados);
                Quadro quadroArquivo = new Quadro(Quadro.RESPOSTA_ARQUIVO, new String[]{nomeArquivo, strDados});
                Gson gson = new Gson();
                String strJson = gson.toJson(quadroArquivo);
                enviaSocket(strJson, ipDestino);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void comparaLista(String[] listaRecebida, String ipFonte) {
        new Thread(() -> {
            String[] meusArquivos = getListaArquivos();
            ArrayList<String> arquivosFaltando = new ArrayList<>();
            for (String arquivo : listaRecebida) {
                if (!Arrays.asList(meusArquivos).contains(arquivo)) {
                    arquivosFaltando.add(arquivo);
                }
            }
            if (arquivosFaltando.size() > 0) {
                pedirArquivos((String[]) arquivosFaltando.toArray(), ipFonte);
            }
        }).start();
    }

    private void pedirArquivos(String[] arquivosFaltando, String ipFonte) {
        Gson gson = new Gson();
        Quadro quadroPedido = new Quadro(Quadro.PEDIDO_ARQUIVO, arquivosFaltando);
        String strJson = gson.toJson(quadroPedido);
        enviaSocket(strJson, ipFonte);
    }

    private void cliente() throws IOException {

        //Ler o arquivo da lista de IP's e adicionar num ArrayList
        Scanner scannerIps = new Scanner(new FileReader("lista_ips.txt"));
        ArrayList<String> contatos = new ArrayList<>();
        while (scannerIps.hasNextLine()) {
            contatos.add(scannerIps.nextLine());
        }
        // Embaralhar lista de IP's para não começar sempre pelo primeiro
        Collections.shuffle(contatos);
        int count = 0;//contador para qual ip da lista pedir a lista

        while (true) {
            boolean pedir = true;

            if (count == contatos.size()) count = 0; // retorna o contador pra zero caso chegue no limite
            String ip = contatos.get(count++); //salva o ip do contato e incrementa o contador

            Socket cliente = new Socket(ip, PORTA_PADRAO);
            System.out.println("CLIENTE: Me conectei com " + ip);
            PrintStream saida = new PrintStream(cliente.getOutputStream());


            Quadro quadroPedidoLista = new Quadro(Quadro.PEDIDO_LISTA, new String[]{""});
            Gson gson = new Gson();
            String jsonPedido = gson.toJson(quadroPedidoLista);
            saida.println(jsonPedido);
            saida.println(getEncerrar());

            saida.close();
            cliente.close();
        }

    }

    private String getEncerrar() {
        Quadro quadroPedidoLista = new Quadro(Quadro.PEDIDO_LISTA, new String[]{""});
        return new Gson().toJson(quadroPedidoLista);
    }


    private static void enviaLista(String ip) throws IOException {
        new Thread(() -> {
            String[] strList = getListaArquivos();

            Quadro quadroLista = new Quadro(Quadro.RESPOSTA_LISTA, strList);
            Gson gson = new Gson();

            String strJson = gson.toJson(quadroLista);
            enviaSocket(strJson, ip);
        }).start();


    }

    private static String[] getListaArquivos() {
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
        return strList;
    }

    private static void enviaSocket(String strJson, String ip) {
        enviaSocket(strJson, ip, 0);
    }

    private static void enviaSocket(String strJson, String ip, int tentativas) {
        try {
            PrintStream saida = null;
            Socket socketResposta = new Socket(ip, PORTA_PADRAO);
            saida = new PrintStream(socketResposta.getOutputStream());
            saida.println(strJson);
        } catch (IOException e) {
            e.printStackTrace();
            if (tentativas < N_TENTATIVAS_ENVIO) {
                tentativas++;
                espera(100 * tentativas);
                enviaSocket(strJson, ip, tentativas);
            } else {
                System.out.println("Número de tentativas esgotado para o envio do quadro " + strJson);
            }
        }
    }

    private static void espera(int tempo) {
        System.out.println("Quadro não enviado. Esperar " + tempo + "ms ...");
        try {
            Thread.sleep(tempo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
