package nabu.protocols.http.client;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

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
import be.nabu.libs.events.impl.EventDispatcherImpl;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.http.api.client.ProxyBypassFilter;
import be.nabu.libs.http.client.DefaultHTTPClient;
import be.nabu.libs.http.client.HTTPProxy;
import be.nabu.libs.http.client.NTLMPrincipalImpl;
import be.nabu.libs.http.client.SPIAuthenticationHandler;
import be.nabu.libs.http.client.connections.PlainConnectionHandler;
import be.nabu.libs.http.client.connections.PooledConnectionHandler;
import be.nabu.libs.http.client.nio.NIOHTTPClientImpl;
import be.nabu.libs.http.core.CustomCookieStore;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.http.server.nio.MemoryMessageDataProvider;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.api.Part;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;
import be.nabu.utils.security.SSLContextType;

@WebService
public class Services {
	
	private ExecutionContext executionContext;
	
	@WebResult(name = "response")
	public HTTPResponse execute(@WebParam(name = "url") @NotNull URI url, @WebParam(name = "method") String method, @WebParam(name = "part") Part part, @WebParam(name = "principal") BasicPrincipal principal, @WebParam(name = "followRedirects") Boolean followRedirects, @WebParam(name = "httpVersion") Double httpVersion, @WebParam(name = "httpClientId") String httpClientId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "forceFullTarget") Boolean forceFullTarget) throws NoSuchAlgorithmException, KeyStoreException, IOException, FormatException, ParseException {
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
		return client.execute(request, principal, "https".equals(url.getScheme()), followRedirects);
	}
	
	public static HTTPTransactionable getTransactionable(ExecutionContext executionContext, String transactionId, String clientId) throws IOException, KeyStoreException, NoSuchAlgorithmException {
		HTTPTransactionable transactionable = (HTTPTransactionable) executionContext.getTransactionContext().get(transactionId, clientId == null ? "$default" : clientId);
		if (transactionable == null) {
			HTTPClientArtifact httpArtifact = clientId == null ? null : executionContext.getServiceContext().getResolver(HTTPClientArtifact.class).resolve(clientId);
			transactionable = new HTTPTransactionable(clientId == null ? "$default" : clientId, newClient(httpArtifact), httpArtifact == null ? false : httpArtifact.getConfig().isStatic());
			executionContext.getTransactionContext().add(transactionId, transactionable);
		}
		return transactionable;
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
	private static HTTPClient newClientInstance(HTTPClientArtifact httpArtifact) throws IOException, KeyStoreException, NoSuchAlgorithmException {
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
			return new DefaultHTTPClient(
	//			connectionHandler, 
				new PlainConnectionHandler(context, connectionTimeout, socketTimeout),
				new SPIAuthenticationHandler(), 
				new CookieManager(new CustomCookieStore(), cookiePolicy.getPolicy()),
				false
			);
		}
		else {
			NIOHTTPClientImpl impl = new NIOHTTPClientImpl(context, 5, 10, maxAmountOfConnectionsPerTarget, new EventDispatcherImpl(), new MemoryMessageDataProvider(), new CookieManager(new CustomCookieStore(), cookiePolicy.getPolicy()), Executors.defaultThreadFactory());
			impl.setRequestTimeout(socketTimeout);
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
	
}
