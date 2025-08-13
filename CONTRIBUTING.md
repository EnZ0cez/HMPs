# Contributing to HMP Algorithm Suite

Thank you for your interest in contributing to the HMP (Heuristic Mining Patterns) Algorithm Suite! This document provides guidelines for contributing to the project.

## 📋 Getting Started

1. **Fork the repository** and clone your fork locally
2. **Create a new branch** for your feature or bug fix
3. **Make your changes** following our coding standards
4. **Test your changes** thoroughly
5. **Submit a pull request** with a clear description

## 🔧 Development Setup

### Prerequisites
- Java 8 or higher
- Git
- A text editor or IDE (IntelliJ IDEA, Eclipse, VS Code)

### Building the Project
```bash
# Clone your fork
git clone https://github.com/yourusername/HMPs.git
cd HMPs

# Build the project
./build.sh

# Run tests
./run.sh HC test.txt
```

## 📝 Coding Standards

### Java Code Style
- Use **4 spaces** for indentation (no tabs)
- Line length limit: **120 characters**
- Use **camelCase** for variables and methods
- Use **PascalCase** for class names
- Use **UPPER_SNAKE_CASE** for constants

### Documentation
- **JavaDoc comments** for all public classes and methods
- **Inline comments** for complex algorithms or logic
- **Clear variable names** that describe their purpose
- **Method descriptions** explaining parameters and return values

### Example:
```java
/**
 * Calculates the compression size in bits for a given database using the current code table.
 * This method evaluates how effectively the current patterns compress the data.
 * 
 * @param database The list of transactions to evaluate
 * @return The total compression size in bits
 */
public static double calculateSizeInBits(List<int[]> database) {
    // Implementation here
}
```

## 🧪 Testing Guidelines

### Algorithm Testing
- Test with **multiple datasets** of varying sizes
- Verify **compression improvements** are as expected
- Check **memory usage** remains reasonable
- Ensure **results are reproducible** with same random seeds

### Performance Testing
- Test with **large datasets** (>1MB)
- Monitor **memory consumption**
- Measure **execution time**
- Compare against baseline implementations

## 📁 File Organization

### New Algorithm Implementations
- Place main algorithm files in `/src/`
- Follow naming convention: `HMP_[ALGORITHM]_[VARIANT].java`
- Include corresponding test datasets in `/Datasets/`

### Utility Classes
- Place utility classes in appropriate packages
- Keep data structures in `/src/ca/pfv/spmf/datastructures/`
- Document dependencies clearly

## 🐛 Bug Reports

When reporting bugs, please include:
- **Java version** and operating system
- **Dataset** that triggers the issue
- **Complete error message** and stack trace
- **Steps to reproduce** the problem
- **Expected vs actual behavior**

### Bug Report Template
```markdown
**Environment:**
- Java Version: [e.g., Java 11]
- OS: [e.g., Ubuntu 20.04]
- Dataset: [e.g., adult.txt]

**Steps to Reproduce:**
1. Run algorithm X with dataset Y
2. Observe error at iteration Z

**Expected Behavior:**
Algorithm should complete successfully

**Actual Behavior:**
[Error message or unexpected behavior]

**Additional Context:**
[Any other relevant information]
```

## ✨ Feature Requests

For new features, please:
- **Check existing issues** for similar requests
- **Describe the use case** clearly
- **Explain the expected behavior**
- **Consider performance implications**
- **Provide example datasets** if applicable

## 🔄 Pull Request Process

1. **Update documentation** for any new features
2. **Add tests** for new functionality
3. **Ensure all tests pass**
4. **Update README.md** if needed
5. **Follow commit message conventions**

### Commit Message Format
```
[TYPE]: Brief description of changes

Detailed explanation if necessary:
- What was changed
- Why it was changed
- Any breaking changes

Fixes #issue_number
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style/formatting
- `refactor`: Code refactoring
- `test`: Adding tests
- `perf`: Performance improvements

### Example Commit Messages
```
feat: Add convergence tracking to SA algorithm

- Implement temperature-based convergence monitoring
- Add convergence statistics to output files
- Update SA_Runner to include convergence checks

Fixes #123
```

```
fix: Correct memory leak in SparseTriangularMatrix

- Properly clear hash maps after processing
- Add memory cleanup in finally blocks
- Improve garbage collection efficiency

Fixes #456
```

## 📚 Documentation Guidelines

### README Updates
- Keep the **Quick Start** section current
- Update **algorithm list** for new implementations
- Add **performance notes** for significant changes

### Code Documentation
- Document **algorithm complexity** (Big O notation)
- Explain **parameter choices** and their impact
- Include **literature references** for algorithms
- Document **known limitations** or edge cases

## 🤝 Code Review Process

All submissions require code review:
- **Automated checks** must pass
- **At least one reviewer** approval required
- **Documentation** must be updated
- **Performance impact** must be considered

### Review Checklist
- [ ] Code follows style guidelines
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] Performance is acceptable
- [ ] No breaking changes (or properly documented)

## 🏷️ Versioning

We use semantic versioning (SemVer):
- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

## 📄 License

By contributing, you agree that your contributions will be licensed under the same terms as the project (GPL v3 for SPMF components).

## 🆘 Getting Help

- **Create an issue** for questions about the codebase
- **Join discussions** in existing issues
- **Check documentation** in the `/docs` folder (if available)
- **Review existing code** for examples and patterns

## 🙏 Recognition

Contributors will be recognized in:
- **CONTRIBUTORS.md** file
- **Release notes** for significant contributions
- **Documentation** for major features

Thank you for contributing to the HMP Algorithm Suite! 🚀