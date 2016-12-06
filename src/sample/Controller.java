package sample;

import com.google.gson.Gson;
import sample.modelo.Quadro;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Controller {

    private final static String DIR_COMPARTILHADO = "src\\dir";
    private static final int PORTA_PADRAO = 54321;
    private static final int N_TENTATIVAS_ENVIO = 5;

    void teste() {

    }

    void iniciar() throws IOException {
        //escuta
        new Thread(this::servidor).start();
        //pergunta
        cliente();
    }

    private void servidor() {
        ServerSocket servidor = null;
        try {
            servidor = new ServerSocket(PORTA_PADRAO);
        } catch (IOException e) {
            e.printStackTrace();
            servidor();
        }
        System.out.println("Porta " + PORTA_PADRAO + " aberta!");

        //noinspection InfiniteLoopStatement
        while (true) {
            Socket cliente = null;
            try {
                assert servidor != null;
                cliente = servidor.accept();
            } catch (IOException e) {
                servidor();
            }
            Socket finalCliente = cliente;
            ServerSocket finalServidor = servidor;
            new Thread(() -> {
                assert finalCliente != null;
                String ip = finalCliente.getInetAddress().getHostAddress();
                System.out.println("Nova conexão com o Cliente " + ip);
                Scanner s = null;
                try {
                    s = new Scanner(finalCliente.getInputStream());

                    while (s.hasNextLine()) {
                        String msg = s.nextLine();
                        System.out.println("RECEBIDO: " + msg + " de " + ip);

                        Gson gson = new Gson();
                        Quadro quadro = gson.fromJson(msg, Quadro.class);

                        switch (quadro.getTipo()) {

                            case Quadro.RESPOSTA_LISTA:
                                comparaLista(quadro.getDados(), ip);
                                break;
                            case Quadro.PEDIDO_LISTA:
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
                    finalServidor.close();
                    finalCliente.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        }
    }

    private void cliente() {

        //Ler o arquivo da lista de IP's e adicionar num ArrayList
        Scanner scannerIps = null;
        try {
            scannerIps = new Scanner(new FileReader("lista_ips.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Erro no arquivo de IPs");
            cliente();
        }
        ArrayList<String> contatos = new ArrayList<>();
        assert scannerIps != null;
        while (scannerIps.hasNextLine()) {
            contatos.add(scannerIps.nextLine());
        }
        // Embaralhar lista de IP's para não começar sempre pelo primeiro
        Collections.shuffle(contatos);
        int count = 0;//contador para qual ip da lista pedir a lista

        //Duas threads que ficam pedindo as listas de arquivos dos outros usuários
        new Thread(() -> loopPedir(contatos, count)).start();
        new Thread(() -> loopPedir(contatos, count + 1)).start();
    }


    @SuppressWarnings("InfiniteLoopStatement")
    private void loopPedir(ArrayList<String> contatos, int count) {
        while (true) {
            if (count == contatos.size()) {
                count = 0;// retorna o contador pra zero caso chegue no limite
                espera(10000); //espera 10s quando já pediu pra todos os contatos
            }
            String ip = contatos.get(count++); //salva o ip do contato e incrementa o contador

            Quadro quadroPedidoLista = new Quadro(Quadro.PEDIDO_LISTA, new String[]{""});
            Gson gson = new Gson();
            String jsonPedido = gson.toJson(quadroPedidoLista);
            enviaSocket(jsonPedido, ip);
            espera(2000);
        }

    }


    private void recebeArquivo(String[] dados) {
        for (int i = 0; i < dados.length; i = i + 2) {
            String nomeArquivo = dados[i];
            System.out.print("Arquivo " + nomeArquivo + " recebido!");
            Path path = Paths.get(DIR_COMPARTILHADO + "\\" + nomeArquivo);
            try {
                byte[] bytesArquivo = Base64.getDecoder().decode(dados[i + 1]);
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
                pedirArquivos(arquivosFaltando, ipFonte);
            }
        }).start();
    }

    private void pedirArquivos(ArrayList<String> arquivosFaltando, String ipFonte) {
        Gson gson = new Gson();
        String[] strArquivos = new String[arquivosFaltando.size()];
        for (int i = 0; i < arquivosFaltando.size(); i++) {
            System.out.println("Arquivo faltando: " + arquivosFaltando.get(i));
            strArquivos[i] = arquivosFaltando.get(i);
        }
        Quadro quadroPedido = new Quadro(Quadro.PEDIDO_ARQUIVO, strArquivos);
        String strJson = gson.toJson(quadroPedido);
        enviaSocket(strJson, ipFonte);
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
            System.out.println("JSON enviado: " + strJson);
        } catch (IOException e) {
            System.out.println("");
            if (tentativas < N_TENTATIVAS_ENVIO) {
                tentativas++;
                System.out.println("Quadro não enviado: " + strJson + "para " + ip);
                System.out.println("Reenviar. Esperar " + tentativas + "s...");
                espera(1000 * tentativas);
                enviaSocket(strJson, ip, tentativas);
            } else {
                System.out.println("Número de tentativas esgotado para o envio do quadro " + strJson);
            }
        }
    }

    private static void espera(int tempo) {
        try {
            Thread.sleep(tempo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
