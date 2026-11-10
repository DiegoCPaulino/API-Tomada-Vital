package br.com.fiap.controller;

import br.com.fiap.dto.EquipamentoRequest;
import br.com.fiap.dto.EquipamentoResponse;
import br.com.fiap.service.ControleDeAcesso;
import br.com.fiap.service.EquipamentoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipamentos")
public class EquipamentoResource {

    private final EquipamentoService equipamentoService;
    private final ControleDeAcesso controleDeAcesso;

    public EquipamentoResource(EquipamentoService equipamentoService, ControleDeAcesso controleDeAcesso) {
        this.equipamentoService = equipamentoService;
        this.controleDeAcesso = controleDeAcesso;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipamentoResponse> cadastrar(@Valid @RequestBody EquipamentoRequest novoEquipamento,
                                                         Authentication authentication) {
        EquipamentoResponse equipamentoResponse = equipamentoService.cadastrar(novoEquipamento, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(equipamentoResponse);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EquipamentoResponse>> consultar(
            @RequestParam(required = false) Long pacienteId,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "codigoDispositivo") Pageable paginacao,
            Authentication authentication) {
        Page<EquipamentoResponse> equipamentos = equipamentoService.consultar(pacienteId, paginacao, authentication);
        return ResponseEntity.ok(equipamentos);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipamentoResponse> consultarPorCodigo(@PathVariable Long id, Authentication authentication) {
        EquipamentoResponse equipamentoResponse = equipamentoService.consultarPorCodigo(id);
        //--- RN15: a família só vê o equipamento dos seus pacientes (operador e admin passam)
        controleDeAcesso.garantirAcesso(authentication, equipamentoResponse.usuarioId());
        return ResponseEntity.ok(equipamentoResponse);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EquipamentoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody EquipamentoRequest equipamento,
                                                         Authentication authentication) {
        //--- primeiro descobre o dono (404 se não existe), confere o acesso e só então altera
        EquipamentoResponse equipamentoCadastrado = equipamentoService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, equipamentoCadastrado.usuarioId());

        EquipamentoResponse equipamentoResponse = equipamentoService.atualizar(id, equipamento);
        return ResponseEntity.ok(equipamentoResponse);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, Authentication authentication) {
        EquipamentoResponse equipamentoCadastrado = equipamentoService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, equipamentoCadastrado.usuarioId());

        equipamentoService.remover(id);
        return ResponseEntity.noContent().build();
    }

}
