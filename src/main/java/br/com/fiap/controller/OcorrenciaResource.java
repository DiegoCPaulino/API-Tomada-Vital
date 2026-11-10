package br.com.fiap.controller;

import br.com.fiap.dto.AtendimentoRequest;
import br.com.fiap.dto.AtendimentoResponse;
import br.com.fiap.dto.OcorrenciaRequest;
import br.com.fiap.dto.OcorrenciaResponse;
import br.com.fiap.dto.ResolverRequest;
import br.com.fiap.enums.StatusOcorrencia;
import br.com.fiap.service.ControleDeAcesso;
import br.com.fiap.service.OcorrenciaService;
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
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/ocorrencias")
@Tag(name = "Ocorrências", description = "Quedas de energia avisadas pelo dispositivo Tomada Vital e a fila de atendimento: " +
        "quem tem menos bateria é atendido primeiro. Os minutos restantes e a prioridade são calculados em cada resposta. " +
        "Os links de ação (iniciar-atendimento, registrar-atendimento, resolver) mudam conforme o status e o papel.")
public class OcorrenciaResource {

    private final OcorrenciaService ocorrenciaService;
    private final ControleDeAcesso controleDeAcesso;

    public OcorrenciaResource(OcorrenciaService ocorrenciaService, ControleDeAcesso controleDeAcesso) {
        this.ocorrenciaService = ocorrenciaService;
        this.controleDeAcesso = controleDeAcesso;
    }

