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
import com.toopher.*;

public class ToopherSecondFactor extends ToopherSecondFactorBase {
    // Name for the debug-log
    private final static String DEBUG_NAME = "ToopherSecondFactor";
    //
    // Name of the resource-bundle
    private final static String amAuthToopherSecondFactor = "amAuthToopherSecondFactor";
    
    // orders defined in the callbacks file
    private final static int STATE_BEGIN = 1;
    private final static int STATE_ENTER_PAIRING_PHRASE = 2;
    private final static int STATE_WAIT_FOR_PAIRING = 3;
    private final static int STATE_NAME_TERMINAL = 4;
    private final static int STATE_WAIT_FOR_AUTH = 5;
    private final static int STATE_ENTER_OTP = 6;
    private final static int STATE_NOTIFY_PAIRING_DEACTIVATED = 7;
    private final static int STATE_TOOPHER_OPT_IN = 8;
    private final static int STATE_ERROR = 9;

    private final static Debug debug = Debug.getInstance(DEBUG_NAME);

    private PairingStatus pairingStatus = null;
    private AuthenticationStatus authStatus = null;

    public ToopherSecondFactor() {
        super();
    }

    private void debug_message(String message){
        if (debug.messageEnabled()) {
            debug.message(message);
        }
    }

    @Override
    // In this method we store service attributes and localized properties
    // for later use
    public void init(Subject subject, Map sharedState, Map options) {
        super.init(subject, sharedState, options);
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        try {
            debug_message("====================================");
            debug_message("process state: " + state);

            HttpServletRequest request = getHttpServletRequest();
            HttpServletResponse response = getHttpServletResponse();

            switch (state) {
    
            case STATE_BEGIN:
                try {
                    authStatus = api.authenticateByUserName(userName, terminalIdentifier, "Log in", null);
                    setStatusCookiePoll();
                    return STATE_WAIT_FOR_AUTH;
                } catch (ToopherUnknownUserError e) {
                    debug_message("ToopherUnknownUserError");
                    if (allowOptOut) {
                        return STATE_TOOPHER_OPT_IN;
                    } else {
                        // need to pair
                        return STATE_ENTER_PAIRING_PHRASE;
                    }
                } catch (ToopherUnknownTerminalError e) {
                    debug_message("ToopherUnknownTerminalError");
                    // user needs to name terminal
                    return STATE_NAME_TERMINAL;
                } catch (ToopherUserDisabledError e) {
                    debug_message("ToopherUserDisabledError");
                    // user does't use toopher - let them in
                    return ISAuthConstants.LOGIN_SUCCEED;
                } catch (RequestError e) {
                    String err = e.getMessage();
                    debug_message("caught unknown request error: " + err);
                    if (err.toLowerCase().contains("pairing has been deactivated")) {
                        debug_message("User has deactivated pairing");
                        return STATE_NOTIFY_PAIRING_DEACTIVATED;
                    } else if (err.toLowerCase().contains("pairing has not been authorized to authenticate")) {
                        debug_message("Pairing is not authorized");
                        return STATE_NOTIFY_PAIRING_DEACTIVATED;
                    }
                    // wasn't handleable - re-throw
                    throw e;
                }
            case STATE_ENTER_PAIRING_PHRASE:
                NameCallback nc = (NameCallback) callbacks[0];
                String pairingPhrase = nc.getName();
                pairingStatus = api.pair(pairingPhrase, userName);
                debug_message("Created new pairing: " + pairingStatus.id);
                setStatusCookiePoll();
                return STATE_WAIT_FOR_PAIRING;
            case STATE_WAIT_FOR_PAIRING:
                pairingStatus = api.getPairingStatus(pairingStatus.id);
                if (pairingStatus.enabled) {
                    return STATE_BEGIN;
                } else {
                    setStatusCookiePoll();
                    return STATE_WAIT_FOR_PAIRING;
                }
            case STATE_NAME_TERMINAL:
                NameCallback ncTerm = (NameCallback) callbacks[0];
                String terminalName = ncTerm.getName();
                try {
                    api.assignUserFriendlyNameToTerminal(userName, terminalName, terminalIdentifier);
                } catch (RequestError e) {
                    debug.error("unable to name terminal: " + e.getMessage());
                }
                return STATE_BEGIN;
            case STATE_WAIT_FOR_AUTH:
                String submitText = request.getParameter("IDButton");
                if (!submitText.toLowerCase().equals("poll")){
                    return STATE_ENTER_OTP;
                }
                authStatus = api.getAuthenticationStatus(authStatus.id);
                if (authStatus.pending) {
                    setStatusCookiePoll();
                    return STATE_WAIT_FOR_AUTH;
                } else {
                    if (authStatus.granted) {
                        return ISAuthConstants.LOGIN_SUCCEED;
                    } else {
                        throw new AuthLoginException("Failed Toopher Authentication");
                    }
                }

            case STATE_ENTER_OTP:
                NameCallback ncOtp = (NameCallback) callbacks[0];
                String otp = ncOtp.getName();
                authStatus = api.getAuthenticationStatusWithOTP(authStatus.id, otp);
                if ((!authStatus.pending) && (authStatus.granted)){
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else {
                    throw new AuthLoginException("Invalid OTP");
                }


            case STATE_NOTIFY_PAIRING_DEACTIVATED:
                ConfirmationCallback ccRepair = (ConfirmationCallback)callbacks[0];
                if (ccRepair.getSelectedIndex() == 0) { // TODO - get rid of this horrible hack
                    return STATE_ENTER_PAIRING_PHRASE;
                } else {
                    // user doesn't want Toopher - make sure they're not bothered again
                    api.setToopherEnabledForUser(userName, false);
                    return ISAuthConstants.LOGIN_SUCCEED;
                }
            case STATE_TOOPHER_OPT_IN:
                ConfirmationCallback optIn = (ConfirmationCallback)callbacks[0];
                if (optIn.getSelectedIndex() == 0) { // TODO - get rid of this horrible hack
                    // user wants to use Toopher
                    return STATE_ENTER_PAIRING_PHRASE;
                } else {
                    // user doesn't want Toopher - make sure they're not bothered again
                    api.setToopherEnabledForUser(userName, false);
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
            debug_message("Error in ToopherSecondFactor::process : " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            debug_message(sw.toString());
            return STATE_ERROR;
        }
    }

    @Override
    public Principal getPrincipal() {
        return new ToopherSecondFactorPrincipal(userName);
    }

}
