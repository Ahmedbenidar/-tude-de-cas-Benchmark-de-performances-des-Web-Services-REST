package ma.ahmedbenidar.benchmark.d.controleur;

import ma.ahmedbenidar.benchmark.d.depot.CategorieDepot;
import ma.ahmedbenidar.benchmark.d.depot.ProduitDepot;
import ma.ahmedbenidar.benchmark.d.modele.Categorie;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ecritures des categories.
 *
 * Spring Data REST exigerait le champ updatedAt dans le corps JSON ; ce
 * controleur l'alimente automatiquement pour conserver un corps d'entree
 * identique aux variantes A et C, sans quoi la comparaison des ecritures
 * porterait sur des requetes differentes.
 */
@BasePathAwareController
public class CategorieEcritureControleur {

    private final CategorieDepot categories;
    private final ProduitDepot produits;

    public CategorieEcritureControleur(CategorieDepot categories, ProduitDepot produits) {
        this.categories = categories;
        this.produits = produits;
    }

    @PostMapping("/categories")
    @ResponseBody
    public ResponseEntity<Object> creer(@RequestBody Map<String, Object> corps) {
        Object code = corps.get("code");
        Object nom = corps.get("name");
        if (code == null || nom == null) {
            return ResponseEntity.badRequest().build();
        }
        Categorie c = new Categorie();
        c.setCode(code.toString());
        c.setName(nom.toString());
        c.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.status(201).body(versVue(categories.save(c)));
    }

    @PutMapping("/categories/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Object> modifier(@PathVariable Long id,
                                           @RequestBody Map<String, Object> corps) {
        Optional<Categorie> existante = categories.findById(id);
        if (existante.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Categorie c = existante.get();
        if (corps.get("code") != null) {
            c.setCode(corps.get("code").toString());
        }
        if (corps.get("name") != null) {
            c.setName(corps.get("name").toString());
        }
        c.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(versVue(categories.save(c)));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (!categories.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        produits.deleteByCategorieId(id);
        categories.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> versVue(Categorie c) {
        Map<String, Object> vue = new LinkedHashMap<>();
        vue.put("id", c.getId());
        vue.put("code", c.getCode());
        vue.put("name", c.getName());
        vue.put("updatedAt", c.getUpdatedAt() == null ? null : c.getUpdatedAt().toString());
        return vue;
    }
}
