package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Categoria;
import br.com.sobrabem.dto.request.CategoriaRequest;
import br.com.sobrabem.dto.response.CategoriaResponse;
import br.com.sobrabem.exception.CodigoErro;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.CategoriaRepository;
import br.com.sobrabem.repository.DoacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static br.com.sobrabem.Fixtures.categoria;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    CategoriaRepository categorias;
    @Mock
    DoacaoRepository doacoes;
    @InjectMocks
    CategoriaService service;

    @Test
    void criarComNomeRepetidoDa409() {
        when(categorias.existsByNomeIgnoreCase("Laticínios")).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.criar(new CategoriaRequest(" Laticínios ", null, 6)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.CATEGORIA_DUPLICADA);
        verify(categorias, never()).save(any());
    }

    @Test
    void criarGravaONomeSemEspacos() {
        when(categorias.existsByNomeIgnoreCase("Bebidas")).thenReturn(false);
        when(categorias.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponse r = service.criar(new CategoriaRequest("  Bebidas ", "Sucos", 24));

        assertThat(r.nome()).isEqualTo("Bebidas");
        assertThat(r.prazoMaxHoras()).isEqualTo(24);
    }

    @Test
    void editarComONomeDeOutraDa409() {
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria(1L, 12)));
        when(categorias.existsByNomeIgnoreCaseAndIdNot("Laticínios", 1L)).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.atualizar(1L, new CategoriaRequest("Laticínios", null, 6)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.CATEGORIA_DUPLICADA);
    }

    @Test
    void excluirComDoacoesDa409() { // RN12
        Categoria c = categoria(1L, 12);
        when(categorias.findById(1L)).thenReturn(Optional.of(c));
        when(doacoes.existsByCategoriaId(1L)).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class, () -> service.excluir(1L));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.REGISTRO_EM_USO);
        verify(categorias, never()).delete(any());
    }

    @Test
    void buscarInexistenteDa404() {
        when(categorias.findById(99L)).thenReturn(Optional.empty());

        NegocioException e = assertThrows(NegocioException.class, () -> service.buscar(99L));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.NAO_ENCONTRADO);
    }
}