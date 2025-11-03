#!/bin/bash

# Build script for HMP Algorithm Suite
# This script compiles all Java files in the project

echo "=== HMP Algorithm Suite Build Script ==="
echo "Compiling Java source files..."

# Create output directory if it doesn't exist
mkdir -p out

# Compile all Java files
find src -name "*.java" -print0 | xargs -0 javac -cp src -d out

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo "Compiled classes are in the 'out' directory"
    echo ""
    echo "To run an algorithm:"
    echo "  java -cp out HMP_HC_Runner"
    echo "  java -cp out HMP_SA_Runner"
else
    echo "❌ Compilation failed!"
    exit 1
fi