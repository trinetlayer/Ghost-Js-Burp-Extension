#!/usr/bin/env bash
# Zero-dependency build: compiles GhostJS against the Montoya API jar and packages
# a loadable Burp extension jar. No Gradle/Maven required.
#
#   ./build.sh            # build dist/ghostjs.jar
#   MONTOYA=2026.7 ./build.sh
set -euo pipefail

cd "$(dirname "$0")"
MONTOYA="${MONTOYA:-2026.7}"
FLATLAF="${FLATLAF:-3.7.1}"
JAR="lib/montoya-api-${MONTOYA}.jar"
FLATLAF_JAR="lib/flatlaf-${FLATLAF}.jar"
OUT="dist/ghostjs.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Downloading Montoya API ${MONTOYA}..."
  mkdir -p lib
  curl -fsSL -o "$JAR" \
    "https://repo1.maven.org/maven2/net/portswigger/burp/extensions/montoya-api/${MONTOYA}/montoya-api-${MONTOYA}.jar"
fi

if [[ ! -f "$FLATLAF_JAR" ]]; then
  echo "Downloading FlatLaf ${FLATLAF}..."
  mkdir -p lib
  curl -fsSL -o "$FLATLAF_JAR" \
    "https://repo1.maven.org/maven2/com/formdev/flatlaf/${FLATLAF}/flatlaf-${FLATLAF}.jar"
fi

echo "Regenerating patterns from the TypeScript engine (if available)..."
if command -v npx >/dev/null 2>&1 && [[ -f "../TrinetLayer Code/server/ghostjs/secret-patterns.ts" ]]; then
  npx --yes tsx export-patterns.mjs || echo "  (skipped — using committed GeneratedPatterns.java)"
else
  echo "  (skipped — using committed GeneratedPatterns.java)"
fi

rm -rf build "$OUT"
mkdir -p build/classes dist

echo "Compiling..."
find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 -cp "$JAR:$FLATLAF_JAR" -d build/classes @build/sources.txt

echo "Bundling FlatLaf classes..."
unzip -oq "$FLATLAF_JAR" -d build/classes
rm -f build/classes/module-info.class

echo "Packaging $OUT..."
cp -R src/main/resources/. build/classes/ 2>/dev/null || true
jar --create --file "$OUT" -C build/classes .

echo "Done -> $OUT"
echo "Load it in Burp: Extensions > Installed > Add > Java > select $OUT"
