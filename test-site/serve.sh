#!/usr/bin/env bash
# Serve the GhostJS test target for browsing through Burp.
cd "$(dirname "$0")"
echo "Serving GhostJS test target at http://localhost:8000/"
echo "Browse it through Burp (proxy 127.0.0.1:8080), then open the GhostJS tab."
exec python3 -m http.server 8000
