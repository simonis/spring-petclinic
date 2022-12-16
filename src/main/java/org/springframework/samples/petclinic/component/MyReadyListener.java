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
		logger.info("=> MyReadyListener::onApplicationEvent(): {}", event);
		Owner owner = owners.findById(1);
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		try {
			logger.info("=> MyReadyListener::onApplicationEvent(): {}", mapper.writeValueAsString(owner));
		}
		catch (JsonProcessingException jpe) {
			logger.info("=> MyReadyListener::onApplicationEvent(): {}", jpe);
		}
		String volker = "[{\"id\":null,\"firstName\":\"Volker\",\"lastName\":\"Simonis\",\"address\":\"Schlossweg 10\",\"city\":\"Walldorf\",\"telephone\":\"0123456789\",\"pets\":[]}, {\"id\":null,\"firstName\":\"Joe\",\"lastName\":\"Cool\",\"address\":\"10 Main Street\",\"city\":\"New York\",\"telephone\":\"0123456789\",\"pets\":[]}]";
		try {
			List<Owner> owners = mapper.readValue(volker, new TypeReference<List<Owner>>() {
			});
			for (Owner o : owners) {
				this.owners.save(o);
			}
		}
		catch (JsonProcessingException jpe) {
			logger.info("=> MyReadyListener::onApplicationEvent(): {}", jpe);
		}
	}

}