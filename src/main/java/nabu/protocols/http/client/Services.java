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

package nabu.protocols.http.client;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.net.ssl.SSLContext;
import javax.validation.constraints.NotNull;

import be.nabu.eai.module.http.client.Cookies;
import be.nabu.eai.module.http.client.HTTPClientArtifact;
import be.nabu.eai.module.http.client.HTTPClientConfiguration.Type;
import be.nabu.eai.module.http.client.HTTPTransactionable;
import be.nabu.eai.module.proxy.ProxyArtifact;
import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.api.HTTPEntity;
import be.nabu.libs.http.api.HTTPInterceptor;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.LinkableHTTPResponse;
import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.http.api.client.ProxyBypassFilter;
import be.nabu.libs.http.api.client.TimedHTTPClient;
import be.nabu.libs.http.client.DefaultHTTPClient;
import be.nabu.libs.http.client.HTTPProxy;
import be.nabu.libs.http.client.NTLMPrincipalImpl;
import be.nabu.libs.http.client.SPIAuthenticationHandler;
import be.nabu.libs.http.client.connections.PlainConnectionHandler;
import be.nabu.libs.http.client.connections.PooledConnectionHandler;
import be.nabu.libs.http.client.nio.NIOHTTPClientImpl;
import be.nabu.libs.http.core.CustomCookieStore;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.http.core.HTTPRequestAuthenticatorFactory;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.core.HttpMessage;
import be.nabu.libs.http.server.nio.MemoryMessageDataProvider;
import be.nabu.libs.nio.PipelineUtils;
import be.nabu.libs.nio.api.Pipeline;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.types.api.KeyValuePair;
import be.nabu.utils.cep.impl.CEPUtils;
import be.nabu.utils.cep.impl.HTTPComplexEventImpl;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.api.Part;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;
import be.nabu.utils.security.SSLContextType;
import nabu.protocols.http.client.types.HTTPAuthentication;

@WebService
public class Services {
	
	private ExecutionContext executionContext;
	
