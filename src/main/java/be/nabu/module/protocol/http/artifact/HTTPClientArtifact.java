package be.nabu.module.protocol.http.artifact;

import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class HTTPClientArtifact extends JAXBArtifact<HTTPClientConfiguration> {

	public HTTPClientArtifact(String id, ResourceContainer<?> directory) {
		super(id, directory, "http-client.xml", HTTPClientConfiguration.class);
	}
	
}
