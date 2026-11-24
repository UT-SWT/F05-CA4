# Course Enrollment Testing Lab

## 1. Project Overview

This project is a testing laboratory for the Course Enrollment application. The goal is to evaluate the application through different software testing techniques, including specification-based testing, structural testing, and mutation testing.

The assignment is organized into four stages:

- **AI-Generated Baseline:** Establish an initial set of tests and analysis generated with the assistance of an AI/LLM.
- **Black-Box Testing:** Design tests using Equivalence Partitioning (EP), Boundary Value Analysis (BVA), and Finite State Machine (FSM) modeling.
- **White-Box Testing:** Analyze the program structure using Control Flow Graphs (CFGs), Edge-Pair Coverage, and Condition/Decision Coverage (CACC).
- **Mutation Testing:** Evaluate the effectiveness of the test suite by identifying and killing program mutants.

The production code is provided as the subject of the assignment. Your task is to develop the test suites and document your analysis without modifying the original implementation.

## 2. Project Structure

The test source directory is organized into the following packages:

```text
src/test/java/edu/university/enrollment/
├── baseline_ai/   # Initial AI-generated tests
├── blackbox/      # EP, BVA, FSM, and transition-pair tests
├── whitebox/      # Edge-Pair and CACC tests
└── mutation/      # Tests targeting surviving mutants
```

Keep each test suite in its corresponding package. Tests generated during the initial AI-assisted stage must remain separate from the tests you develop in later stages.


## 3. Environment and Configuration

The project uses the following tools and technologies:

- JDK 17 or newer
- Apache Maven 3.9 or newer (recommended)
- JUnit 5 for unit testing
- JaCoCo for code coverage measurement
- PITest for mutation testing


## 4. Running the Tests

Run the following commands from the project root directory.

### 4.1. Execute JUnit Tests

```powershell
mvn clean test
```

This command compiles the project and executes the available JUnit tests.

### 4.2. Generate the JaCoCo Report

```powershell
mvn clean test jacoco:report
```

After the command completes, open the generated HTML report:

```text
target/site/jacoco/index.html
```

### 4.3. Run Mutation Testing

```powershell
mvn test org.pitest:pitest-maven:mutationCoverage
```

The PITest HTML report is generated at:

```text
target/pit-reports/index.html
```
