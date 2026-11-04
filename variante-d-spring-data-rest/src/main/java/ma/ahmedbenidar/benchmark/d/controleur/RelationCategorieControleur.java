package ma.ahmedbenidar.benchmark.d.controleur;

import ma.ahmedbenidar.benchmark.d.depot.CategorieDepot;
import ma.ahmedbenidar.benchmark.d.depot.ProduitDepot;
import ma.ahmedbenidar.benchmark.d.modele.Categorie;
import ma.ahmedbenidar.benchmark.d.modele.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Relation inverse categorie vers produits.
 *
 * Spring Data REST n'expose cette association que sous la forme
 * /categories/{id}/produits. Le sujet impose /categories/{id}/items avec une
 * pagination identique aux variantes A et C : cette route est donc declaree
 * explicitement et renvoie la meme enveloppe JSON plate que les deux autres
 * variantes, afin que la comparaison des mesures porte sur des reponses
 * comparables.
 */
@BasePathAwareController
public class RelationCategorieControleur {

    private final CategorieDepot categories;
    private final ProduitDepot produits;

    public RelationCategorieControleur(CategorieDepot categories, ProduitDepot produits) {
        this.categories = categories;
        this.produits = produits;
    }

    @GetMapping("/categories/{id}/items")
    @ResponseBody
    public ResponseEntity<Object> itemsDeLaCategorie(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Optional<Categorie> categorie = categories.findById(id);
        if (categorie.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Categorie c = categorie.get();
        Page<Produit> resultat = produits.findByCategorieIdOrderByIdAsc(id,
                PageRequest.of(page, size, Sort.by("id").ascending()));

        List<Object> vues = new ArrayList<>();
        for (Produit p : resultat.getContent()) {
            vues.add(versVue(p, c));
        }

        Map<String, Object> enveloppe = new LinkedHashMap<>();
        enveloppe.put("content", vues);
        enveloppe.put("page", resultat.getNumber());
        enveloppe.put("size", resultat.getSize());
        enveloppe.put("totalElements", resultat.getTotalElements());
        enveloppe.put("totalPages", resultat.getTotalPages());
        return ResponseEntity.ok(enveloppe);
    }

    private Map<String, Object> versVue(Produit p, Categorie c) {
        Map<String, Object> vue = new LinkedHashMap<>();
        vue.put("id", p.getId());
        vue.put("sku", p.getSku());
        vue.put("name", p.getName());
        vue.put("description", p.getDescription());
        vue.put("price", p.getPrice());
        vue.put("stock", p.getStock());
        vue.put("categoryId", c.getId());
        vue.put("categoryCode", c.getCode());
        vue.put("categoryName", c.getName());
        vue.put("updatedAt", p.getUpdatedAt() == null ? null : p.getUpdatedAt().toString());
        return vue;
    }
}
