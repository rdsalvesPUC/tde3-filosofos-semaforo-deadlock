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