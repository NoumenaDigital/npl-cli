#!/bin/bash
# Quick test to verify the replay implementation compiles and tests pass

set -e

echo "🔍 Checking Replay Verification Implementation..."
echo ""

echo "1️⃣ Checking for created files..."
FILES=(
    "src/main/kotlin/com/noumenadigital/npl/cli/service/ReplayRunner.kt"
    "src/test/kotlin/com/noumenadigital/npl/cli/service/ReplayRunnerTest.kt"
    "docs/replay-verification.md"
    "example/verify-with-replay.sh"
    "REPLAY_IMPLEMENTATION.md"
    "REPLAY_QUICKSTART.md"
    "REPLAY_COMPLETE.md"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✅ $file"
    else
        echo "  ❌ $file (MISSING)"
        exit 1
    fi
done
echo ""

echo "2️⃣ Running ReplayRunner tests..."
mvn test -Dtest=ReplayRunnerTest -q
echo "  ✅ ReplayRunner tests passed"
echo ""

echo "3️⃣ Running VerifyCommand tests..."
mvn test -Dtest=VerifyCommandTest -q
echo "  ✅ VerifyCommand tests passed"
echo ""

echo "4️⃣ Compiling project..."
mvn compile -q
echo "  ✅ Project compiles successfully"
echo ""

echo "✨ All checks passed!"
echo ""
echo "📚 Documentation:"
echo "  - Quick Start: REPLAY_QUICKSTART.md"
echo "  - Full Docs:   docs/replay-verification.md"
echo "  - Summary:     REPLAY_COMPLETE.md"
echo ""
echo "🚀 Usage:"
echo "  npl verify --audit audit.json --sources ./protocol"
echo ""
echo "  With environment variables:"
echo "  NPL_CLEANUP=true npl verify --audit audit.json --sources ./protocol"
echo ""

