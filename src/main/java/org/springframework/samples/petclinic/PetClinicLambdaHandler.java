package org.springframework.samples.petclinic;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class PetClinicLambdaHandler implements RequestHandler<HttpApiV2ProxyRequest, AwsProxyResponse> {

	private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

	static {
		try {
			handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(PetClinicApplication.class);
		}
		catch (ContainerInitializationException ex) {
			throw new RuntimeException("Unable to load spring boot application", ex);
		}
	}

	@Override
	public AwsProxyResponse handleRequest(HttpApiV2ProxyRequest input, Context context) {
		boolean log = Boolean.getBoolean("PetClinicLambdaHandler.log");
		if (log) {
			new Throwable().printStackTrace(System.out);
		}
		return handler.proxy(input, context);
	}

}
