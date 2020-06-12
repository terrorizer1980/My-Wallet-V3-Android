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