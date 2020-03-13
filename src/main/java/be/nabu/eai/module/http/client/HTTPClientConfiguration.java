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
@XmlType(propOrder = { "proxy", "keystore", "sslContextType", "cookiePolicy", "maxAmountOfConnectionsPerTarget", "socketTimeout", "connectionTimeout", "type", "static", "ioPoolSize", "processPoolSize" })
public class HTTPClientConfiguration {
	
	private Integer socketTimeout, connectionTimeout, maxAmountOfConnectionsPerTarget, ioPoolSize, processPoolSize;
	private ProxyArtifact proxy;
	private KeyStoreArtifact keystore;
	private Cookies cookiePolicy;
	private SSLContextType sslContextType;
	private Type type;
	private boolean isStatic;
	
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
	
}
