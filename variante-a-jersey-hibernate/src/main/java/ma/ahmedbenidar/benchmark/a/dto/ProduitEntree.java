package ma.ahmedbenidar.benchmark.a.dto;

import java.math.BigDecimal;

/**
 * Corps JSON attendu pour la creation et la mise a jour d'un produit.
 * Le champ description ne sert qu'au scenario HEAVY-body (payload de 5 KB).
 */
public class ProduitEntree {

    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
