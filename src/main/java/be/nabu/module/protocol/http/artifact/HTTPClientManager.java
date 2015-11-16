package be.nabu.module.protocol.http.artifact;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class HTTPClientManager extends JAXBArtifactManager<HTTPClientConfiguration, HTTPClientArtifact> {

	public HTTPClientManager() {
		super(HTTPClientArtifact.class);
	}

	@Override
	protected HTTPClientArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new HTTPClientArtifact(id, container);
	}

}
