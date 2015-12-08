package be.nabu.eai.module.http.artifact;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class HTTPClientArtifact extends JAXBArtifact<HTTPClientConfiguration> {

	public HTTPClientArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "http-client.xml", HTTPClientConfiguration.class);
	}
	
}
