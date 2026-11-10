package br.com.fiap.controller;

import br.com.fiap.dto.PacienteRequest;
import br.com.fiap.dto.PacienteResponse;
import br.com.fiap.service.ControleDeAcesso;
import br.com.fiap.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Pacientes", description = "Pessoas que dependem de um aparelho elétrico para viver. Cada paciente pertence a uma família " +
        "(usuário FAMILIA), que cadastra e acompanha os seus pelo app. Operador e admin veem todos.")
public class PacienteResource {

    private final PacienteService pacienteService;
    private final ControleDeAcesso controleDeAcesso;

    public PacienteResource(PacienteService pacienteService, ControleDeAcesso controleDeAcesso) {
        this.pacienteService = pacienteService;
        this.controleDeAcesso = controleDeAcesso;
    }

    @Operation(
            summary = "Cadastrar paciente",
            description = "Cadastra a pessoa que depende do aparelho elétrico, ligada à família logada (o dono vem do token). " +
                    "É feito antes de qualquer queda: o bairro e o telefone são o que a central usa para mandar a equipe. " +
                    "Quem pode: FAMILIA.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Paciente cadastrado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PacienteResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Falha na validação dos dados do paciente.",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (só a família cadastra paciente).", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do paciente a ser cadastrado.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PacienteRequest.class)))
    public ResponseEntity<EntityModel<PacienteResponse>> cadastrar(@Valid @RequestBody PacienteRequest novoPaciente,
                                                                   @Parameter(hidden = true) Authentication authentication) {
        PacienteResponse pacienteResponse = pacienteService.cadastrar(novoPaciente, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(pacienteResponse));
    }

    @Operation(
            summary = "Listar pacientes",
            description = "Lista os pacientes com paginação e ordenação (padrão: 10 por página, por nome), com filtro opcional por " +
                    "parte do nome. Quem pode: FAMILIA (recebe só os seus), OPERADOR e ADMIN (recebem todos).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página com os pacientes (HAL: _embedded.pacientes, _links e page)."
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão.", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<PacienteResponse>>> consultar(
            @Parameter(description = "Parte do nome do paciente (sem diferenciar maiúsculas)") @RequestParam(required = false) String nome,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "nome") Pageable paginacao,
            @Parameter(hidden = true) PagedResourcesAssembler<PacienteResponse> assembler,
            @Parameter(hidden = true) Authentication authentication) {
        Page<PacienteResponse> pacientes = pacienteService.consultar(nome, paginacao, authentication);
        //--- o assembler transforma a Page em PagedModel (HAL): cada item com os seus links e, no fim, "page"
        //--- (tamanho, total, página atual) e os links first/prev/next/last para o app navegar entre as páginas
        return ResponseEntity.ok(assembler.toModel(pacientes, this::comLinks));
    }

    @Operation(
            summary = "Consultar paciente pelo código",
            description = "Consulta um paciente pelo identificador (resposta guardada em cache). " +
                    "Quem pode: FAMILIA (só o seu), OPERADOR e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paciente encontrado, com os links self e equipamentos.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PacienteResponse.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "O paciente pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado.", content = @Content)
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<PacienteResponse>> consultarPorCodigo(
            @Parameter(required = true, description = "Código do paciente") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        PacienteResponse pacienteResponse = pacienteService.consultarPorCodigo(id);
        //--- RN15: a família só vê o paciente dela (operador e admin passam).
        //--- Fica aqui, DEPOIS do método cacheado: assim vale também quando a resposta vem do cache
        controleDeAcesso.garantirAcesso(authentication, pacienteResponse.usuarioId());
        return ResponseEntity.ok(comLinks(pacienteResponse));
    }

    @Operation(
            summary = "Atualizar paciente",
            description = "Altera os dados de um paciente (o dono não muda) e limpa o paciente do cache. " +
                    "Quem pode: FAMILIA (só o seu) e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paciente atualizado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PacienteResponse.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Falha na validação dos dados do paciente.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (operador) ou o paciente pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado.", content = @Content)
    })
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Novos dados do paciente.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PacienteRequest.class)))
    public ResponseEntity<EntityModel<PacienteResponse>> atualizar(
            @Parameter(required = true, description = "Código do paciente") @PathVariable Long id,
            @Valid @RequestBody PacienteRequest paciente,
            @Parameter(hidden = true) Authentication authentication) {
        //--- primeiro descobre o dono (404 se não existe), confere o acesso e só então altera
        PacienteResponse pacienteCadastrado = pacienteService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, pacienteCadastrado.usuarioId());

        PacienteResponse pacienteResponse = pacienteService.atualizar(id, paciente);
        return ResponseEntity.ok(comLinks(pacienteResponse));
    }

    @Operation(
            summary = "Remover paciente",
            description = "Apaga um paciente e o limpa do cache. Só apaga paciente sem equipamentos. " +
                    "Quem pode: FAMILIA (só o seu) e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente removido com sucesso.", content = @Content),
            @ApiResponse(responseCode = "400", description = "O paciente ainda tem equipamentos.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (operador) ou o paciente pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado.", content = @Content)
    })
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> remover(
            @Parameter(required = true, description = "Código do paciente") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
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
