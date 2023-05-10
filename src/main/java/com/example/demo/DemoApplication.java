package com.example.demo;

import java.sql.DriverManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
