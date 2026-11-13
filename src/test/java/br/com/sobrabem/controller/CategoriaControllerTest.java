package br.com.sobrabem.controller;

import br.com.sobrabem.config.SecurityConfig;
import br.com.sobrabem.dto.response.CategoriaResponse;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.security.CustomAccessDeniedHandler;
import br.com.sobrabem.security.CustomEntryPoint;
import br.com.sobrabem.service.CategoriaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@Import({SecurityConfig.class, CustomEntryPoint.class, CustomAccessDeniedHandler.class})
@TestPropertySource(properties = "sobrabem.jwt.secret=segredo-so-para-teste-com-mais-de-32-caracteres")
class CategoriaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CategoriaService service;

    private static RequestPostProcessor comPapel(String papel) {
        return jwt().jwt(j -> j.subject("1").claim("papel", papel))
                .authorities(new SimpleGrantedAuthority("ROLE_" + papel));
    }

    @Test
    void semTokenDa401NoFormatoDoContrato() throws Exception {
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://sobrabem.app/erros/nao-autenticado"))
                .andExpect(jsonPath("$.instance").value("/api/categorias"))
                .andExpect(jsonPath("$.erros").isArray())
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    void receptorCriandoCategoriaDa403() throws Exception {
        mvc.perform(post("/api/categorias").with(comPapel("RECEPTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bebidas\",\"prazoMaxHoras\":6}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        verifyNoInteractions(service);
    }

    @Test
    void dadosInvalidosDa400ComOsCampos() throws Exception {
        mvc.perform(post("/api/categorias").with(comPapel("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"ab\",\"prazoMaxHoras\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros.length()").value(2));

        verifyNoInteractions(service);
    }

    @Test
    void adminCriaComLocation() throws Exception {
        when(service.criar(any())).thenReturn(new CategoriaResponse(6L, "Bebidas", null, 6));

        mvc.perform(post("/api/categorias").with(comPapel("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bebidas\",\"prazoMaxHoras\":6}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/categorias/6"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.descricao").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void qualquerLogadoLista() throws Exception {
        when(service.listar()).thenReturn(List.of(new CategoriaResponse(1L, "Pães e bolos", null, 12)));

        mvc.perform(get("/api/categorias").with(comPapel("DOADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Pães e bolos"));
    }

    @Test
    void negocioExceptionViraOStatusDoCodigo() throws Exception {
        when(service.buscar(999L)).thenThrow(NegocioException.naoEncontrado("Categoria não encontrada"));

        mvc.perform(get("/api/categorias/999").with(comPapel("DOADOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.detail").value("Categoria não encontrada"));
    }

    @Test
    void idNaoNumericoDa400NoCampoId() throws Exception {
        mvc.perform(get("/api/categorias/abc").with(comPapel("DOADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("id"));
    }
}