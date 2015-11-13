package be.nabu.module.protocol.http;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.http.client.DefaultHTTPClient;
import be.nabu.libs.services.api.Transactionable;

public class HTTPTransactionable implements Transactionable {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private String id;
	private DefaultHTTPClient client;

	public HTTPTransactionable(String id, DefaultHTTPClient client) {
		this.id = id;
		this.client = client;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void start() {
		// do nothing
	}

	@Override
	public void commit() {
		try {
			client.getConnectionHandler().close();
		}
		catch (IOException e) {
			logger.warn("Could not close connection pool properly", e);
		}
	}

	@Override
	public void rollback() {
		try {
			client.getConnectionHandler().close();
		}
		catch (IOException e) {
			logger.warn("Could not close connection pool properly", e);
		}		
	}

	public DefaultHTTPClient getClient() {
		return client;
	}

}
