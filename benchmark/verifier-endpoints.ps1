# Rejoue les endpoints des trois variantes et journalise les reponses reelles.
# Le journal produit sert de source aux captures de terminal du README.
#
# Volontairement place sur des identifiants libres (zone 2001+ pour les
# categories, 100001+ pour les items) pour ne pas toucher au jeu de donnees
# 1..100000 utilise par la campagne de mesure.

$Racine = "C:\Users\dell\Desktop\tp204_benchmark_rest"
$Journal = Join-Path $Racine "docs\logs\verification-endpoints.log"

$Variantes = @(
    @{ Code = "A"; Port = 9201; Libelle = "Variante A - Jersey JAX-RS + Hibernate" },
    @{ Code = "C"; Port = 9202; Libelle = "Variante C - Spring Boot @RestController" },
    @{ Code = "D"; Port = 9203; Libelle = "Variante D - Spring Data REST" }
)

$lignes = New-Object System.Collections.Generic.List[string]

function Lire-Contenu {
    param($Reponse)
    if ($Reponse.Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Reponse.Content)
    }
    return [string]$Reponse.Content
}

function Tester {
    param($Methode, $Url, $Corps)
    try {
        $p = @{ Uri = $Url; Method = $Methode; UseBasicParsing = $true; TimeoutSec = 60 }
        if ($Corps) {
            $p["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Corps)
            $p["ContentType"] = "application/json; charset=utf-8"
        }
        $chrono = [System.Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-WebRequest @p
        $chrono.Stop()
        $texte = (Lire-Contenu $r) -replace "\s+", " "
        if ($texte.Length -gt 150) { $texte = $texte.Substring(0, 150) + " ..." }
        $script:lignes.Add(("  {0,-6} {1,4}  {2,5} ms  {3,7} o  {4}" -f $Methode, $r.StatusCode,
                [int]$chrono.ElapsedMilliseconds, $r.RawContentLength, $Url))
        if ($texte) { $script:lignes.Add("         corps : $texte") }
        return $texte
    }
    catch {
        $code = ""
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        $script:lignes.Add(("  {0,-6} '{1}'  ECHEC  {2}" -f $Methode, $code, $Url))
        return $null
    }
}

foreach ($v in $Variantes) {
    $b = "http://localhost:$($v.Port)"
    $lignes.Add("")
    $lignes.Add("==================================================================")
    $lignes.Add(" $($v.Libelle)")
    $lignes.Add(" $b")
    $lignes.Add("==================================================================")

    $lignes.Add("")
    $lignes.Add("-- Categories : liste, detail, creation, modification, suppression")
    Tester "GET" "$b/categories?page=0&size=5"
    Tester "GET" "$b/categories/7"

    $code = "VERIF-$($v.Code)"
    $reponse = Tester "POST" "$b/categories" ('{"code":"' + $code + '","name":"Categorie de verification"}')
    $idCat = 0
    if ($reponse -and $reponse -match '"id"\s*:\s*(\d+)') { $idCat = [int]$matches[1] }
    if ($idCat -gt 0) {
        Tester "PUT" "$b/categories/$idCat" ('{"code":"' + $code + '","name":"Categorie renommee"}')
        Tester "GET" "$b/categories/$idCat"
        Tester "DELETE" "$b/categories/$idCat"
        Tester "GET" "$b/categories/$idCat"
    }

    $lignes.Add("")
    $lignes.Add("-- Items : liste paginee, detail, filtre par categorie, relation inverse")
    Tester "GET" "$b/items?page=0&size=5"
    Tester "GET" "$b/items/4242"
    if ($v.Code -eq "D") {
        Tester "GET" "$b/items/search/parCategorie?categorieId=7&size=10"
        Tester "GET" "$b/items/4242/categorie"
    }
    else {
        Tester "GET" "$b/items?categoryId=7&page=0&size=10"
    }
    Tester "GET" "$b/categories/7/items?page=0&size=10"

    $lignes.Add("")
    $lignes.Add("-- Items : creation, relecture, modification, relecture, suppression")
    $sku = "VERIF-$($v.Code)-0001"
    $corps = '{"sku":"' + $sku + '","name":"Article de verification",' +
             '"description":"Ligne creee par le script de verification des endpoints.",' +
             '"price":123.45,"stock":7,"categoryId":7}'
    $reponse = Tester "POST" "$b/items" $corps
    $idItem = 0
    if ($reponse -and $reponse -match '"id"\s*:\s*(\d+)') { $idItem = [int]$matches[1] }
    if ($idItem -gt 0) {
        Tester "GET" "$b/items/$idItem"
        Tester "PUT" "$b/items/$idItem" ('{"sku":"' + $sku + '","name":"Article modifie",' +
            '"description":"Description remplacee par le script de verification.",' +
            '"price":999.90,"stock":3,"categoryId":7}')
        Tester "GET" "$b/items/$idItem"
        Tester "DELETE" "$b/items/$idItem"
        Tester "GET" "$b/items/$idItem"
    }
}

[System.IO.File]::WriteAllLines($Journal, $lignes, (New-Object System.Text.UTF8Encoding $false))
Write-Host "Journal ecrit : $Journal"
