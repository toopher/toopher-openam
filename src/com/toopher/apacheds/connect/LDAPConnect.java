package com.toopher.apacheds.connect;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

import com.toopher.config.Config;

public class LDAPConnect {
	final public String userBaseDir = Config.resourceConf
			.getString("USER_BASE_DIR");
	final public String ldapUrl = Config.resourceConf.getString("LDAP_URL");

	public Hashtable credentialLDAP() throws NamingException {
		Hashtable env = new Hashtable();
		if (Config.resourceConf != null) {
			env.put(Context.SECURITY_AUTHENTICATION,
					Config.resourceConf.getString("SECURITY_AUTHENTICATION"));
			env.put(Context.SECURITY_PRINCIPAL,
					Config.resourceConf.getString("BIND_DN_OR_USER"));
			env.put(Context.SECURITY_CREDENTIALS,
					Config.resourceConf.getString("BIND_PASSWORD"));
			env.put(Context.INITIAL_CONTEXT_FACTORY,
					Config.resourceConf.getString("INITIAL_CONTEXT_FACTORY"));
			env.put(Context.PROVIDER_URL, ldapUrl);

		}
		return env;
	}
}
/*
 * DirContext ctx = new InitialDirContext(env); NamingEnumeration enm =
 * ctx.list(""); while (enm.hasMore()) { System.out.println(enm.next()); }
 * 
 * enm.close(); ctx.close();
 */