	@WebResult(name = "response")
	public HTTPResponse execute(@WebParam(name = "url") @NotNull URI url, @WebParam(name = "method") String method, @WebParam(name = "part") Part part, @WebParam(name = "principal") BasicPrincipal principal, @WebParam(name = "followRedirects") Boolean followRedirects, @WebParam(name = "httpVersion") Double httpVersion, @WebParam(name = "httpClientId") String httpClientId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "forceFullTarget") Boolean forceFullTarget, @WebParam(name = "timeout") Long timeout, @WebParam(name = "timeoutUnit") TimeUnit unit, @WebParam(name = "authentication") HTTPAuthentication authentication) throws NoSuchAlgorithmException, KeyStoreException, IOException, FormatException, ParseException {
		if (followRedirects == null) {
			followRedirects = true;
		}
		if (httpVersion == null) {
			httpVersion = 1.1;
		}
		if (method == null) {
			method = "GET";
		}
		if (part == null) {
			part = new PlainMimeEmptyPart(null, new MimeHeader("Content-Length", "0"));
		}
		if (forceFullTarget == null) {
			forceFullTarget = false;
		}
		
		HTTPClient client = getTransactionable(executionContext, transactionId, httpClientId).getClient();
		ModifiablePart modifiablePart = part instanceof ModifiablePart ? (ModifiablePart) part : MimeUtils.wrapModifiable(part);
		if (httpVersion >= 1.1 && MimeUtils.getHeader("Host", modifiablePart.getHeaders()) == null) {
			modifiablePart.setHeader(new MimeHeader("Host", url.getAuthority()));
		}
		
		Header contentLengthHeader = MimeUtils.getHeader("Content-Length", modifiablePart.getHeaders());
		// if we don't have a specific content length, check that we have chunked encoding
		if (contentLengthHeader == null && !"get".equalsIgnoreCase(method)) {
			String transferEncoding = MimeUtils.getTransferEncoding(modifiablePart.getHeaders());
			if (transferEncoding == null) {
				modifiablePart.setHeader(new MimeHeader("Transfer-Encoding", "chunked"));
			}
		}
		
		String target;
		if (forceFullTarget) {
			target = url.toString();
		}
		else {
			target = url.getPath() == null || url.getPath().isEmpty() ? "/" : url.getPath();
			if (url.getQuery() != null) {
				target += "?" + url.getQuery();
			}
			if (url.getFragment() != null) {
				target += "#" + url.getFragment();
			}
		}
		HTTPRequest request = new DefaultHTTPRequest(
			method,
			target,
			modifiablePart,
			httpVersion
		);
		
		if (authentication != null && authentication.getSecurityType() != null && !HTTPRequestAuthenticatorFactory.getInstance().getAuthenticator(authentication.getSecurityType())
				.authenticate(request, authentication.getSecurityContext(), null, false)) {
			throw new IllegalStateException("Could not authenticate the request");
		}
		
		if (principal != null && principal.getName() != null) {
			int index = principal.getName().indexOf('/');
			if (index < 0) {
				index = principal.getName().indexOf('\\');
			}
			// if we have a slash, assume NTLM
			if (index > 0) {
				principal = new NTLMPrincipalImpl(principal.getName().substring(0, index), principal.getName().substring(index + 1), principal.getName());
			}
		}
		if (client instanceof TimedHTTPClient) {
			return ((TimedHTTPClient) client).execute(request, principal, "https".equals(url.getScheme()), followRedirects, timeout, unit);
		}
		else {
			return client.execute(request, principal, "https".equals(url.getScheme()), followRedirects);
		}
	}
	
	public static HTTPTransactionable getTransactionable(ExecutionContext executionContext, String transactionId, String clientId) throws IOException, KeyStoreException, NoSuchAlgorithmException {
		HTTPClientArtifact httpArtifact = clientId == null ? null : executionContext.getServiceContext().getResolver(HTTPClientArtifact.class).resolve(clientId);
		return getTransactionable(executionContext, transactionId, httpArtifact);
//		HTTPTransactionable transactionable = (HTTPTransactionable) executionContext.getTransactionContext().get(transactionId, clientId == null ? "$default" : clientId);
//		if (transactionable == null) {
//			transactionable = new HTTPTransactionable(clientId == null ? "$default" : clientId, newClient(httpArtifact), httpArtifact == null ? false : httpArtifact.getConfig().isStatic());
//			executionContext.getTransactionContext().add(transactionId, transactionable);
//		}
//		return transactionable;
	}
	
	public static HTTPTransactionable getTransactionable(ExecutionContext executionContext, String transactionId, HTTPClientArtifact httpArtifact) throws IOException, KeyStoreException, NoSuchAlgorithmException {
		HTTPTransactionable transactionable = (HTTPTransactionable) executionContext.getTransactionContext().get(transactionId, httpArtifact == null ? "$default" : httpArtifact.getId());
		if (transactionable == null) {
			transactionable = new HTTPTransactionable(httpArtifact == null ? "$default" : httpArtifact.getId(), newClient(httpArtifact), httpArtifact == null ? false : httpArtifact.getConfig().isStatic());
			executionContext.getTransactionContext().add(transactionId, transactionable);
		}
		return transactionable;
	}
	
	private static Map<String, HTTPClient> staticClients = new HashMap<String, HTTPClient>();
	
	public static HTTPClient newClient(HTTPClientArtifact httpArtifact) throws IOException, KeyStoreException, NoSuchAlgorithmException {
		if (httpArtifact != null && httpArtifact.getConfig().isStatic()) {
			if (!staticClients.containsKey(httpArtifact.getId())) {
				synchronized(staticClients) {
					if (!staticClients.containsKey(httpArtifact.getId())) {
						staticClients.put(httpArtifact.getId(), newClientInstance(httpArtifact));
					}
				}
			}
			return staticClients.get(httpArtifact.getId());
		}
		return newClientInstance(httpArtifact);
	}
	
	@SuppressWarnings("resource")
	private static HTTPClient newClientInstance(final HTTPClientArtifact httpArtifact) throws IOException, KeyStoreException, NoSuchAlgorithmException {
		int maxAmountOfConnectionsPerTarget = httpArtifact == null || httpArtifact.getConfiguration().getMaxAmountOfConnectionsPerTarget() == null ? 5 : httpArtifact.getConfiguration().getMaxAmountOfConnectionsPerTarget(); 
		Cookies cookiePolicy = httpArtifact == null || httpArtifact.getConfiguration().getCookiePolicy() == null ? Cookies.ACCEPT_ALL : httpArtifact.getConfiguration().getCookiePolicy();
		SSLContextType sslContextType = httpArtifact == null || httpArtifact.getConfiguration().getSslContextType() == null ? SSLContextType.TLS : httpArtifact.getConfiguration().getSslContextType();

		SSLContext context;
		if (httpArtifact != null && httpArtifact.getConfiguration().getKeystore() != null) {
			context = httpArtifact.getConfiguration().getKeystore().getKeyStore().newContext(sslContextType);
		}
		else {
			context = SSLContext.getDefault();
		}
		
		final boolean captureErrors = httpArtifact != null && httpArtifact.getConfig().isCaptureErrors();
		final boolean captureSuccessful = httpArtifact != null && httpArtifact.getConfig().isCaptureSuccessful();
		final EventDispatcher complexEventDispatcher = httpArtifact == null ? null : httpArtifact.getRepository().getComplexEventDispatcher();
		HTTPInterceptor interceptor = complexEventDispatcher != null && (captureErrors || captureSuccessful) ? new HTTPInterceptor() {
			@Override
			public HTTPEntity intercept(HTTPEntity response) {
				if (response instanceof HTTPResponse) {
					if ((captureSuccessful && ((HTTPResponse) response).getCode() < 400) || (captureErrors && ((HTTPResponse) response).getCode() >= 400)) {
						if (response instanceof LinkableHTTPResponse) {
							HTTPRequest request = ((LinkableHTTPResponse) response).getRequest();
							if (request != null) {
								HTTPComplexEventImpl event = new HTTPComplexEventImpl();
								event.setResponseCode(((HTTPResponse) response).getCode());
								event.setMethod(request.getMethod());
								event.setArtifactId(httpArtifact.getId());
								event.setEventCategory("http-message");
								try {
									event.setRequestUri(HTTPUtils.getURI(request, false));
								}
								catch (FormatException e) {
									// best effort
									e.printStackTrace();
								}
								Pipeline pipeline = PipelineUtils.getPipeline();
								CEPUtils.enrich(event, getClass(), "http-message-out", pipeline == null || pipeline.getSourceContext() == null ? null : pipeline.getSourceContext().getSocketAddress(), null, null);
								event.setApplicationProtocol("HTTP");
								event.setCorrelationId(MimeUtils.getCorrelationId(request.getContent().getHeaders()));
								HttpMessage messageIn = HTTPUtils.toMessage(request);
								HttpMessage messageOut = HTTPUtils.toMessage(response);
								event.setData("# Request\n\n" + messageIn.getMessage() + "\n\n# Response\n\n" + messageOut.getMessage());
								complexEventDispatcher.fire(event, this);
							}
						}
					}
				}
				return response;
			}
		} : null;
				
		ProxyArtifact proxy = httpArtifact == null ? null : httpArtifact.getConfiguration().getProxy();
		int connectionTimeout = httpArtifact == null || httpArtifact.getConfiguration().getConnectionTimeout() == null ? 1000*60*30 : httpArtifact.getConfiguration().getConnectionTimeout();
		int socketTimeout = httpArtifact == null || httpArtifact.getConfiguration().getSocketTimeout() == null ? 1000*60*30 : httpArtifact.getConfiguration().getSocketTimeout();

		if (httpArtifact == null || httpArtifact.getConfig().getType() == null || httpArtifact.getConfig().getType() == Type.SYNCHRONOUS) {
			PooledConnectionHandler connectionHandler = new PooledConnectionHandler(context, maxAmountOfConnectionsPerTarget);
			connectionHandler.setConnectionTimeout(connectionTimeout);
			connectionHandler.setSocketTimeout(socketTimeout);
			if (proxy != null) {
				final String username = proxy.getConfiguration().getUsername();
				final String password = proxy.getConfiguration().getPassword();
				connectionHandler.setProxy(new HTTPProxy(
					proxy.getConfiguration().getHost(), 
					proxy.getConfiguration().getPort(), 
					username == null ? null :
						new BasicPrincipal() {
							private static final long serialVersionUID = 1L;
							@Override
							public String getName() {
								return username;
							}
							@Override
							public String getPassword() {
								return password;
							}
						},
					new SPIAuthenticationHandler(),
					connectionHandler.getConnectionTimeout(),
					connectionHandler.getSocketTimeout(),
					context
				), generateProxyBypassFilters(proxy.getConfiguration().getBypass()).toArray(new ProxyBypassFilter[0]));
			}
			DefaultHTTPClient client = new DefaultHTTPClient(
	//			connectionHandler, 
				new PlainConnectionHandler(context, connectionTimeout, socketTimeout),
				new SPIAuthenticationHandler(), 
//				new CookieManager(new CustomCookieStore(), cookiePolicy.getPolicy()),
				new CookieManager(),
				false
			);
			client.getExecutor().setInterceptor(interceptor);
			if (httpArtifact != null) {
				client.setForceContentLength(httpArtifact.getConfig().isForceContentLength());
			}
			return client;
		}
		else {
			int ioPoolSize = httpArtifact.getConfig().getIoPoolSize() != null ? httpArtifact.getConfig().getIoPoolSize() : 5;
			int processPoolSize = httpArtifact.getConfig().getProcessPoolSize() != null ? httpArtifact.getConfig().getProcessPoolSize() : 10;
			// we need at least a couple of io threads so we don't get stuck
			NIOHTTPClientImpl impl = new NIOHTTPClientImpl(context, Math.max(3, ioPoolSize), Math.max(1, processPoolSize), maxAmountOfConnectionsPerTarget, new EventDispatcherImpl(), new MemoryMessageDataProvider(), new CookieManager(new CustomCookieStore(), cookiePolicy.getPolicy()), Executors.defaultThreadFactory());
			impl.setRequestTimeout(socketTimeout);
			impl.setInterceptor(interceptor);
			impl.setMaxChunkSize(httpArtifact.getConfig().getMaxChunkSize());
			return impl;
		}
	}
	
	private static List<ProxyBypassFilter> generateProxyBypassFilters(String bypass) {
		List<ProxyBypassFilter> filters = new ArrayList<ProxyBypassFilter>();
		if (bypass != null) {
			for (String part : bypass.split("[\\s]*,[\\s]*")) {
				int index = part.indexOf(':');
				final int bypassPort = index > 0 ? Integer.parseInt(part.substring(index + 1)) : 80;
				final String bypassServer = index > 0 ? part.substring(0, index) : part;
				filters.add(new ProxyBypassFilter() {
					@Override
					public boolean bypass(String host, int port) {
						return bypassServer.equals(host) && bypassPort == port;
					}
				});
			}
		}
		return filters;
	}
	
	@WebResult(name = "part")
	public Part newFormInput(@WebParam(name = "parameters") List<KeyValuePair> parameters, @WebParam(name = "headers") List<Header> headers) {
		StringBuilder content = new StringBuilder();
		for (KeyValuePair parameter: parameters) {
			if (!content.toString().isEmpty()) {
				content.append("&");
			}
			content.append(URIUtils.encodeURL(parameter.getKey()));
			content.append("=");
			content.append(URIUtils.encodeURL(parameter.getValue()));
		}
		byte [] bytes = content.toString().getBytes(Charset.forName("UTF-8"));
		if (headers == null) {
			headers = new ArrayList<Header>();
		}
		headers.add(new MimeHeader("Content-Length", "" + bytes.length));
		headers.add(new MimeHeader("Content-Type", "application/x-www-form-urlencoded"));
		return new PlainMimeContentPart(null, IOUtils.wrap(bytes, true), headers.toArray(new Header[headers.size()]));
	}
}
