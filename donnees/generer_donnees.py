#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Generateur du jeu de donnees de l'etude de cas.

Produit deux fichiers CSV prets a etre charges dans MariaDB / MySQL :
  - category.csv : 2 000 lignes (id, code, name, updated_at)
  - item.csv     : 100 000 lignes (id, sku, name, price, stock, category_id, updated_at)

Les identifiants sont explicites pour que les fichiers JMeter puissent
reutiliser un pool d'ids stable d'une execution a l'autre.
"""
import csv
import os
import random
import sys
from datetime import datetime, timedelta

NB_CATEGORIES = 2000
NB_ITEMS = 100000
GRAINE = 20261103

THEMES = [
    "Informatique", "Telephonie", "Electromenager", "Mobilier", "Eclairage",
    "Outillage", "Jardinage", "Sport", "Loisirs", "Cuisine",
    "Literie", "Salle de bain", "Decoration", "Automobile", "Bricolage",
    "Papeterie", "Audio", "Video", "Photo", "Gaming",
]
FAMILLES = [
    "Standard", "Premium", "Pro", "Eco", "Compact", "Grand format",
    "Connecte", "Nomade", "Industriel", "Scolaire",
]
UNITES = [
    "lot de 2", "lot de 4", "unite", "pack", "coffret", "recharge",
]


def ecrire_categories(chemin, alea, base_date):
    with open(chemin, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        for i in range(1, NB_CATEGORIES + 1):
            theme = THEMES[(i - 1) % len(THEMES)]
            famille = FAMILLES[(i // len(THEMES)) % len(FAMILLES)]
            code = "CAT-%04d" % i
            nom = "%s %s %03d" % (theme, famille, i)
            date = base_date - timedelta(minutes=alea.randint(0, 525600))
            w.writerow([i, code, nom, date.strftime("%Y-%m-%d %H:%M:%S")])


def ecrire_items(chemin, alea, base_date):
    with open(chemin, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        for i in range(1, NB_ITEMS + 1):
            # Repartition volontairement irreguliere : 50 produits en moyenne,
            # mais de 1 a 200 selon la categorie, pour rendre le filtre
            # par categorie reellement discriminant.
            categorie = alea.randint(1, NB_CATEGORIES)
            theme = THEMES[(categorie - 1) % len(THEMES)]
            famille = FAMILLES[(categorie // len(THEMES)) % len(FAMILLES)]
            unite = UNITES[i % len(UNITES)]
            nom = "%s %s %s %06d" % (theme, famille, unite, i)
            sku = "SKU-%08d" % i
            prix = round(alea.uniform(9.9, 4999.0), 2)
            stock = alea.choice([0, alea.randint(1, 50), alea.randint(50, 900)])
            date = base_date - timedelta(minutes=alea.randint(0, 525600))
            w.writerow([i, sku, nom, "%.2f" % prix, stock, categorie,
                        date.strftime("%Y-%m-%d %H:%M:%S")])


def main():
    dossier = os.path.dirname(os.path.abspath(__file__))
    alea = random.Random(GRAINE)
    base_date = datetime(2026, 11, 1, 12, 0, 0)

    chemin_cat = os.path.join(dossier, "category.csv")
    chemin_item = os.path.join(dossier, "item.csv")

    ecrire_categories(chemin_cat, alea, base_date)
    ecrire_items(chemin_item, alea, base_date)

    print("category.csv : %d lignes" % NB_CATEGORIES)
    print("item.csv     : %d lignes" % NB_ITEMS)
    return 0


if __name__ == "__main__":
    sys.exit(main())
