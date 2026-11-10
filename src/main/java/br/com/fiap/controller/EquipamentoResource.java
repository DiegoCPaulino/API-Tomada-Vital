package br.com.fiap.controller;

import br.com.fiap.dto.EquipamentoRequest;
import br.com.fiap.dto.EquipamentoResponse;
import br.com.fiap.service.ControleDeAcesso;
import br.com.fiap.service.EquipamentoService;
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
@RequestMapping("/api/equipamentos")
@Tag(name = "Equipamentos", description = "Aparelhos elétricos de que o paciente depende (concentrador de oxigênio, ventilador...), " +
        "cada um com o dispositivo Tomada Vital (código TV-000) ligado na tomada e a autonomia da bateria, usada para calcular a fila.")
public class EquipamentoResource {

    private final EquipamentoService equipamentoService;
    private final ControleDeAcesso controleDeAcesso;

    public EquipamentoResource(EquipamentoService equipamentoService, ControleDeAcesso controleDeAcesso) {
        this.equipamentoService = equipamentoService;
        this.controleDeAcesso = controleDeAcesso;
    }

    @Operation(
            summary = "Cadastrar equipamento",
            description = "Cadastra um equipamento para um paciente. O código do dispositivo é único. " +
                    "Quem pode: FAMILIA (só para o seu paciente) e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Equipamento cadastrado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EquipamentoResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Falha na validação dos dados, tipo inexistente ou código do dispositivo já cadastrado.",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (operador) ou o paciente pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado.", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do equipamento a ser cadastrado.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EquipamentoRequest.class)))
    public ResponseEntity<EntityModel<EquipamentoResponse>> cadastrar(@Valid @RequestBody EquipamentoRequest novoEquipamento,
                                                                      @Parameter(hidden = true) Authentication authentication) {
        EquipamentoResponse equipamentoResponse = equipamentoService.cadastrar(novoEquipamento, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(equipamentoResponse));
    }

    @Operation(
            summary = "Listar equipamentos",
            description = "Lista os equipamentos com paginação e ordenação (padrão: 10 por página, pelo código do dispositivo), " +
                    "com filtro opcional por paciente. Quem pode: FAMILIA (recebe só os seus; pedir o paciente de outra " +
                    "família dá 403), OPERADOR e ADMIN (recebem todos).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página com os equipamentos (HAL: _embedded.equipamentos, _links e page)."
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "O paciente informado pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente informado não encontrado.", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<EquipamentoResponse>>> consultar(
            @Parameter(description = "Código do paciente (mostra só os equipamentos dele)") @RequestParam(required = false) Long pacienteId,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "codigoDispositivo") Pageable paginacao,
            @Parameter(hidden = true) PagedResourcesAssembler<EquipamentoResponse> assembler,
            @Parameter(hidden = true) Authentication authentication) {
        Page<EquipamentoResponse> equipamentos = equipamentoService.consultar(pacienteId, paginacao, authentication);
        //--- o assembler transforma a Page em PagedModel (HAL): cada item com os seus links e, no fim, "page"
        //--- (tamanho, total, página atual) e os links first/prev/next/last para o app navegar entre as páginas
        return ResponseEntity.ok(assembler.toModel(equipamentos, this::comLinks));
    }

    @Operation(
            summary = "Consultar equipamento pelo código",
            description = "Consulta um equipamento pelo identificador (resposta guardada em cache). " +
                    "Quem pode: FAMILIA (só o dos seus pacientes), OPERADOR e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Equipamento encontrado, com os links self e paciente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EquipamentoResponse.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "O equipamento pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Equipamento não encontrado.", content = @Content)
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<EquipamentoResponse>> consultarPorCodigo(
            @Parameter(required = true, description = "Código do equipamento") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        EquipamentoResponse equipamentoResponse = equipamentoService.consultarPorCodigo(id);
        //--- RN15: a família só vê o equipamento dos seus pacientes (operador e admin passam).
        //--- Fica aqui, DEPOIS do método cacheado: assim vale também quando a resposta vem do cache
        controleDeAcesso.garantirAcesso(authentication, equipamentoResponse.usuarioId());
        return ResponseEntity.ok(comLinks(equipamentoResponse));
    }

    @Operation(
            summary = "Atualizar equipamento",
            description = "Altera tipo, autonomia e código do dispositivo e limpa o equipamento do cache. O paciente não pode mudar. " +
                    "Quem pode: FAMILIA (só o seu) e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Equipamento atualizado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = EquipamentoResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Falha na validação, troca de paciente ou código do dispositivo de outro equipamento.",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (operador) ou o equipamento pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Equipamento não encontrado.", content = @Content)
    })
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Novos dados do equipamento (o pacienteId precisa ser o mesmo).",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EquipamentoRequest.class)))
    public ResponseEntity<EntityModel<EquipamentoResponse>> atualizar(
            @Parameter(required = true, description = "Código do equipamento") @PathVariable Long id,
            @Valid @RequestBody EquipamentoRequest equipamento,
            @Parameter(hidden = true) Authentication authentication) {
        //--- primeiro descobre o dono (404 se não existe), confere o acesso e só então altera
        EquipamentoResponse equipamentoCadastrado = equipamentoService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, equipamentoCadastrado.usuarioId());

        EquipamentoResponse equipamentoResponse = equipamentoService.atualizar(id, equipamento);
        return ResponseEntity.ok(comLinks(equipamentoResponse));
    }

    @Operation(
            summary = "Remover equipamento",
            description = "Apaga um equipamento e o limpa do cache. Equipamento que já teve queda é histórico e não pode ser apagado. " +
                    "Quem pode: FAMILIA (só o seu) e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Equipamento removido com sucesso.", content = @Content),
            @ApiResponse(responseCode = "400", description = "O equipamento tem histórico de quedas.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (operador) ou o equipamento pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Equipamento não encontrado.", content = @Content)
    })
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> remover(
            @Parameter(required = true, description = "Código do equipamento") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        EquipamentoResponse equipamentoCadastrado = equipamentoService.consultarPorCodigo(id);
        controleDeAcesso.garantirAcesso(authentication, equipamentoCadastrado.usuarioId());

        equipamentoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    //--- HATEOAS: self (o próprio equipamento) e paciente (quem usa o aparelho).
    //--- Os parâmetros que não fazem parte da URL (Authentication) vão como null
    private EntityModel<EquipamentoResponse> comLinks(EquipamentoResponse equipamento) {
        return EntityModel.of(equipamento,
                linkTo(methodOn(EquipamentoResource.class).consultarPorCodigo(equipamento.id(), null)).withSelfRel(),
                linkTo(methodOn(PacienteResource.class).consultarPorCodigo(equipamento.pacienteId(), null)).withRel("paciente"));
    }

}
