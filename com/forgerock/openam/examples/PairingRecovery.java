package com.forgerock.openam.examples;

import java.security.Principal;
import java.util.Map;
import java.util.ResourceBundle;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.toopher.apacheds.connect.UserManager;
import com.toopher.api.ToopherUseage;
import com.toopher.config.Config;

public class PairingRecovery extends AMLoginModule {
	// Name for the debug-log
	private final static String DEBUG_NAME = "PairingRecovery";
	// Name of the resource-bundle
	private final static String amAuthPairingRecovery = "amAuthPairingRecovery";
	// orders defined in the callbacks file
	private String USERNAME = "-";
	private final static int STATE_BEGIN = 1;
	private final static int STATE_AUTH = 2;
	private final static int STATE_ERROR = 3;

	private final static Debug debug = Debug.getInstance(DEBUG_NAME);

	private Map options;
	private ResourceBundle bundle;
	private Map sharedState;

	public PairingRecovery() {
		super();
	}

	@Override
	// In this method we store service attributes and localized properties
	// for later use
	public void init(Subject subject, Map sharedState, Map options) {
		if (debug.messageEnabled()) {
			debug.message("PairingRecovery::init");
		}
		if (isUseFirstPassEnabled()) {
			this.sharedState = sharedState;
		}
		this.options = options;
		ToopherUseage.toopherPrintln("-_-");
		bundle = amCache.getResBundle(amAuthPairingRecovery, getLoginLocale());
		ResourceBundle rb = amCache.getResBundle("toopherConfig",
				getLoginLocale());
		ToopherUseage.toopherPrintln("-----"+rb.getString("SECURITY_AUTHENTICATION"));
		Config.getConfigFile(rb);
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {
		if (debug.messageEnabled()) {
			debug.message("PairingRecovery::process state: " + state);
		}
		USERNAME = (String) sharedState.get(getUserKey());
		System.out.println("====================================");
		HttpServletRequest request = getHttpServletRequest();
		if (getHttpServletRequest() != null) {
			ToopherUseage.toopherPrintln("getHttpServletRequest"
					+ getHttpServletRequest());
		} else {
			ToopherUseage.toopherPrintln("getHttpServletRequest is null");
		}
		UserManager userManager = new UserManager();
		if (isUseFirstPassEnabled() ) {
			try {
				String name = (String) sharedState.get(getUserKey());
				if(userManager.findPairingByUID(name)!=null){
				request.setAttribute("username", name);
				request.setAttribute("isSecond", true);
				request.setAttribute("OTPS", false);
				request.setAttribute("isRecovery", false);
				request.setAttribute("question", userManager.findPairingRecoveryQuestion(name));
				request.setAttribute("haspairingId", true);
				}else{
					throw new InvalidPasswordException("No pairing for the user ", USERNAME);
				}
			} catch (Exception e) {
			}
		}
		switch (state) {

		case STATE_BEGIN:
			substituteUIStrings();
			return STATE_AUTH;

		case STATE_AUTH:
			String username = (String) sharedState.get(getUserKey());
			String password = request.getParameter("IDToken2");
			ToopherUseage.toopherPrintln(password);
			try {
				if (username != null) {
					String myAnswer=password;
					ToopherUseage.toopherPrintln(password);
					if(userManager.recoverPairing(username, myAnswer)){
						request.setAttribute("removepairing", true) ;
						throw new AuthLoginException( "Pairing removed");
					}
				}
				throw new InvalidPasswordException("Some Error occurred", USERNAME);
			} catch (Exception e) {
				e.printStackTrace();
				throw new InvalidPasswordException("Some Error occurred", USERNAME);
			}
		case STATE_ERROR:
			return STATE_ERROR;
		default:
			throw new AuthLoginException("invalid state");

		}
	}

	@Override
	public Principal getPrincipal() {
		return new PairingRecoveryPrincipal(USERNAME);
	}

	private void setErrorText(String err) throws AuthLoginException {
		// Receive correct string from properties and substitute the
		// header in callbacks order 3.
		// substituteHeader(STATE_ERROR, bundle.getString(err));
		System.err.println("C-" + STATE_ERROR + " bundle:-"
				+ bundle.getString(err));
	}

	private void substituteUIStrings() throws AuthLoginException {
		// Get service specific attribute configured in OpenAM
		String ssa = CollectionHelper.getMapAttr(options,
				"PairingRecovery-service-specific-attribute");

		// Get property from bundle
		String new_hdr = ssa + " "
				+ bundle.getString("PairingRecovery-ui-login-header");
		// substituteHeader(STATE_AUTH, new_hdr);
		System.err.println("C-" + STATE_AUTH + " bundle:-" + new_hdr);
		Callback[] cbs_phone = getCallback(STATE_AUTH);

		replaceCallback(
				STATE_AUTH,
				0,
				new NameCallback(bundle
						.getString("PairingRecovery-ui-username-prompt")));

	}

}