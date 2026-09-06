# Test Results

## Execution status

This repository contains a repeatable smoke-test script and a Week 4 verification plan. The test commands must be executed in an environment with the application's PostgreSQL/Redis dependencies available.

### Automated smoke test
- Script: `scripts/week4-smoke-test.ps1`
- Expected health result: `UP`
- Expected protected-resource result: HTTP `401` or `403` without credentials

### Gradle test suite
Run:

```bash
./gradlew test
```

Record the final pass/fail result here after execution. Do not mark the suite as passing unless it has actually been executed.

### Deployment verification
- Docker Compose: pending local execution
- Kubernetes: manifest/configuration review completed; cluster execution pending
