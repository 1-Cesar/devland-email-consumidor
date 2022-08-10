package br.com.dbc.emailconsumer.dto;

import br.com.dbc.emailconsumer.enums.TipoMensagem;
import lombok.Data;

@Data
public class EmailDto {

    private Integer idUsuario;

    private String nome;

    private String email;

    private String foto;

    TipoMensagem tipoMensagem;
}
