package com.maruko

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis

// --- Configuração ---
const val NUM_COROUTINES = 8
const val INCREMENTOS_POR_COROUTINE = 250000
const val VALOR_ESPERADO = NUM_COROUTINES * INCREMENTOS_POR_COROUTINE

/*
Contadores declarados no top-level (como globais)
para serem compartilhados por todas as coroutines.
*/
var contadorProblema = 0
var contadorSolucao = 0

// O Mutex servirá como o semáforo binário.
val mutex = Mutex()

fun main() = runBlocking { // runBlocking inicia o ambiente de coroutines

    println("Iniciando Demonstração: CONDIÇÃO DE CORRIDA (Kotlin)")
    println("---------------------------------------------")

    val tempoProblema = measureTimeMillis {
        // Cria uma lista de coroutines
        val jobs = List(NUM_COROUTINES) {
            // launch(Dispatchers.Default) inicia a coroutine em um
            // pool de threads compartilhado, permitindo paralelismo real.
            // seria uma implementação de n para m, logo cada coroutine é uma thread virtual
            // que serão executadas em vcárias threads
            launch(Dispatchers.Default) {
                for (i in 1..INCREMENTOS_POR_COROUTINE) {
                    val valorLocal = contadorProblema
                    contadorProblema = valorLocal + 1
                }
            }
        }
        jobs.joinAll() // Espera todas as coroutines terminarem
    }

    println("Valor Esperado:          $VALOR_ESPERADO")
    println("Valor Final (Problema):  $contadorProblema")
    println("Tempo (Problema):        ${tempoProblema} ms\n")

    // --- 2. A Solução: Mutex (Semáforo Binário) ---

    println("Iniciando Demonstração: SOLUÇÃO COM MUTEX")
    println("---------------------------------------------")

    val tempoSolucao = measureTimeMillis {
        val jobs = List(NUM_COROUTINES) {
            launch(Dispatchers.Default) {
                for (i in 1..INCREMENTOS_POR_COROUTINE) {

                    // mutex.withLock { ... }
                    // Garante que apenas UMA coroutine por vez
                    // possa executar este bloco de código.

                    mutex.withLock {
                        val valorLocal = contadorSolucao
                        contadorSolucao = valorLocal + 1
                    }
                }
            }
        }
        jobs.joinAll() // Espera todas terminarem
    }

    println("Valor Esperado:          $VALOR_ESPERADO")
    println("Valor Final (Solução):   $contadorSolucao")
    println("Tempo (Solução):         ${tempoSolucao} ms")
}