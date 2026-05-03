#!/usr/bin/env bash
echo "==================================="
echo "  CraftLaunch v4.0 Minecraft Launcher"
echo "==================================="
echo ""

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found!"
    echo ""
    echo "Install Java 17 or newer:"
    echo "  Ubuntu/Debian : sudo apt install default-jre"
    echo "  macOS         : brew install openjdk"
    echo "  Or download   : https://adoptium.net/"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
echo "[OK] Java $JAVA_VER detected."
echo "Starting CraftLaunch..."
echo ""

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$SCRIPT_DIR/CraftLaunch.jar"
