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
@RequestMapping("/api/pacientes")
public class PacienteResource {

    private final PacienteService pacienteService;
    private final ControleDeAcesso controleDeAcesso;

    public PacienteResource(PacienteService pacienteService, ControleDeAcesso controleDeAcesso) {
        this.pacienteService = pacienteService;
        this.controleDeAcesso = controleDeAcesso;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<PacienteResponse>> cadastrar(@Valid @RequestBody PacienteRequest novoPaciente,
                                                      Authentication authentication) {
        PacienteResponse pacienteResponse = pacienteService.cadastrar(novoPaciente, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(pacienteResponse));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<PacienteResponse>>> consultar(
            @RequestParam(required = false) String nome,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "nome") Pageable paginacao,
            PagedResourcesAssembler<PacienteResponse> assembler,
            Authentication authentication) {
        Page<PacienteResponse> pacientes = pacienteService.consultar(nome, paginacao, authentication);
        //--- o assembler transforma a Page em PagedModel (HAL): cada item com os seus links e, no fim, "page"
        //--- (tamanho, total, página atual) e os links first/prev/next/last para o app navegar entre as páginas
        return ResponseEntity.ok(assembler.toModel(pacientes, this::comLinks));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<PacienteResponse>> consultarPorCodigo(@PathVariable Long id, Authentication authentication) {
        PacienteResponse pacienteResponse = pacienteService.consultarPorCodigo(id);
        //--- RN15: a família só vê o paciente dela (operador e admin passam)
        controleDeAcesso.garantirAcesso(authentication, pacienteResponse.usuarioId());
        return ResponseEntity.ok(comLinks(pacienteResponse));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<PacienteResponse>> atualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest paciente,
                                                      Authentication authentication) {
        //--- primeiro descobre o dono (404 se não existe), confere o acesso e só então altera
        PacienteResponse pacienteCadastrado = pacienteService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, pacienteCadastrado.usuarioId());

        PacienteResponse pacienteResponse = pacienteService.atualizar(id, paciente);
        return ResponseEntity.ok(comLinks(pacienteResponse));
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, Authentication authentication) {
        PacienteResponse pacienteCadastrado = pacienteService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, pacienteCadastrado.usuarioId());

        pacienteService.remover(id);
        return ResponseEntity.noContent().build();
    }

    //--- HATEOAS: a resposta leva os links para onde o app pode ir a partir dela, sem montar URL na mão.
    //--- linkTo(methodOn(...)) gera o endereço a partir do próprio método do controller: se o caminho mudar, o link acompanha.
    //--- Os parâmetros que não fazem parte da URL (Pageable, assembler, Authentication) vão como null
    private EntityModel<PacienteResponse> comLinks(PacienteResponse paciente) {
        return EntityModel.of(paciente,
                linkTo(methodOn(PacienteResource.class).consultarPorCodigo(paciente.id(), null)).withSelfRel(),
                linkTo(methodOn(EquipamentoResource.class).consultar(paciente.id(), null, null, null)).withRel("equipamentos"));
    }

}
