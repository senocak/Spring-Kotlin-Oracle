# SpringBoot Kotlin Oracle 

```sh
git clone https://github.com/senocak/Spring-Kotlin-Couchbase.git
```

## Technology Stack
- Backend
  - Jvm, 17.0.10
  - Kotlin, 1.9.23
  - Gradle, 8.7
  - SpringBoot, 3.4.0
  - Oracle, 23
  - Swagger
  - Jupiter (JUnit 5)
  - Mockito
- Ops
  - Mac OS, 14.4.1
  - Docker, 26.1.3

### Running Backend Side
```sh 
cd backend
./gradlew clean build -Pprofile=unit #runs only unit tests and build the package
./gradlew clean build -Pprofile=integration #runs only integration tests and build the package
./gradlew bootRun # runs the SpringBoot application via commandline
```

### Running using docker
```sh 
docker-compose up -d
```

### Application Metrics and Monitoring

The application provides comprehensive metrics through a custom actuator endpoint at `/actuator/appHealth`. This endpoint offers:

- Application Metrics
  - Request counts and error rates
  - Application uptime
  - Success rate statistics
  - Performance metrics
    - Global metrics
      - Average response time
      - Maximum response time
      - Minimum response time
      - 95th percentile response time (based on last 1000 requests)
    - Per-endpoint metrics (grouped by HTTP method)
      - Metrics organized by HTTP method (GET, POST, etc.)
      - Per-path performance statistics
      - Request counts and error rates per endpoint
      - Response time statistics (avg, min, max, p95)
      - Success rate per endpoint

- System Metrics
  - JVM memory usage
  - Available processors
  - JVM version information

- Connection Status
  - Redis connectivity
  - Database health
  - Scheduler status

- Security Information
  - Lock status
  - Build properties
  - Health indicators

To access the metrics:
```sh
curl http://localhost:8083/actuator/appHealth
```

### Spring Boot Actuator Misconfigurations
- https://www.wiz.io/blog/spring-boot-actuator-misconfigurations
