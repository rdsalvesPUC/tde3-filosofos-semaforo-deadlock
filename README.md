# TDE3 - Trabalho Final - Filósofos, Semana e Deadlocks

## Grupo:
- Rodrigo da Silva Alves
- Marco Alija Ramos
- Richard Mickael 

## Video de Explicações
[]()

## Parte 1 - Filósofos
### 1.1 Descrição do Problema
O Jantar dos Filósofos modela cinco filósofos sentados em uma mesa circular. Cada filósofo alterna entre três estados principais: Pensando Comendo Com fome

Para comer, cada filósofo precisa de dois garfos (esquerda e direita), que são recursos compartilhados entre vizinhos. Esse compartilhamento gera riscos de: Exclusão mútua Deadlock Inanição (starvation) Problemas de sincronização

No protocolo ingênuo (“pegar primeiro um garfo, depois o outro”), pode ocorrer um cenário em que: Todos os filósofos ficam com fome ao mesmo tempo. Todos pegam o garfo à esquerda. Todos passam a esperar o garfo à direita. Ninguém progride: impasse total (deadlock).

O deadlock ocorre quando todas as quatro condições de Coffman são satisfeitas simultaneamente: Exclusão mútua = Cada garfo só pode estar com um filósofo por vez.

Manter-e-esperar (hold and wait) = Um filósofo segura um garfo enquanto espera outro.

Não preempção = Os garfos não podem ser retirados à força; só são liberados voluntariamente.

Espera circular = Cada filósofo depende do próximo em um ciclo circular (0 espera o 1, o 1 espera o 2, ..., o 4 espera o 0).

Ao invés de usar hierarquia de recursos como você colocou no exemplo, vou utilizar um árbitro (tipo um garçom) central que controla o acesso aos garfos. Como funciona esse garçom: Quando um filósofo fica com fome, ele pede ao garçom permissão para comer. O garçom autoriza somente se os DOIS garfos do filósofo estiverem livres. Caso contrário, o filósofo entra em uma fila de espera e dorme. Quando um filósofo termina de comer, ele devolve os garfos ao garçom, que então verifica se alguém da fila pode ser atendido.

Desse jeito, o filósofo nunca chega a segurar apenas um garfo, o que impede a formação do ciclo de espera.

Condição de Coffman quebrada A solução com árbitro quebra a condição de Espera Circular: Não existe mais um ciclo de dependências entre os filósofos. Toda espera acontece exclusivamente em relação ao garçom, que decide quem pode entrar na área crítica. O garçom só libera os dois garfos simultaneamente, impedindo dependências encadeadas.

### 1.2 Pseudocódigo
#### Estados
```java
const N = 5                  
enum Estado { PENSANDO, COM_FOME, COMENDO }

Estado estado[N]             
bool garfolivre[N]           

fila pedidos                 

mutex m                      
condicao podecomer[N]
```

#### Funções do Árbitro
```java
func garfosdisponiveis(i):
    esquerda = i
    direita = (i + 1) mod N
    return garfolivre[esquerda] AND garfolivre[direita]

func reservargarfos(i):
    esquerda = i
    direita = (i + 1) mod N
    garfolivre[esquerda] = false
    garfolivre[direita]  = false

func liberargarfos(i):
    esquerda = i
    direita = (i + 1) mod N
    garfolivre[esquerda] = true
    garfolivre[direita]  = true

func tentaratenderFila():
    para cada filosofo j na fila 'pedidos' em ordem:
        se estado[j] == COM_FOME AND garfosDisponiveis(j):
            reservargarfos(j)
            estado[j] = COMENDO
            remover j da fila 'pedidos'
            sinalizar podecomer[j]
```
#### Processo do Filósofo
```java
processo filosofo(i):

enquanto verdadeiro:
    PENSAR()

    m.lock()
    estado[i] = COM_FOME
    enfileirar(pedidos, i)
    tentaratenderFila()

    enquanto estado[i] != COMENDO:
        esperar(podeComer[i], m)
    fim-enquanto

    m.unlock()

    COMER()

    m.lock()
    estado[i] = PENSANDO
    liberargarfos(i)
    tentaratenderFila()
    m.unlock()
```

