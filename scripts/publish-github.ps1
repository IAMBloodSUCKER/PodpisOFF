# Publish to GitHub

Requires one-time login: `gh auth login` (or open https://github.com/login/device)

```powershell
cd I:\projects\PodpisOFF
$env:GIT_AUTHOR_NAME = "bloodSUCKER"
$env:GIT_COMMITTER_NAME = "bloodSUCKER"
$env:GIT_AUTHOR_EMAIL = "91571876+IAMBloodSUCKER@users.noreply.github.com"
$env:GIT_COMMITTER_EMAIL = "91571876+IAMBloodSUCKER@users.noreply.github.com"

$gh = "$env:TEMP\gh-cli\bin\gh.exe"
if (-not (Test-Path $gh)) { $gh = "gh" }

& $gh auth status
git -c core.fsync=none add -A
git -c core.fsync=none diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
  git -c core.fsync=none commit -m "chore: update"
}

& $gh repo create PodpisOFF --public --source=. --remote=origin --push --description "ПодписOFF / SubOFF — subscription tracker PWA"
git -c core.fsync=none tag v1.0.0
git -c core.fsync=none push origin v1.0.0
```

Commits use GitHub noreply email — your personal email is not exposed.
