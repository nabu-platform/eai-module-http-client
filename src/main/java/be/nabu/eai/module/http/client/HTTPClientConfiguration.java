package be.nabu.eai.module.http.client;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.proxy.ProxyArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.security.SSLContextType;

@XmlRootElement(name = "httpClient")
@XmlType(propOrder = { "proxy", "keystore", "sslContextType", "cookiePolicy", "maxAmountOfConnectionsPerTarget", "socketTimeout", "connectionTimeout", "maxChunkSize", "type", "static", "ioPoolSize", "processPoolSize", "captureErrors", "captureSuccessful" })
public class HTTPClientConfiguration {
	
	private Integer socketTimeout, connectionTimeout, maxAmountOfConnectionsPerTarget, ioPoolSize, processPoolSize, maxChunkSize;
	private ProxyArtifact proxy;
	private KeyStoreArtifact keystore;
	private Cookies cookiePolicy;
	private SSLContextType sslContextType;
	private Type type;
	private boolean isStatic;
	// capture requests based on the response
	private boolean captureErrors, captureSuccessful;
		
	public Integer getSocketTimeout() {
		return socketTimeout;
	}
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	@EnvironmentSpecific
	public Integer getMaxAmountOfConnectionsPerTarget() {
		return maxAmountOfConnectionsPerTarget;
	}
	public void setMaxAmountOfConnectionsPerTarget(Integer maxAmountOfConnectionsPerTarget) {
		this.maxAmountOfConnectionsPerTarget = maxAmountOfConnectionsPerTarget;
	}
	
	// @2021-10-24: we've never actually used a proxy before, either we run on our own infrastructure or firewall rules were added
	// if we ever do need a proxy, we need to incorporate all the necessary settings _in_ the http client configuration
	// otherwise it is hard to "arbitrarily" configure a proxy in prd where none exists in dev for example
	// the proxy artifact seems to do nothing special except hold on to the configuration parameters anyway.
	@Deprecated
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public ProxyArtifact getProxy() {
		return proxy;
	}
	public void setProxy(ProxyArtifact proxy) {
		this.proxy = proxy;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeystore() {
		return keystore;
	}
	public void setKeystore(KeyStoreArtifact keystore) {
		this.keystore = keystore;
	}
	
	public Cookies getCookiePolicy() {
		return cookiePolicy;
	}
	public void setCookiePolicy(Cookies cookiePolicy) {
		this.cookiePolicy = cookiePolicy;
	}
	
	public SSLContextType getSslContextType() {
		return sslContextType;
	}
	public void setSslContextType(SSLContextType sslContextType) {
		this.sslContextType = sslContextType;
	}
	
	@Advanced
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}

	@Advanced
	@Comment(title = "Static clients are never closed but are shared by the entire server instance")
	public boolean isStatic() {
		return isStatic;
	}
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public enum Type {
		SYNCHRONOUS,
		ASYNCHRONOUS
	}

	@Advanced
	public Integer getIoPoolSize() {
		return ioPoolSize;
	}
	public void setIoPoolSize(Integer ioPoolSize) {
		this.ioPoolSize = ioPoolSize;
	}
	
	@Advanced
	public Integer getProcessPoolSize() {
		return processPoolSize;
	}
	public void setProcessPoolSize(Integer processPoolSize) {
		this.processPoolSize = processPoolSize;
	}
	
	@Advanced
	public boolean isCaptureErrors() {
		return captureErrors;
	}
	public void setCaptureErrors(boolean captureErrors) {
		this.captureErrors = captureErrors;
	}
	
	@Advanced
	public boolean isCaptureSuccessful() {
		return captureSuccessful;
	}
	public void setCaptureSuccessful(boolean captureSuccessful) {
		this.captureSuccessful = captureSuccessful;
	}
	
	@Advanced
	public Integer getMaxChunkSize() {
		return maxChunkSize;
	}
	public void setMaxChunkSize(Integer maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}
	
}
