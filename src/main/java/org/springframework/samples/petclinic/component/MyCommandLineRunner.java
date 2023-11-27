package org.springframework.samples.petclinic.component;

import java.util.Arrays;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;

@Component
public class MyCommandLineRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MyCommandLineRunner.class);

	private final OwnerRepository owners;

	@Autowired
	public MyCommandLineRunner(OwnerRepository owners) {
		this.owners = owners;
	}

	private static final String FIRST_NAMES = "data/firstNames.json";

	private static final String LAST_NAMES = "data/lastNames.json";

	private static final String STREET_NAMES = "data/streets.json";

	private static final String CITY_NAMES = "data/cities.json";

	@Override
	public void run(String... args) throws Exception {
		logger.info("=> MyCommandLineRunner: started with {}", Arrays.toString(args));

		boolean jsonProcessing = System.getProperty("MyCommandLineRunner.jsonProcessing") == null
				|| Boolean.getBoolean("MyCommandLineRunner.jsonProcessing");
		boolean systemGC = Boolean.getBoolean("MyCommandLineRunner.systemGC");

		if (jsonProcessing) {
			ObjectMapper mapper = new ObjectMapper();
			String[] firstNames = mapper.readValue(
					MyCommandLineRunner.class.getClassLoader().getResourceAsStream(FIRST_NAMES), String[].class);
			logger.info("=> MyCommandLineRunner: read {} first names from {}", firstNames.length, FIRST_NAMES);
			String[] lastNames = mapper.readValue(
					MyCommandLineRunner.class.getClassLoader().getResourceAsStream(LAST_NAMES), String[].class);
			logger.info("=> MyCommandLineRunner: read {} last names from {}", lastNames.length, LAST_NAMES);
			String[] streetNames = mapper.readValue(
					MyCommandLineRunner.class.getClassLoader().getResourceAsStream(STREET_NAMES), String[].class);
			logger.info("=> MyCommandLineRunner read {} street names from {}", streetNames.length, STREET_NAMES);
			String[] cityNames = mapper.readValue(
					MyCommandLineRunner.class.getClassLoader().getResourceAsStream(CITY_NAMES), String[].class);
			logger.info("=> MyCommandLineRunner read {} city names from {}", cityNames.length, CITY_NAMES);

			int count = 10_000;
			if (args.length > 0) {
				count = Integer.parseInt(args[0]);
			}
			Random rand = new Random();
			int i = 0, failed = 0;
			while (i < count) {
				String ownerString = String.format(
						"{\"id\":null,\"firstName\":\"%s\",\"lastName\":\"%s\",\"address\":\"%d %s\",\"city\":\"%s\",\"telephone\":\"%d\",\"pets\":[]}",
						firstNames[i % firstNames.length], lastNames[i % lastNames.length], rand.nextInt(1_000),
						streetNames[i % streetNames.length], cityNames[i % cityNames.length],
						100_000_0000 + rand.nextInt(200_000_0000));
				try {
					Owner owner = mapper.readValue(ownerString, Owner.class);
					owners.save(owner);
				}
				catch (JsonProcessingException jpe) {
					logger.info("=> MyCommandLineRunner::onApplicationEvent(): {}", jpe);
					failed++;
				}
				i++;
			}
			logger.info("=> MyCommandLineRunner: inserted {} owners into the database", i - failed);
		}

		if (systemGC) {
			logger.info("=> MyCommandLineRunner: calling System.gc()");
			System.gc();
		}
	}

}
