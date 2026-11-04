package ma.ahmedbenidar.benchmark.c.lecture;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ma.ahmedbenidar.benchmark.c.dto.ProduitVue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lectures des produits en SQL natif.
 *
 * La jointure est ecrite avec STRAIGHT_JOIN : sur ce jeu de donnees,
 * l'optimiseur de MariaDB choisit sinon un plan qui prend la table category
 * comme table motrice, ce qui declenche un tri temporaire sur les 100 000
 * lignes de item et fait passer la requete de 50 ms a plus de 12 secondes.
 * STRAIGHT_JOIN force item en table motrice et permet un parcours d'index
 * suivi d'une jointure par cle primaire.
 */
@Component
public class ProduitLecteur {

    private static final String COLONNES =
            "p.id, p.sku, p.name, p.description, p.price, p.stock, "
                    + "c.id, c.code, c.name, p.updated_at ";

    private static final String DEPUIS =
            " FROM item p INNER JOIN category c ON c.id = p.category_id ";

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<ProduitVue> lister(int page, int taille) {
        @SuppressWarnings("unchecked")
        List<Object[]> lignes = em.createNativeQuery(
                        "SELECT STRAIGHT_JOIN " + COLONNES + DEPUIS + "ORDER BY p.id")
                .setFirstResult(page * taille)
                .setMaxResults(taille)
                .getResultList();
        return assembler(lignes);
    }

    @Transactional(readOnly = true)
    public List<ProduitVue> listerParCategorie(Long categoryId, int page, int taille) {
        @SuppressWarnings("unchecked")
        List<Object[]> lignes = em.createNativeQuery(
                        "SELECT STRAIGHT_JOIN " + COLONNES + DEPUIS
                                + "WHERE p.category_id = ?1 ORDER BY p.id")
                .setParameter(1, categoryId)
                .setFirstResult(page * taille)
                .setMaxResults(taille)
                .getResultList();
        return assembler(lignes);
    }

    @Transactional(readOnly = true)
    public long compter() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM item").getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    public long compterParCategorie(Long categoryId) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM item WHERE category_id = ?1")
                .setParameter(1, categoryId)
                .getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    public Optional<ProduitVue> trouver(Long id) {
        @SuppressWarnings("unchecked")
        List<Object[]> lignes = em.createNativeQuery(
                        "SELECT STRAIGHT_JOIN " + COLONNES + DEPUIS + "WHERE p.id = ?1")
                .setParameter(1, id)
                .setMaxResults(1)
                .getResultList();
        return lignes.isEmpty() ? Optional.empty() : Optional.of(versVue(lignes.get(0)));
    }

    private List<ProduitVue> assembler(List<Object[]> lignes) {
        List<ProduitVue> vues = new ArrayList<>(lignes.size());
        for (Object[] ligne : lignes) {
            vues.add(versVue(ligne));
        }
        return vues;
    }

    private ProduitVue versVue(Object[] ligne) {
        ProduitVue vue = new ProduitVue();
        vue.setId(ligne[0] == null ? null : ((Number) ligne[0]).longValue());
        vue.setSku(ligne[1] == null ? null : ligne[1].toString());
        vue.setName(ligne[2] == null ? null : ligne[2].toString());
        vue.setDescription(ligne[3] == null ? null : ligne[3].toString());
        vue.setPrice(ligne[4] == null ? null : new BigDecimal(ligne[4].toString()));
        vue.setStock(ligne[5] == null ? null : ((Number) ligne[5]).intValue());
        vue.setCategoryId(ligne[6] == null ? null : ((Number) ligne[6]).longValue());
        vue.setCategoryCode(ligne[7] == null ? null : ligne[7].toString());
        vue.setCategoryName(ligne[8] == null ? null : ligne[8].toString());
        vue.setUpdatedAt(texte(ligne[9]));
        return vue;
    }

    private String texte(Object valeur) {
        if (valeur == null) {
            return null;
        }
        if (valeur instanceof Timestamp t) {
            return t.toLocalDateTime().toString();
        }
        if (valeur instanceof LocalDateTime d) {
            return d.toString();
        }
        return valeur.toString();
    }
}
