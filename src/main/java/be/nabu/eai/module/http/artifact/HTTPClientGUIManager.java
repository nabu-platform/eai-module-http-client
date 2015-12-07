package be.nabu.eai.module.http.artifact;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class HTTPClientGUIManager extends BaseJAXBGUIManager<HTTPClientConfiguration, HTTPClientArtifact> {

	public HTTPClientGUIManager() {
		super("HTTP Client", HTTPClientArtifact.class, new HTTPClientManager(), HTTPClientConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected HTTPClientArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new HTTPClientArtifact(entry.getId(), entry.getContainer());
	}

	@Override
	public String getCategory() {
		return "Protocols";
	}
}
