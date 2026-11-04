"""Genere les plans JMeter de l'etude de cas.

Les quatre scenarios sont decrits une seule fois puis declines pour les trois
variantes : seules changent les URL qui different reellement (la variante D
expose la recherche par relation sous /items/search/parCategorie et non
/items?categoryId=).

Aucune valeur de charge n'est figee dans les fichiers produits : hote, port,
threads, montee et duree sont lus au lancement avec -J..., ce qui permet de
rejouer exactement le meme plan sur chaque palier de concurrence.
"""

import os

BASE = r"C:\Users\dell\Desktop\tp204_benchmark_rest"
DONNEES = BASE + "/jmeter/donnees"
SORTIE = BASE + "/jmeter"

VARIANTES = {
    "a": {"libelle": "A - Jersey Hibernate", "port": 9201,
          "cheminCategorie": "/items",
          "argsCategorie": [("categoryId", "${idCategorie}"),
                            ("page", "${page}"), ("size", "50")]},
    "c": {"libelle": "C - Spring RestController", "port": 9202,
          "cheminCategorie": "/items",
          "argsCategorie": [("categoryId", "${idCategorie}"),
                            ("page", "${page}"), ("size", "50")]},
    "d": {"libelle": "D - Spring Data REST", "port": 9203,
          "cheminCategorie": "/items/search/parCategorie",
          "argsCategorie": [("categorieId", "${idCategorie}"), ("size", "50")]},
}

CORPS_ITEM = ('{"sku":"P-${__RandomString(12,abcdefghijklmnopqrstuvwxyz0123456789)}",'
              '"name":"${nom}","description":"${description}","price":${prix},'
              '"stock":${stock},"categoryId":${idCategorie}}')

VARIABLES_CORPS = "nom,description,prix,stock,idCategorie"


def variable(nom, valeur):
    return ('<elementProp name="%s" elementType="Argument">'
            '<stringProp name="Argument.name">%s</stringProp>'
            '<stringProp name="Argument.value">%s</stringProp>'
            '<stringProp name="Argument.metadata">=</stringProp>'
            '</elementProp>' % (nom, nom, valeur))


def csv(nom, fichier, variables, boucler="true"):
    return ('<CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" '
            'testname="%s" enabled="true">'
            '<stringProp name="filename">%s</stringProp>'
            '<stringProp name="fileEncoding">UTF-8</stringProp>'
            '<stringProp name="variableNames">%s</stringProp>'
            '<boolProp name="ignoreFirstLine">true</boolProp>'
            '<stringProp name="delimiter">|</stringProp>'
            '<boolProp name="quotedData">false</boolProp>'
            '<boolProp name="recycle">%s</boolProp>'
            '<boolProp name="stopThread">false</boolProp>'
            '<stringProp name="shareMode">shareMode.all</stringProp>'
            '</CSVDataSet>' % (nom, fichier, variables, boucler))


def parametre(nom, valeur):
    return ('<elementProp name="%s" elementType="HTTPArgument">'
            '<boolProp name="HTTPArgument.always_encode">false</boolProp>'
            '<stringProp name="Argument.name">%s</stringProp>'
            '<stringProp name="Argument.value">%s</stringProp>'
            '<stringProp name="Argument.metadata">=</stringProp>'
            '<boolProp name="HTTPArgument.use_equals">true</boolProp>'
            '</elementProp>' % (nom, nom, valeur))


def assertion(nom):
    """Un code hors 2xx doit compter comme une erreur dans le rapport."""
    return ('<ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" '
            'testname="%s" enabled="true">'
            '<collectionProp name="Asserion.test_strings">'
            '<stringProp name="49586">2..</stringProp></collectionProp>'
            '<stringProp name="Assertion.custom_message"></stringProp>'
            '<stringProp name="Assertion.test_field">Assertion.response_code</stringProp>'
            '<boolProp name="Assertion.assume_success">false</boolProp>'
            '<intProp name="Assertion.test_type">2</intProp>'
            '</ResponseAssertion>' % nom)


