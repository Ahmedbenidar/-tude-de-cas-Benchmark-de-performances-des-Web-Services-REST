package ma.ahmedbenidar.benchmark.a.ressource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ahmedbenidar.benchmark.a.dao.CategorieDao;
import ma.ahmedbenidar.benchmark.a.dao.ProduitDao;
import ma.ahmedbenidar.benchmark.a.dto.CategorieEntree;
import ma.ahmedbenidar.benchmark.a.dto.PageVue;
import ma.ahmedbenidar.benchmark.a.dto.ProduitVue;
import ma.ahmedbenidar.benchmark.a.modele.Categorie;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ressource JAX-RS des categories. Le contrat JSON est identique a celui des
 * variantes Spring : id, code, name, updatedAt.
 */
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategorieRessource {

    private final CategorieDao dao = new CategorieDao();

    @GET
    public Response lister(@QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
        int p = Math.max(page, 0);
        int t = Math.min(Math.max(size, 1), 500);
        List<Categorie> categories = dao.lister(p, t);
        long total = dao.compter();
        List<Map<String, Object>> vues = categories.stream().map(CategorieRessource::versVue).toList();
        return Response.ok(new PageVue<>(vues, p, t, total)).build();
    }

    @GET
    @Path("{id}")
    public Response trouver(@PathParam("id") Long id) {
        return dao.trouver(id)
                .map(c -> Response.ok(versVue(c)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("{id}/items")
    public Response produitsDeLaCategorie(@PathParam("id") Long id,
                                          @QueryParam("page") @DefaultValue("0") int page,
                                          @QueryParam("size") @DefaultValue("20") int size) {
        if (!dao.existe(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        int p = Math.max(page, 0);
        int t = Math.min(Math.max(size, 1), 500);
        ProduitDao produitsDao = new ProduitDao();
        List<ProduitVue> produits = produitsDao.listerParCategorie(id, p, t);
        long total = produitsDao.compterParCategorie(id);
        return Response.ok(new PageVue<>(produits, p, t, total)).build();
    }

    @POST
    public Response creer(CategorieEntree entree) {
        if (entree == null || entree.getCode() == null || entree.getName() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Categorie creee = dao.creer(entree.getCode(), entree.getName());
        return Response.status(Response.Status.CREATED).entity(versVue(creee)).build();
    }

    @PUT
    @Path("{id}")
    public Response modifier(@PathParam("id") Long id, CategorieEntree entree) {
        if (entree == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Optional<Categorie> modifiee = dao.modifier(id, entree.getCode(), entree.getName());
        return modifiee
                .map(c -> Response.ok(versVue(c)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("{id}")
    public Response supprimer(@PathParam("id") Long id) {
        return dao.supprimer(id)
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
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
