# Relatório de Uso de Semáforos em Concorrência

## 1. Introdução

A Parte 2 do trabalho foca na demonstração e correção de um problema clássico em sistemas concorrentes: a **condição de corrida (race condition)**. Utilizamos o mecanismo de semáforos para controlar o acesso a um recurso compartilhado, garantindo a exclusão mútua e a consistência dos dados.

Um semáforo controla o acesso a um recurso por meio de uma contagem de "permissões". Quando inicializado com uma única permissão (`Semaphore(1)`), ele funciona como um lock de exclusão mútua, já que apenas uma Thread conseguirá acessar o recurso ao mesmo tempo.

## 2. Objetivo e Implementação

O desafio do contador concorrente tem como objetivo principal:

1.  Demonstrar uma condição de corrida ao incrementar um contador estático compartilhado por múltiplas threads sem sincronização.
2.  Corrigir essa condição de corrida utilizando um semáforo binário.
3.  Comparar os resultados, o tempo de execução e discutir o impacto no *throughput* e as garantias conceituais.

### Configuração

-   **Total de Threads (T):** 8
-   **Incrementos por Thread (M):** 250.000
-   **Valor Esperado:** `T × M = 2.000.000`

### Modelo de Solução

A implementação em Java utilizou a classe `java.util.concurrent.Semaphore`. A versão corrigida usa:
```java
static final Semaphore sem = new Semaphore(1, true);
```
A configuração com `true` garante que o semáforo seja justo (*fair*), aplicando uma política **FIFO (First-In, First-Out)** na ordem de aquisição do recurso.

## 3. Resultados da Execução

Abaixo estão os resultados obtidos comparando a execução sem sincronização e a execução corrigida com o semáforo.

### 3.1. Versão Sem Sincronização (Condição de Corrida)

| Esperado  | Obtido  | Perdidos  | Tempo  |
|-----------|---------|-----------|--------|
| 2.000.000 | 624.065 | 1.375.935 | 0,046s |
| 2.000.000 | 489.796 | 1.510.204 | 0,051s |
| 2.000.000 | 564.438 | 1.435.562 | 0,041s |

**Análise:** A operação de incremento do contador (`count++`) é **não atômica**. Múltiplas threads tentam ler, modificar e escrever o valor simultaneamente, resultando em uma perda muito grande de incrementos (mais de 1,3 milhão perdidos em alguns testes). O valor final obtido é inconsistente e imprevisível.

### 3.2. Versão Corrigida Com Semáforo Binário

| Esperado  | Obtido    | Perdidos | Tempo   |
|-----------|-----------|----------|---------|
| 2.000.000 | 2.000.000 | 0        | 18,289s |
| 2.000.000 | 2.000.000 | 0        | 15,682s |

**Análise da Consistência:** O semáforo garantiu a **exclusão mútua** sobre a operação (o incremento do contador). O método `acquire()` bloqueou o acesso, permitindo que apenas uma thread por vez executasse `count++`, e `release()` liberou o recurso para a próxima thread. Isso assegurou que o valor final do contador fosse o valor esperado (`T × M`) e que o número de incrementos perdidos fosse zero.

## 4. Discussão: Throughput e Garantias de Memória

### Trade-off de Throughput

A comparação dos tempos de execução demonstra o *trade-off* imposto pela sincronização.

-   Embora a versão sem sincronização tenha rodado em menos de 0,06 segundos (mesmo sendo incorreta), a versão corrigida com semáforo exigiu mais de 15 segundos.
-   A sincronização eliminou o erro, mas em troca temos um custo de desempenho muito grande, resultando em uma redução de ***throughput***.
-   Essa redução ocorre porque a execução do código foi forçada a ser sequencial. O sistema teve que gerenciar 2.000.000 de operações de *lock* e *unlock* (`acquire` e `release`) do semáforo, e o tempo total ficou inflado se comparado a uma thread apenas realizando o mesmo trabalho, pois a sobrecarga da coordenação das threads e troca de contexto consumiram mais tempo que o cálculo puro.

