package ma.ahmedbenidar.benchmark.c.depot;

import ma.ahmedbenidar.benchmark.c.modele.Produit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduitDepot extends JpaRepository<Produit, Long> {
}
