package br.com.sobrabem.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Na subida, lê o corpus, gera os embeddings e monta o índice em memória.
 * Só existe no modo gemini: no modo fake não há chave e a API não pode chamar o Gemini.
 */
@Configuration
@ConditionalOnProperty(name = "sobrabem.ia.modo", havingValue = "gemini")
public class CorpusLoader {

    private static final Logger log = LoggerFactory.getLogger(CorpusLoader.class);

    // arquivo -> categoriaId (ids fixos da V3__categorias.sql).
    // O id vai como texto para o filtro comparar "3" com "3", sem risco de Long contra Integer.
    static final Map<String, String> CATEGORIA_POR_ARQUIVO = Map.of(
            "paes-bolos.md", "1",
            "hortifruti.md", "2",
            "laticinios.md", "3",
            "refeicoes.md", "4",
            "mercearia.md", "5");

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) throws IOException {
        List<Document> trechos = new ArrayList<>();
        for (Map.Entry<String, String> e : CATEGORIA_POR_ARQUIVO.entrySet()) {
            String arquivo = e.getKey();
            String markdown = new ClassPathResource("corpus/" + arquivo).getContentAsString(StandardCharsets.UTF_8);
            for (String secao : secoes(markdown)) {
                trechos.add(new Document(secao, Map.of("categoriaId", e.getValue(), "arquivo", arquivo)));
            }
        }

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(trechos);
        log.info("Corpus indexado: {} arquivos, {} trechos", CATEGORIA_POR_ARQUIVO.size(), trechos.size());
        return store;
    }

    /**
     * Cada seção "## " do markdown vira um trecho, prefixado com o título "# " do arquivo.
     * Uma seção = um alimento, então a busca devolve exatamente a regra daquele alimento.
     */
    static List<String> secoes(String markdown) {
        List<String> secoes = new ArrayList<>();
        String tituloArquivo = "";
        StringBuilder atual = null;
        for (String linha : markdown.split("\\R")) {
            if (linha.startsWith("# ")) {
                tituloArquivo = linha.substring(2).trim();
            } else if (linha.startsWith("## ")) {
                if (atual != null) {
                    secoes.add(atual.toString().trim());
                }
                atual = new StringBuilder(tituloArquivo).append(" - ").append(linha.substring(3).trim()).append('\n');
            } else if (atual != null) {
                atual.append(linha).append('\n');
            }
        }
        if (atual != null) {
            secoes.add(atual.toString().trim());
        }
        return secoes;
    }
}