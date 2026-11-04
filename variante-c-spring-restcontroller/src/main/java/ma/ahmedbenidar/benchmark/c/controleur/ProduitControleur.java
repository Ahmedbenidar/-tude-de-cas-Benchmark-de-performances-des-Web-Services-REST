package ma.ahmedbenidar.benchmark.c.controleur;

import ma.ahmedbenidar.benchmark.c.depot.CategorieDepot;
import ma.ahmedbenidar.benchmark.c.depot.ProduitDepot;
import ma.ahmedbenidar.benchmark.c.dto.PageVue;
import ma.ahmedbenidar.benchmark.c.dto.ProduitVue;
import ma.ahmedbenidar.benchmark.c.lecture.ProduitLecteur;
import ma.ahmedbenidar.benchmark.c.modele.Categorie;
import ma.ahmedbenidar.benchmark.c.modele.Produit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/items")
public class ProduitControleur {

    private final ProduitDepot produits;
    private final CategorieDepot categories;
    private final ProduitLecteur lecteur;

    public ProduitControleur(ProduitDepot produits, CategorieDepot categories, ProduitLecteur lecteur) {
        this.produits = produits;
        this.categories = categories;
        this.lecteur = lecteur;
    }

    @GetMapping
    public PageVue<ProduitVue> lister(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) Long categoryId) {
        int p = Math.max(page, 0);
        int t = Math.min(Math.max(size, 1), 500);
        List<ProduitVue> contenu = categoryId == null
                ? lecteur.lister(p, t)
                : lecteur.listerParCategorie(categoryId, p, t);
        long total = categoryId == null ? lecteur.compter() : lecteur.compterParCategorie(categoryId);
        return new PageVue<>(contenu, p, t, total);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProduitVue> trouver(@PathVariable Long id) {
        return lecteur.trouver(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
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
        return ResponseEntity.status(201).body(lecteur.trouver(cree.getId()).orElse(null));
    }

    @PutMapping("/{id}")
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
        produits.save(produit);
        return lecteur.trouver(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (!produits.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        produits.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
