#!/bin/bash
set -e

echo "Setting up Mill build for iw-support project..."
echo "============================================="

# Step 1: Build and publish mill-iw-support locally
echo ""
echo "Step 1: Building mill-iw-support library..."
cd ../iw-project-support/mill-iw-support
./mill core.publishLocal

echo ""
echo "Step 2: Returning to project directory..."
cd ../../project

echo ""
echo "Step 3: Testing core module compilation..."
./mill verify.compile

echo ""
echo "✅ Setup complete! You can now use Mill to build the core module."
echo ""
echo "Available commands:"
echo "  ./mill core.jvm.compile    - Compile JVM core module"
echo "  ./mill core.js.compile     - Compile JS core module"
echo "  ./mill verify.compile      - Compile all core modules"
echo "  ./mill core.jvm.test.test  - Run JVM tests"
echo "  ./mill core.js.test.test   - Run JS tests"
echo "  ./mill verify.test         - Run all tests"
echo ""
echo "To see all available tasks:"
echo "  ./mill resolve __"