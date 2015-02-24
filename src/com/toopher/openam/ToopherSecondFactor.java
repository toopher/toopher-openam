// vim: sw=4:ts=4:cindent
package com.toopher.openam;

import java.math.BigInteger;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.HashMap;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.shared.datastruct.CollectionHelper;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

import com.toopher.*;

public class ToopherSecondFactor extends AMLoginModule {
    // Name for the debug-log
    private final static String DEBUG_NAME = "ToopherSecondFactor";
    //
    // Name of the resource-bundle
    private final static String amAuthToopherSecondFactor = "amAuthToopherSecondFactor";

    // orders defined in the callbacks file
    private final static int STATE_BEGIN = 1;
    private final static int STATE_SHOW_IFRAME = 2;
    private final static int STATE_ERROR = 3;

    private final static String JSP_IFRAME_TRIGGER_VAR = "toopherIframeEnabled";
    private final static String JSP_IFRAME_URL_VAR = "toopherIframeSrcUrl";
    private final static Debug debug = Debug.getInstance(DEBUG_NAME);

    private Pairing pairingStatus = null;
    private AuthenticationRequest authStatus = null;

    // Name for the debug-log
    protected final static String KEY_AUTHLEVEL = "iplanet-am-auth-ToopherSecondFactor-auth-level";
    protected final static String KEY_API_URL = "iplanet-am-auth-ToopherSecondFactor-apiUrl";
    protected final static String KEY_CONSUMER_KEY = "iplanet-am-auth-ToopherSecondFactor-consumerKey";
    protected final static String KEY_CONSUMER_SECRET = "iplanet-am-auth-ToopherSecondFactor-consumerSecret";
    protected final static String KEY_ALLOW_OPT_OUT = "iplanet-am-auth-ToopherSecondFactor-allowOptOut";
    protected final static String KEY_MAIL_ATTR = "iplanet-am-auth-ToopherSecondFactor-mailAttribute";

    private static final SecureRandom secureRandom = new SecureRandom();

    protected Map options;
    protected Map sharedState;
    protected ToopherIframe api;
    protected boolean allowOptOut;
    protected String userName;
    protected String userEmail;
    protected String requestToken;

    public ToopherSecondFactor() {
        super();
    }

    private void debug_message(String message) {
        if (debug.messageEnabled()) {
            debug.message(message);
        }
    }

    @Override
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

        api = new ToopherIframe(toopherConsumerKey, toopherConsumerSecret, toopherApiUrl);

        userName = (String) sharedState.get(getUserKey());
        requestToken = new BigInteger(20 * 8, secureRandom).toString(32);
    }

    private int ajaxPollingResponse(boolean poll) throws JSONException, IOException {
        HttpServletResponse response = getHttpServletResponse();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("poll", poll);
        JSONObject json = new JSONObject(params);
        json.write(response.getWriter());
        response.getWriter().flush();
        return 0;
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        try {
            HttpServletRequest request = getHttpServletRequest();
            ServletContext context = request.getServletContext();
            boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
            HttpServletResponse response = getHttpServletResponse();
            if (isAjax) {
                boolean keepPolling = false;
                if (pairingStatus != null) {
                    pairingStatus.refreshFromServer();
                    keepPolling = !pairingStatus.enabled;
                } else if (authStatus != null) {
                    authStatus.refreshFromServer();
                    keepPolling = authStatus.pending;
                }
                ajaxPollingResponse(keepPolling);
                return state;
            }

            switch (state) {

            case STATE_BEGIN:
                String iframeSrc = api.getAuthenticationUrl(userName, userEmail, requestToken);
                context.setAttribute(JSP_IFRAME_TRIGGER_VAR, true);
                context.setAttribute(JSP_IFRAME_URL_VAR, iframeSrc);
                return STATE_SHOW_IFRAME;
            case STATE_SHOW_IFRAME:



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
