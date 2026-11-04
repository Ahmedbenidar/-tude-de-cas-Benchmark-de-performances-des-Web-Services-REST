package ma.ahmedbenidar.benchmark.d.controleur;

import ma.ahmedbenidar.benchmark.d.depot.CategorieDepot;
import ma.ahmedbenidar.benchmark.d.depot.ProduitDepot;
import ma.ahmedbenidar.benchmark.d.modele.Categorie;
import ma.ahmedbenidar.benchmark.d.modele.Produit;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Ecritures des produits.
 *
 * Spring Data REST expose automatiquement les lectures, mais il attend les
 * associations sous forme de lien HAL ; pour conserver un corps JSON identique
 * aux variantes A et C, les creations et modifications passent par ce
 * controleur qui accepte un simple champ categoryId.
 */
@BasePathAwareController
public class ProduitEcritureControleur {

    private final ProduitDepot produits;
    private final CategorieDepot categories;

    public ProduitEcritureControleur(ProduitDepot produits, CategorieDepot categories) {
        this.produits = produits;
        this.categories = categories;
    }

    @PostMapping("/items")
    @ResponseBody
    @Transactional
    public ResponseEntity<Object> creer(@RequestBody Map<String, Object> corps) {
        Object categoryId = corps.get("categoryId");
        if (corps.get("sku") == null || corps.get("name") == null
                || corps.get("price") == null || corps.get("stock") == null
                || categoryId == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Categorie> categorie = categories.findById(Long.valueOf(categoryId.toString()));
        if (categorie.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Produit produit = new Produit();
        produit.setSku(corps.get("sku").toString());
        produit.setName(corps.get("name").toString());
        if (corps.get("description") != null) {
            produit.setDescription(corps.get("description").toString());
        }
        produit.setPrice(new BigDecimal(corps.get("price").toString()));
        produit.setStock(Integer.valueOf(corps.get("stock").toString()));
        produit.setCategorie(categorie.get());
        produit.setUpdatedAt(LocalDateTime.now());
        Produit cree = produits.save(produit);
        return ResponseEntity.created(URI.create("/items/" + cree.getId())).body(versVue(cree));
    }

    @PutMapping("/items/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Object> modifier(@PathVariable Long id,
                                           @RequestBody Map<String, Object> corps) {
        Optional<Produit> existant = produits.findById(id);
        if (existant.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Produit produit = existant.get();
        if (corps.get("sku") != null) {
            produit.setSku(corps.get("sku").toString());
        }
        if (corps.get("name") != null) {
            produit.setName(corps.get("name").toString());
        }
        if (corps.get("description") != null) {
            produit.setDescription(corps.get("description").toString());
        }
        if (corps.get("price") != null) {
            produit.setPrice(new BigDecimal(corps.get("price").toString()));
        }
        if (corps.get("stock") != null) {
            produit.setStock(Integer.valueOf(corps.get("stock").toString()));
        }
        if (corps.get("categoryId") != null) {
            Optional<Categorie> categorie = categories.findById(Long.valueOf(corps.get("categoryId").toString()));
            if (categorie.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            produit.setCategorie(categorie.get());
        }
        produit.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(versVue(produits.save(produit)));
    }

    /**
     * Reponse volontairement plate, identique aux variantes A et C, pour que
     * les assertions JMeter portent sur les memes champs.
     */
    private java.util.Map<String, Object> versVue(Produit p) {
        java.util.Map<String, Object> vue = new java.util.LinkedHashMap<>();
        vue.put("id", p.getId());
        vue.put("sku", p.getSku());
        vue.put("name", p.getName());
        vue.put("description", p.getDescription());
        vue.put("price", p.getPrice());
        vue.put("stock", p.getStock());
        vue.put("categoryId", p.getCategorie() == null ? null : p.getCategorie().getId());
        vue.put("updatedAt", p.getUpdatedAt() == null ? null : p.getUpdatedAt().toString());
        return vue;
    }
}
