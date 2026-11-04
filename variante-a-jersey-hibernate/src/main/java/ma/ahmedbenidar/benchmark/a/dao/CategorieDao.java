package ma.ahmedbenidar.benchmark.a.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import ma.ahmedbenidar.benchmark.a.modele.Categorie;
import ma.ahmedbenidar.benchmark.a.util.Persistance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acces aux categories. Chaque methode ouvre et referme son EntityManager :
 * pas de session longue, ce qui correspond au comportement d'un conteneur
 * Spring en mode OSIV desactive.
 */
public class CategorieDao {

    public List<Categorie> lister(int page, int taille) {
        EntityManager em = Persistance.ouvrir();
        try {
            return em.createQuery(
                            "select c from Categorie c order by c.id", Categorie.class)
                    .setFirstResult(page * taille)
                    .setMaxResults(taille)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long compter() {
        EntityManager em = Persistance.ouvrir();
        try {
            return em.createQuery("select count(c) from Categorie c", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Optional<Categorie> trouver(Long id) {
        EntityManager em = Persistance.ouvrir();
        try {
            return Optional.ofNullable(em.find(Categorie.class, id));
        } finally {
            em.close();
        }
    }

    public Categorie creer(String code, String nom) {
        EntityManager em = Persistance.ouvrir();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Categorie c = new Categorie();
            c.setCode(code);
            c.setName(nom);
            c.setUpdatedAt(LocalDateTime.now());
            em.persist(c);
            tx.commit();
            return c;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<Categorie> modifier(Long id, String code, String nom) {
        EntityManager em = Persistance.ouvrir();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Categorie c = em.find(Categorie.class, id);
            if (c == null) {
                tx.rollback();
                return Optional.empty();
            }
            if (code != null) {
                c.setCode(code);
            }
            if (nom != null) {
                c.setName(nom);
            }
            c.setUpdatedAt(LocalDateTime.now());
            tx.commit();
            return Optional.of(c);
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean supprimer(Long id) {
        EntityManager em = Persistance.ouvrir();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Categorie c = em.find(Categorie.class, id);
            if (c == null) {
                tx.rollback();
                return false;
            }
            em.createQuery("delete from Produit p where p.categoryId = :cid")
                    .setParameter("cid", id)
                    .executeUpdate();
            em.remove(c);
            tx.commit();
            return true;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean existe(Long id) {
        EntityManager em = Persistance.ouvrir();
        try {
            return em.createQuery(
                            "select count(c) from Categorie c where c.id = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult() > 0L;
        } finally {
            em.close();
        }
    }
}
