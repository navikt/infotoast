# infotoast
Application for handling rules used for infotrygd and later on persisting them

## Technologies used
* Kotlin
* Ktor
* Gradle
* Kafka


## Requirements for developing the application
#### Java
Make sure you have the Java JDK 25 installed
You can check which version you have installed using this command:
``` bash
java -version
```
or install it: https://sdkman.io/

#### Docker
Make sure you have docker installed using this command:
You can check which version you have installed using this command:
``` bash
docker --version
```
or install it: https://docs.docker.com/engine/install/

#### Docker Compose
Make sure you have docker compose installed using this command:
You can check which version you have installed using this command:
``` bash
docker compose version
```
or install it: https://docs.docker.com/compose/install/


### Building the application
To build the application locally and run tests you can simply run
``` bash
./gradlew clean build
 ```

### Running the application locally
Run in development to enable hot reloading and stubbed external dependencies:

With Gradle :

``` bash
./gradlew runLocal
```

With IntelliJ:
In IntelliJ run configuration, first run the main function, then edit it to add `-Dio.ktor.development=true` to the VM
options, and `-config=application-local.conf` in "program options".


### Finding new available dependencies
``` bash
./gradlew dependencyUpdates
```

### Upgrading the Gradle wrapper version to latest
``` bash
./gradlew :wrapper --gradle-version latest
```

### Contact

This project is maintained by [navikt/tsm](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/infotoast/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-symfoni](https://nav-it.slack.com/archives/C07MY3KCDS5)