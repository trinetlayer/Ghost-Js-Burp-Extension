#!/usr/bin/env bash
# Zero-dependency test runner: mirrors build.sh's no-Gradle philosophy.
# Downloads JUnit Platform Console Launcher if missing, compiles test sources
# against it, then runs the suite.
#
#   ./test.sh
#
# Honours JUNIT env var (default 5.10.2 → platform 1.10.2).
# Exit code = test result count.
set -euo pipefail

cd "$(dirname "$0")"

JUNIT="${JUNIT:-5.10.2}"
PLATFORM="${PLATFORM:-1.10.2}"

LAUNCHER="lib/junit-platform-console-${PLATFORM}.jar"
LAUNCHER_API="lib/junit-platform-launcher-${PLATFORM}.jar"
API="lib/junit-jupiter-api-${JUNIT}.jar"
ENGINE="lib/junit-jupiter-engine-${JUNIT}.jar"
PLATFORM_ENGINE="lib/junit-platform-engine-${PLATFORM}.jar"
PLATFORM_COMMONS="lib/junit-platform-commons-${PLATFORM}.jar"
PLATFORM_REPORTING="lib/junit-platform-reporting-${PLATFORM}.jar"
OPENTEST="lib/opentest4j-1.3.0.jar"
APIGUARDIAN="lib/apiguardian-api-1.1.2.jar"

JARS=("$LAUNCHER" "$LAUNCHER_API" "$API" "$ENGINE" "$PLATFORM_ENGINE" "$PLATFORM_COMMONS" "$PLATFORM_REPORTING" "$OPENTEST" "$APIGUARDIAN")
BASE="https://repo1.maven.org/maven2"

download() {
  local path="$1" url="$2"
  if [[ ! -f "$path" ]]; then
    echo "Downloading $(basename "$path")..."
    mkdir -p lib
    curl -fsSL -o "$path" "$url"
  fi
}

download "$LAUNCHER"          "$BASE/org/junit/platform/junit-platform-console/${PLATFORM}/junit-platform-console-${PLATFORM}.jar"
download "$LAUNCHER_API"      "$BASE/org/junit/platform/junit-platform-launcher/${PLATFORM}/junit-platform-launcher-${PLATFORM}.jar"
download "$API"               "$BASE/org/junit/jupiter/junit-jupiter-api/${JUNIT}/junit-jupiter-api-${JUNIT}.jar"
download "$ENGINE"            "$BASE/org/junit/jupiter/junit-jupiter-engine/${JUNIT}/junit-jupiter-engine-${JUNIT}.jar"
download "$PLATFORM_ENGINE"   "$BASE/org/junit/platform/junit-platform-engine/${PLATFORM}/junit-platform-engine-${PLATFORM}.jar"
download "$PLATFORM_COMMONS"  "$BASE/org/junit/platform/junit-platform-commons/${PLATFORM}/junit-platform-commons-${PLATFORM}.jar"
download "$PLATFORM_REPORTING" "$BASE/org/junit/platform/junit-platform-reporting/${PLATFORM}/junit-platform-reporting-${PLATFORM}.jar"
download "$OPENTEST"          "$BASE/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar"
download "$APIGUARDIAN"       "$BASE/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"

MONTOYA="${MONTOYA:-2026.7}"
FLATLAF="${FLATLAF:-3.7.1}"
MONTOYA_JAR="lib/montoya-api-${MONTOYA}.jar"
FLATLAF_JAR="lib/flatlaf-${FLATLAF}.jar"

if [[ ! -f "$MONTOYA_JAR" ]]; then
  echo "Downloading Montoya API ${MONTOYA}..."
  mkdir -p lib
  curl -fsSL -o "$MONTOYA_JAR" \
    "https://repo1.maven.org/maven2/net/portswigger/burp/extensions/montoya-api/${MONTOYA}/montoya-api-${MONTOYA}.jar"
fi
if [[ ! -f "$FLATLAF_JAR" ]]; then
  echo "Downloading FlatLaf ${FLATLAF}..."
  mkdir -p lib
  curl -fsSL -o "$FLATLAF_JAR" \
    "https://repo1.maven.org/maven2/com/formdev/flatlaf/${FLATLAF}/flatlaf-${FLATLAF}.jar"
fi

PROD_JARS="${MONTOYA_JAR}:${FLATLAF_JAR}"

rm -rf build/classes build/test-classes
mkdir -p build/classes build/test-classes

echo "Compiling main..."
find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 -cp "$PROD_JARS" -d build/classes @build/sources.txt

TEST_CP="$(IFS=:; echo "${JARS[*]}"):$PROD_JARS:build/classes"

echo "Compiling tests..."
find src/test/java -name '*.java' > build/test-sources.txt
javac -encoding UTF-8 -cp "$TEST_CP" -d build/test-classes @build/test-sources.txt

echo "Running tests..."
exec java -cp "$TEST_CP:build/test-classes" \
  org.junit.platform.console.ConsoleLauncher \
  execute \
  --scan-class-path \
  --disable-banner \
  --details=tree