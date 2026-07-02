#!/usr/bin/env bash
# Runs the FitFuel backend (Spring Boot, in-memory local profile) and the
# static frontend together for local development.
#
# Usage:
#   ./run-local.sh
#
# Backend:  http://localhost:8080
# Frontend: http://localhost:8090/frontend/
#
# Stop both with Ctrl+C.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_PORT=8090

if ! command -v mvn >/dev/null 2>&1; then
  echo "Error: Maven ('mvn') is required but was not found on PATH." >&2
  echo "Install it with: brew install maven" >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: python3 is required but was not found on PATH." >&2
  exit 1
fi

cleanup() {
  echo
  echo "Shutting down..."
  [[ -n "${BACKEND_PID:-}" ]] && kill "$BACKEND_PID" 2>/dev/null || true
  [[ -n "${FRONTEND_PID:-}" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
  wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "Starting backend (Spring Boot, local profile) on http://localhost:8080 ..."
(cd "$BACKEND_DIR" && mvn spring-boot:run -Dspring-boot.run.profiles=local) &
BACKEND_PID=$!

echo "Starting frontend static server on http://localhost:$FRONTEND_PORT/frontend/ ..."
(cd "$ROOT_DIR" && python3 -m http.server "$FRONTEND_PORT") &
FRONTEND_PID=$!

echo
echo "FitFuel is starting up:"
echo "  Backend:  http://localhost:8080"
echo "  Frontend: http://localhost:$FRONTEND_PORT/frontend/"
echo
echo "Press Ctrl+C to stop both."

wait
