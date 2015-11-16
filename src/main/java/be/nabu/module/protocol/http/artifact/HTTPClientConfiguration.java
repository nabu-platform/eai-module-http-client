package be.nabu.module.protocol.http.artifact;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.eai.repository.artifacts.proxy.DefinedProxy;
import be.nabu.module.protocol.http.Cookies;
import be.nabu.utils.security.SSLContextType;

@XmlRootElement(name = "httpClient")
@XmlType(propOrder = { "proxy", "keystore", "sslContextType", "cookiePolicy", "maxAmountOfConnectionsPerTarget", "socketTimeout", "connectionTimeout" })
public class HTTPClientConfiguration {
	
	private Integer socketTimeout, connectionTimeout, maxAmountOfConnectionsPerTarget;
	private DefinedProxy proxy;
	private DefinedKeyStore keystore;
	private Cookies cookiePolicy;
	private SSLContextType sslContextType;
	
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
	public Integer getMaxAmountOfConnectionsPerTarget() {
		return maxAmountOfConnectionsPerTarget;
	}
	public void setMaxAmountOfConnectionsPerTarget(
			Integer maxAmountOfConnectionsPerTarget) {
		this.maxAmountOfConnectionsPerTarget = maxAmountOfConnectionsPerTarget;
	}
	public DefinedProxy getProxy() {
		return proxy;
	}
	public void setProxy(DefinedProxy proxy) {
		this.proxy = proxy;
	}
	public DefinedKeyStore getKeystore() {
		return keystore;
	}
	public void setKeystore(DefinedKeyStore keystore) {
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
}
