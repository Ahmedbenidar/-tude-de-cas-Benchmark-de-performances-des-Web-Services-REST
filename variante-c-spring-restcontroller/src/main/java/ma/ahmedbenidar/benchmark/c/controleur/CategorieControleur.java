package ma.ahmedbenidar.benchmark.c.controleur;

import ma.ahmedbenidar.benchmark.c.depot.CategorieDepot;
import ma.ahmedbenidar.benchmark.c.dto.PageVue;
import ma.ahmedbenidar.benchmark.c.lecture.ProduitLecteur;
import ma.ahmedbenidar.benchmark.c.modele.Categorie;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/categories")
public class CategorieControleur {

    private final CategorieDepot categories;
    private final ProduitLecteur lecteur;

    public CategorieControleur(CategorieDepot categories, ProduitLecteur lecteur) {
        this.categories = categories;
        this.lecteur = lecteur;
    }

    @GetMapping
    public PageVue<Map<String, Object>> lister(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        int p = Math.max(page, 0);
        int t = Math.min(Math.max(size, 1), 500);
        List<Categorie> contenu = categories.findAll(PageRequest.of(p, t, Sort.by("id"))).getContent();
        long total = categories.count();
        return new PageVue<>(contenu.stream().map(CategorieControleur::versVue).toList(), p, t, total);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> trouver(@PathVariable Long id) {
        return categories.findById(id)
                .map(c -> ResponseEntity.ok(versVue(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<PageVue<Object>> produitsDeLaCategorie(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!categories.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        int p = Math.max(page, 0);
        int t = Math.min(Math.max(size, 1), 500);
        List<Object> contenu = List.copyOf(lecteur.listerParCategorie(id, p, t));
        long total = lecteur.compterParCategorie(id);
        return ResponseEntity.ok(new PageVue<>(contenu, p, t, total));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@RequestBody Map<String, Object> corps) {
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

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable Long id,
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

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (!categories.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categories.supprimerParCategorie(id);
        categories.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    static Map<String, Object> versVue(Categorie c) {
        Map<String, Object> vue = new LinkedHashMap<>();
        vue.put("id", c.getId());
        vue.put("code", c.getCode());
        vue.put("name", c.getName());
        vue.put("updatedAt", c.getUpdatedAt() == null ? null : c.getUpdatedAt().toString());
        return vue;
    }
}
