package ma.ahmedbenidar.benchmark.c.depot;

import ma.ahmedbenidar.benchmark.c.modele.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategorieDepot extends JpaRepository<Categorie, Long> {

    @Modifying
    @Query("delete from Produit p where p.categorie.id = :cid")
    int supprimerParCategorie(@Param("cid") Long categoryId);
}
