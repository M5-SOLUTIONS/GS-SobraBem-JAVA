package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.dto.request.UsuarioUpdateRequest;
import br.com.sobrabem.dto.response.UsuarioResponse;
import br.com.sobrabem.exception.CodigoErro;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.DoacaoRepository;
import br.com.sobrabem.repository.ReservaRepository;
import br.com.sobrabem.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final DoacaoRepository doacoes;
    private final ReservaRepository reservas;

    public UsuarioService(UsuarioRepository usuarios, DoacaoRepository doacoes, ReservaRepository reservas) {
        this.usuarios = usuarios;
        this.doacoes = doacoes;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse atualizarMe(Long id, UsuarioUpdateRequest req) {
        Usuario usuario = buscarEntidade(id);
        if (usuario.getPapel() == Papel.DOADOR && AuthService.isBlank(req.endereco())) {
            throw NegocioException.validacao("endereco", "é obrigatório para doador");
        }
        // mesma regra do registro: endereço é o local de retirada, só existe para doador
        String endereco = usuario.getPapel() == Papel.DOADOR ? req.endereco().trim() : null;
        usuario.atualizarPerfil(req.nome().trim(), req.telefone(), endereco, req.cidade().trim(), req.uf());
        return UsuarioResponse.de(usuario);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Papel papel, Pageable pageable) {
        return usuarios.buscar(papel, pageable).map(UsuarioResponse::de);
    }

    @Transactional
    public void excluir(Long id) {
        Usuario usuario = buscarEntidade(id);
        if (doacoes.existsByDoadorId(id) || reservas.existsByReceptorId(id)) {
            throw new NegocioException(CodigoErro.REGISTRO_EM_USO,
                    "O usuário tem doações ou reservas e não pode ser excluído");
        }
        usuarios.delete(usuario);
    }

    private Usuario buscarEntidade(Long id) {
        return usuarios.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado"));
    }
}