git checkout --orphan 1.20.1-new
git add -A
git commit -m "Initial commit"
git branch -D 1.20.1
git branch -m 1.20.1
git push -f origin 1.20.1