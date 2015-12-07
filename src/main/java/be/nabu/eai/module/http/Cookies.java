package be.nabu.eai.module.http;

import java.net.CookiePolicy;

public enum Cookies {
	ACCEPT_ALL(CookiePolicy.ACCEPT_ALL),
	ACCEPT_NONE(CookiePolicy.ACCEPT_NONE),
	ACCEPT_ORIGINAL_SERVER(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
	;
	
	private CookiePolicy policy;

	private Cookies(CookiePolicy policy) {
		this.policy = policy;
	}
	public CookiePolicy getPolicy() {
		return policy;
	}
}
