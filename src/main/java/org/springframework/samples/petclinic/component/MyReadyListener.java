package org.springframework.samples.petclinic.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;

@Component
public class MyReadyListener {

	private static final Logger logger = LoggerFactory.getLogger(MyReadyListener.class);

	private final OwnerRepository owners;

	@Autowired
	public MyReadyListener(OwnerRepository owners) {
		this.owners = owners;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationEvent(ApplicationReadyEvent event) {
		logger.info("=> MyReadyListener::onApplicationEvent(): {}", event.getTimeTaken().toMillis() / 1000.0);
	}

}