    @Operation(
            summary = "Registrar queda de energia",
            description = "Registra a queda avisada para um equipamento. É o mesmo corpo que o dispositivo IoT envia pela rede " +
                    "celular quando a luz cai (código do dispositivo e bateria no momento da queda). A API calcula os minutos " +
                    "no alerta (autonomia x bateria / 100) e a previsão de esgotamento, e devolve a posição na fila. " +
                    "Quem pode: FAMILIA (só para o seu equipamento) e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Queda registrada: ocorrência ABERTA, com prioridade e posição na fila.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OcorrenciaResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Falha na validação (bateria fora de 0 a 100) ou o dispositivo já tem uma queda em aberto.",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (operador) ou o equipamento pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Dispositivo não cadastrado.", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Aviso de queda (o mesmo corpo do dispositivo IoT).",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OcorrenciaRequest.class)))
    public ResponseEntity<EntityModel<OcorrenciaResponse>> registrarQueda(@Valid @RequestBody OcorrenciaRequest novaOcorrencia,
                                                                          @Parameter(hidden = true) Authentication authentication) {
        OcorrenciaResponse ocorrenciaResponse = ocorrenciaService.registrarQueda(novaOcorrencia, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(ocorrenciaResponse, authentication));
    }

    @Operation(
            summary = "Consultar a fila de atendimento",
            description = "Todas as ocorrências abertas (ABERTA e EM_ATENDIMENTO), na ordem de atendimento: quem fica sem " +
                    "bateria primeiro vem primeiro (previsão de esgotamento; no empate, quem caiu antes). Sem paginação: " +
                    "a fila inteira é a informação. Quem pode: OPERADOR e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "A fila (HAL: _embedded.ocorrencias, cada uma com a posição e os links de ação, e _links.self)."
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (a família não vê a fila da central).", content = @Content)
    })
    //--- sem paginação: a central precisa ver a fila inteira, na ordem
    @GetMapping(path = "/fila", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<OcorrenciaResponse>>> consultarFila(
            @Parameter(hidden = true) Authentication authentication) {
        List<EntityModel<OcorrenciaResponse>> fila = ocorrenciaService.consultarFila()
                .stream()
                .map(ocorrencia -> comLinks(ocorrencia, authentication))
                .toList();
        return ResponseEntity.ok(CollectionModel.of(fila,
                linkTo(methodOn(OcorrenciaResource.class).consultarFila(null)).withSelfRel()));
    }

    @Operation(
            summary = "Listar ocorrências com filtros",
            description = "Lista as ocorrências (abertas e resolvidas) com paginação e ordenação (padrão: 10 por página, pela " +
                    "previsão de esgotamento). Filtros opcionais e combináveis: status, bairro do paciente e minutosMaximos " +
                    "(só as abertas cuja bateria acaba em até X minutos; 120 = as críticas agora). " +
                    "Quem pode: FAMILIA (recebe só as suas), OPERADOR e ADMIN (recebem todas).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página com as ocorrências (HAL: _embedded.ocorrencias, _links e page)."
            ),
            @ApiResponse(responseCode = "400", description = "Status inexistente ou ordenação por um campo que não existe (ex.: prioridade).", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<OcorrenciaResponse>>> consultar(
            @Parameter(description = "Status da ocorrência (ABERTA, EM_ATENDIMENTO ou RESOLVIDA)") @RequestParam(required = false) StatusOcorrencia status,
            @Parameter(description = "Bairro do paciente (sem diferenciar maiúsculas)", example = "Centro") @RequestParam(required = false) String bairro,
            @Parameter(description = "Só as abertas cuja bateria acaba em até estes minutos (120 = as críticas)", example = "120") @RequestParam(required = false) Integer minutosMaximos,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "previsaoEsgotamento") Pageable paginacao,
            @Parameter(hidden = true) PagedResourcesAssembler<OcorrenciaResponse> assembler,
            @Parameter(hidden = true) Authentication authentication) {
        Page<OcorrenciaResponse> ocorrencias = ocorrenciaService.consultar(status, bairro, minutosMaximos, paginacao, authentication);
        //--- o assembler transforma a Page em PagedModel (HAL): cada item com os seus links e, no fim, "page"
        //--- e os links first/prev/next/last para navegar entre as páginas
        return ResponseEntity.ok(assembler.toModel(ocorrencias, ocorrencia -> comLinks(ocorrencia, authentication)));
    }

    @Operation(
            summary = "Consultar ocorrência pelo código",
            description = "Consulta uma ocorrência pelo identificador, com os minutos restantes e a prioridade calculados agora " +
                    "(ou no fim, se resolvida) e a posição na fila. Sem cache: os minutos mudam a cada minuto. " +
                    "Quem pode: FAMILIA (só as dos seus pacientes, sem links de ação), OPERADOR e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ocorrência encontrada, com os links self, equipamento, paciente, atendimentos e as ações possíveis.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OcorrenciaResponse.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "A ocorrência pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada.", content = @Content)
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<OcorrenciaResponse>> consultarPorCodigo(
            @Parameter(required = true, description = "Código da ocorrência") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        OcorrenciaResponse ocorrenciaResponse = ocorrenciaService.consultarPorCodigo(id, authentication);
        return ResponseEntity.ok(comLinks(ocorrenciaResponse, authentication));
    }

    @Operation(
            summary = "Consultar os atendimentos da ocorrência",
            description = "A linha do tempo da ocorrência (cada passo da central), em ordem de data e hora. " +
                    "Quem pode: FAMILIA (só as dos seus pacientes), OPERADOR e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Os atendimentos (HAL: _embedded.atendimentos, cada um com o link ocorrencia, e _links.self)."
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "A ocorrência pertence a outra família.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada.", content = @Content)
    })
    @GetMapping(path = "/{id}/atendimentos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<AtendimentoResponse>>> consultarAtendimentos(
            @Parameter(required = true, description = "Código da ocorrência") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        List<EntityModel<AtendimentoResponse>> atendimentos = ocorrenciaService.consultarAtendimentos(id, authentication)
                .stream()
                .map(this::comLinks)
                .toList();
        return ResponseEntity.ok(CollectionModel.of(atendimentos,
                linkTo(methodOn(OcorrenciaResource.class).consultarAtendimentos(id, null)).withSelfRel()));
    }

    @Operation(
            summary = "Registrar atendimento",
            description = "Registra um passo da central na ocorrência (equipe a caminho, gerador entregue, paciente removido). " +
                    "O primeiro atendimento muda a ocorrência de ABERTA para EM_ATENDIMENTO. ENERGIA_RESTABELECIDA não é aceita " +
                    "aqui: entra pelo resolver. Quem pode: OPERADOR e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Atendimento registrado, com o link para a ocorrência.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AtendimentoResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Falha na validação, ação inexistente, ENERGIA_RESTABELECIDA ou ocorrência já resolvida.",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (a família não atende ocorrências).", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada.", content = @Content)
    })
    @PostMapping(path = "/{id}/atendimentos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "O passo do atendimento.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AtendimentoRequest.class)))
    public ResponseEntity<EntityModel<AtendimentoResponse>> registrarAtendimento(
            @Parameter(required = true, description = "Código da ocorrência") @PathVariable Long id,
            @Valid @RequestBody AtendimentoRequest novoAtendimento) {
        AtendimentoResponse atendimentoResponse = ocorrenciaService.registrarAtendimento(id, novoAtendimento);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(atendimentoResponse));
    }

    @Operation(
            summary = "Resolver ocorrência",
            description = "Encerra a ocorrência quando a energia volta: preenche o fim, muda para RESOLVIDA e acrescenta o " +
                    "atendimento ENERGIA_RESTABELECIDA com a observação enviada (ou \"Energia restabelecida\"). Corpo opcional. " +
                    "Só resolve ocorrência EM_ATENDIMENTO. Quem pode: OPERADOR e ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ocorrência resolvida (sem posição na fila e sem links de ação).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OcorrenciaResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Falha na validação, ocorrência ainda ABERTA (ninguém atendeu) ou já resolvida.",
                    content = @Content
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado (token ausente, inválido ou vencido).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão (a família não resolve ocorrências).", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada.", content = @Content)
    })
    //--- o corpo é opcional (required = false): o operador pode só clicar em "resolver".
    //--- Sem "consumes": um POST sem corpo também não manda Content-Type, e com "consumes" daria 415
    @PostMapping(path = "/{id}/resolver", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Observação do encerramento (opcional).",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResolverRequest.class)))
    public ResponseEntity<EntityModel<OcorrenciaResponse>> resolver(
            @Parameter(required = true, description = "Código da ocorrência") @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolverRequest resolverRequest,
            @Parameter(hidden = true) Authentication authentication) {
        OcorrenciaResponse ocorrenciaResponse = ocorrenciaService.resolver(id, resolverRequest);
        return ResponseEntity.ok(comLinks(ocorrenciaResponse, authentication));
    }

    //--- HATEOAS mostra ao cliente o que ele pode fazer AGORA com esta ocorrência: o app e a tela da central
    //--- não precisam conhecer as regras, só olhar quais links vieram.
    //--- Sempre: self, equipamento, paciente e atendimentos (a linha do tempo).
    //--- Ações (contrato §4.4): só para quem pode atender (operador e admin) e conforme o status:
    //---   ABERTA -> iniciar-atendimento · EM_ATENDIMENTO -> registrar-atendimento e resolver · RESOLVIDA -> nenhuma.
    //--- Os parâmetros que não fazem parte da URL (corpo, Authentication) vão como null
    private EntityModel<OcorrenciaResponse> comLinks(OcorrenciaResponse ocorrencia, Authentication authentication) {
        EntityModel<OcorrenciaResponse> model = EntityModel.of(ocorrencia,
                linkTo(methodOn(OcorrenciaResource.class).consultarPorCodigo(ocorrencia.id(), null)).withSelfRel(),
                linkTo(methodOn(EquipamentoResource.class).consultarPorCodigo(ocorrencia.equipamentoId(), null)).withRel("equipamento"),
                linkTo(methodOn(PacienteResource.class).consultarPorCodigo(ocorrencia.pacienteId(), null)).withRel("paciente"),
                linkTo(methodOn(OcorrenciaResource.class).consultarAtendimentos(ocorrencia.id(), null)).withRel("atendimentos"));

        if (controleDeAcesso.podeAtender(authentication)) {
            if (ocorrencia.status() == StatusOcorrencia.ABERTA) {
                model.add(linkTo(methodOn(OcorrenciaResource.class).registrarAtendimento(ocorrencia.id(), null)).withRel("iniciar-atendimento"));
            } else if (ocorrencia.status() == StatusOcorrencia.EM_ATENDIMENTO) {
                model.add(linkTo(methodOn(OcorrenciaResource.class).registrarAtendimento(ocorrencia.id(), null)).withRel("registrar-atendimento"));
                model.add(linkTo(methodOn(OcorrenciaResource.class).resolver(ocorrencia.id(), null, null)).withRel("resolver"));
            }
        }

        return model;
    }

    //--- o atendimento não tem endpoint próprio: o link leva à ocorrência a que ele pertence
    private EntityModel<AtendimentoResponse> comLinks(AtendimentoResponse atendimento) {
        return EntityModel.of(atendimento,
                linkTo(methodOn(OcorrenciaResource.class).consultarPorCodigo(atendimento.ocorrenciaId(), null)).withRel("ocorrencia"));
    }

}
