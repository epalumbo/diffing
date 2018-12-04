# Diffing API

Binary diff utility exposed through a reactive REST API

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

* JDK 1.8
* Be connected to the Internet (required to download application's dependencies during first build)

### Installing

* Configure JDK (properly set JAVA_HOME).
* Clone this repository, then use the included [Gradle Wrapper](https://gradle.org/) to execute a first setup build.
```sh
git clone git@github.com:epalumbo/diffing.git
cd diffing
./gradlew build
```
* **Recommended**: import the project into an IDE. [IntelliJ IDEA CE](https://www.jetbrains.com/idea/) works great with this technology stack. Be sure to use the "auto-import" feature when opening the project.

## Running the tests

Just run the Gradle check task:
```sh
./gradlew check
```
## Generating code coverage reports

Execute [JaCoCo](https://github.com/jacoco/jacoco) task like this:
```sh
./gradlew jacocoTestReport
```

