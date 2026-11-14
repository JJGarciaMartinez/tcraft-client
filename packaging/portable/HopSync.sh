#!/bin/sh
# Starts HopSync with the Java 21 found on the PATH
cd "$(dirname "$0")" || exit 1
exec java -jar HopSync.jar "$@"
