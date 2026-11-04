package ma.ahmedbenidar.benchmark.d.modele;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Categorie exposee automatiquement par Spring Data REST.
 * L'annotation Jackson evite que le proxy Hibernate ne soit serialise tel quel
 * lorsqu'une categorie est atteinte par association depuis un produit.
 */
@Entity
@Table(name = "category")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Versant inverse de la relation. Il permet a Spring Data REST d'exposer
     * GET /categories/{id}/items comme les variantes A et C. Le champ est
     * ignore par Jackson pour ne pas alourdir la reponse d'une categorie.
     */
    @OneToMany(mappedBy = "categorie", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Produit> produits;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Produit> getProduits() { return produits; }
    public void setProduits(List<Produit> produits) { this.produits = produits; }

    @PrePersist
    @PreUpdate
    void horodater() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}
