package org.springframework.samples.petclinic.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;

@Component
public class MyCheckpointRestoreListener implements Resource {

	private static final Logger logger = LoggerFactory.getLogger(MyCheckpointRestoreListener.class);

	private final OwnerRepository owners;

	private final ConfigurableApplicationContext context;

	@Autowired
	public MyCheckpointRestoreListener(ConfigurableApplicationContext context, OwnerRepository owners) {
		this.context = context;
		this.owners = owners;
		Core.getGlobalContext().register(this);
		logger.info("=> MyCheckpointRestoreListener: Registering in {}", Core.getGlobalContext());
	}

	@Override
	public void beforeCheckpoint(Context<? extends Resource> context) {
		logger.info("=> MyCheckpointRestoreListener: beforeCheckpoint");
	}

	@Override
	public void afterRestore(Context<? extends Resource> context) {
		logger.info("=> MyCheckpointRestoreListener: afterRestore");
	}

}
