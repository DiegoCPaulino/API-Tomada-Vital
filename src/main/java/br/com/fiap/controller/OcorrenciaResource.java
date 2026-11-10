package br.com.fiap.controller;

import br.com.fiap.dto.OcorrenciaRequest;
import br.com.fiap.dto.OcorrenciaResponse;
import br.com.fiap.enums.StatusOcorrencia;
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

    public OcorrenciaResource(OcorrenciaService ocorrenciaService) {
        this.ocorrenciaService = ocorrenciaService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<OcorrenciaResponse>> registrarQueda(@Valid @RequestBody OcorrenciaRequest novaOcorrencia,
                                                                          Authentication authentication) {
        OcorrenciaResponse ocorrenciaResponse = ocorrenciaService.registrarQueda(novaOcorrencia, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(comLinks(ocorrenciaResponse));
    }

    //--- sem paginação: a central precisa ver a fila inteira, na ordem
    @GetMapping(path = "/fila", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<OcorrenciaResponse>>> consultarFila(Authentication authentication) {
        List<EntityModel<OcorrenciaResponse>> fila = ocorrenciaService.consultarFila()
                .stream()
                .map(this::comLinks)
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
        return ResponseEntity.ok(assembler.toModel(ocorrencias, this::comLinks));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<OcorrenciaResponse>> consultarPorCodigo(@PathVariable Long id,
                                                                              Authentication authentication) {
        OcorrenciaResponse ocorrenciaResponse = ocorrenciaService.consultarPorCodigo(id, authentication);
        return ResponseEntity.ok(comLinks(ocorrenciaResponse));
    }

    //--- HATEOAS: self (a própria ocorrência), equipamento (o aparelho que avisou) e paciente (quem precisa de ajuda).
    //--- Os parâmetros que não fazem parte da URL (Authentication) vão como null
    private EntityModel<OcorrenciaResponse> comLinks(OcorrenciaResponse ocorrencia) {
        return EntityModel.of(ocorrencia,
                linkTo(methodOn(OcorrenciaResource.class).consultarPorCodigo(ocorrencia.id(), null)).withSelfRel(),
                linkTo(methodOn(EquipamentoResource.class).consultarPorCodigo(ocorrencia.equipamentoId(), null)).withRel("equipamento"),
                linkTo(methodOn(PacienteResource.class).consultarPorCodigo(ocorrencia.pacienteId(), null)).withRel("paciente"));
    }

}
