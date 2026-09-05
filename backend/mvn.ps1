param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$mavenHome = Join-Path $scriptDir ".maven\apache-maven-3.9.6"
$classworldsConf = Join-Path $mavenHome "bin\m2.conf"
$bootClasspath = Join-Path $mavenHome "boot\*"

& java -cp $bootClasspath `
    "-Dclassworlds.conf=$classworldsConf" `
    "-Dmaven.home=$mavenHome" `
    "-Dmaven.multiModuleProjectDirectory=$scriptDir" `
    org.codehaus.plexus.classworlds.launcher.Launcher `
    @MavenArgs
exit $LASTEXITCODE
