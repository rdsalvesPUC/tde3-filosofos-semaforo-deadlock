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
### 2.1
### 2.2
### 2.2

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