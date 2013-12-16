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

import com.toopher.*;

public abstract class ToopherSecondFactorBase extends AMLoginModule {
    // Name for the debug-log
    private final static String DEBUG_NAME = "ToopherSecondFactorBase";
    protected final static String KEY_AUTHLEVEL = "iplanet-am-auth-ToopherSecondFactor-auth-level";
    protected final static String KEY_API_URL = "iplanet-am-auth-ToopherSecondFactor-apiUrl";
    protected final static String KEY_CONSUMER_KEY = "iplanet-am-auth-ToopherSecondFactor-consumerKey";
    protected final static String KEY_CONSUMER_SECRET = "iplanet-am-auth-ToopherSecondFactor-consumerSecret";
    protected final static String KEY_ALLOW_OPT_OUT = "iplanet-am-auth-ToopherSecondFactor-allowOptOut";
    protected final static String KEY_MAIL_ATTR = "iplanet-am-auth-ToopherSecondFactor-mailAttribute";

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String TERMINAL_ID_COOKIE_NAME = "toopher_terminal_id";
    private static final int TERMINAL_ID_COOKIE_MAX_AGE = 10 * 365 * 24 * 60 * 60;  // 10 years
    
    private final static Debug debug = Debug.getInstance(DEBUG_NAME);

    protected Map options;
    protected Map sharedState;
    protected ToopherAPI api;
    protected String terminalIdentifier;
    protected boolean allowOptOut;
    protected String userName;

    public ToopherSecondFactorBase() {
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
        if (isUseFirstPassEnabled()) {
            this.sharedState = sharedState;
        }
        if (sharedState == null) {
            debug_message("sharedState is null!");
        }
        if (options == null) {
            debug_message("options is null");
        }
        this.options = options;

        String authLevel = CollectionHelper.getMapAttr(options, KEY_AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("ToopherSecondFactor.init() : " + "Unable to set auth level " + authLevel, e);
            }
        }

        allowOptOut = CollectionHelper.getMapAttr(options, KEY_ALLOW_OPT_OUT).compareToIgnoreCase("true") == 0;

        String toopherConsumerKey = CollectionHelper.getMapAttr(options, KEY_CONSUMER_KEY);
        String toopherConsumerSecret = CollectionHelper.getMapAttr(options, KEY_CONSUMER_SECRET);
        String toopherApiUrl = CollectionHelper.getMapAttr(options, KEY_API_URL);

        try {
            api = new ToopherAPI(toopherConsumerKey, toopherConsumerSecret, toopherApiUrl);
        } catch (URISyntaxException e) {
            debug.error("Error parsing Toopher API URL: " + e.getMessage());
        }

        clearStatusCookie();
        terminalIdentifier = getTerminalIdentifier();

        userName = (String) sharedState.get(getUserKey());
    }

    private String getTerminalIdentifier() {
        HttpServletRequest request = getHttpServletRequest();
        for (Cookie cookie : request.getCookies()){
            if(cookie.getName().equals(TERMINAL_ID_COOKIE_NAME)) {
                // terminal identifier has already been set
                return cookie.getValue();
            }
        }

        // no cookie has been set yet - set one on the response
        String terminalIdentifier = new BigInteger(20 * 8, secureRandom).toString(32);
        HttpServletResponse response = getHttpServletResponse();
        Cookie terminalCookie = new Cookie(TERMINAL_ID_COOKIE_NAME, terminalIdentifier);
        terminalCookie.setMaxAge(TERMINAL_ID_COOKIE_MAX_AGE);
        terminalCookie.setPath("/");
        terminalCookie.setSecure(true);
        response.addCookie(terminalCookie);
        debug_message("Added Toopher TerminalIdentifier cookie: " + terminalIdentifier);
        return terminalIdentifier;
    }
    

    protected void clearStatusCookie() {
        getHttpServletResponse().addCookie(new Cookie("toopher_auth_status", "n/a"));
    }
    protected void setStatusCookiePoll() {
        getHttpServletResponse().addCookie(new Cookie("toopher_auth_status", "poll"));
    }

    @Override
    public Principal getPrincipal() {
        return new ToopherSecondFactorPrincipal(userName);
    }

}
