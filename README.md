# Java Archiver

[![Build Status](https://github.com/SBTopZZZ-LG/java_archiver/workflows/Java%20Archiver%20CI/CD%20Pipeline/badge.svg)](https://github.com/SBTopZZZ-LG/java_archiver/actions)
[![codecov](https://codecov.io/gh/SBTopZZZ-LG/java_archiver/branch/main/graph/badge.svg)](https://codecov.io/gh/SBTopZZZ-LG/java_archiver)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SBTopZZZ-LG_java_archiver&metric=alert_status)](https://sonarcloud.io/dashboard?id=SBTopZZZ-LG_java_archiver)

A Java program to create archives from directories with optional password protection and encryption.

## Features

- **Archive Creation**: Create compressed archives from directories
- **Password Protection**: Optional AES encryption with password protection
- **Cross-Platform**: Works on Windows, Linux, and macOS
- **Command Line Interface**: Both interactive and non-interactive modes
- **Extraction**: Extract archives with integrity verification
- **Listing**: View archive contents without extraction

## Building

This project uses Maven for build management:

```bash
# Compile the project
mvn compile

# Run tests
mvn test

# Build with tests and coverage
mvn clean package

# Generate coverage report
mvn jacoco:report
```

## Usage

### Interactive Mode
```bash
java -jar target/java-archiver-3.0.0.jar
```

### Non-Interactive Mode
```bash
# Create archive
java -jar target/java-archiver-3.0.0.jar create /path/to/source /path/to/archive.archivit

# Create password-protected archive
java -jar target/java-archiver-3.0.0.jar create /path/to/source /path/to/archive.archivit mypassword

# Extract archive
java -jar target/java-archiver-3.0.0.jar extract /path/to/archive.archivit /path/to/destination

# List archive contents
java -jar target/java-archiver-3.0.0.jar list /path/to/archive.archivit
```

## Development

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ArchiverAPITest

# Run tests with coverage
mvn test jacoco:report
```

### Code Quality
```bash
# Run Checkstyle
mvn checkstyle:check

# Run SpotBugs
mvn spotbugs:check

# Run all quality checks
mvn verify
```

## CI/CD Pipeline

This project includes a comprehensive CI/CD pipeline that:

- **Tests**: Runs on Java 17 and 21
- **Linting**: Checkstyle and SpotBugs analysis
- **Coverage**: JaCoCo code coverage with 92% target
- **Build**: Creates executable JAR artifacts
- **Reporting**: Generates detailed test and coverage reports

The pipeline posts results as GitHub Job Summaries and comments on pull requests.

## Architecture

- **Models**: Core data structures (`Binary`, `SerializableObject`)
- **Utilities**: Helper classes for I/O, encryption, and serialization
- **Main Classes**: Interactive and non-interactive entry points
- **API**: Programmatic interface for archive operations

## Testing

The project includes:
- **Unit Tests**: 142+ tests covering all major components
- **Integration Tests**: End-to-end workflow testing
- **Edge Case Testing**: Handles various error conditions
- **Performance Testing**: Large file and directory handling

Current test coverage: ~33% (target: 92%)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass and coverage targets are met
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
