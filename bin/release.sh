#!/bin/bash
#
# Performs a release on PharmGKB/PharmCAT.
#
#
set -e
set -u
set -o pipefail


branch=$(git rev-parse --abbrev-ref HEAD)
if [[ $branch != 'development' ]]
then
  echo "Must run on development branch."
  exit 1
fi

diffs=$(git diff | wc -l)
if [[ $diffs -gt 0 ]]
then
  echo "There are uncommitted changes."
  exit 1
fi

commits=$(git rev-list HEAD...origin/development | wc -l)
if [[ $commits -gt 0 ]]
then
  echo "This branch is ${commits} behind origin/development."
  exit 1
fi


# update main branch so that we trigger GH actions (because release commit has [skip ci]
echo ""
echo "Updating main branch..."
git checkout main
git pull
git rebase development
git push

# switching back to development
echo ""
echo "Switching back to development..."
git checkout development


# do release
echo "Creating release..."
yarn release


# update repo
echo ""
echo "Updating repo..."
git fetch


# update main branch
echo ""
echo "Updating main branch..."
git checkout main
git pull
git rebase development
git push

# switching back to development
git checkout development


echo ""
echo "Done."
