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
import ma.ahmedbenidar.benchmark.a.dto.PageVue;
import ma.ahmedbenidar.benchmark.a.dto.ProduitEntree;
import ma.ahmedbenidar.benchmark.a.dto.ProduitVue;
import ma.ahmedbenidar.benchmark.a.modele.Produit;

import java.util.List;
import java.util.Optional;

/**
 * Ressource JAX-RS des produits. Les lectures renvoient une projection avec le
 * nom de la categorie obtenue par jointure, sans requete supplementaire.
 */
@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProduitRessource {

    private final ProduitDao dao = new ProduitDao();
    private final CategorieDao categories = new CategorieDao();

    @GET
    public Response lister(@QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size,
                           @QueryParam("categoryId") Long categoryId) {
        int p = Math.max(page, 0);
        int t = Math.min(Math.max(size, 1), 500);
        List<ProduitVue> produits;
        long total;
        if (categoryId != null) {
            produits = dao.listerParCategorie(categoryId, p, t);
            total = dao.compterParCategorie(categoryId);
        } else {
            produits = dao.lister(p, t);
            total = dao.compter();
        }
        return Response.ok(new PageVue<>(produits, p, t, total)).build();
    }

    @GET
    @Path("{id}")
    public Response trouver(@PathParam("id") Long id) {
        return dao.trouver(id)
                .map(v -> Response.ok(v).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response creer(ProduitEntree entree) {
        if (entree == null || entree.getSku() == null || entree.getName() == null
                || entree.getPrice() == null || entree.getStock() == null
                || entree.getCategoryId() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!categories.existe(entree.getCategoryId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Produit p = new Produit();
        p.setSku(entree.getSku());
        p.setName(entree.getName());
        p.setDescription(entree.getDescription());
        p.setPrice(entree.getPrice());
        p.setStock(entree.getStock());
        p.setCategoryId(entree.getCategoryId());
        Produit cree = dao.creer(p);
        return Response.status(Response.Status.CREATED)
                .entity(dao.trouver(cree.getId()).orElse(null))
                .build();
    }

    @PUT
    @Path("{id}")
    public Response modifier(@PathParam("id") Long id, ProduitEntree entree) {
        if (entree == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (entree.getCategoryId() != null && !categories.existe(entree.getCategoryId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Produit modifications = new Produit();
        modifications.setSku(entree.getSku());
        modifications.setName(entree.getName());
        modifications.setDescription(entree.getDescription());
        modifications.setPrice(entree.getPrice());
        modifications.setStock(entree.getStock());
        modifications.setCategoryId(entree.getCategoryId());
        Optional<Produit> modifie = dao.modifier(id, modifications);
        if (modifie.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return dao.trouver(id)
                .map(v -> Response.ok(v).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("{id}")
    public Response supprimer(@PathParam("id") Long id) {
        return dao.supprimer(id)
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }
}
