#!/bin/bash

# Run script for HMP Algorithm Suite
# Usage: ./run.sh [algorithm] [dataset]
# Example: ./run.sh HC adult.txt

# Check if algorithm is specified
if [ $# -eq 0 ]; then
    echo "Usage: $0 [algorithm] [dataset]"
    echo ""
    echo "Available algorithms:"
    echo "  HC          - Hill Climbing (standard)"
    echo "  SA          - Simulated Annealing (standard)"
    echo "  HC_CONV     - Hill Climbing with convergence"
    echo "  SA_CONV     - Simulated Annealing with convergence"
    echo "  HC_CLASS    - Hill Climbing for classification"
    echo "  SA_CLASS    - Simulated Annealing for classification"
    echo "  HC_NOMAT    - Hill Climbing without matrix"
    echo "  SA_NOMAT    - Simulated Annealing without matrix"
    echo "  HC_COMP     - Hill Climbing set compression"
    echo "  SA_COMP     - Simulated Annealing set compression"
    echo ""
    echo "Available datasets:"
    ls Datasets/*.txt 2>/dev/null | sed 's|Datasets/||' | head -10
    echo "... and more"
    exit 1
fi

ALGORITHM=$1
DATASET=${2:-"adult.txt"}

# Map algorithm names to class names
case $ALGORITHM in
    HC)         CLASS="HMP_HC_Runner" ;;
    SA)         CLASS="HMP_SA_Runner" ;;
    HC_CONV)    CLASS="HMP_HC_Conv" ;;
    SA_CONV)    CLASS="HMP_SA_Convergence" ;;
    HC_CLASS)   CLASS="HMP_HC_Runner_Classfication" ;;
    SA_CLASS)   CLASS="HMP_SA_Runner_Classification" ;;
    HC_NOMAT)   CLASS="HMP_HC_Runner_NoMatrix" ;;
    SA_NOMAT)   CLASS="HMP_SA_Runner_NoMatrix" ;;
    HC_COMP)    CLASS="HMP_HC_Set_Compression" ;;
    SA_COMP)    CLASS="HMP_SA_Set_Compression" ;;
    *)
        echo "❌ Unknown algorithm: $ALGORITHM"
        exit 1
        ;;
esac

# Check if compiled classes exist
if [ ! -d "out" ]; then
    echo "⚠️  Compiled classes not found. Running build script..."
    ./build.sh
fi

# Check if dataset exists
if [ ! -f "Datasets/$DATASET" ]; then
    echo "❌ Dataset not found: Datasets/$DATASET"
    exit 1
fi

echo "=== Running HMP Algorithm ==="
echo "Algorithm: $ALGORITHM ($CLASS)"
echo "Dataset: $DATASET"
echo "Starting execution..."
echo ""

# Run the algorithm
java -cp out:src -Xmx4g $CLASS

echo ""
echo "✅ Execution completed!"
echo "Check result files for output."