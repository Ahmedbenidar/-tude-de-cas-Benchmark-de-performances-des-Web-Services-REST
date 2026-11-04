package ma.ahmedbenidar.benchmark.a.dto;

/**
 * Corps JSON attendu pour la creation et la mise a jour d'une categorie.
 */
public class CategorieEntree {

    private String code;
    private String name;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
