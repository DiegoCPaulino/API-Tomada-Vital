package br.com.fiap.controller;

import br.com.fiap.dto.PacienteRequest;
import br.com.fiap.dto.PacienteResponse;
import br.com.fiap.service.ControleDeAcesso;
import br.com.fiap.service.PacienteService;
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
@RequestMapping("/api/pacientes")
public class PacienteResource {

    private final PacienteService pacienteService;
    private final ControleDeAcesso controleDeAcesso;

    public PacienteResource(PacienteService pacienteService, ControleDeAcesso controleDeAcesso) {
        this.pacienteService = pacienteService;
        this.controleDeAcesso = controleDeAcesso;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PacienteResponse> cadastrar(@Valid @RequestBody PacienteRequest novoPaciente,
                                                      Authentication authentication) {
        PacienteResponse pacienteResponse = pacienteService.cadastrar(novoPaciente, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteResponse);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<PacienteResponse>> consultar(
            @RequestParam(required = false) String nome,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "nome") Pageable paginacao,
            Authentication authentication) {
        Page<PacienteResponse> pacientes = pacienteService.consultar(nome, paginacao, authentication);
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PacienteResponse> consultarPorCodigo(@PathVariable Long id, Authentication authentication) {
        PacienteResponse pacienteResponse = pacienteService.consultarPorCodigo(id);
        //--- RN15: a família só vê o paciente dela (operador e admin passam)
        controleDeAcesso.garantirAcesso(authentication, pacienteResponse.usuarioId());
        return ResponseEntity.ok(pacienteResponse);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PacienteResponse> atualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest paciente,
                                                      Authentication authentication) {
        //--- primeiro descobre o dono (404 se não existe), confere o acesso e só então altera
        PacienteResponse pacienteCadastrado = pacienteService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, pacienteCadastrado.usuarioId());

        PacienteResponse pacienteResponse = pacienteService.atualizar(id, paciente);
        return ResponseEntity.ok(pacienteResponse);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, Authentication authentication) {
        PacienteResponse pacienteCadastrado = pacienteService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, pacienteCadastrado.usuarioId());

        pacienteService.remover(id);
        return ResponseEntity.noContent().build();
    }

}
