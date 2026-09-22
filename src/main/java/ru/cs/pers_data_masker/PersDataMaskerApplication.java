package ru.cs.pers_data_masker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PersDataMaskerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersDataMaskerApplication.class, args);
	}

}
