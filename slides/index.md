---
theme: gaia
---

# JDBC driver class loading

---

Does this code work? 👍 always 🤔 sometimes 👎 never

```java

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

  static Logger log = LoggerFactory.getLogger(DemoApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Override
  public void run(String... args) {
    var executor = ForkJoinPool.commonPool();
    var future = CompletableFuture.runAsync(() -> persistentHelloWorld(), executor);
    future.join();
  }

  @SneakyThrows
  static void persistentHelloWorld() {
    try (var conn = DriverManager.getConnection("jdbc:hsqldb:mem:test", "SA", "");
      var stmt = conn.createStatement()) {
      stmt.execute("create table test (key numeric, value varchar(1024))");
      stmt.execute("insert into test values (1, 'Hello world!')");
      try (var rs = stmt.executeQuery("select value from test where key = 1")) {
        rs.next();
        log.info(rs.getString(1));
      }
    }
  }
}
```

---

# Running

The application works when executed with Gradle:
```shell
gradle bootRun
```

It fails with an exception when run as an executable jar:
```shell
gradle assemble && java -jar build/libs/driver-manager-classloading.jar
```

---

# Why?

[DriverManager](https://github.com/openjdk/jdk17/blob/master/src/java.sql/share/classes/java/sql/DriverManager.java#L601) loads available drivers with `ServiceLoader`:

```java
ServiceLoader<Driver> loadedDrivers=ServiceLoader.load(Driver.class);
Iterator<Driver> driversIterator=loadedDrivers.iterator();
```

[ServiceLoader](https://github.com/openjdk/jdk17/blob/master/src/java.base/share/classes/java/util/ServiceLoader.java#L1695) uses current thread context class loader:

```java
public static <S> ServiceLoader<S> load(Class<S> service) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return new ServiceLoader<>(Reflection.getCallerClass(), service, cl);
}
```

---

# Class loaders

Bootstrap - native, loads classes in `$JAVA_HOME/lib`
☝️ Platform - classes from JCP modules (`jdk.internal.loader.ClassLoaders$PlatformClassLoader`)
☝️☝️ Application - our own classes (`jdk.internal.loader.ClassLoaders$AppClassLoader`)

[Thread context class loader](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Thread.html#getContextClassLoader())

> The context ClassLoader is provided by the creator of the thread for use by code running in this thread when loading classes and resources. 

---

# Spring Boot

- [Executable jar](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html) main class delegated to Spring Boot:
```
Main-Class: org.springframework.boot.loader.JarLauncher
Start-Class: com.example.demo.DemoApplication
```
<!-- unzip -p build/libs/driver-manager-classloading.jar META-INF/MANIFEST.MF -->

- Libs in `BOOT-INF/lib`, e.g. `BOOT-INF/lib/hsqldb-2.7.1.jar`
- Special class loader required: `org.springframework.boot.loader.LaunchedURLClassLoader`

---

# ForkJoinPool

- Subclasses `Thread` as `ForkJoinWorkerThread`
- [Sets its context classloader](https://github.com/openjdk/jdk17/blob/master/src/java.base/share/classes/java/util/concurrent/ForkJoinWorkerThread.java#L81) as `ClassLoader.getSystemClassLoader())`
- 💥 It's `ClassLoaders$AppClassLoader`!

---

# Solution

- Load JDBC driver outside of the `ForkJoinWorkerThread`: 
```java
void ensureDriverLoaded(String jdbcUrl) {
  DriverManager.getDriver(jdbcUrl);
}
```
- Use `spring-jdbc` [DriverManagerDataSource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/DriverManagerDataSource.html) - `setDriverClassName` does the same as ☝️
 
---

# Bonus

Why am I using `CompletableFuture` (for the demo purposes)?

---

# Answer

`ForkJoinPool.submit()` followed by `Future.join()` (or `get()`) may be executed in the caller thread!

`LaunchedURLClassLoader` will be used instead of `ClassLoaders$AppClassLoader`.

---