def requete(nom, methode, chemin, args=None, corps=None):
    args = args or []
    if corps is None:
        porteur = ('<collectionProp name="Arguments.arguments">%s</collectionProp>'
                   % "".join(parametre(n, v) for n, v in args))
        brut = "false"
    else:
        porteur = ('<collectionProp name="Arguments.arguments">'
                   '<elementProp name="" elementType="HTTPArgument">'
                   '<boolProp name="HTTPArgument.always_encode">false</boolProp>'
                   '<stringProp name="Argument.value">%s</stringProp>'
                   '<stringProp name="Argument.metadata">=</stringProp>'
                   '</elementProp></collectionProp>' % corps)
        brut = "true"
    return ('<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" '
            'testname="%s" enabled="true">'
            '<elementProp name="HTTPsampler.Arguments" elementType="Arguments" '
            'guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="Parametres" enabled="true">'
            '%s</elementProp>'
            '<stringProp name="HTTPSampler.domain"></stringProp>'
            '<stringProp name="HTTPSampler.port"></stringProp>'
            '<stringProp name="HTTPSampler.protocol"></stringProp>'
            '<stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>'
            '<stringProp name="HTTPSampler.path">%s</stringProp>'
            '<stringProp name="HTTPSampler.method">%s</stringProp>'
            '<boolProp name="HTTPSampler.follow_redirects">true</boolProp>'
            '<boolProp name="HTTPSampler.auto_redirects">false</boolProp>'
            '<boolProp name="HTTPSampler.use_keepalive">true</boolProp>'
            '<boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>'
            '<boolProp name="HTTPSampler.postBodyRaw">%s</boolProp>'
            '<stringProp name="HTTPSampler.embedded_url_re"></stringProp>'
            '</HTTPSamplerProxy><hashTree>%s<hashTree/></hashTree>'
            % (nom, porteur, chemin, methode, brut, assertion(nom)))


def ponderateur(nom, pourcentage, noeud):
    return ('<ThroughputController guiclass="ThroughputControllerGui" '
            'testclass="ThroughputController" testname="%s" enabled="true">'
            '<intProp name="ThroughputController.style">1</intProp>'
            '<boolProp name="ThroughputController.perThread">false</boolProp>'
            '<intProp name="ThroughputController.maxThroughput">1</intProp>'
            '<stringProp name="ThroughputController.percentThroughput">%s</stringProp>'
            '</ThroughputController><hashTree>%s</hashTree>' % (nom, pourcentage, noeud))


