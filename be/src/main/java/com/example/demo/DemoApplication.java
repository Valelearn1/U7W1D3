package com.example.demo;

import com.example.demo.config.DatabaseUrl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// PRIMA RIGA, sempre: traduce il DATABASE_URL di Render in formato JDBC.
		// Se finisce dopo SpringApplication.run, Spring ha gia' letto la configurazione
		// e cerca il database all'indirizzo sbagliato.
		DatabaseUrl.applica();

		SpringApplication.run(DemoApplication.class, args);
	}

}
