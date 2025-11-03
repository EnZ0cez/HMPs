# HMP (Heuristic MDL based Pattern mining) Algorithm Suite


## Overview

This project provides implementations of advanced pattern mining algorithms that discover frequent patterns in transactional databases to achieve optimal data compression. The algorithms use different heuristic optimization approaches:

- **Hill Climbing (HC)** - Local search optimization
- **Simulated Annealing (SA)** - Probabilistic optimization with temperature cooling

## Project Structure

```
HMPs/
├── src/                          # Main source code
│   ├── HMP_HC_*.java            # Hill Climbing implementations
│   ├── HMP_SA_*.java            # Simulated Annealing implementations
│   ├── ca/pfv/spmf/             # SPMF library components
│   │   ├── datastructures/      # Data structures (maps, matrices)
│   │   └── algorithms/          # Core algorithms
│   ├── TransactionDatabase.java # Database handling
│   ├── SparseTriangularMatrix.java # Efficient matrix operations
│   └── *.java                   # Utility classes                 # 
├── Datasets/                    # Test datasets and data files
├── .gitignore                   # Git ignore rules
└── README.md                    # This file
```

## Algorithm Implementations

### Hill Climbing Variants
- **HMP_HC_Runner.java** - Standard Hill Climbing pattern mining
- **HMP_HC_Conv.java** - Hill Climbing with convergence analysis
- **HMP_HC_Runner_Classification.java** - HC for classification tasks
- **HMP_HC_Runner_NoMatrix.java** - HC without matrix optimization
- **HMP_HC_Set_Compression.java** - HC focused on set compression

### Simulated Annealing Variants
- **HMP_SA_Runner.java** - Standard Simulated Annealing pattern mining
- **HMP_SA_Convergence.java** - SA with convergence tracking
- **HMP_SA_Runner_Classification.java** - SA for classification tasks
- **HMP_SA_Runner_NoMatrix.java** - SA without matrix optimization
- **HMP_SA_Set_Compression.java** - SA focused on set compression

## Prerequisites

- **Java 8+** - Required for running the algorithms
- **Memory** - Datasets can be large; ensure adequate heap space
- **Storage** - Sufficient space for datasets and output files

## Quick Start

### 1. Clone the Repository
```bash
git clone <https://github.com/EnZ0cez/HMPs-CPP.git>
cd HMPs
```

### 2. Compile the Project
```bash
# Compile all Java files
javac -cp src src/*.java src/ca/pfv/spmf/datastructures/*/*.java
```

### 3. Run an Algorithm
```bash
# Run Hill Climbing algorithm
java -cp src HMP_HC_Runner

# Run Simulated Annealing algorithm
java -cp src HMP_SA_Runner
```

### 4. View Results
Results are saved in files named `result_[ALGORITHM]_[DATASET].txt`

## Datasets

The `Datasets/` directory contains various transactional datasets:

- **adult.txt** - Adult census data
- **accident.txt** - Traffic accident records
- **mushroom.txt** - Mushroom characteristics
- **retail.txt** - Retail transaction data
- **chess.txt** - Chess game positions
- **And many more...**

### Dataset Format
Datasets use a simple format where each line represents a transaction:
```
item1 item2 item3
item4 item5
item1 item4
```

## Configuration

### Algorithm Parameters

#### Hill Climbing
- `MAX_ITERATIONS`: Maximum iterations per pattern (default: 31)
- `IMPROVEMENT_THRESHOLD`: Minimum improvement required (default: 0.001)
- `max_code_table_size`: Maximum patterns in code table (default: 1201)

#### Simulated Annealing
- `INITIAL_TEMPERATURE`: Starting temperature (default: 100)
- `MIN_TEMPERATURE`: Minimum temperature threshold (default: 0.1)
- `COOLING_RATE`: Temperature reduction rate (default: 0.8)
- `max_code_table_size`: Maximum patterns in code table (default: 1583)

### Customizing Input
Modify the `filePath` variable in the main method of any runner class:
```java
String filePath = "Datasets/input.txt";
```

## Output

Each algorithm run produces detailed output including:
- Initial compression size
- Final compression size
- Number of iterations
- Memory usage statistics
- Execution time
- Pattern discovery metrics

Example output format:
```
Processing file: adult.txt, Run: 1
Initial compression size: 1234567.89
Final compression size: 987654.32
Compression improvement: 20.0%
Memory usage: 456 MB
Execution time: 12.34 seconds
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-algorithm`)
3. Commit your changes (`git commit -am 'Add new algorithm variant'`)
4. Push to the branch (`git push origin feature/new-algorithm`)
5. Create a Pull Request

## License

This project incorporates components from the SPMF Data Mining Software:
- SPMF components are licensed under GPL v3
- See individual file headers for specific license information
