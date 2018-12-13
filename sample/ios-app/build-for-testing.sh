#!/usr/bin/env bash

usage() {
  echo 1>&2 -e "Usage: bash" "${BASH_SOURCE##*/} <Simulator UDID>"
  echo 1>&2
  echo 1>&2 -e "To list available simulators, run $(tput bold)xcrun simctl list devices$(tput sgr0)"
  echo 1>&2
}

if ! XCODEBUILD="$(command -v xcodebuild)"; then
  echo 1>&2 -e "$(tput setaf 1)ERROR: $(tput bold)xcodebuild$(tput sgr0) $(tput setaf 1)not found$(tput sgr0)"
  exit 1
fi

XCODEBUILD_DESTINATION="${1:-${DESTINATION_SIMULATOR_ID}}"
if [[ -z ${XCODEBUILD_DESTINATION} ]]; then
  echo 1>&2 -e "$(tput setaf 1)ERROR: Required simulator UDID is missing.$(tput sgr0)"
  echo 1>&2
  usage
  exit 1
fi

$XCODEBUILD build-for-testing -derivedDataPath derived-data -workspace sample-app.xcworkspace -scheme UITesting -sdk iphonesimulator -destination "platform=iOS Simulator,id=$XCODEBUILD_DESTINATION"
