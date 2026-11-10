package br.com.fiap.service;

import br.com.fiap.enums.Prioridade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

//--- teste de unidade puro: a regra não usa Spring nem banco, então não precisa subir a aplicação
public class RegraDePrioridadeTest {

    @Test
    public void dadoAutonomia480EBateria20Calcular96MinutosECritica() {
        int minutos = RegraDePrioridade.calcularMinutosNoAlerta(480, 20);

        Assertions.assertEquals(96, minutos);
        Assertions.assertEquals(Prioridade.CRITICA, RegraDePrioridade.classificar(480, minutos));
    }

    @Test
    public void dadoAutonomia480EBateria60Calcular288MinutosEAlta() {
        int minutos = RegraDePrioridade.calcularMinutosNoAlerta(480, 60);

        Assertions.assertEquals(288, minutos);
        Assertions.assertEquals(Prioridade.ALTA, RegraDePrioridade.classificar(480, minutos));
    }

    @Test
    public void dadoAutonomia480EBateria100Calcular480MinutosEModerada() {
        int minutos = RegraDePrioridade.calcularMinutosNoAlerta(480, 100);

        Assertions.assertEquals(480, minutos);
        Assertions.assertEquals(Prioridade.MODERADA, RegraDePrioridade.classificar(480, minutos));
    }

    @Test
    public void dado120MinutosRestantesAindaECritica() {
        Assertions.assertEquals(Prioridade.CRITICA, RegraDePrioridade.classificar(480, 120));
    }

    @Test
    public void dado121MinutosRestantesJaEAlta() {
        Assertions.assertEquals(Prioridade.ALTA, RegraDePrioridade.classificar(480, 121));
    }

    @Test
    public void dado360MinutosRestantesAindaEAlta() {
        Assertions.assertEquals(Prioridade.ALTA, RegraDePrioridade.classificar(480, 360));
    }

    @Test
    public void dado361MinutosRestantesJaEModerada() {
        Assertions.assertEquals(Prioridade.MODERADA, RegraDePrioridade.classificar(480, 361));
    }

    @Test
    public void dadoAparelhoComAutonomiaZeroComQualquerBateriaECritica() {
        Assertions.assertEquals(0, RegraDePrioridade.calcularMinutosNoAlerta(0, 0));
        Assertions.assertEquals(0, RegraDePrioridade.calcularMinutosNoAlerta(0, 50));
        Assertions.assertEquals(0, RegraDePrioridade.calcularMinutosNoAlerta(0, 100));
        Assertions.assertEquals(Prioridade.CRITICA, RegraDePrioridade.classificar(0, 0));
        //--- mesmo que algum valor de minutos chegasse aqui, autonomia 0 continua CRITICA
        Assertions.assertEquals(Prioridade.CRITICA, RegraDePrioridade.classificar(0, 500));
    }

    @Test
    public void dadaReferenciaAntesDaPrevisaoCalcularMinutosQueFaltam() {
        LocalDateTime referencia = LocalDateTime.of(2026, 11, 23, 14, 40);
        LocalDateTime previsao = LocalDateTime.of(2026, 11, 23, 15, 38);

        Assertions.assertEquals(58, RegraDePrioridade.calcularMinutosRestantes(previsao, referencia));
    }

    @Test
    public void dadaReferenciaDepoisDaPrevisaoMinutosRestantesSaoZeroENuncaNegativos() {
        LocalDateTime previsao = LocalDateTime.of(2026, 11, 23, 14, 0);
        LocalDateTime referencia = LocalDateTime.of(2026, 11, 23, 15, 30);

        Assertions.assertEquals(0, RegraDePrioridade.calcularMinutosRestantes(previsao, referencia));
    }

    @Test
    public void dadoInicioEMinutosNoAlertaPrevisaoEInicioMaisMinutos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 23, 23, 30);

        //--- 96 minutos depois das 23:30 vira o dia: 01:06 do dia 24
        Assertions.assertEquals(LocalDateTime.of(2026, 11, 24, 1, 6),
                RegraDePrioridade.calcularPrevisaoEsgotamento(inicio, 96));
    }

}
