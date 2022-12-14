package org.springframework.samples.petclinic.component;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MyCommandLineRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MyCommandLineRunner.class);

	@Override
	public void run(String... args) throws Exception {
		logger.info("=> MyCommandLineRunner started with: {}", Arrays.toString(args));
	}

}
