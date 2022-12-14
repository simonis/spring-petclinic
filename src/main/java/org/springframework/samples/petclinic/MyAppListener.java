package org.springframework.samples.petclinic;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class MyAppListener implements SpringApplicationRunListener {

	private static final Logger logger = LoggerFactory.getLogger(MyAppListener.class);

	// Constructor is required.
	public MyAppListener(SpringApplication application, String[] args) {
	}

	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		logger.info("=> MyAppListener::contextLoaded()");
	}

	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {
		logger.info("=> MyAppListener::contextPrepared()");
	}

	@Override
	public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
			ConfigurableEnvironment environment) {
		logger.info("=> MyAppListener::environmentPrepared()");
	}

	@Override
	public void failed(ConfigurableApplicationContext context, Throwable exception) {
		logger.info("=> MyAppListener::failed()");
	}

	@Override
	public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
		logger.info("=> MyAppListener::ready(): {} seconds", timeTaken.toMillis() / 1000.0);
	}

	@Override
	public void started(ConfigurableApplicationContext context, Duration timeTaken) {
		logger.info("=> MyAppListener::started(): {} seconds", timeTaken.toMillis() / 1000.0);
	}

	@Override
	public void starting(ConfigurableBootstrapContext bootstrapContext) {
		logger.info("=> MyAppListener::starting()");
	}

}
