package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Categoria;
import br.com.sobrabem.dto.request.CategoriaRequest;
import br.com.sobrabem.dto.response.CategoriaResponse;
import br.com.sobrabem.exception.CodigoErro;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.CategoriaRepository;
import br.com.sobrabem.repository.DoacaoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final DoacaoRepository doacoes;

    public CategoriaService(CategoriaRepository categorias, DoacaoRepository doacoes) {
        this.categorias = categorias;
        this.doacoes = doacoes;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categorias.findAll(Sort.by("nome")).stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.de(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest req) {
        String nome = req.nome().trim();
        if (categorias.existsByNomeIgnoreCase(nome)) throw duplicada();
        Categoria categoria = categorias.save(new Categoria(nome, req.descricao(), req.prazoMaxHoras()));
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest req) {
        Categoria categoria = buscarEntidade(id);
        String nome = req.nome().trim();
        if (categorias.existsByNomeIgnoreCaseAndIdNot(nome, id)) throw duplicada();
        // sem save(): a entidade está gerenciada e o Hibernate grava a mudança no commit
        categoria.atualizar(nome, req.descricao(), req.prazoMaxHoras());
        return CategoriaResponse.de(categoria);
    }

    /** só exclui se não houver doações nessa categoria. */
    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidade(id);
        if (doacoes.existsByCategoriaId(id)) {
            throw new NegocioException(CodigoErro.REGISTRO_EM_USO, "A categoria tem doações e não pode ser excluída");
        }
        categorias.delete(categoria);
    }

    private Categoria buscarEntidade(Long id) {
        return categorias.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Categoria não encontrada"));
    }

    private static NegocioException duplicada() {
        return new NegocioException(CodigoErro.CATEGORIA_DUPLICADA, "Já existe uma categoria com este nome");
    }
}