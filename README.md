# JDBC Driver class loading issues with Spring Boot and ForkJoinPool

Check [slides](slides/index.md) for the problem statement and explanation.

To start the slideshow:
- install [Marp CLI](https://github.com/marp-team/marp-cli)
- execute `marp --server ./slides`
- go to `http://localhost:8080/` 

# Running

- with Gradle `bootRun`:
```shell
gradle bootRun
```

As an executable jar:
```shell
gradle assemble && java -jar build/libs/driver-manager-classloading.jar
```