// vim: sw=4:ts=4:cindent
package com.toopher.openam;

import java.security.Principal;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.ResourceBundle;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.toopher.api.*;

import com.toopher.usermanagement.*;
import com.toopher.usermanagement.dal.*;
import com.toopher.usermanagement.dal.ldap.*;

public class ToopherSecondFactor extends AMLoginModule {
	// Name for the debug-log
	private final static String DEBUG_NAME = "ToopherSecondFactor";
	// Name of the resource-bundle
	private final static String amAuthToopherSecondFactor = "amAuthToopherSecondFactor";
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final String TERMINAL_ID_COOKIE_NAME = "toopher_terminal_id";
	private static final int TERMINAL_ID_COOKIE_MAX_AGE = 10 * 365 * 24 * 60 * 60;  // 10 years
	
	// orders defined in the callbacks file
	private String USERNAME = "-";
	private final static int STATE_BEGIN = 1;
	private final static int STATE_ENTER_PAIRING_PHRASE = 2;
	private final static int STATE_WAIT_FOR_PAIRING = 3;
	private final static int STATE_NAME_TERMINAL = 4;
	private final static int STATE_WAIT_FOR_AUTH = 5;
	private final static int STATE_TOOPHER_OPT_IN = 6;
	private final static int STATE_ERROR = 7;

	private final static Debug debug = Debug.getInstance(DEBUG_NAME);

	private Map options;
	private ResourceBundle bundle;
	private Map sharedState;
	private ToopherUserManager toopherUserManager;
	private ToopherAPI api;
	private AuthenticationStatus auth;

	public ToopherSecondFactor() {
		super();
	}

