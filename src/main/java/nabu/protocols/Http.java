package nabu.protocols;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.net.ssl.SSLContext;
import javax.validation.constraints.NotNull;

import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.eai.repository.artifacts.proxy.DefinedProxy;
import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.client.ProxyBypassFilter;
import be.nabu.libs.http.client.DefaultHTTPClient;
import be.nabu.libs.http.client.HTTPProxy;
import be.nabu.libs.http.client.SPIAuthenticationHandler;
import be.nabu.libs.http.client.connections.PooledConnectionHandler;
import be.nabu.libs.http.core.CustomCookieStore;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.module.protocol.http.Cookies;
import be.nabu.module.protocol.http.HTTPTransactionable;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.api.Part;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;
import be.nabu.utils.security.SSLContextType;

@WebService
public class Http {
	
	public static final String HTTP_TRANSACTION_ID = "$httpClients";
	
	private ExecutionContext executionContext;
	
	@WebResult(name = "response")
	public HTTPResponse execute(@WebParam(name = "url") @NotNull URI url, @WebParam(name = "method") String method, @WebParam(name = "principal") BasicPrincipal principal, @WebParam(name = "part") Part part, @WebParam(name = "followRedirects") Boolean followRedirects, @WebParam(name = "httpVersion") Double httpVersion, @WebParam(name = "clientId") String clientId, @WebParam(name = "proxyId") String proxyId, @WebParam(name = "sslContextType") SSLContextType contextType, @WebParam(name = "cookiePolicy") Cookies policy, @WebParam(name = "keystoreId") String keystoreId, @WebParam(name = "maxAmountOfConnectionsPerTarget") Integer maxAmountOfConnectionsPerTarget) throws NoSuchAlgorithmException, KeyStoreException, IOException, FormatException, ParseException {
		if (clientId == null) {
			clientId = "default";
		}
		if (maxAmountOfConnectionsPerTarget == null) {
			maxAmountOfConnectionsPerTarget = 5;
		}
		if (policy == null) {
			policy = Cookies.ACCEPT_ALL;
		}
		if (contextType == null) {
			contextType = SSLContextType.TLS;
		}
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
		HTTPTransactionable transactionable = (HTTPTransactionable) executionContext.getTransactionContext().get(HTTP_TRANSACTION_ID, clientId);
		if (transactionable == null) {
			SSLContext context;
			if (keystoreId != null) {
				DefinedKeyStore keystore = executionContext.getServiceContext().getResolver(DefinedKeyStore.class).resolve(keystoreId);
				if (keystore == null) {
					throw new IllegalArgumentException("Invalid keystore id: " + keystoreId);
				}
				context = keystore.getKeyStore().newContext(contextType);
			}
			else {
				context = SSLContext.getDefault();
			}
			DefinedProxy proxy = null;
			if (proxyId != null) {
				proxy = executionContext.getServiceContext().getResolver(DefinedProxy.class).resolve(proxyId);
				if (proxy == null) {
					throw new IllegalArgumentException("Invalid proxy id: " + proxyId);
				}
			}
			PooledConnectionHandler connectionHandler = new PooledConnectionHandler(context, maxAmountOfConnectionsPerTarget);
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
			transactionable = new HTTPTransactionable(clientId, new DefaultHTTPClient(
				connectionHandler, 
				new SPIAuthenticationHandler(), 
				new CookieManager(new CustomCookieStore(), policy.getPolicy()),
				false
			));
			executionContext.getTransactionContext().add(HTTP_TRANSACTION_ID, transactionable);
		}
		DefaultHTTPClient client = transactionable.getClient();
		ModifiablePart modifiablePart = part instanceof ModifiablePart ? (ModifiablePart) part : MimeUtils.wrapModifiable(part);
		if (httpVersion >= 1.1 && MimeUtils.getHeader("Host", modifiablePart.getHeaders()) == null) {
			modifiablePart.setHeader(new MimeHeader("Host", url.getAuthority()));
		}
		HTTPRequest request = new DefaultHTTPRequest(
			method,
			url.getPath(),
			modifiablePart,
			httpVersion
		);
		return client.execute(request, principal, "https".equals(url.getScheme()), followRedirects);
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
