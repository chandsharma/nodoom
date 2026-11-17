#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SIGNING_SERVICE="NoDoom Android signing key"
SIGNING_ACCOUNT="chandsharma"
SIGNING_PASSWORD=$(security find-generic-password \
  -a "$SIGNING_ACCOUNT" \
  -s "$SIGNING_SERVICE" \
  -w)

export NODOOM_STORE_FILE="$PROJECT_DIR/keystore/nodoom-release.jks"
export NODOOM_STORE_PASSWORD="$SIGNING_PASSWORD"
export NODOOM_KEY_ALIAS="nodoom-release"
export NODOOM_KEY_PASSWORD="$SIGNING_PASSWORD"

cd "$PROJECT_DIR"
./gradlew --no-daemon clean test lint assembleRelease bundleRelease

unset SIGNING_PASSWORD
unset NODOOM_STORE_PASSWORD
unset NODOOM_KEY_PASSWORD

