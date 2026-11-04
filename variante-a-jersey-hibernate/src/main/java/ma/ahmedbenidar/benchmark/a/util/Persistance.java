package ma.ahmedbenidar.benchmark.a.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Point d'acces unique a l'EntityManagerFactory Hibernate, construit une
 * seule fois au demarrage. La configuration vient de META-INF/persistence.xml.
 */
public final class Persistance {

    private static volatile EntityManagerFactory fabrique;

    private Persistance() {
    }

    public static EntityManagerFactory fabrique() {
        EntityManagerFactory locale = fabrique;
        if (locale == null) {
            synchronized (Persistance.class) {
                locale = fabrique;
                if (locale == null) {
                    locale = Persistence.createEntityManagerFactory("benchmark");
                    fabrique = locale;
                }
            }
        }
        return fabrique;
    }

    public static EntityManager ouvrir() {
        return fabrique().createEntityManager();
    }

    public static void fermer() {
        if (fabrique != null) {
            fabrique.close();
            fabrique = null;
        }
    }
}
