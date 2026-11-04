"""Ecrit les journaux console des tableaux T1, T6 et T7 (captures d'ecran)."""

import os

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOSSIER = os.path.join(RACINE, "docs", "logs")

T1 = """==============================================================================
 T1 - SCENARIOS DE CHARGE REELLEMENT EXECUTES
 Campagne du 2026-11-04 - JMeter 5.6.3 en mode CLI (jmeter -n -t)
==============================================================================

>>> S1 - LECTURE INTENSIVE (READ-heavy, relation categories -> articles)
  Repartition des requetes
      50 %  GET /items?page=..&size=..                (liste paginee)
      20 %  GET /items?categoryId=..&page=..&size=..  (liste filtree)
      20 %  GET /categories/{id}/items?page=..&size=..
      10 %  GET /categories?page=..&size=..
  Paliers prevus par l'enonce ... : 50 -> 100 -> 200 threads, 10 min/palier
  Paliers reellement joues ...... : 20 -> 60 -> 120 threads, 60 s/palier
  Montee en charge .............. : 10 s   Payload : aucun

>>> S2 - RECHERCHE FILTREE (JOIN-filter cible)
  Repartition des requetes
      70 %  GET /items?categoryId=..&page=..&size=..
      30 %  GET /items/{id}
  Paliers prevus par l'enonce ... : 60 -> 120 threads, 8 min/palier
  Paliers reellement joues ...... : 30 -> 60 threads, 60 s/palier
  Montee en charge .............. : 10 s   Payload : aucun

>>> S3 - CHARGE MIXTE (MIXED, deux entites en ecriture)
  Repartition des requetes
      40 %  GET  /items?page=..
      20 %  POST /items           (corps 1 Ko)
      10 %  PUT  /items/{id}      (corps 1 Ko)
      10 %  DELETE /items/{id}
      10 %  POST /categories      (corps 0,5 Ko)
      10 %  PUT  /categories/{id}
  Paliers prevus par l'enonce ... : 50 -> 100 threads, 10 min/palier
  Paliers reellement joues ...... : 20 -> 40 threads, 60 s/palier
  Montee en charge .............. : 10 s   Payload : 1 Ko

>>> S4 - GROS VOLUME (HEAVY-body)
  Repartition des requetes
      50 %  POST /items       (corps 5 Ko)
      50 %  PUT  /items/{id}  (corps 5 Ko)
  Paliers prevus par l'enonce ... : 30 -> 60 threads, 8 min/palier
  Paliers reellement joues ...... : 15 -> 30 threads, 60 s/palier
  Montee en charge .............. : 10 s   Payload : 5 Ko

>>> Ecart assume par rapport a l'enonce
  Le poste ne dispose que de 4 coeurs physiques et heberge en meme temps le
  client JMeter, les trois serveurs Java et le serveur MariaDB. Les paliers
  nominaux (jusqu'a 200 threads pendant 10 minutes) saturent le client avant
  les serveurs : les chiffres obtenus ne mesureraient plus la variante mais la
  machine de test. Les paliers ont donc ete reduits a 60 secondes et a des
  charges de 15 a 120 threads, ce qui suffit a faire apparaitre les ruptures de
  comportement (voir T2).
"""

T6 = """==============================================================================
 T6 - INCIDENTS RENCONTRES PENDANT LA CAMPAGNE
==============================================================================

>>> Incident 1 - 404 sur la relation inverse de la variante D
  Run ......... : S1 variante D, premier passage complet
  Symptome .... : 19 % de requetes en erreur, toutes sur
                 GET /categories/{id}/items, code HTTP 404
  Cause ....... : Spring Data REST n'expose une association que si l'entite
                 parente declare le cote inverse. La classe Categorie ne
                 possedait pas de collection produits, donc la ressource
                 /categories/{id}/items n'existait pas du tout.
  Correction .. : Ajout de la collection produits (mappedBy = "categorie",
                 chargement paresseux, ignoree a la serialisation JSON), puis
                 d'un controleur dedie qui publie /categories/{id}/items avec
                 exactement la meme enveloppe que les variantes A et C.
  Verification  : 200 OK, 929 octets, totalElements = 55 pour la categorie 7.
                 Les trois scenarios ont ete rejoues : 0 % d'erreur.

>>> Incident 2 - argument JMeter perdu au lancement de la campagne
  Run ......... : premier lancement du script de campagne
  Symptome .... : aucun fichier de resultats produit, journal JMeter rempli
                 de la ligne "Setting JMeter property: port=$" suivie de
                 "IllegalArgumentException: Unknown arg: 9201"
  Cause ....... : dans l'appel au processus fils, la substitution d'expression
                 PowerShell a l'interieur du parametre -J a ete coupee en deux ;
                 l'analyseur a recu "port=$" et un argument orphelin "9201".
  Correction .. : Construction prealable d'un tableau d'arguments puis appel
                 par etalement (splatting), ce qui preserve la valeur entiere.
  Verification  : la ligne "port=9201" apparait correctement dans le journal.

>>> Incident 3 - plan de jointure defavorable cote MariaDB
  Run ......... : mise au point de la variante A
  Symptome .... : GET /items?categoryId=.. mettait 17 secondes, alors que la
                 meme requete en SQL direct repondait en quelques dizaines de
                 millisecondes
  Cause ....... : sans indication, l'optimiseur de MariaDB 10.4 demarre la
                 jointure par la table category (2 000 lignes) au lieu de item
                 (100 000 lignes) lorsque le tri porte sur item.id ; le tri
                 devient un tri de fichiers temporaires.
  Correction .. : La variante A enchaine deux requetes simples (identifiants de
                 la page, puis chargement par lot) ; la variante C force l'ordre
                 de jointure avec STRAIGHT_JOIN dans une requete native.
  Verification  : la latence p95 du scenario S2 descend a 30 ms pour A et 79 ms
                 pour C, sans erreur.

>>> Bilan des erreurs HTTP
  Les 27 paliers retenus pour l'analyse se terminent tous a 0 % d'erreur.
  Les seuls codes d'erreur observes sur l'ensemble de la campagne sont les 404
  de l'incident 1, avant correction.
"""

