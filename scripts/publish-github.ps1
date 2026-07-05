# Publish to GitHub (run once after: gh auth login)

$ErrorActionPreference = "Stop"
$gh = "$env:TEMP\gh-cli\bin\gh.exe"
if (-not (Test-Path $gh)) {
    Write-Host "Download gh CLI first or install: winget install GitHub.cli"
    exit 1
}

& $gh auth status
if ($LASTEXITCODE -ne 0) {
    Write-Host "Run: gh auth login"
    exit 1
}

Set-Location $PSScriptRoot\..

if (-not (Test-Path .git)) {
    git init -b main
}

$env:GIT_AUTHOR_NAME = "bloodSUCKER"
$env:GIT_COMMITTER_NAME = "bloodSUCKER"
$env:GIT_AUTHOR_EMAIL = "91571876+IAMBloodSUCKER@users.noreply.github.com"
$env:GIT_COMMITTER_EMAIL = "91571876+IAMBloodSUCKER@users.noreply.github.com"

git add -A
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -m "feat: PodpisOFF / SubOFF subscription tracker MVP"
}

& $gh repo create PodpisOFF --public --source=. --remote=origin --push --description "ПодписOFF / SubOFF — subscription tracker PWA"

git tag v1.0.0
git push origin v1.0.0

Write-Host "Done: https://github.com/IAMBloodSUCKER/PodpisOFF"
