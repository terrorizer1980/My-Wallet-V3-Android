#!/bin/bash
#
#  make-release.sh
#  Blockchain
#
#  Compatibility
#  -------------
#  ‣ This script only runs on macOS using Bash 3.0+
#  ‣ Requires Xcode Command Line Tools.
#
#  What It Does
#  ------------
#  TODO
#

cd ..

if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
    printf '\e[1;31m%-6s\e[m\n' "Making a new build requires that you have a clean git working directory. Please commit your changes or stash them to continue."
    exit 1
fi

if ! [ -x "$(command -v agvtool)" ]; then
    printf '\e[1;31m%-6s\e[m\n' "You are missing the Xcode Command Line Tools. To install them, please run: xcode-select --install."
    exit 1
fi

git fetch --tags
latestTag=$(git describe --tags $(git rev-list --tags --max-count=1))

printf "\nLatest release is: $latestTag"

read -p "\nWhat would you like to increase? (M/P/m)" version_increase


if [ $version_increase != "M" ] || [ $version_increase != "P" ] || [ $version_increase != "m" ]; then
    printf "$version_increase is an invalid command. Please select one of (M - Major / P - Patch / m - minor)"
    exit 1
fi

if [ $version_increase != "M" ]; then
  printf "# TODO increase major version"
fi


if [ $version_increase != "P" ]; then
  printf "# TODO increase patch version"
fi

if [ $version_increase != "m" ]; then
  printf "# TODO increase minor version"
fi
