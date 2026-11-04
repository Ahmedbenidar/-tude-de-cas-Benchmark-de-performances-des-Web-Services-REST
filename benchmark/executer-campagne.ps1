# Lance la campagne de mesure complete : 4 scenarios x 3 variantes.
#
# Chaque palier de concurrence est un run JMeter distinct (meme plan .jmx,
# seul -Jthreads change) afin de conserver les fichiers JTL par palier et de
# pouvoir tracer la montee en charge. La base est remise dans son etat de
# reference avant chaque run, car MIXED et HEAVY-body creent puis suppriment
# des lignes et useraient sinon le pool d'identifiants.
#
# Duree volontairement reduite par rapport au sujet (60 s par palier au lieu
# de 8 a 10 min) : la campagne doit rester reproductible sur un poste unique
# ou le client de charge et les trois serveurs se partagent les memes 4 coeurs.

param(
    [string[]]$Variante = @("a", "c", "d"),
    [string[]]$Scenario = @("1", "2", "3", "4")
)

$Racine = "C:\Users\dell\Desktop\tp204_benchmark_rest"
$Jmeter = "C:\Users\dell\tools\apache-jmeter-5.6.3\bin\jmeter.bat"
$Mysql  = "C:\xampp1\mysql\bin\mysql.exe"
$Sortie = Join-Path $Racine "resultats\campagne"
New-Item -ItemType Directory -Force -Path $Sortie | Out-Null

$Ports = @{ "a" = 9201; "c" = 9202; "d" = 9203 }
$Paliers = @{
    "1" = @(20, 60, 120)   # READ-heavy   : 50 -> 100 -> 200 dans le sujet
    "2" = @(30, 60)        # JOIN-filter  : 60 -> 120
    "3" = @(20, 40)        # MIXED        : 50 -> 100
    "4" = @(15, 30)        # HEAVY-body   : 30 -> 60
}

function Reinitialiser-Base {
    Get-Content (Join-Path $Racine "sql\00-reinitialiser.sql") |
        & $Mysql -u root --local-infile=1 | Out-Null
}

foreach ($v in $Variante) {
    $port = $Ports[$v]
    foreach ($s in $Scenario) {
        foreach ($t in $Paliers[$s]) {
            # Seuls MIXED et HEAVY-body ecrivent : les scenarios de lecture
            # peuvent enchainer les paliers sur un jeu de donnees intact.
            if ($s -eq "3" -or $s -eq "4") { Reinitialiser-Base }
            $nom  = "s$s-variante-$v-t$t"
            $plan = Join-Path $Racine "jmeter\scenario-$s-variante-$v.jmx"
            $jtl  = Join-Path $Sortie "$nom.jtl"
            $log  = Join-Path $Sortie "$nom.log"
            Write-Host "=== $nom  port $port  threads $t ==="
            $arguments = @(
                "-n", "-t", $plan,
                ("-Jport=" + $port),
                ("-Jthreads=" + $t),
                "-Jmontee=10",
                "-Jduree=60",
                "-l", $jtl, "-j", $log
            )
            & $Jmeter @arguments 2>&1 | Out-Null
        }
    }
}
Write-Host "Campagne terminee."
