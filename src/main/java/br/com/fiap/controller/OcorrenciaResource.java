package br.com.fiap.controller;

import br.com.fiap.dto.AtendimentoRequest;
import br.com.fiap.dto.AtendimentoResponse;
import br.com.fiap.dto.OcorrenciaRequest;
import br.com.fiap.dto.OcorrenciaResponse;
import br.com.fiap.dto.ResolverRequest;
import br.com.fiap.enums.StatusOcorrencia;
import br.com.fiap.service.ControleDeAcesso;
import br.com.fiap.service.OcorrenciaService;
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
public class OcorrenciaResource {

    private final OcorrenciaService ocorrenciaService;
    private final ControleDeAcesso controleDeAcesso;

    public OcorrenciaResource(OcorrenciaService ocorrenciaService, ControleDeAcesso controleDeAcesso) {
        this.ocorrenciaService = ocorrenciaService;
        this.controleDeAcesso = controleDeAcesso;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<OcorrenciaResponse>> registrarQueda(@Valid @RequestBody OcorrenciaRequest novaOcorrencia,
                                                                          Authentication authentication) {
        OcorrenciaResponse ocorrenciaResponse = ocorrenciaService.registrarQueda(novaOcorrencia, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(ocorrenciaResponse, authentication));
    }

    //--- sem paginação: a central precisa ver a fila inteira, na ordem
    @GetMapping(path = "/fila", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<OcorrenciaResponse>>> consultarFila(Authentication authentication) {
        List<EntityModel<OcorrenciaResponse>> fila = ocorrenciaService.consultarFila()
                .stream()
                .map(ocorrencia -> comLinks(ocorrencia, authentication))
                .toList();
        return ResponseEntity.ok(CollectionModel.of(fila,
                linkTo(methodOn(OcorrenciaResource.class).consultarFila(null)).withSelfRel()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<OcorrenciaResponse>>> consultar(
            @RequestParam(required = false) StatusOcorrencia status,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) Integer minutosMaximos,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "previsaoEsgotamento") Pageable paginacao,
            PagedResourcesAssembler<OcorrenciaResponse> assembler,
            Authentication authentication) {
        Page<OcorrenciaResponse> ocorrencias = ocorrenciaService.consultar(status, bairro, minutosMaximos, paginacao, authentication);
        return ResponseEntity.ok(assembler.toModel(ocorrencias, ocorrencia -> comLinks(ocorrencia, authentication)));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<OcorrenciaResponse>> consultarPorCodigo(@PathVariable Long id,
                                                                              Authentication authentication) {
        OcorrenciaResponse ocorrenciaResponse = ocorrenciaService.consultarPorCodigo(id, authentication);
        return ResponseEntity.ok(comLinks(ocorrenciaResponse, authentication));
    }

    @GetMapping(path = "/{id}/atendimentos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<AtendimentoResponse>>> consultarAtendimentos(@PathVariable Long id,
                                                                                                  Authentication authentication) {
        List<EntityModel<AtendimentoResponse>> atendimentos = ocorrenciaService.consultarAtendimentos(id, authentication)
                .stream()
                .map(this::comLinks)
                .toList();
        return ResponseEntity.ok(CollectionModel.of(atendimentos,
                linkTo(methodOn(OcorrenciaResource.class).consultarAtendimentos(id, null)).withSelfRel()));
    }

    @PostMapping(path = "/{id}/atendimentos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<AtendimentoResponse>> registrarAtendimento(@PathVariable Long id,
                                                                                 @Valid @RequestBody AtendimentoRequest novoAtendimento) {
        AtendimentoResponse atendimentoResponse = ocorrenciaService.registrarAtendimento(id, novoAtendimento);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(atendimentoResponse));
    }

    //--- o corpo é opcional (required = false): o operador pode só clicar em "resolver".
    //--- Sem "consumes": um POST sem corpo também não manda Content-Type, e com "consumes" daria 415
    @PostMapping(path = "/{id}/resolver", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<OcorrenciaResponse>> resolver(@PathVariable Long id,
                                                                    @Valid @RequestBody(required = false) ResolverRequest resolverRequest,
                                                                    Authentication authentication) {
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
