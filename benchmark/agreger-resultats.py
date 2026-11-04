"""Agrege les fichiers JTL de la campagne et produit les tableaux T2, T4 et T5.

Le JTL est lu en CSV : une ligne par requete. Le debit est calcule sur la
fenetre reellement couverte par les threads (dernier depart - premier depart)
et non sur la duree demandee, sinon le ramp-up fausse la moyenne.
"""

import csv
import glob
import os
import statistics

BASE = r"C:\Users\dell\Desktop\tp204_benchmark_rest\resultats\campagne"
SORTIE = r"C:\Users\dell\Desktop\tp204_benchmark_rest\resultats"


def percentile(valeurs, p):
    if not valeurs:
        return 0.0
    valeurs = sorted(valeurs)
    rang = (len(valeurs) - 1) * p / 100.0
    bas = int(rang)
    haut = min(bas + 1, len(valeurs) - 1)
    return valeurs[bas] + (valeurs[haut] - valeurs[bas]) * (rang - bas)


def lire(chemin):
    with open(chemin, newline="", encoding="utf-8") as f:
        lignes = list(csv.DictReader(f))
    for l in lignes:
        l["elapsed"] = float(l["elapsed"])
        l["timeStamp"] = int(l["timeStamp"])
        l["success"] = l["success"] == "true"
        l["responseCode"] = l.get("responseCode", "")
    return derniere_execution(lignes)


def derniere_execution(lignes):
    """Ne garde que le dernier passage contigu du fichier.

    Un JTL est ouvert en mode append : si une campagne est relancee sur le meme
    fichier, les deux passages s'y retrouvent et la fenetre temporelle devient
    absurde (le debit s'effondre). On decoupe donc le fichier aux coupures de
    plus de 15 s et on ne conserve que le dernier bloc.
    """
    if not lignes:
        return lignes
    ordonnees = sorted(lignes, key=lambda l: l["timeStamp"])
    blocs = [[ordonnees[0]]]
    for ligne in ordonnees[1:]:
        if ligne["timeStamp"] - blocs[-1][-1]["timeStamp"] > 15000:
            blocs.append([ligne])
        else:
            blocs[-1].append(ligne)
    return blocs[-1]



def resume(lignes):
    if not lignes:
        return None
    debuts = [l["timeStamp"] for l in lignes]
    fenetre = (max(debuts) - min(debuts)) / 1000.0
    durees = [l["elapsed"] for l in lignes]
    erreurs = [l for l in lignes if not l["success"]]
    return {
        "n": len(lignes),
        "rps": len(lignes) / fenetre if fenetre > 0 else 0.0,
        "p50": percentile(durees, 50),
        "p95": percentile(durees, 95),
        "p99": percentile(durees, 99),
        "moy": statistics.mean(durees),
        "err": 100.0 * len(erreurs) / len(lignes),
        "codes": sorted(set(l["responseCode"] for l in erreurs)),
    }


def paliers():
    """Retrouve (scenario, variante, threads) a partir du nom de fichier."""
    for chemin in sorted(glob.glob(os.path.join(BASE, "s*-variante-*-t*.jtl"))):
        nom = os.path.basename(chemin)[:-4]
        morceaux = nom.split("-")
        yield morceaux[0][1:], morceaux[2], int(morceaux[3][1:]), chemin


def main():
    donnees = {}
    for scenario, variante, threads, chemin in paliers():
        donnees[(scenario, variante, threads)] = lire(chemin)

    with open(os.path.join(SORTIE, "t2-resume.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter=";")
        w.writerow(["scenario", "variante", "threads", "requetes", "rps",
                    "p50_ms", "p95_ms", "p99_ms", "moyenne_ms", "erreurs_pct", "codes_erreur"])
        for (s, v, t), lignes in sorted(donnees.items()):
            r = resume(lignes)
            if not r:
                continue
            w.writerow([s, v, t, r["n"], round(r["rps"], 1),
                        round(r["p50"], 1), round(r["p95"], 1), round(r["p99"], 1),
                        round(r["moy"], 1), round(r["err"], 2), " ".join(r["codes"])])

    with open(os.path.join(SORTIE, "t4-t5-endpoints.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter=";")
        w.writerow(["scenario", "variante", "threads", "endpoint", "rps",
                    "p95_ms", "erreurs_pct", "codes_erreur"])
        for (s, v, t), lignes in sorted(donnees.items()):
            par_label = {}
            for l in lignes:
                par_label.setdefault(l["label"], []).append(l)
            for label, sous in sorted(par_label.items()):
                r = resume(sous)
                w.writerow([s, v, t, label, round(r["rps"], 1), round(r["p95"], 1),
                            round(r["err"], 2), " ".join(r["codes"])])

    print("t2-resume.csv et t4-t5-endpoints.csv ecrits dans", SORTIE)


if __name__ == "__main__":
    main()
