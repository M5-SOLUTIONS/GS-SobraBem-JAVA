package br.com.sobrabem.dto.response;

import br.com.sobrabem.ai.OrientacaoResultado;

import java.util.List;

public record OrientacaoResponse(String orientacao, List<String> fontes, String aviso) {

    public static OrientacaoResponse de(OrientacaoResultado r) {
        return new OrientacaoResponse(r.orientacao(), r.fontes() == null ? List.of() : r.fontes(), r.aviso());
    }
}