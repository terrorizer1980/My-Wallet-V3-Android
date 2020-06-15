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

#if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
#    printf '\e[1;31m%-6s\e[m\n' "Making a new build requires that you have a clean git working directory. Please commit your changes or stash them to continue."
#    exit 1
#fi

dependenciesFilePath="./buildSrc/src/main/java/Dependencies.kt"
currentVersionCode=$(awk '/const val versionCode = / {print $5}' $dependenciesFilePath)
echo "Current versionCode is: $currentVersionCode"

currentVersionName=$(awk '/const val versionName = / {print $5}' $dependenciesFilePath)
echo "Current versionName is: $currentVersionName"

if ! [ -x "$(command -v agvtool)" ]; then
    printf '\e[1;31m%-6s\e[m\n' "You are missing the Xcode Command Line Tools. To install them, please run: xcode-select --install."
    exit 1
fi

git fetch --tags
latestTag=$(git describe --tags $(git rev-list --tags --max-count=1))

echo "Latest release tag is: $latestTag"

read -p "What would you like to increase? (M/P/m)" version_increase

if [ $version_increase != "M" ] && [ $version_increase != "P" ] && [ $version_increase != "m" ]; then
    echo "$version_increase is an invalid command. Please select one of (M - Major / P - Patch / m - minor)"
    exit 1
fi

updatedVersionCode=$((currentVersionCode + 1))
echo "updatedVersionCode is: $updatedVersionCode"

strippedVersion="${currentVersionName%\"}"
strippedVersion="${strippedVersion#\"}"
echo "strippedVersion $strippedVersion"

splitNamesM=$( echo "$strippedVersion" | cut -d "." -f 1)
splitNamesP=$( echo "$strippedVersion" | cut -d "." -f 2)
splitNamesm=$( echo "$strippedVersion" | cut -d "." -f 3)
echo "splitNamesM $splitNamesM"
echo "splitNamesP $splitNamesP"
echo "splitNamesm $splitNamesm"

newVersion=""
if [ $version_increase == "M" ]; then
  echo "> Increasing major version"
  increasedMajor=$((splitNamesM + 1))
  newVersion="\"$increasedMajor.0.0\""
  echo "updatedMajorVersion $newVersion"
fi

if [ $version_increase == "P" ]; then
  echo "> Increasing patch version"
  increasedPatch=$((splitNamesP + 1))
  newVersion="\"$splitNamesM.$increasedPatch.0\""
  echo "updatedPatchVersion $newVersion"
fi

if [ $version_increase == "m" ]; then
  echo "> Increasing minor version"
  increasedMinor=$((splitNamesm + 1))
  newVersion="\"$splitNamesM.$splitNamesP.$increasedMinor\""
  echo "updatedMinorVersion $newVersion"
fi

sed -i -e "s/$currentVersionCode/$updatedVersionCode/g" $dependenciesFilePath
sed -i -e "s/$currentVersionName/$newVersion/g" $dependenciesFilePath


