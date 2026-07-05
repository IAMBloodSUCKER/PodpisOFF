# Publish to GitHub

```powershell
cd I:\projects\PodpisOFF
$env:GIT_AUTHOR_NAME = "bloodSUCKER"
$env:GIT_COMMITTER_NAME = "bloodSUCKER"
$env:GIT_AUTHOR_EMAIL = "91571876+IAMBloodSUCKER@users.noreply.github.com"
$env:GIT_COMMITTER_EMAIL = "91571876+IAMBloodSUCKER@users.noreply.github.com"

git -c core.fsync=none remote add origin https://github.com/IAMBloodSUCKER/PodpisOFF.git 2>$null
git -c core.fsync=none push -u origin main
git -c core.fsync=none tag v1.0.0
git -c core.fsync=none push origin v1.0.0
```

Commits use GitHub noreply email — your personal email is not exposed.