def scenarios(v):
    lectureCat = csv("CSV categories", DONNEES + "/ids-categories-lecture.csv", "idCategorie")
    lectureItem = csv("CSV items", DONNEES + "/ids-items-lecture.csv", "idItem")
    poolModif = csv("CSV items a modifier", DONNEES + "/ids-items-modif.csv", "idItem")
    poolSuppr = csv("CSV items a supprimer", DONNEES + "/ids-items-suppression.csv",
                    "idItem", "false")
    poolCat = csv("CSV categories a modifier", DONNEES + "/ids-categories-ecriture.csv",
                  "idCategorie")
    corpsLeger = csv("CSV corps 1 Ko", DONNEES + "/corps-leger.csv", VARIABLES_CORPS)
    corpsLourd = csv("CSV corps 5 Ko", DONNEES + "/corps-lourd.csv", VARIABLES_CORPS)

    read = ("READ-heavy", [lectureItem, lectureCat, corpsLeger], [
        ponderateur("Liste items 50 pourcent", "50.0", requete(
            "GET items page et size", "GET", "/items",
            [("page", "${__Random(0,1500)}"), ("size", "50")])),
        ponderateur("Items d une categorie 20 pourcent", "20.0", requete(
            "GET items par categorie", "GET", v["cheminCategorie"], v["argsCategorie"])),
        ponderateur("Categorie vers items 20 pourcent", "20.0", requete(
            "GET categorie id items", "GET", "/categories/${idCategorie}/items",
            [("page", "${__Random(0,3)}"), ("size", "50")])),
        ponderateur("Liste categories 10 pourcent", "10.0", requete(
            "GET categories page et size", "GET", "/categories",
            [("page", "${__Random(0,30)}"), ("size", "50")])),
    ])

    filtre = ("JOIN-filter", [lectureItem, lectureCat, corpsLeger], [
        ponderateur("Filtre par categorie 70 pourcent", "70.0", requete(
            "GET items filtres par categorie", "GET", v["cheminCategorie"], v["argsCategorie"])),
        ponderateur("Item par identifiant 30 pourcent", "30.0", requete(
            "GET item par id", "GET", "/items/${idItem}")),
    ])

    mixed = ("MIXED", [lectureItem, lectureCat, corpsLeger, poolModif, poolSuppr, poolCat], [
        ponderateur("Liste items 40 pourcent", "40.0", requete(
            "GET items page", "GET", "/items",
            [("page", "${__Random(0,1500)}"), ("size", "20")])),
        ponderateur("Creation item 20 pourcent", "20.0", requete(
            "POST items 1 Ko", "POST", "/items", corps=CORPS_ITEM)),
        ponderateur("Modification item 10 pourcent", "10.0", requete(
            "PUT items id 1 Ko", "PUT", "/items/${idItem}", corps=CORPS_ITEM)),
        ponderateur("Suppression item 10 pourcent", "10.0", requete(
            "DELETE items id", "DELETE", "/items/${idItem}")),
        ponderateur("Creation categorie 10 pourcent", "10.0", requete(
            "POST categories", "POST", "/categories",
            corps='{"code":"C-${__RandomString(10,abcdefghijklmnopqrstuvwxyz0123456789)}",'
                  '"name":"Categorie de test"}')),
        ponderateur("Modification categorie 10 pourcent", "10.0", requete(
            "PUT categories id", "PUT", "/categories/${idCategorie}",
            corps='{"code":"MAJ-${idCategorie}","name":"Categorie mise a jour"}')),
    ])

    lourd = ("HEAVY-body", [lectureItem, lectureCat, corpsLourd, poolModif], [
        ponderateur("Creation item 5 Ko 50 pourcent", "50.0", requete(
            "POST items 5 Ko", "POST", "/items", corps=CORPS_ITEM)),
        ponderateur("Modification item 5 Ko 50 pourcent", "50.0", requete(
            "PUT items id 5 Ko", "PUT", "/items/${idItem}", corps=CORPS_ITEM)),
    ])

    return [("1", read), ("2", filtre), ("3", mixed), ("4", lourd)]


ENTETES = ('<HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" '
           'testname="Entetes JSON" enabled="true">'
           '<collectionProp name="HeaderManager.headers">'
           '<elementProp name="" elementType="Header">'
           '<stringProp name="Header.name">Content-Type</stringProp>'
           '<stringProp name="Header.value">application/json</stringProp></elementProp>'
           '<elementProp name="" elementType="Header">'
           '<stringProp name="Header.name">Accept</stringProp>'
           '<stringProp name="Header.value">application/json</stringProp></elementProp>'
           '</collectionProp></HeaderManager>')

DEFAUTS = ('<ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" '
           'testname="Parametres HTTP par defaut" enabled="true">'
           '<elementProp name="HTTPsampler.Arguments" elementType="Arguments" '
           'guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="Parametres" enabled="true">'
           '<collectionProp name="Arguments.arguments"/></elementProp>'
           '<stringProp name="HTTPSampler.domain">${hote}</stringProp>'
           '<stringProp name="HTTPSampler.port">${port}</stringProp>'
           '<stringProp name="HTTPSampler.protocol">${protocole}</stringProp>'
           '<stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>'
           '<stringProp name="HTTPSampler.connect_timeout">10000</stringProp>'
           '<stringProp name="HTTPSampler.response_timeout">120000</stringProp>'
           '<boolProp name="HTTPSampler.use_keepalive">true</boolProp>'
           '<boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>'
           '</ConfigTestElement>')

GROUPE = ('<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" '
          'testname="Palier de charge" enabled="true">'
          '<stringProp name="ThreadGroup.on_sample_error">continue</stringProp>'
          '<elementProp name="ThreadGroup.main_controller" elementType="LoopController" '
          'guiclass="LoopControlPanel" testclass="LoopController" testname="Boucle" enabled="true">'
          '<boolProp name="LoopController.continue_forever">true</boolProp>'
          '<stringProp name="LoopController.loops">-1</stringProp></elementProp>'
          '<stringProp name="ThreadGroup.num_threads">${threads}</stringProp>'
          '<stringProp name="ThreadGroup.ramp_time">${montee}</stringProp>'
          '<boolProp name="ThreadGroup.scheduler">true</boolProp>'
          '<stringProp name="ThreadGroup.duration">${duree}</stringProp>'
          '<boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>'
          '</ThreadGroup>')

