Descrição do Problema

O Jantar dos Filósofos modela cinco filósofos sentados em uma mesa circular. Cada filósofo alterna entre três estados principais:
Pensando
Comendo
Com fome

Para comer, cada filósofo precisa de dois garfos (esquerda e direita), que são recursos compartilhados entre vizinhos. Esse compartilhamento gera riscos de:
Exclusão mútua
Deadlock
Inanição (starvation)
Problemas de sincronização

No protocolo ingênuo (“pegar primeiro um garfo, depois o outro”), pode ocorrer um cenário em que:
Todos os filósofos ficam com fome ao mesmo tempo.
Todos pegam o garfo à esquerda.
Todos passam a esperar o garfo à direita.
Ninguém progride: impasse total (deadlock).

O deadlock ocorre quando todas as quatro condições de Coffman são satisfeitas simultaneamente:
Exclusão mútua = Cada garfo só pode estar com um filósofo por vez.

Manter-e-esperar (hold and wait) = Um filósofo segura um garfo enquanto espera outro.

Não preempção = Os garfos não podem ser retirados à força; só são liberados voluntariamente.

Espera circular = Cada filósofo depende do próximo em um ciclo circular (0 espera o 1, o 1 espera o 2, ..., o 4 espera o 0).

Ao invés de usar hierarquia de recursos como você colocou no exemplo, vou utilizar um árbitro (tipo um garçom) central que controla o acesso aos garfos. Como funciona esse garçom:
Quando um filósofo fica com fome, ele pede ao garçom permissão para comer.
O garçom autoriza somente se os DOIS garfos do filósofo estiverem livres.
Caso contrário, o filósofo entra em uma fila de espera e dorme.
Quando um filósofo termina de comer, ele devolve os garfos ao garçom, que então verifica se alguém da fila pode ser atendido.

Desse jeito, o filósofo nunca chega a segurar apenas um garfo, o que impede a formação do ciclo de espera.

Condição de Coffman quebrada
A solução com árbitro quebra a condição de Espera Circular:
Não existe mais um ciclo de dependências entre os filósofos.
Toda espera acontece exclusivamente em relação ao garçom, que decide quem pode entrar na área crítica.
O garçom só libera os dois garfos simultaneamente, impedindo dependências encadeadas.

Pseudocódigo
------------------------------------------------------------------------------

Estados
const N = 5                  
enum Estado { PENSANDO, COM_FOME, COMENDO }

Estado estado[N]             
bool garfoLivre[N]           

fila pedidos                 

mutex m                      

condicao podeComer[N]        
------------------------------------------------------------------------------

Funções do árbitro

func garfosDisponiveis(i):
    esquerda = i
    direita = (i + 1) mod N
    return garfoLivre[esquerda] AND garfoLivre[direita]

func reservarGarfos(i):
    esquerda = i
    direita = (i + 1) mod N
    garfoLivre[esquerda] = false
    garfoLivre[direita]  = false

func liberarGarfos(i):
    esquerda = i
    direita = (i + 1) mod N
    garfoLivre[esquerda] = true
    garfoLivre[direita]  = true

Função central do garçom
func tentarAtenderFila():
    para cada filosofo j na fila 'pedidos' em ordem:
        se estado[j] == COM_FOME AND garfosDisponiveis(j):
            reservarGarfos(j)
            estado[j] = COMENDO
            remover j da fila 'pedidos'
            sinalizar podeComer[j]
------------------------------------------------------------------------------

Processo do filósofo
processo Filosofo(i):

  enquanto verdadeiro:
        PENSAR()
        
  m.lock()
  estado[i] = COM_FOME
  enfileirar(pedidos, i)
  tentarAtenderFila()

  enquanto estado[i] != COMENDO:
    esperar(podeComer[i], m)
  fim-enquanto

  m.unlock()

  COMER()

  m.lock()
  estado[i] = PENSANDO
  liberarGarfos(i)
  tentarAtenderFila()
  m.unlock()