### Garantias Happens-Before

O uso do `java.util.concurrent.Semaphore` não apenas garante a exclusão mútua, mas também fornece garantias cruciais de consistência de memória.

-   O mecanismo assegura a relação ***happens-before*** entre o método `release()` de uma thread e o método `acquire()` da thread seguinte.
-   Na prática, isso significa que todas as modificações de memória (como a escrita do novo valor do contador) feitas pela thread que está saindo da seção crítica (`release`) são **visíveis** para a thread que está entrando (`acquire`). Esta garantia é essencial para preservar a visibilidade e a ordem corretas das operações, garantindo que a thread que adquire o semáforo veja o valor mais recente e correto do contador.

## 5. Comparativo: Java Threads vs. Kotlin Coroutines com Mutex

O mesmo problema foi implementado em Kotlin utilizando coroutines e um `Mutex` (Mutual Exclusion), que serve quase como um semáforo binário.

### 5.1. Resultados da Execução (Kotlin Coroutines)

| Demonstração        | Valor Esperado | Valor Final | Tempo  |
|---------------------|----------------|-------------|--------|
| Condição de Corrida | 2.000.000      | 480.252     | 142 ms |
| Solução com Mutex   | 2.000.000      | 2.000.000   | 417 ms |

### 5.2. Análise da Diferença de Desempenho

A diferença de desempenho entre a solução Java com `Semaphore` (mais de 15 segundos) e a solução Kotlin com `Mutex` (417 ms) é muito grande e ilustra as vantagens da concorrência estruturada com coroutines (uma implementação de Threads N para M).

1.  **Threads de SO vs. Coroutines Leves:**
    *   **Java `Thread`:** Cada `Thread` em Java é mapeada diretamente para uma thread do sistema operacional (SO). Threads de SO são recursos caros. A criação, o gerenciamento e, principalmente, a **troca de contexto** entre elas são operações lentas, pois exigem a intervenção do escalonador do SO.
    *   **Kotlin `Coroutine`:** Coroutines são "threads virtuais" ou "threads leves" gerenciadas pelo próprio runtime do Kotlin. Milhares de coroutines podem ser executadas em um pequeno pool de threads de SO (como o `Dispatchers.Default`). A troca de contexto entre coroutines é muito mais rápida, pois acontece no nível da aplicação, sem a sobrecarga de chamadas ao sistema.

2.  **Bloqueio vs. Suspensão:**
    *   **Bloqueio (`Semaphore.acquire()`):** Quando uma `Thread` Java chama `acquire()` e o semáforo não está disponível, a **thread inteira é bloqueada** pelo SO. Ela fica em estado de espera (waiting), sem poder realizar qualquer outro trabalho, consumindo memória e recursos do sistema até ser notificada. Com 8 threads competindo intensamente pelo mesmo lock, o sistema gasta a maior parte do tempo trocando o contexto entre essas threads bloqueadas.
    *   **Suspensão (`Mutex.withLock`):** Quando uma coroutine tenta adquirir um `Mutex` e ele está ocupado, ela **suspende**. A suspensão não bloqueia a thread do SO subjacente. Em vez disso, a coroutine salva seu estado e libera a thread para executar outra coroutine. Quando o `Mutex` é liberado, a coroutine suspensa é agendada para continuar sua execução em qualquer thread disponível no pool.

### Conclusão do Comparativo

A abordagem de suspensão das coroutines é muito mais eficiente para cenários com alta contenção de locks (como este). Em vez de ter 8 threads de SO caras paradas e esperando, temos um pool de threads que está sempre ocupado executando o trabalho disponível. A sobrecarga de gerenciar o bloqueio é drasticamente reduzida, resultando em um *throughput* muito maior e um tempo de execução extremamente menor (417 ms vs. ~15.000 ms). Isso demonstra o poder dos modelos de concorrência modernos e não-bloqueantes.
