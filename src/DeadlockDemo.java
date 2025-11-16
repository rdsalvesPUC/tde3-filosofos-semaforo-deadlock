import java.util.concurrent.*;

public class DeadlockDemo {

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

    static Lock[] fila = {
            new Lock("LOCK_A", LOCK_A),
            new Lock("LOCK_B", LOCK_B)
    };

    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
            String t_name = Thread.currentThread().getName();
            //System.out.printf("\n%s: tentando iniciar LOCK_A\n", t_name);

            for (int i = 0; i < fila.length; i++) {
                System.out.printf("\n%s: tentando iniciar %s\n", t_name, fila[i].nome);

                synchronized (fila[i].lock) {
                    System.out.printf("%s: adquiriu %s\n", t_name, fila[i].nome);
                    dormir(50);
                }
            }
            System.out.printf("%s: concluiu", t_name);

            //synchronized (LOCK_A) {
            //    System.out.printf("%s: adquiriu LOCK_A\n", t_name);
            //
            //    dormir(50);
            //
            //    System.out.printf("%s: tentando iniciar LOCK_B\n", t_name);
            //    synchronized (LOCK_B) {
            //        System.out.printf("%s: adquiriu LOCK_B\n", t_name);
            //        System.out.println("T1 concluiu");
            //    }
            //}
        });

        Thread t2 = new Thread(() -> {
            String t_name = Thread.currentThread().getName();
            //System.out.printf("\n%s: tentando iniciar LOCK_B\n", t_name);

            for (int i = 0; i < fila.length; i++) {
                System.out.printf("\n%s: tentando iniciar %s\n", t_name, fila[i].nome);

                synchronized (fila[i].lock) {
                    System.out.printf("%s: adquiriu %s\n", t_name, fila[i].nome);
                    dormir(50);
                }
            }
            System.out.printf("%s: concluiu", t_name);

            //synchronized (LOCK_B) {
            //    System.out.printf("%s: adquiriu LOCK_B\n", t_name);
            //
            //    dormir(50);
            //
            //    System.out.printf("%s: tentando iniciar LOCK_A\n", t_name);
            //    synchronized (LOCK_A) {
            //        System.out.printf("%s: adquiriu LOCK_A\n", t_name);
            //        System.out.println("T2 concluiu");
            //    }
            //}
        });

        t1.start();
        t2.start();

        dormir(1000);

        System.out.println(monitor(t1));
        System.out.println(monitor(t2));
    }

    static void dormir(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); }
    }

    static String monitor(Thread XPTO) {
        String nome = XPTO.getName();
        Thread.State estado = XPTO.getState();

        String resultado = String.format(
                "\n%s: %s",
                nome, estado);

        return resultado;
    }
}
