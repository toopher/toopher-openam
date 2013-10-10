package com.forgerock.openam.examples;

import java.security.Principal;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.toopher.apacheds.connect.UserManager;
import com.toopher.api.ToopherUseage;
import com.toopher.config.Config;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class SampleAuth extends AMLoginModule {

	// Name for the debug-log
	private final static String DEBUG_NAME = "SampleAuth";

	// Name of the resource bundle
	private final static String amAuthSampleAuth = "amAuthSampleAuth";

	// User names for authentication logic
	private String USERNAME = "_";
	private final static String ERROR_1_NAME = "test1";
	private final static String ERROR_2_NAME = "test2";

	// Orders defined in the callbacks file
	private final static int STATE_BEGIN = 1;
	private final static int STATE_AUTH = 2;
	private final static int STATE_ERROR = 3;

	private final static Debug debug = Debug.getInstance(DEBUG_NAME);

	private Map options;
	private ResourceBundle bundle;

	public SampleAuth() {
		super();
	}

	@Override
	// This method stores service attributes and localized properties
	// for later use.
	public void init(Subject subject, Map sharedState, Map options) {
		if (debug.messageEnabled()) {
			debug.message("SampleAuth::init");
		}
		this.options = options;
		bundle = amCache.getResBundle(amAuthSampleAuth, getLoginLocale());
		ResourceBundle rb = amCache.getResBundle("toopherConfig",
				getLoginLocale());
		ToopherUseage.toopherPrintln(rb.getString("SECURITY_AUTHENTICATION"));
		Config.getConfigFile(rb);
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {

		if (debug.messageEnabled()) {
			debug.message("SampleAuth::process state: " + state);
		}

		switch (state) {

		case STATE_BEGIN:
			// No time wasted here - simply modify the UI and
			// proceed to next state
			substituteUIStrings();
			return STATE_AUTH;

		case STATE_AUTH:
			// Get data from callbacks. Refer to callbacks XML file.
			NameCallback nc = (NameCallback) callbacks[0];
			PasswordCallback pc = (PasswordCallback) callbacks[1];
			String username = nc.getName();
			String password = new String(pc.getPassword());
			USERNAME=username;
			if (new UserManager().validateUser(username, password)) {
				storeUsernamePasswd(username, password);
				return ISAuthConstants.LOGIN_SUCCEED;
			}
			throw new InvalidPasswordException("password is wrong", USERNAME);

		case STATE_ERROR:
			return STATE_ERROR;
		default:
			throw new AuthLoginException("invalid state");

		}
	}

	@Override
	public Principal getPrincipal() {
		return new SampleAuthPrincipal(USERNAME);
	}

	private void setErrorText(String err) throws AuthLoginException {
		// Receive correct string from properties and substitute the
		// header in callbacks order 3.
		// substituteHeader(STATE_ERROR, bundle.getString(err));
	}

	private void substituteUIStrings() throws AuthLoginException {
		// Get service specific attribute configured in OpenAM
		String ssa = CollectionHelper.getMapAttr(options,
				"sampleauth-service-specific-attribute");

		// Get property from bundle
		String new_hdr = ssa + " "
				+ bundle.getString("sampleauth-ui-login-header");
		// substituteHeader(STATE_AUTH, new_hdr);

		replaceCallback(
				STATE_AUTH,
				0,
				new NameCallback(bundle
						.getString("sampleauth-ui-username-prompt")));

		replaceCallback(
				STATE_AUTH,
				1,
				new PasswordCallback(bundle
						.getString("sampleauth-ui-password-prompt"), false));
	}

}