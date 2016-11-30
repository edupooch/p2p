package sample.modelo;

/**
 * Created by edupooch on 30/11/2016.
 */
public class Quadro {

    public static final String PEDIDO_ARQUIVO = "par";
    public static final String PEDIDO_LISTA = "pli";
    public static final String RESPOSTA_ARQUIVO = "rar";
    public static final String RESPOSTA_LISTA = "rli";
    public static final String DELETAR_ARQUIVO = "del";
    public static final String ENCERRAR_CONEXAO = "sai";

    private String tipo;
    private String id;
    private String[] dados;

    public Quadro(String tipo, String id, String[] dados) {
        this.tipo = tipo;
        this.id = id;
        this.dados = dados;
    }

    public String getTipo() {
        return tipo;
    }

    public String getId() {
        return id;
    }

    public String[] getDados() {
        return dados;
    }


}