# Export temps reel vers InfluxDB : present pour respecter le livrable attendu,
# desactive car aucun serveur InfluxDB n'est installe sur le poste de mesure.
BACKEND = ('<BackendListener guiclass="BackendListenerGui" testclass="BackendListener" '
           'testname="Backend Listener InfluxDB desactive" enabled="false">'
           '<elementProp name="arguments" elementType="Arguments" guiclass="ArgumentsPanel" '
           'testclass="Arguments" enabled="true">'
           '<collectionProp name="Arguments.arguments">'
           '<elementProp name="influxdbMetricsSender" elementType="Argument">'
           '<stringProp name="Argument.name">influxdbMetricsSender</stringProp>'
           '<stringProp name="Argument.value">'
           'org.apache.jmeter.visualizers.backend.influxdb.HttpMetricsSender</stringProp>'
           '<stringProp name="Argument.metadata">=</stringProp></elementProp>'
           '<elementProp name="influxdbUrl" elementType="Argument">'
           '<stringProp name="Argument.name">influxdbUrl</stringProp>'
           '<stringProp name="Argument.value">'
           'http://localhost:8086/api/v2/write?bucket=jmeter&amp;org=perf</stringProp>'
           '<stringProp name="Argument.metadata">=</stringProp></elementProp>'
           '</collectionProp></elementProp>'
           '<stringProp name="classname">'
           'org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient'
           '</stringProp></BackendListener>')


def construire(v, numero, nom, fichiers, noeuds):
    variables = "".join([
        variable("hote", "${__P(hote,localhost)}"),
        variable("port", "${__P(port,%d)}" % v["port"]),
        variable("protocole", "${__P(protocole,http)}"),
        variable("threads", "${__P(threads,50)}"),
        variable("montee", "${__P(montee,10)}"),
        variable("duree", "${__P(duree,60)}"),
        variable("page", "${__P(page,0)}"),
        variable("variante", v["libelle"]),
    ])
    titre = "Scenario %s %s - variante %s" % (numero, nom, v["libelle"])
    plan = ('<TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="%s" enabled="true">'
            '<stringProp name="TestPlan.comments">Etude de cas benchmark REST, variante %s'
            '</stringProp>'
            '<boolProp name="TestPlan.functional_mode">false</boolProp>'
            '<boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>'
            '<boolProp name="TestPlan.serialize_threadgroups">false</boolProp>'
            '<elementProp name="TestPlan.user_defined_variables" elementType="Arguments" '
            'guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables" enabled="true">'
            '<collectionProp name="Arguments.arguments">%s</collectionProp></elementProp>'
            '<stringProp name="TestPlan.user_define_classpath"></stringProp>'
            '</TestPlan>' % (titre, v["libelle"], variables))

    sans_enfant = "".join("%s<hashTree/>\n" % e
                          for e in [DEFAUTS, ENTETES] + fichiers + [BACKEND])
    arbre = (plan + "\n<hashTree>\n" + sans_enfant
             + GROUPE + "<hashTree>" + "".join(noeuds) + "</hashTree>"
             + "\n</hashTree>")
    return ('<?xml version="1.0" encoding="UTF-8"?>\n'
            '<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">\n<hashTree>\n'
            + arbre + "\n</hashTree>\n</jmeterTestPlan>\n")


def main():
    total = 0
    for vcode, v in VARIANTES.items():
        for numero, (nom, fichiers, noeuds) in scenarios(v):
            xml = construire(v, numero, nom, fichiers, noeuds)
            chemin = os.path.join(SORTIE, "scenario-%s-variante-%s.jmx" % (numero, vcode))
            with open(chemin, "w", encoding="utf-8", newline="\n") as f:
                f.write(xml)
            total += 1
    print("%d plans generes" % total)


if __name__ == "__main__":
    main()
