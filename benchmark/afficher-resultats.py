"""Affiche les tableaux de resultats agreges dans la console (sortie capturee en image)."""

import csv
import os
import sys

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESULTATS = os.path.join(RACINE, "resultats")

LIBELLES_VARIANTES = {
    "a": "A - Jersey + Hibernate  ",
    "c": "C - Spring RestController",
    "d": "D - Spring Data REST   ",
}

LIBELLES_SCENARIOS = {
    1: "S1 - LECTURE INTENSIVE (READ-heavy)",
    2: "S2 - RECHERCHE FILTREE (JOIN-filter)",
    3: "S3 - CHARGE MIXTE (MIXED)",
    4: "S4 - GROS VOLUME (HEAVY-body 5 Ko)",
}

ENTETES = [
    ("variante", "Variante", 27),
    ("threads", "Thr.", 5),
    ("requetes", "Requetes", 9),
    ("rps", "Req/s", 9),
    ("p50_ms", "p50 ms", 8),
    ("p95_ms", "p95 ms", 9),
    ("p99_ms", "p99 ms", 9),
    ("moyenne_ms", "Moy. ms", 9),
    ("erreurs_pct", "Err. %", 7),
]

ENTETES_ENDPOINT = [
    ("endpoint", "Endpoint", 27),
    ("rps", "Req/s", 9),
    ("p95_ms", "p95 ms", 9),
    ("erreurs_pct", "Err. %", 7),
]


def ligne_separatrice(largeurs):
    return "+" + "+".join("-" * (l + 2) for l in largeurs) + "+"


def formater(valeur, largeur, cle):
    if cle == "variante":
        valeur = LIBELLES_VARIANTES.get(valeur, valeur)
    return str(valeur).rjust(largeur)


def afficher_tableau(entetes, lignes):
    largeurs = [e[2] for e in entetes]
    print(ligne_separatrice(largeurs))
    print("|" + "|".join(" " + e[1].ljust(e[2]) + " " for e in entetes) + "|")
    print(ligne_separatrice(largeurs))
    for ligne in lignes:
        cellules = []
        for cle, _, largeur in entetes:
            cellules.append(" " + formater(ligne.get(cle, ""), largeur, cle) + " ")
        print("|" + "|".join(cellules) + "|")
    print(ligne_separatrice(largeurs))


def lire(nom):
    chemin = os.path.join(RESULTATS, nom)
    with open(chemin, encoding="utf-8") as fichier:
        return list(csv.DictReader(fichier, delimiter=";"))


def bloc_t2(resume, scenario):
    print("=" * 78)
    print(" T2 - " + LIBELLES_SCENARIOS[scenario])
    print(" Campagne du 2026-11-04 - paliers de 60 s, montee en charge 10 s")
    print("=" * 78)
    lignes = [l for l in resume if int(l["scenario"]) == scenario]
    lignes.sort(key=lambda l: (l["variante"], int(l["threads"])))
    afficher_tableau(ENTETES, lignes)


def bloc_endpoints(endpoints, scenario):
    print("=" * 78)
    print(" T4 / T5 - DETAIL PAR ENDPOINT - " + LIBELLES_SCENARIOS[scenario])
    print("=" * 78)
    lignes = [l for l in endpoints if int(l["scenario"]) == scenario]
    lignes.sort(key=lambda l: (l["variante"], int(l["threads"]), l["endpoint"]))
    afficher_tableau(ENTETES_ENDPOINT, lignes)


def main():
    resume = lire("t2-resume.csv")
    endpoints = lire("t4-t5-endpoints.csv")

    dossier = os.path.join(RACINE, "docs", "logs")
    for scenario in (1, 2, 3, 4):
        with open(os.path.join(dossier, "t2-scenario-%d.log" % scenario), "w", encoding="utf-8") as sortie:
            rediriger(bloc_t2, sortie, resume, scenario)
        with open(os.path.join(dossier, "t4-t5-scenario-%d.log" % scenario), "w", encoding="utf-8") as sortie:
            rediriger(bloc_endpoints, sortie, endpoints, scenario)


def rediriger(fonction, flux, *arguments):
    ancien = sys.stdout
    sys.stdout = flux
    try:
        fonction(*arguments)
    finally:
        sys.stdout = ancien


if __name__ == '__main__':
    main()
