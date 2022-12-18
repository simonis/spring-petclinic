package org.springframework.samples.petclinic.component;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MyCommandLineRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MyCommandLineRunner.class);

	@Override
	public void run(String... args) throws Exception {
		logger.info("=> MyCommandLineRunner: started with {}", Arrays.toString(args));
		ObjectMapper mapper = new ObjectMapper();
		String[] firstNames = mapper.readValue(
				MyCommandLineRunner.class.getClassLoader().getResourceAsStream("data/firstNames.json"), String[].class);
		logger.info("=> MyCommandLineRunner: read {} first names", firstNames.length);
		String[] lastNames = mapper.readValue(
				MyCommandLineRunner.class.getClassLoader().getResourceAsStream("data/lastNames.json"), String[].class);
		logger.info("=> MyCommandLineRunner: read {} last names", lastNames.length);
		String[] streetNames = mapper.readValue(
				MyCommandLineRunner.class.getClassLoader().getResourceAsStream("data/streets.json"), String[].class);
		logger.info("=> MyCommandLineRunner read {} street names", streetNames.length);
		String[] cityNames = mapper.readValue(
				MyCommandLineRunner.class.getClassLoader().getResourceAsStream("data/cities.json"), String[].class);
		logger.info("=> MyCommandLineRunner read {} city names", cityNames.length);
	}

}
