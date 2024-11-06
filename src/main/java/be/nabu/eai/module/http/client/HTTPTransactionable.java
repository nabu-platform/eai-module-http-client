/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.http.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.services.api.Transactionable;

public class HTTPTransactionable implements Transactionable {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private String id;
	private HTTPClient client;
	private boolean closed;
	private boolean isStatic;

	public HTTPTransactionable(String id, HTTPClient client, boolean isStatic) {
		this.id = id;
		this.client = client;
		this.isStatic = isStatic;
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
			closed = true;
			if (!isStatic) {
				client.close();
			}
		}
		catch (IOException e) {
			logger.warn("Could not close connection pool properly", e);
		}
	}

	@Override
	public void rollback() {
		try {
			closed = true;
			if (!isStatic) {
				client.close();
			}
		}
		catch (IOException e) {
			logger.warn("Could not close connection pool properly", e);
		}		
	}

	public HTTPClient getClient() {
		return client;
	}

	public boolean isClosed() {
		return closed;
	}

}
