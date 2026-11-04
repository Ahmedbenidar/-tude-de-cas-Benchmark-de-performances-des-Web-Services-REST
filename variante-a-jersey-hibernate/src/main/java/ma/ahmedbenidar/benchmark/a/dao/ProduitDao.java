package ma.ahmedbenidar.benchmark.a.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import ma.ahmedbenidar.benchmark.a.dto.ProduitVue;
import ma.ahmedbenidar.benchmark.a.modele.Categorie;
import ma.ahmedbenidar.benchmark.a.modele.Produit;
import ma.ahmedbenidar.benchmark.a.util.Persistance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Acces aux produits.
 *
 * Les lectures sont faites en deux temps volontairement : d'abord la page de
 * produits (une seule requete, pagination SQL par offset), puis un unique
 * chargement groupe des categories concernees. On obtient donc deux requetes
 * par appel quelle que soit la taille de la page, au lieu du phenomene N+1
 * que provoquerait un chargement paresseux de la relation produit par produit.
 */
public class ProduitDao {

    public List<ProduitVue> lister(int page, int taille) {
        EntityManager em = Persistance.ouvrir();
        try {
            List<Produit> produits = em.createQuery(
                            "select p from Produit p order by p.id", Produit.class)
                    .setFirstResult(page * taille)
                    .setMaxResults(taille)
                    .getResultList();
            return assembler(em, produits);
        } finally {
            em.close();
        }
    }

    public List<ProduitVue> listerParCategorie(Long categoryId, int page, int taille) {
        EntityManager em = Persistance.ouvrir();
        try {
            List<Produit> produits = em.createQuery(
                            "select p from Produit p where p.categoryId = :cid order by p.id",
                            Produit.class)
                    .setParameter("cid", categoryId)
                    .setFirstResult(page * taille)
                    .setMaxResults(taille)
                    .getResultList();
            return assembler(em, produits);
        } finally {
            em.close();
        }
    }

    private List<ProduitVue> assembler(EntityManager em, List<Produit> produits) {
        if (produits.isEmpty()) {
            return List.of();
        }
        Set<Long> identifiants = new HashSet<>();
        for (Produit p : produits) {
            identifiants.add(p.getCategoryId());
        }
        List<Categorie> categories = em.createQuery(
                        "select c from Categorie c where c.id in :ids", Categorie.class)
                .setParameter("ids", identifiants)
                .getResultList();
        Map<Long, Categorie> index = new HashMap<>(categories.size() * 2);
        for (Categorie c : categories) {
            index.put(c.getId(), c);
        }
        List<ProduitVue> vues = new ArrayList<>(produits.size());
        for (Produit p : produits) {
            Categorie c = index.get(p.getCategoryId());
            vues.add(versVue(p, c));
        }
        return vues;
    }

    private ProduitVue versVue(Produit p, Categorie c) {
        ProduitVue v = new ProduitVue();
        v.setId(p.getId());
        v.setSku(p.getSku());
        v.setName(p.getName());
        v.setDescription(p.getDescription());
        v.setPrice(p.getPrice() == null ? null : new BigDecimal(p.getPrice().toString()));
        v.setStock(p.getStock());
        v.setCategoryId(p.getCategoryId());
        v.setCategoryCode(c == null ? null : c.getCode());
        v.setCategoryName(c == null ? null : c.getName());
        v.setUpdatedAt(p.getUpdatedAt() == null ? null : p.getUpdatedAt().toString());
        return v;
    }

    public long compter() {
        EntityManager em = Persistance.ouvrir();
        try {
            return em.createQuery("select count(p) from Produit p", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long compterParCategorie(Long categoryId) {
        EntityManager em = Persistance.ouvrir();
        try {
            return em.createQuery(
                            "select count(p) from Produit p where p.categoryId = :cid", Long.class)
                    .setParameter("cid", categoryId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Optional<ProduitVue> trouver(Long id) {
        EntityManager em = Persistance.ouvrir();
        try {
            Produit p = em.find(Produit.class, id);
            if (p == null) {
                return Optional.empty();
            }
            return Optional.of(versVue(p, em.find(Categorie.class, p.getCategoryId())));
        } finally {
            em.close();
        }
    }

    public Produit creer(Produit p) {
        EntityManager em = Persistance.ouvrir();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            p.setUpdatedAt(LocalDateTime.now());
            em.persist(p);
            tx.commit();
            return p;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<Produit> modifier(Long id, Produit modifications) {
        EntityManager em = Persistance.ouvrir();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Produit p = em.find(Produit.class, id);
            if (p == null) {
                tx.rollback();
                return Optional.empty();
            }
            if (modifications.getSku() != null) {
                p.setSku(modifications.getSku());
            }
            if (modifications.getName() != null) {
                p.setName(modifications.getName());
            }
            if (modifications.getDescription() != null) {
                p.setDescription(modifications.getDescription());
            }
            if (modifications.getPrice() != null) {
                p.setPrice(modifications.getPrice());
            }
            if (modifications.getStock() != null) {
                p.setStock(modifications.getStock());
            }
            if (modifications.getCategoryId() != null) {
                p.setCategoryId(modifications.getCategoryId());
            }
            p.setUpdatedAt(LocalDateTime.now());
            tx.commit();
            return Optional.of(p);
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
            Produit p = em.find(Produit.class, id);
            if (p == null) {
                tx.rollback();
                return false;
            }
            em.remove(p);
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
}
