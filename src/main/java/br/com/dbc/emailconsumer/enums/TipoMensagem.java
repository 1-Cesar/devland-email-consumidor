package br.com.dbc.emailconsumer.enums;

import java.util.Arrays;

public enum TipoMensagem {

    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    CADASTROINCOMPLETO("cadastro_incompleto"),
    MISSYOU("miss_you");

    private String tipoMensagem;

    TipoMensagem(String tipoMensagem) {
        this.tipoMensagem = tipoMensagem;
    }

    public String getTipo() {
        return tipoMensagem;
    }

    public static TipoMensagem ofTipo(String tipoMensagem) {
        return Arrays.stream(TipoMensagem.values())
                .filter(tp -> tp.getTipo().equals(tipoMensagem))
                .findFirst()
                .get();
    }
}
