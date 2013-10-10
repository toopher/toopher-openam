package com.forgerock.openam.examples;

import java.security.Principal;
import java.util.Map;
import java.util.ResourceBundle;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.toopher.apacheds.connect.UserManager;
import com.toopher.api.ToopherUseage;
import com.toopher.config.Config;

public class SampleAuthTwo extends AMLoginModule {
	// Name for the debug-log
	private final static String DEBUG_NAME = "SampleAuthTwo";
	// Name of the resource-bundle
	private final static String amAuthSampleAuthTwo = "amAuthSampleAuthTwo";
	// orders defined in the callbacks file
	private String USERNAME = "-";
	private final static int STATE_BEGIN = 1;
	private final static int STATE_AUTH = 2;
	private final static int STATE_ERROR = 3;

	private final static Debug debug = Debug.getInstance(DEBUG_NAME);

	private Map options;
	private ResourceBundle bundle;
	private Map sharedState;

	public SampleAuthTwo() {
		super();
	}

	@Override
	// In this method we store service attributes and localized properties
	// for later use
	public void init(Subject subject, Map sharedState, Map options) {
		if (debug.messageEnabled()) {
			debug.message("SampleAuthTwo::init");
		}
		if (isUseFirstPassEnabled()) {
			this.sharedState = sharedState;
		}
		this.options = options;
		bundle = amCache.getResBundle(amAuthSampleAuthTwo, getLoginLocale());
		ResourceBundle rb = amCache.getResBundle("toopherConfig",
				getLoginLocale());
		ToopherUseage.toopherPrintln(rb.getString("SECURITY_AUTHENTICATION"));
		Config.getConfigFile(rb);
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {
		if (debug.messageEnabled()) {
			debug.message("SampleAuthTwo::process state: " + state);
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
		JSONObject data = null;
		if (isUseFirstPassEnabled()) {
			try {
				String name = (String) sharedState.get(getUserKey());
				String pair = userManager.findPairingByUID(name);
				request.setAttribute("username", name);
				request.setAttribute("isSecond", true);
				request.setAttribute("OTPS", false);
				request.setAttribute("haspairingId", pair != null);
			} catch (Exception e) {
			}
		}
		switch (state) {

		case STATE_BEGIN:
			substituteUIStrings();
			return STATE_AUTH;

		case STATE_AUTH:
			// NameCallback nc = (NameCallback) callbacks[0];
			String username = (String) sharedState.get(getUserKey());
			// PasswordCallback pc = (PasswordCallback) callbacks[1];
			String password = request.getParameter("IDToken2");
			ToopherUseage.toopherPrintln(password);
			try {
				if (username != null) {
					String pairingId = userManager.findPairingByUID(username);
					ToopherUseage toopherUseage = new ToopherUseage();
					if (pairingId == null) {
						ToopherUseage.toopherPrintln(password);
						data = toopherUseage.toopherPairingVerification(
								username, password);
						if ((Boolean) data.get("flag")) {
							request.setAttribute("haspairing", userManager
									.addOrUpdatePairing(username,
											(String) data.get("pairingId")));
							throw new InvalidPasswordException(
									"Device is paired",
									Config.resourceConf
											.getString("DEFAULT_TERMINAL"));
						}
					} else {
						String _terminal = request.getParameter("terminalId");
						ToopherUseage.toopherPrintln(_terminal);
						String terminal = userManager.findTerminalByUID(
								username, _terminal);
						if (terminal != null)
							password = terminal;
						if (password == null || password.trim() == "")
							password = Config.resourceConf
									.getString("DEFAULT_TERMINAL");
						if (_terminal.equals("OTP-RE")) {
							data = toopherUseage.toopherUserAuth(_terminal,
									_terminal,
									userManager.findPairingByUID(username),
									password);
						} else {
							data = toopherUseage.toopherUserAuth(password,
									_terminal,
									userManager.findPairingByUID(username),
									null);
							if (!_terminal.equals("OTP-RE")
									&& !(Boolean) data.get("flag")
									&& !(Boolean) data.get("deniedByUser")) {
								request.setAttribute("OTPS", true);
								substituteUIStrings();
								return STATE_AUTH;
							}
						}
						if (!_terminal.equals("OTP-RE") && terminal == null
								&& (Boolean) data.get("flag")) {
							userManager.addTerminalName(username, _terminal,
									password);
						}
					}
					if (data != null && (Boolean) data.get("flag")) {
						return ISAuthConstants.LOGIN_SUCCEED;
					} else
						System.err.println("Not Done");
				}
				throw new InvalidPasswordException("Some Error occurred",
						USERNAME);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		case STATE_ERROR:
			return STATE_ERROR;
		default:
			throw new AuthLoginException("invalid state");

		}
	}

	@Override
	public Principal getPrincipal() {
		return new SampleAuthPrincipalTwo(USERNAME);
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
				"SampleAuthTwo-service-specific-attribute");

		// Get property from bundle
		String new_hdr = ssa + " "
				+ bundle.getString("SampleAuthTwo-ui-login-header");
		// substituteHeader(STATE_AUTH, new_hdr);
		System.err.println("C-" + STATE_AUTH + " bundle:-" + new_hdr);
		Callback[] cbs_phone = getCallback(STATE_AUTH);

		replaceCallback(
				STATE_AUTH,
				0,
				new NameCallback(bundle
						.getString("SampleAuthTwo-ui-username-prompt")));

	}

}