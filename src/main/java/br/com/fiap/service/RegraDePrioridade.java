package br.com.fiap.service;

import br.com.fiap.enums.Prioridade;

import java.time.Duration;
import java.time.LocalDateTime;

//--- A regra de negócio da Tomada Vital, a mesma do Oracle, do app e do Arduino:
//--- quando a luz cai, a bateria do aparelho aguenta "autonomia x bateria / 100" minutos.
//--- Somando isso ao horário da queda, sabemos a hora em que a bateria acaba (previsão de esgotamento).
//--- Quanto menos minutos faltam até essa hora, mais urgente é o atendimento:
//---   até 120 minutos (ou aparelho sem bateria) = CRITICA · de 121 a 360 = ALTA · acima de 360 = MODERADA
//--- Classe só com métodos estáticos e sem Spring: é conta pura, fácil de testar com JUnit.
public class RegraDePrioridade {

    //--- quanto a bateria aguenta no momento da queda. Ex.: 480 min com 20% = 96 min
    public static int calcularMinutosNoAlerta(int autonomiaMinutos, int bateriaPercentual) {
        return (int) Math.round(autonomiaMinutos * bateriaPercentual / 100.0); //--- 100.0 para a divisão não perder os decimais antes do arredondamento
    }

    //--- o horário em que a bateria acaba: é por ele que a fila é ordenada
    public static LocalDateTime calcularPrevisaoEsgotamento(LocalDateTime inicio, int minutosNoAlerta) {
        return inicio.plusMinutes(minutosNoAlerta);
    }

    //--- minutos entre a referência (agora, ou o fim da ocorrência) e a previsão; depois da previsão fica 0, nunca negativo
    public static int calcularMinutosRestantes(LocalDateTime previsaoEsgotamento, LocalDateTime referencia) {
        long minutos = Duration.between(referencia, previsaoEsgotamento).toMinutes();
        return (int) Math.max(0, minutos);
    }

    //--- aparelho com autonomia 0 é sempre CRITICA: sem bateria, o paciente fica sem o aparelho na hora em que a luz cai
    public static Prioridade classificar(int autonomiaMinutos, int minutosRestantes) {
        if (autonomiaMinutos == 0 || minutosRestantes <= 120) {
            return Prioridade.CRITICA;
        }
        if (minutosRestantes <= 360) {
            return Prioridade.ALTA;
        }
        return Prioridade.MODERADA;
    }

}