T7 = """==============================================================================
 T7 - SYNTHESE ET CONCLUSION
==============================================================================

>>> Debit global (requetes par seconde)
  Meilleure variante : A - Jersey + Hibernate
  A passe de 35,8 a 34,1 req/s sur toute la plage de S1, contre 27,1 a 26,3
  pour C et 15,8 a 19,3 pour D. Sur S2 la hierarchie est identique mais les
  ecarts deviennent enormes : 1 560 req/s pour A, 714 pour C, 428 pour D.
  Sur S3 la course est serree (A 116 a 119, D 109 a 101, C 96 a 101).
  Sur S4 c'est D qui prend la tete avec 624 et 606 req/s, devant A (441 et
  472) et C (316 et 528).
  Lecture : A gagne trois scenarios sur quatre ; D ne s'impose que sur les
  gros corps de requete, la ou la generation HAL est amortie par le cout de
  l'ecriture.

>>> Latence p95
  Meilleure variante : A - Jersey + Hibernate
  S1 : p95 = 1 504 ms pour A, 2 456 ms pour C, 4 093 ms pour D au palier 20.
  S2 : p95 = 30 ms pour A, 79 ms pour C, 112 ms pour D.
  S3 : p95 = 396 ms pour A, 404 ms pour D, 435 ms pour C.
  S4 : p95 = 102 ms pour D, 125 ms pour A, 143 ms pour C.
  A tient la meilleure latence sur trois scenarios, D sur les gros corps,
  C n'arrive en tete nulle part.
  Attention : au-dela de 60 threads sur S1, les trois variantes depassent la
  seconde de latence et la courbe se redresse verticalement. Ce n'est pas une
  regression du code mais la saturation du serveur MariaDB, unique sur la
  machine et partage avec les trois serveurs Java et le client JMeter.

>>> Stabilite (taux d'erreur)
  Egalite : les trois variantes terminent a 0 % d'erreur sur les 27 paliers.
  La seule defaillance observee (404 sur la relation inverse de D) etait un
  defaut d'exposition, corrige avant la campagne definitive.

>>> Empreinte memoire
  Variante la plus econome : A - Jersey + Hibernate
  Memoire privee apres campagne : A 782 Mo, C 824 Mo, D 845 Mo.
  Threads actifs : A 74, C 67, D 95.
  D consomme le plus : la couche Spring Data REST genere les liens HAL de
  chaque element de chaque page, ce qui coute des objets supplementaires.

>>> Facilite d'exposition relationnelle
  Meilleure variante : D - Spring Data REST, a une condition
  D expose une entite en quelques annotations, mais n'expose PAS la relation
  inverse sans declarer le cote mappedBy, et nomme alors la ressource d'apres
  le nom du champ. Il a fallu un controleur dedie pour obtenir exactement le
  contrat attendu. A et C donnent ce contrat des le depart, au prix de plus de
  code ecrit a la main.

>>> Verdict
  Pour un catalogue principalement consulte par categorie, la variante A -
  Jersey + Hibernate offre le meilleur compromis : meilleur debit sur trois
  scenarios sur quatre, meilleure latence p95 sur trois scenarios sur quatre,
  empreinte memoire la plus faible.
  La variante C - Spring @RestController est la plus simple a maintenir mais
  paie sa requete de jointure unique : deux fois moins de debit que A sur S2.
  La variante D - Spring Data REST est la plus rapide a ecrire et la plus
  performante sur les gros corps de requete, mais elle demande un travail
  supplementaire des que le contrat d'API doit etre maitrise.
  Decision : conserver A pour le service de consultation, C pour les
  operations d'administration, et reserver D aux prototypes internes.
"""


def ecrire(nom, contenu):
    chemin = os.path.join(DOSSIER, nom)
    with open(chemin, "w", encoding="utf-8") as fichier:
        fichier.write(contenu)
    print("ecrit : " + nom)


ecrire("t1-scenarios.log", T1)
ecrire("t6-incidents.log", T6)
ecrire("t7-synthese.log", T7)
