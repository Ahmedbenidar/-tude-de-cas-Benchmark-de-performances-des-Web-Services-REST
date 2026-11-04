"""Genere les jeux de donnees utilises par les plans JMeter.

Tous les fichiers utilisent la barre verticale comme separateur et des fins de
ligne Unix : les valeurs generees n'en contiennent aucune, le corps JSON peut
donc etre reconstruit directement dans le plan, sans echappement.

Les identifiants sont repartis en trois zones disjointes (lecture, mise a jour,
suppression) : une suppression ne peut jamais invalider un identifiant encore
utilise par une lecture du meme scenario.
"""

import csv
import os
import random

GRAINE = 20261103
BASE = r"C:\Users\dell\Desktop\tp204_benchmark_rest\jmeter\donnees"

MOTS = ["capteur", "module", "carte", "boitier", "connecteur", "relais", "sonde",
        "adaptateur", "support", "cable", "antenne", "batterie", "ventilateur",
        "radiateur", "filtre", "fusible", "rail", "plot", "vis", "entretoise"]


def ecrire(nom, entete, lignes):
    chemin = os.path.join(BASE, nom)
    with open(chemin, "w", newline="", encoding="utf-8") as f:
        redacteur = csv.writer(f, delimiter="|", lineterminator="\n")
        redacteur.writerow(entete)
        redacteur.writerows(lignes)
    return chemin


def texte(longueur):
    morceaux, total = [], 0
    while total < longueur:
        mot = random.choice(MOTS)
        morceaux.append(mot)
        total += len(mot) + 1
    return " ".join(morceaux)[:longueur]


def main():
    random.seed(GRAINE)
    os.makedirs(BASE, exist_ok=True)

    items = random.sample(range(1, 100001), 60000)
    ecrire("ids-items-lecture.csv", ["idItem"], [[i] for i in items[:20000]])
    ecrire("ids-items-modif.csv", ["idItem"], [[i] for i in items[20000:40000]])
    ecrire("ids-items-suppression.csv", ["idItem"], [[i] for i in items[40000:60000]])

    categories = random.sample(range(1, 2001), 1600)
    ecrire("ids-categories-lecture.csv", ["idCategorie"],
           [[c] for c in categories[:800]])
    ecrire("ids-categories-ecriture.csv", ["idCategorie"],
           [[c] for c in categories[800:1600]])

    for nom, taille in (("corps-leger.csv", 900), ("corps-lourd.csv", 4900)):
        lignes = []
        for i in range(300):
            lignes.append([
                "Article %03d" % i,
                texte(taille),
                "%.2f" % round(random.uniform(5, 900), 2),
                random.randint(0, 500),
                random.randint(1, 2000),
            ])
        ecrire(nom, ["nom", "description", "prix", "stock", "idCategorie"], lignes)

    print("jeux de donnees JMeter generes dans %s" % BASE)


if __name__ == "__main__":
    main()