## Parte 2 - Semáforos
### 2.1 Descrição do Problema
O Problema 2 foca na demonstração e correção de um problema clássico em sistemas concorrentes: a **condição de corrida (race condition)**. Utilizamos o mecanismo de semáforos para controlar o acesso a um recurso compartilhado, garantindo a exclusão mútua e a consistência dos dados.

Um semáforo controla o acesso a um recurso por meio de uma contagem de "permissões". Quando inicializado com uma única permissão (`Semaphore(1)`), ele funciona como um lock de exclusão mútua, já que apenas uma Thread conseguirá acessar o recurso ao mesmo tempo.

O desafio do contador concorrente tem como objetivo principal:
1.  Demonstrar uma condição de corrida ao incrementar um contador estático compartilhado por múltiplas threads sem sincronização.
2.  Corrigir essa condição de corrida utilizando um semáforo binário.
3.  Comparar os resultados, o tempo de execução e discutir o impacto no *throughput* e as garantias conceituais.

### 2.2 Implementação sem Semáforos
Aqui buscamos fazer um código em que muitas Threads vão compartilhar um mesmo recurso em uma operação que
apesar de parecer muito simples, consiste em 3 passos, ler, incrementar e então escrever o novo valor, sem um
tratamento adequado, o que acontece é que multiplas threads podem ler o mesmo valor, incrementar com base naquele valor lido no começo,
todas chegam ao meso resultado e o escrevem na variável, logo perdemos vários incrementos.
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CorridaSemControle {
    static class ContadorIncorreto {
        private int c = 0;

        public void incrementar() {
            c++;
        }

        public int getValor() {
            return c;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final int NUM_THREADS = 8;
        final int INCREMENTOS_POR_THREAD = 250_000;
        final int TOTAL_ESPERADO = NUM_THREADS * INCREMENTOS_POR_THREAD;

        System.out.println("--- Teste do Contador Incorreto ---");
        System.out.println("Total Esperado: " + TOTAL_ESPERADO);

        ContadorIncorreto contador = new ContadorIncorreto();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        long tempoInicial = System.nanoTime();
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTOS_POR_THREAD; j++) {
                    contador.incrementar();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        long tempoFinal = System.nanoTime();
        long tempoTotal = tempoFinal - tempoInicial;


        System.out.println("Resultado Final: " + contador.getValor());
        System.out.printf("Tempo Total: %.3fms%n", tempoTotal / 1e6);
    }
}
```
### 2.3 Implementação com Semáforos
Já nesse código nós criamos um Semáforo que funciona como um cadeado que tem apenas uma chave 
então quando uma Thread chama o acquire, ela pega essa chave e começa a fazer a incrementação, enquanto ela tá fazendo 
essa operação, todas as outras Threads ficam esperando, pois elas precisam esperar que a primeira thread libere a chave para
então começar a trabalhar.
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CorridaComSemaforo {
    static class ContadorComSemaforo {
        private final Semaphore semaforo = new Semaphore(1,true);
        private int c = 0;

        public void incrementar() {
            try {
                // Pede permissão para entrar
                semaforo.acquire();

                c++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Libera a permissão
                semaforo.release();
            }
        }

        public int getValor() {
            return c;
        }
    }


    public static void main(String[] args) throws InterruptedException {
        final int NUM_THREADS = 8;
        final int INCREMENTOS_POR_THREAD = 250_000;
        final int TOTAL_ESPERADO = NUM_THREADS * INCREMENTOS_POR_THREAD;

        System.out.println("--- Teste do Contador COM Semáforo ---");
        System.out.println("Total Esperado: " + TOTAL_ESPERADO);

        ContadorComSemaforo contador = new ContadorComSemaforo();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        long tempoInicial = System.nanoTime();

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTOS_POR_THREAD; j++) {
                    contador.incrementar();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long tempoFinal = System.nanoTime();
        long tempoTotal = tempoFinal - tempoInicial;

        System.out.println("Resultado Final: " + contador.getValor());
        System.out.printf("Tempo Total: %.3fms%n", tempoTotal / 1e6);

    }
}
```
### 2.4 Resultados
#### 2.4.1 Versão Sem Sincronização (Condição de Corrida)

| Esperado  | Obtido  | Perdidos  | Tempo  |
|-----------|---------|-----------|--------|
| 2.000.000 | 624.065 | 1.375.935 | 0,046s |
| 2.000.000 | 489.796 | 1.510.204 | 0,051s |
| 2.000.000 | 564.438 | 1.435.562 | 0,041s |

**Análise:** A operação de incremento do contador (`count++`) é **não atômica**. Múltiplas threads tentam ler, modificar e escrever o valor simultaneamente, resultando em uma perda muito grande de incrementos (mais de 1,3 milhão perdidos em alguns testes). O valor final obtido é inconsistente e imprevisível.

### 2.4.2 Versão Corrigida Com Semáforo Binário

| Esperado  | Obtido    | Perdidos | Tempo   |
|-----------|-----------|----------|---------|
| 2.000.000 | 2.000.000 | 0        | 18,289s |
| 2.000.000 | 2.000.000 | 0        | 15,682s |

**Análise da Consistência:** O semáforo garantiu a **exclusão mútua** sobre a operação (o incremento do contador). O método `acquire()` bloqueou o acesso, permitindo que apenas uma thread por vez executasse `count++`, e `release()` liberou o recurso para a próxima thread. Isso assegurou que o valor final do contador fosse o valor esperado (`T × M`) e que o número de incrementos perdidos fosse zero.

**Trade-off de Throughput**: A comparação dos tempos de execução demonstra o *trade-off* imposto pela sincronização.
-   Embora a versão sem sincronização tenha rodado em menos de 0,06 segundos (mesmo sendo incorreta), a versão corrigida com semáforo exigiu mais de 15 segundos.
-   A sincronização eliminou o erro, mas em troca temos um custo de desempenho muito grande, resultando em uma redução de ***throughput***.
-   Essa redução ocorre porque a execução do código foi forçada a ser sequencial. O sistema teve que gerenciar 2.000.000 de operações de *lock* e *unlock* (`acquire` e `release`) do semáforo, e o tempo total ficou inflado se comparado a uma thread apenas realizando o mesmo trabalho, pois a sobrecarga da coordenação das threads e troca de contexto consumiram mais tempo que o cálculo puro.

**Garantias Happens-Before**: O uso do `java.util.concurrent.Semaphore` não apenas garante a exclusão mútua, mas também fornece garantias cruciais de consistência de memória.
-   O mecanismo assegura a relação ***happens-before*** entre o método `release()` de uma thread e o método `acquire()` da thread seguinte.
-   Na prática, isso significa que todas as modificações de memória (como a escrita do novo valor do contador) feitas pela thread que está saindo da seção crítica (`release`) são **visíveis** para a thread que está entrando (`acquire`). Esta garantia é essencial para preservar a visibilidade e a ordem corretas das operações, garantindo que a thread que adquire o semáforo veja o valor mais recente e correto do contador.

### Extra: Java Threads vs. Kotlin Coroutines com Mutex
O mesmo problema foi implementado em Kotlin utilizando coroutines e um `Mutex` (Mutual Exclusion), que serve quase como um semáforo binário.

#### Resultados da Execução (Kotlin Coroutines)

| Demonstração        | Valor Esperado | Valor Final | Tempo  |
|---------------------|----------------|-------------|--------|
| Condição de Corrida | 2.000.000      | 480.252     | 142 ms |
| Solução com Mutex   | 2.000.000      | 2.000.000   | 417 ms |

#### Análise da Diferença de Desempenho

A diferença de desempenho entre a solução Java com `Semaphore` (mais de 15 segundos) e a solução Kotlin com `Mutex` (417 ms) é muito grande e ilustra as vantagens da concorrência estruturada com coroutines (uma implementação de Threads N para M).

1.  **Threads de SO vs. Coroutines Leves:**
    *   **Java `Thread`:** Cada `Thread` em Java é mapeada diretamente para uma thread do sistema operacional (SO). Threads de SO são recursos caros. A criação, o gerenciamento e, principalmente, a **troca de contexto** entre elas são operações lentas, pois exigem a intervenção do escalonador do SO.
    *   **Kotlin `Coroutine`:** Coroutines são "threads virtuais" ou "threads leves" gerenciadas pelo próprio runtime do Kotlin. Milhares de coroutines podem ser executadas em um pequeno pool de threads de SO (como o `Dispatchers.Default`). A troca de contexto entre coroutines é muito mais rápida, pois acontece no nível da aplicação, sem a sobrecarga de chamadas ao sistema.

2.  **Bloqueio vs. Suspensão:**
    *   **Bloqueio (`Semaphore.acquire()`):** Quando uma `Thread` Java chama `acquire()` e o semáforo não está disponível, a **thread inteira é bloqueada** pelo SO. Ela fica em estado de espera (waiting), sem poder realizar qualquer outro trabalho, consumindo memória e recursos do sistema até ser notificada. Com 8 threads competindo intensamente pelo mesmo lock, o sistema gasta a maior parte do tempo trocando o contexto entre essas threads bloqueadas.
    *   **Suspensão (`Mutex.withLock`):** Quando uma coroutine tenta adquirir um `Mutex` e ele está ocupado, ela **suspende**. A suspensão não bloqueia a thread do SO subjacente. Em vez disso, a coroutine salva seu estado e libera a thread para executar outra coroutine. Quando o `Mutex` é liberado, a coroutine suspensa é agendada para continuar sua execução em qualquer thread disponível no pool.


## Parte 3 - Deadlocks
### 3.1 Evidências do Deadlock
Aplicamos alguns outputs em terminal para provar que o algoritmo atual está em deadlock
```java
Thread t1 = new Thread(() -> {
String t_name = Thread.currentThread().getName();
System.out.printf("\n%s: tentando iniciar LOCK_A\n", t_name);

            synchronized (LOCK_A) {
                System.out.printf("%s: adquiriu LOCK_A\n", t_name);

                dormir(50);

                System.out.printf("%s: tentando iniciar LOCK_B\n", t_name);
                synchronized (LOCK_B) {
                    System.out.printf("%s: adquiriu LOCK_B\n", t_name);
                    System.out.println("T1 concluiu");
                }
            }
        });
```
Também criamos uma função de monitoramento que puxa o nome e estado atual das Threads após determinado tempo aguardando:
```java
static String monitor(Thread A, Thread B) {
    String nome_A = A.getName();
    Thread.State estado_A = A.getState();

    String nome_B = B.getName();
    Thread.State estado_B = B.getState();

    String resultado = String.format(
            "\n%s: %s\n%s: %s",
            nome_A, estado_A, nome_B, estado_B);

    return resultado;
}
```
### Resultados no Terminal
```text
C:\Users\rdsalves\.jdks\openjdk-22.0.2\bin\java.exe "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.4\lib\idea_rt.jar=63875:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.4\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath C:\Users\rdsalves\IdeaProjects\tde3-filosofos-semaforo-deadlock\out\production\tde3-filosofos-semaforo-deadlock DeadlockDemo

Thread-0: tentando iniciar LOCK_A
Thread-0: adquiriu LOCK_A

Thread-1: tentando iniciar LOCK_B
Thread-1: adquiriu LOCK_B
Thread-1: tentando iniciar LOCK_A
Thread-0: tentando iniciar LOCK_B

Thread-0: BLOCKED
Thread-1: BLOCKED
```

### 3.2 Análise das Quatro Condições de Coffman
#### 1. Exclusão Mútua (Mutual Exclusion)
Os recursos `LOCK_A` e `LOCK_B` só podem ser adquiridos por uma única thread por vez, pois estão protegidos por blocos `synchronized`. Isso impede acesso concorrente simultâneo aos mesmos recursos.

```java
synchronized (LOCK_A) { ... }
synchronized (LOCK_B) { ... }
```

#### 2. Manter e Esperar (Hold and Wait)
Cada thread mantém um lock enquanto tenta adquirir o segundo:

- A Thread-0 segura `LOCK_A` e tenta adquirir `LOCK_B`.
- A Thread-1 segura `LOCK_B` e tenta adquirir `LOCK_A`.

Logs que evidenciam isso:

```text
C:\Users\rdsalves\.jdks\openjdk-22.0.2\bin\java.exe "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.4\lib\idea_rt.jar=63875:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.4\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath C:\Users\rdsalves\IdeaProjects\tde3-filosofos-semaforo-deadlock\out\production\tde3-filosofos-semaforo-deadlock DeadlockDemo

Thread-0: tentando iniciar LOCK_A
Thread-0: adquiriu LOCK_A

Thread-1: tentando iniciar LOCK_B
Thread-1: adquiriu LOCK_B
Thread-1: tentando iniciar LOCK_A
Thread-0: tentando iniciar LOCK_B

Thread-0: BLOCKED
Thread-1: BLOCKED
```

#### 3. Não Preempção (No Preemption)
Uma vez que uma thread entra em um bloco `synchronized`, o lock não pode ser retirado à força. A thread só libera o recurso quando sai da seção crítica. Isso impede recuperação automática.

#### 4. Espera Circular (Circular Wait)
O deadlock ocorre porque há um ciclo fechado de espera:

- Thread-0 → segura `LOCK_A` → espera `LOCK_B`
- Thread-1 → segura `LOCK_B` → espera `LOCK_A`

**Representação:**

```text
Thread-0 → LOCK_A → espera → LOCK_B (com Thread-1)
Thread-1 → LOCK_B → espera → LOCK_A (com Thread-0)
```

#### Conclusão
Todas as condições necessárias para deadlock estão presentes:

| Condição de Coffman | Presente |
|----------------------|----------|
| Exclusão Mútua       | ✔ |
| Manter e Esperar     | ✔ |
| Não Preempção        | ✔ |
| Espera Circular      | ✔ |

Como essas quatro condições ocorrem ao mesmo tempo, o deadlock é inevitável e o programa permanece indefinidamente no estado `BLOCKED`.

### 3.3 Implementação Corrigida
Para a última etapa do trabalho, foi solicitado a correção do deadlock da seguinte forma:
>Implementação corrigida e explicação de como a estratégia aplicada removeu a condição de espera circular ou manter-e-esperar, relacionando com o tratamento de deadlock e a solução por hierarquia de recursos do problema dos filósofos

- Apesar do enunciado dizer para resolvermos uma das duas condições de Coffman, o mesmo enunciado pede para usar **Hierarquia de Recursos**.
- O problema é que **Hierarquia de Recursos** só poderia resolver a **Espera Circular**.
- Não tem como resolver **Manter e Esperar** com **Hierarquia de Recursos**.

#### Resolvendo com Hierarquia de Recursos (Espera Circular)
- Para garantir a visualização correta dos recursos com seus respectivos nomes, precisamos criar uma classe e instanciar objetos:

**Classe e Objetos**
```java
static class Lock {
        String nome;
        final Object lock;

        Lock(String nome, Object lock) {
            this.nome = nome;
            this.lock = lock;
        }
    }

    static final Object LOCK_A = new Object();
    static final Object LOCK_B = new Object();
```

- Alinhamos os LOCKs em uma fila que sempre respeitará a mesma sequencia.
- Cada Thread chama essa fila, e sempre seguirá a mesma ordem

**Fila**
```java
static Lock[] fila = {
            new Lock("LOCK_A", LOCK_A),
            new Lock("LOCK_B", LOCK_B)
    };
```
- Alteramos completamente a forma de chamar os recursos.
- Invés de termos duas chamadas "hard coded" dos `LOCKS` com dois `synchronized()`, agora temos um loop iterando sobre a `fila[]`

**Loop de iteração**
```java
for (int i = 0; i < fila.length; i++) {
                System.out.printf("\n%s: tentando iniciar %s\n", t_name, fila[i].nome);

                synchronized (fila[i].lock) {
                    System.out.printf("%s: adquiriu %s\n", t_name, fila[i].nome);
                    dormir(50);
                }
            }
            System.out.printf("%s: concluiu", t_name);
```
- Por último, temos o resultado desses ajustes sendo apresentados no console:
```text
C:\Users\rdsalves\.jdks\openjdk-22.0.2\bin\java.exe "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.4\lib\idea_rt.jar=50737:C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.4\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath C:\Users\rdsalves\IdeaProjects\tde3-filosofos-semaforo-deadlock\out\production\tde3-filosofos-semaforo-deadlock DeadlockDemo

Thread-0: tentando iniciar LOCK_A
Thread-0: adquiriu LOCK_A

Thread-1: tentando iniciar LOCK_A

Thread-0: tentando iniciar LOCK_B
Thread-0: adquiriu LOCK_B
Thread-1: adquiriu LOCK_A
Thread-0: concluiu
Thread-1: tentando iniciar LOCK_B
Thread-1: adquiriu LOCK_B
Thread-1: concluiu
Thread-0: TERMINATED

Thread-1: TERMINATED

Process finished with exit code 0
```