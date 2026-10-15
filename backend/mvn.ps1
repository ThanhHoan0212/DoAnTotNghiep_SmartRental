param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$wrapperJar = ".\.mvn\wrapper\maven-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $wrapperJar = Join-Path $scriptDir ".mvn\wrapper\maven-wrapper.jar"
}

& java "-Dmaven.multiModuleProjectDirectory=." `
    -cp $wrapperJar `
    org.apache.maven.wrapper.MavenWrapperMain `
    @MavenArgs
exit $LASTEXITCODE
