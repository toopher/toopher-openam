package com.toopher.apacheds.connect;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.toopher.api.ToopherUseage;

final public class UserManager extends LDAPConnect {
	/**
	 * 
	 * @param uid
	 * @param password
	 * @return
	 */
	final public boolean validateUser(String uid, String password) {
		System.err.println(uid+""+password);
		DirContext ctx = null;
		NamingEnumeration enm = null;
		try {
			Hashtable env = credentialLDAP();
			System.err.println(env);
			env.put(Context.SECURITY_PRINCIPAL, "uid=" + uid + ","
					+ userBaseDir);
			env.put(Context.SECURITY_CREDENTIALS, password);
			ctx = new InitialDirContext(env);
			enm = ctx.list("");
			return enm.hasMore();
		} catch (NamingException ne) {
			ToopherUseage.toopherPrintln("Error in UserManager.validateUser:--"
					+ ne.getMessage());
		} finally {
			try {
				enm.close();
				ctx.close();
			} catch (Exception e) {
				ToopherUseage
						.toopherPrintln("Error in finally UserManager.validateUser:--"
								+ e.getMessage());
			}
		}
		return false;
	}

	public boolean addOrUpdatePairing(String uid, String pairingId) {
		try {
			DirContext ctx = new InitialDirContext(credentialLDAP());
			if (findPairingByUID(uid) == null) {
				Attributes userAttributes = new BasicAttributes(true);
				BasicAttribute basicattribute = new BasicAttribute(
						"objectclass", "toopherPair");
				basicattribute.add(1, "top");
				userAttributes.put(basicattribute);
				userAttributes.put(new BasicAttribute("cn", "pairingid"));
				userAttributes.put(new BasicAttribute("toopherPairingId",
						pairingId));
				ctx.createSubcontext("cn=pairingid,uid=" + uid + ","
						+ userBaseDir, userAttributes);
			} else {
				ModificationItem[] mods = new ModificationItem[1];
				mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
						new BasicAttribute("toopherPairingId", pairingId));
				ctx.modifyAttributes("cn=pairingid,uid=" + uid + ","
						+ userBaseDir, mods);
			}
			ctx.close();
			return true;
		} catch (NamingException e) {
			ToopherUseage
					.toopherPrintln("Error in UserManager.addOrUpdatePairing :--"
							+ e.getMessage());
		}
		return false;
	}

	public String findPairingByUID(String uid) {
		try {
			Attributes attributes = search("uid=" + uid + "," + userBaseDir,
					"(&(objectClass=toopherPair)(cn=pairingid))",
					new String[] { "toopherPairingId" });
			if (attributes != null
					&& attributes.get("toopherPairingId") != null) {
				Attribute attribute = attributes.get("toopherPairingId");
				return (String) attribute.get(0);
			}

		} catch (NamingException e) {
			ToopherUseage
					.toopherPrintln("Error in UserManager.findPairingByUID:--"
							+ e.getMessage());
		}
		return null;
	}

	public boolean addTerminalName(String uid, String terminalKey,
			String terminalValue) {
		try {
			DirContext ctx = new InitialDirContext(credentialLDAP());
			Attributes userAttributes = new BasicAttributes(true);
			BasicAttribute basicattribute = new BasicAttribute("objectclass",
					"toopherFriendlyNameConfiguration");
			basicattribute.add(1, "top");
			userAttributes.put(basicattribute);
			userAttributes.put(new BasicAttribute("cn", terminalKey));
			userAttributes.put(new BasicAttribute(
					"toopherTerminalFriendlyName", terminalValue));
			ctx.createSubcontext("cn=" + terminalKey + ",uid=" + uid + ","
					+ userBaseDir, userAttributes);
			return true;
		} catch (NamingException e) {
			ToopherUseage
					.toopherPrintln("Error in UserManager.addTerminalName:--"
							+ e.getMessage());
		}
		return false;
	}

	public String findTerminalByUID(String uid, String terminalKey) {
		try {
			Attributes attributes = search("uid=" + uid + "," + userBaseDir,
					"(&(objectClass=toopherFriendlyNameConfiguration)(cn="
							+ terminalKey + "))",
					new String[] { "toopherTerminalFriendlyName" });
			if (attributes != null
					&& attributes.get("toopherTerminalFriendlyName") != null) {
				Attribute attribute = attributes
						.get("toopherTerminalFriendlyName");
				return (String) attribute.get(0);
			}

		} catch (NamingException e) {
			ToopherUseage
					.toopherPrintln("Error in UserManager.findTerminalByUID:--"
							+ e.getMessage());
		}
		return null;

	}

	public Attributes search(String searchBaseDN, String searchFilter,
			String attrs[]) {
		try {
			DirContext ctx = new InitialDirContext(credentialLDAP());
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			searchControls.setReturningAttributes(attrs);
			NamingEnumeration<SearchResult> results = ctx.search(searchBaseDN,
					searchFilter, searchControls);
			if (results.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) results.next();
				if (searchResult != null) {
					return searchResult.getAttributes();
				}
			}
		} catch (NamingException e) {
			ToopherUseage
					.toopherPrintln("Error in UserManager.findPairingByUID:--"
							+ e.getMessage());
		}
		return null;
	}

}
