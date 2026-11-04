# Echantillonne les ressources JVM des trois variantes pendant un palier de
# charge identique (scenario READ-heavy, 60 threads, 90 s).
#
# Prometheus et Grafana ne sont pas installes sur le poste de mesure : les
# chiffres du tableau T3 sont donc releves directement sur la JVM avec les
# outils du JDK (jcmd pour le tas et les classes, jstat pour le GC) et avec
# le compteur CPU du systeme pour le processus.

param(
    [int]$Threads = 60,
    [int]$Duree = 90,
    [int]$Periode = 3
)

$Racine = "C:\Users\dell\Desktop\tp204_benchmark_rest"
$Jmeter = "C:\Users\dell\tools\apache-jmeter-5.6.3\bin\jmeter.bat"
$Jcmd   = "C:\Program Files\Java\jdk-21\bin\jcmd.exe"
$Jstat  = "C:\Program Files\Java\jdk-21\bin\jstat.exe"
$Journal = Join-Path $Racine "docs\logs\ressources-jvm.log"

# Les identifiants de processus changent a chaque redemarrage : on les retrouve
# a partir de la ligne de commande de chaque serveur.
function Trouver-Pid {
    param([string]$Motif)
    $p = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -like "*$Motif*" } |
        Select-Object -First 1
    if ($p) { return [int]$p.ProcessId }
    return 0
}

$Variantes = @(
    @{ Code = "A"; Port = 9201; Motif = "variante-a-jersey-hibernate"; Libelle = "Variante A - Jersey JAX-RS + Hibernate" },
    @{ Code = "C"; Port = 9202; Motif = "variante-c-spring-restcontroller"; Libelle = "Variante C - Spring Boot @RestController" },
    @{ Code = "D"; Port = 9203; Motif = "variante-d-spring-data-rest"; Libelle = "Variante D - Spring Data REST" }
)
foreach ($v in $Variantes) { $v["Pid"] = Trouver-Pid $v["Motif"] }

$lignes = New-Object System.Collections.Generic.List[string]
$lignes.Add("Releve des ressources JVM pendant un palier READ-heavy")
$lignes.Add("Threads : $Threads   duree : $Duree s   periode d echantillonnage : $Periode s")
$lignes.Add("Outils : jcmd GC.heap_info, jstat -gcutil, compteur CPU du processus")
$lignes.Add("")

foreach ($v in $Variantes) {
    $lignes.Add("==================================================================")
    $lignes.Add(" $($v.Libelle)   pid $($v.Pid)   port $($v.Port)")
    $lignes.Add("==================================================================")

    $plan = Join-Path $Racine "jmeter\scenario-1-variante-$($v.Code.ToLower()).jmx"
    $jtl  = Join-Path $Racine "resultats\campagne\ressources-$($v.Code.ToLower()).jtl"
    $log  = Join-Path $Racine "resultats\campagne\ressources-$($v.Code.ToLower()).log"
    $arguments = @("-n", "-t", $plan, ("-Jport=" + $v.Port), ("-Jthreads=" + $Threads),
        "-Jmontee=10", ("-Jduree=" + $Duree), "-l", $jtl, "-j", $log)

    $processus = Get-Process -Id $v.Pid
    $cpuAvant = $processus.TotalProcessorTime.TotalSeconds
    $mesure = [System.Diagnostics.Stopwatch]::StartNew()
    $jmeter = Start-Process -FilePath $Jmeter -ArgumentList $arguments -PassThru -WindowStyle Hidden

    Start-Sleep -Seconds 12
    $lignes.Add("  heure    CPU %   tas Mo   tas max Mo   GC jeune %   GC vieux %   threads")
    while ((Get-Process -Id $v.Pid -ErrorAction SilentlyContinue) -and $mesure.Elapsed.TotalSeconds -lt ($Duree - 5)) {
        $p = Get-Process -Id $v.Pid
        $tas = (& $Jcmd $v.Pid GC.heap_info 2>$null) -join " "
        $mo = 0; $max = 0
        if ($tas -match "garbage-first heap\s+total (\d+)K, used (\d+)K") { $max = [int]$matches[1] / 1024; $mo = [int]$matches[2] / 1024 }
        elseif ($tas -match "total (\d+)K, used (\d+)K") { $max = [int]$matches[1] / 1024; $mo = [int]$matches[2] / 1024 }
        $gc = ((& $Jstat -gcutil $v.Pid 2>$null) | Select-Object -Last 1) -split "\s+"
        $jeune = if ($gc.Count -ge 5) { $gc[4] } else { "n/d" }
        $vieux = if ($gc.Count -ge 6) { $gc[5] } else { "n/d" }
        $cpu = 100.0 * ($p.TotalProcessorTime.TotalSeconds - $cpuAvant) / $mesure.Elapsed.TotalSeconds
        $lignes.Add(("  {0}  {1,5}   {2,6}   {3,8}   {4,10}   {5,10}   {6,6}" -f
                (Get-Date -Format "HH:mm:ss"), [math]::Round($cpu, 1), [math]::Round($mo, 0),
                [math]::Round($max, 0), $jeune, $vieux, $p.Threads.Count))
        Start-Sleep -Seconds $Periode
    }

    Wait-Process -Id $jmeter.Id -ErrorAction SilentlyContinue
    $lignes.Add("")
}

[System.IO.File]::WriteAllLines($Journal, $lignes, (New-Object System.Text.UTF8Encoding $false))
Write-Host "Releve ecrit : $Journal"
