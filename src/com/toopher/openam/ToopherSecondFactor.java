package com.toopher.openam;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.HashMap;
import java.io.StringWriter;
import java.io.PrintWriter;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.shared.datastruct.CollectionHelper;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

import com.toopher.*;

public class ToopherSecondFactor extends AMLoginModule {
    // Name for the debug-log
    private final static String DEBUG_NAME = "ToopherSecondFactor";

    // orders defined in the callbacks file
    private final static int STATE_BEGIN = 1;
    private final static int STATE_SHOW_IFRAME = 2;
    private final static int STATE_ERROR = 3;

    private final static String JSP_IFRAME_URL_VAR = "toopherIframeSrcUrl";
    private final static Debug debug = Debug.getInstance(DEBUG_NAME);

    protected final static String KEY_AUTHLEVEL = "iplanet-am-auth-ToopherSecondFactor-auth-level";
    protected final static String KEY_API_URL = "iplanet-am-auth-ToopherSecondFactor-apiUrl";
    protected final static String KEY_CONSUMER_KEY = "iplanet-am-auth-ToopherSecondFactor-consumerKey";
    protected final static String KEY_CONSUMER_SECRET = "iplanet-am-auth-ToopherSecondFactor-consumerSecret";
    protected final static String KEY_MAIL_ATTR = "iplanet-am-auth-ToopherSecondFactor-mailAttribute";

    private static final SecureRandom secureRandom = new SecureRandom();

    protected Map options;
    protected Map sharedState;
    protected ToopherIframe api;
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

        this.options = options;

        String authLevel = CollectionHelper.getMapAttr(options, KEY_AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("ToopherSecondFactor.init() : " + "Unable to set auth level " + authLevel, e);
            }
        }

        String toopherConsumerKey = CollectionHelper.getMapAttr(options, KEY_CONSUMER_KEY);
        String toopherConsumerSecret = CollectionHelper.getMapAttr(options, KEY_CONSUMER_SECRET);
        String toopherApiUrl = CollectionHelper.getMapAttr(options, KEY_API_URL);

        api = new ToopherIframe(toopherConsumerKey, toopherConsumerSecret, toopherApiUrl);

        userName = (String) sharedState.get(getUserKey());

        String userEmailAttr = CollectionHelper.getMapAttr(options, KEY_MAIL_ATTR);
        // TODO: figure out how to get the user's mail attribute into userEmail

        requestToken = new BigInteger(20 * 8, secureRandom).toString(32);
    }

    private Map<String, String[]> decodeUrlEncodedDict(String urlEncodedDict) throws Exception {
        String[] iframeDataStrs = urlEncodedDict.split("&");
        Map<String, String[]> params = new HashMap<String, String[]>(iframeDataStrs.length);
        for (String iframeDataStr : iframeDataStrs) {
            String[] iframeDataKeyValue = iframeDataStr.split("=");

            if (iframeDataKeyValue.length != 2) {
                throw new Exception("Invalid iframeDataStr: " + iframeDataStr);
            }
            String key = iframeDataKeyValue[0];
            String value = URLDecoder.decode(iframeDataKeyValue[1], "UTF-8");
            params.put(key, new String[]{ value });
        }
        return params;
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        try {
            debug_message("ToopherSecondFactor: state = " + Integer.toString(state));
            HttpServletRequest request = getHttpServletRequest();

            switch (state) {
            case STATE_BEGIN:
                String iframeSrc = api.getAuthenticationUrl(userName, "", requestToken);
                request.setAttribute(JSP_IFRAME_URL_VAR, iframeSrc);
                return STATE_SHOW_IFRAME;
            case STATE_SHOW_IFRAME:
                NameCallback signatureCallback = (NameCallback) callbacks[0];
                String iframeData = signatureCallback.getName();
                debug_message("Toopher Signature: >" + iframeData + "<");
                Map<String, String> validatedParams = api.validatePostback(decodeUrlEncodedDict(iframeData), requestToken);
                debug_message("Toopher Iframe signature is valid");
                if (validatedParams.get("granted").equals("true")) {
                    debug_message("Toopher authentication granted");
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else {
                    debug_message("Toopher authentication denied");
                    throw new AuthLoginException("Failed Toopher Authentication");
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