	private ToopherUserManager getToopherUserManager() {

		debug.message("Creating ToopherUserManager");
		Set serverUris = (Set)options.get("iplanet-am-auth-ToopherSecondFactor-serverList");
		int numServers = serverUris.size();
		String[] hosts = new String[numServers];
		int[] ports = new int[numServers];
		int index = 0;
		for(Object _serverUri : serverUris){
			String serverUri = (String)_serverUri;
			try {
				URI url = new URI(serverUri);
				hosts[index] = url.getHost();
				if (url.getPort() == -1){
					ports[index] = 389;
				} else {
					ports[index] = url.getPort();
				}
				index = index + 1;
			} catch (URISyntaxException e) {
				debug.message("Error parsing url: " + e.getMessage());
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				debug.message(sw.toString());
				//pass
			}
		}
		int numConnections = CollectionHelper.getIntMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-numConnections", 1, debug);
		String bindDN = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-bindUserDN");
		String bindPW = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-bindUserPassword");
		String baseDC = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-ldapBaseDC");
		String searchDN = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-ldapUserSearchDN");
		String uidAttr = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-ldapUidAttribute");
		String usernameAttr = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-ldapUsernameAttribute");
		ToopherUserLdapDataStoreOptions toopherLdapOptions = new ToopherUserLdapDataStoreOptions(
				hosts,
				ports,
				numConnections,
				bindDN,
				bindPW,
				baseDC,
				searchDN,
				uidAttr);

		ToopherUserLdapDataStore ds = new ToopherUserLdapDataStore(new LDAPConnectionOptions(), toopherLdapOptions);
		try {
			ds.connect();
		} catch (ToopherDataStoreException e) {
			debug.message("Error in ToopherSecondFactor::getToopherUserManager : " + e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			debug.message(sw.toString());
			return null;
		}
		return new ToopherUserManager(ds);

	}

	@Override
	// In this method we store service attributes and localized properties
	// for later use
	public void init(Subject subject, Map sharedState, Map options) {
		if (isUseFirstPassEnabled()) {
			this.sharedState = sharedState;
		}
		if (sharedState == null) {
			debug.message("sharedState is null!");
		} else {
			debug.message("sharedState is " + sharedState.toString());
		}
		if (options == null) {
			debug.message("options is null");
		} else {
			debug.message("options is " + options.toString());
		}
		this.options = options;
		bundle = amCache.getResBundle(amAuthToopherSecondFactor, getLoginLocale());
		this.toopherUserManager = getToopherUserManager();

		String toopherConsumerKey = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-consumerKey");
		String toopherConsumerSecret = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ToopherSecondFactor-consumerSecret");
		api = new ToopherAPI(toopherConsumerKey, toopherConsumerSecret);

		clearStatusCookie();
	}

	private ToopherUserTerminal getToopherUserTerminal(ToopherUser user) throws ToopherDataStoreException{
		HttpServletRequest request = getHttpServletRequest();
		ToopherUserTerminal result = null;
		for (Cookie cookie : request.getCookies()){
			if(cookie.getName().equals(TERMINAL_ID_COOKIE_NAME)) {
				// terminal has already been set
				debug.message(String.format("toopher_terminal_id = %s", cookie.getValue()));
				return toopherUserManager.getOrCreateToopherUserTerminal(user, cookie.getValue());
			}
		}

		// no cookie has been set yet - set one on the response
		//
		String terminalId = new BigInteger(20 * 8, secureRandom).toString(32);
		HttpServletResponse response = getHttpServletResponse();
		Cookie terminalCookie = new Cookie(TERMINAL_ID_COOKIE_NAME, terminalId);
		terminalCookie.setMaxAge(TERMINAL_ID_COOKIE_MAX_AGE);
		terminalCookie.setPath("/");
		response.addCookie(terminalCookie);
		return toopherUserManager.getOrCreateToopherUserTerminal(user, terminalId);
	}
	

	private void clearStatusCookie() {
		getHttpServletResponse().addCookie(new Cookie("toopher_auth_status", "n/a"));
	}
	private void setStatusCookiePoll() {
		getHttpServletResponse().addCookie(new Cookie("toopher_auth_status", "poll"));
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {
		try {
			debug.message("====================================");
			if (debug.messageEnabled()) {
				debug.message("SampleAuthTwo::process state: " + state);
			}
			USERNAME = (String) sharedState.get(getUserKey());
			HttpServletRequest request = getHttpServletRequest();
			HttpServletResponse response = getHttpServletResponse();

			String username = (String) sharedState.get(getUserKey());
			String password = (String) sharedState.get(getPwdKey());
			ToopherUser user = toopherUserManager.getToopherUserByName(username);	
			ToopherUserTerminal terminal = getToopherUserTerminal(user);
			if (terminal == null) {
				debug.message("Error retrieving terminal ID.  Is toopher-openam.js included?");
				return STATE_ERROR;
			}
			debug.message("user name is " + user.getName());
			debug.message("user pairingId is " + user.getPairingId());
			debug.message("user requireToopherLogin is " + user.getRequireToopherLogin());

			debug.message("about to branch on state");
			switch (state) {
	
			case STATE_BEGIN:
				// determine if user is toopher-enabled at all.  If not, just skip toopher authentication
				if (user.getRequireToopherLogin() == null) {
					return STATE_TOOPHER_OPT_IN;
				} else if (user.getRequireToopherLogin() == false) {
					// user does't use toopher - let them in
					return ISAuthConstants.LOGIN_SUCCEED;
				} else {
					// do Toopher Auth
					if ((user.getPairingId() == null) || (user.getPairingId().equals(""))) {
						// need to pair
						return STATE_ENTER_PAIRING_PHRASE;
					} else {
						if (terminal.getFriendlyName() != null) {
							// user has already named terminal
							debug.message("user has already named terminal");
							Map<String,String> extras = new HashMap<String,String>();
							debug.message(String.format("terminal_name = %s, terminal_name_extra = %s", terminal.getFriendlyName(), terminal.getTerminalNameExtra()));
							extras.put("terminal_name_extra", terminal.getTerminalNameExtra());
							auth = api.authenticate(user.getPairingId(), terminal.getFriendlyName(), "Log in", extras);
							debug.message(String.format("terminal_id is %s", auth.terminalId));
							debug.message(String.format("auth_id is %s", auth.id));
							setStatusCookiePoll();
							return STATE_WAIT_FOR_AUTH;
						} else {
							debug.message("user needs to name terminal");
							return STATE_NAME_TERMINAL;
						}
					}
				}
			case STATE_ENTER_PAIRING_PHRASE:
				NameCallback nc = (NameCallback) callbacks[0];
				String pairingPhrase = nc.getName();
				PairingStatus pairing = api.pair(pairingPhrase, user.getName());
				user.setPairingId(pairing.id);
				toopherUserManager.save(user);
				setStatusCookiePoll();
				return STATE_WAIT_FOR_PAIRING;
			case STATE_WAIT_FOR_PAIRING:
				PairingStatus pairingStatus = api.getPairingStatus(user.getPairingId());
				if (pairingStatus.enabled) {
					return STATE_BEGIN;
				} else {
					setStatusCookiePoll();
					return STATE_WAIT_FOR_PAIRING;
				}
			case STATE_NAME_TERMINAL:
				NameCallback ncTerm = (NameCallback) callbacks[0];
				String terminalName = ncTerm.getName();
				terminal.setFriendlyName(terminalName);
				toopherUserManager.save(terminal);
				return STATE_BEGIN;
			case STATE_WAIT_FOR_AUTH:
				auth = api.getAuthenticationStatus(auth.id);
				if (auth.pending) {
					setStatusCookiePoll();
					return STATE_WAIT_FOR_AUTH;
				} else {
					if (auth.granted) {
						return ISAuthConstants.LOGIN_SUCCEED;
					} else {
						throw new AuthLoginException("Failed Toopher Authentication");
					}
				}

			case STATE_TOOPHER_OPT_IN:

				//ChoiceCallback optIn = (ChoiceCallback)callbacks[0];
				ConfirmationCallback optIn = (ConfirmationCallback)callbacks[0];
				debug.message("callback is " + optIn.toString());
				debug.message("callback selected index is " + optIn.getSelectedIndex());
				debug.message("callback default index is " + optIn.getDefaultOption());
				if (optIn.getSelectedIndex() == 0) { // TODO - get rid of this horrible hack
					// user wants to use Toopher
					user.setRequireToopherLogin(true);
					toopherUserManager.save(user);
				} else {
					// user doesn't want Toopher - make sure they're not bothered again
					user.setRequireToopherLogin(false);
					toopherUserManager.save(user);

					return ISAuthConstants.LOGIN_SUCCEED;
				}


			case STATE_ERROR:
				return STATE_ERROR;
			default:
				throw new AuthLoginException("invalid state " + state);

			}
		} catch (LoginException e) {
			throw e;
		} catch (Exception e) {
			debug.message("Error in ToopherSecondFactor::process : " + e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			debug.message(sw.toString());
			return STATE_ERROR;
		}
	}

	@Override
	public Principal getPrincipal() {
		return new ToopherSecondFactorPrincipal(USERNAME);
	}

}
