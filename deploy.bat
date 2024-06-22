git add .
git commit -m %1
git push
cd pathfinder
git add .
git commit -m %1
npm run deploy
git push
cd ..