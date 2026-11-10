package br.com.fiap.controller;

import br.com.fiap.dto.OcorrenciaRequest;
import br.com.fiap.dto.OcorrenciaResponse;
import br.com.fiap.service.OcorrenciaService;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
