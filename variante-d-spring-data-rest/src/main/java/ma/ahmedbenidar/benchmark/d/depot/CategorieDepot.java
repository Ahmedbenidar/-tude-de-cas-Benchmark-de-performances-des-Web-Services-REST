package ma.ahmedbenidar.benchmark.d.depot;

import ma.ahmedbenidar.benchmark.d.modele.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "categories", path = "categories")
public interface CategorieDepot extends JpaRepository<Categorie, Long> {
}
