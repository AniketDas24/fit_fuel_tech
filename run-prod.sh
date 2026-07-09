#!/usr/bin/env bash
# Builds and runs the FitFuel backend with the PRODUCTION profile.
#
# Usage:
#   1. cp .env.prod.example .env.prod   and fill in real production values
#   2. ./run-prod.sh
#
# This runs the backend only (Postgres, live keys). In production the frontend is
# served by a real static host / CDN, not python http.server, and its API_BASE in
# frontend/index.html must point at the deployed backend URL rather than localhost.
#
# Stop with Ctrl+C.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"

if [[ ! -f "$ROOT_DIR/.env.prod" ]]; then
  echo "Error: .env.prod not found. Copy .env.prod.example to .env.prod and fill it in." >&2
  exit 1
fi

echo "Loading environment variables from .env.prod ..."
set -a
# shellcheck disable=SC1091
source "$ROOT_DIR/.env.prod"
set +a

if ! command -v mvn >/dev/null 2>&1; then
  echo "Error: Maven ('mvn') is required but was not found on PATH." >&2
  exit 1
fi

echo "Building backend jar ..."
(cd "$BACKEND_DIR" && mvn -q clean package -DskipTests)

JAR=$(find "$BACKEND_DIR/target" -maxdepth 1 -name '*.jar' ! -name '*-sources.jar' | head -n 1)
if [[ -z "$JAR" ]]; then
  echo "Error: build succeeded but no runnable jar was found in target/." >&2
  exit 1
fi

echo "Starting backend (PROD profile) ..."
exec java -jar "$JAR" --spring.profiles.active=prod
