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
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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
    public ResponseEntity<EntityModel<EquipamentoResponse>> cadastrar(@Valid @RequestBody EquipamentoRequest novoEquipamento,
                                                         Authentication authentication) {
        EquipamentoResponse equipamentoResponse = equipamentoService.cadastrar(novoEquipamento, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(equipamentoResponse));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<EquipamentoResponse>>> consultar(
            @RequestParam(required = false) Long pacienteId,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "codigoDispositivo") Pageable paginacao,
            PagedResourcesAssembler<EquipamentoResponse> assembler,
            Authentication authentication) {
        Page<EquipamentoResponse> equipamentos = equipamentoService.consultar(pacienteId, paginacao, authentication);
        //--- o assembler transforma a Page em PagedModel (HAL): cada item com os seus links e, no fim, "page"
        //--- (tamanho, total, página atual) e os links first/prev/next/last para o app navegar entre as páginas
        return ResponseEntity.ok(assembler.toModel(equipamentos, this::comLinks));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<EquipamentoResponse>> consultarPorCodigo(@PathVariable Long id, Authentication authentication) {
        EquipamentoResponse equipamentoResponse = equipamentoService.consultarPorCodigo(id);
        //--- RN15: a família só vê o equipamento dos seus pacientes (operador e admin passam)
        controleDeAcesso.garantirAcesso(authentication, equipamentoResponse.usuarioId());
        return ResponseEntity.ok(comLinks(equipamentoResponse));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<EquipamentoResponse>> atualizar(@PathVariable Long id, @Valid @RequestBody EquipamentoRequest equipamento,
                                                         Authentication authentication) {
        //--- primeiro descobre o dono (404 se não existe), confere o acesso e só então altera
        EquipamentoResponse equipamentoCadastrado = equipamentoService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, equipamentoCadastrado.usuarioId());

        EquipamentoResponse equipamentoResponse = equipamentoService.atualizar(id, equipamento);
        return ResponseEntity.ok(comLinks(equipamentoResponse));
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, Authentication authentication) {
        EquipamentoResponse equipamentoCadastrado = equipamentoService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, equipamentoCadastrado.usuarioId());

        equipamentoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    //--- HATEOAS: self (o próprio equipamento) e paciente (quem usa o aparelho)
    private EntityModel<EquipamentoResponse> comLinks(EquipamentoResponse equipamento) {
        return EntityModel.of(equipamento,
                linkTo(methodOn(EquipamentoResource.class).consultarPorCodigo(equipamento.id(), null)).withSelfRel(),
                linkTo(methodOn(PacienteResource.class).consultarPorCodigo(equipamento.pacienteId(), null)).withRel("paciente"));
    }

}
