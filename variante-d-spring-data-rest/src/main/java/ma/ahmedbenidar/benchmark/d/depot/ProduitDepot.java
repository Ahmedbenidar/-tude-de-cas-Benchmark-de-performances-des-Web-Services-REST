package ma.ahmedbenidar.benchmark.d.depot;

import ma.ahmedbenidar.benchmark.d.modele.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

/**
 * Depôt des produits expose automatiquement par Spring Data REST sous
 * /items. La recherche par categorie est publiee en /items/search/parCategorie
 * pour reproduire l'endpoint de filtrage des deux autres variantes.
 */
@RepositoryRestResource(collectionResourceRel = "items", path = "items")
public interface ProduitDepot extends JpaRepository<Produit, Long> {

    @RestResource(path = "parCategorie", rel = "parCategorie")
    Page<Produit> findByCategorieIdOrderByIdAsc(Long categorieId, Pageable pageable);

    @RestResource(exported = false)
    List<Produit> deleteByCategorieId(Long categorieId);
}
