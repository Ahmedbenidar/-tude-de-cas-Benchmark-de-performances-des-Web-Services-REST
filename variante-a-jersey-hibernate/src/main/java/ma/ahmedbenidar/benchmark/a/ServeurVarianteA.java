package ma.ahmedbenidar.benchmark.a;

import ma.ahmedbenidar.benchmark.a.util.Persistance;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lanceur de la variante A : serveur Grizzly embarque + ressources JAX-RS.
 * Port 9201, pool de threads dimensionne pour supporter les paliers JMeter
 * jusqu'a 200 utilisateurs virtuels concurrents.
 */
public final class ServeurVarianteA {

    private static final int PORT = 9201;
    private static final int THREADS_MAX = 200;

    private ServeurVarianteA() {
    }

    public static void main(String[] args) throws Exception {
        ResourceConfig configuration = new ResourceConfig()
                .packages("ma.ahmedbenidar.benchmark.a.ressource");

        URI base = URI.create("http://0.0.0.0:" + PORT + "/");
        HttpServer serveur = GrizzlyHttpServerFactory.createHttpServer(base, configuration, false);

        ThreadPoolConfig pool = ThreadPoolConfig.defaultConfig()
                .setPoolName("grizzly-worker")
                .setCorePoolSize(32)
                .setMaxPoolSize(THREADS_MAX);
        serveur.getListener("grizzly").getTransport().setWorkerThreadPoolConfig(pool);

        // Ouverture explicite de l'EntityManagerFactory : si la base est
        // injoignable on echoue au demarrage plutot qu'a la premiere requete.
        Persistance.fabrique();

        serveur.start();

        String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        System.out.println("=================================================");
        System.out.println(" Variante A - Jersey JAX-RS + Hibernate");
        System.out.println(" Demarrage      : " + horodatage);
        System.out.println(" Adresse        : http://localhost:" + PORT + "/");
        System.out.println(" Base de donnees: benchmark_rest (MariaDB 10.4)");
        System.out.println(" Threads Grizzly: 32 coeur / " + THREADS_MAX + " max");
        System.out.println(" Endpoints      : /categories, /items");
        System.out.println("=================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[arret] fermeture du serveur Grizzly");
            serveur.shutdownNow();
            Persistance.fermer();
        }));

        Thread.currentThread().join();
    }
}
