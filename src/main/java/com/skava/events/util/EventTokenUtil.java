package com.skava.events.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skava.events.constants.ConstantValues;
import com.skava.events.http.HttpClientService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class EventTokenUtil {
    private static final Logger LOG = LoggerFactory.getLogger(EventTokenUtil.class);

    public static String getServiceToken() throws JSONException, UnsupportedEncodingException, ParseException {
        String storeKey, encryptedKey, serviceToken;

        storeKey = getStoreKeyCall();
        encryptedKey = getEncryptedTokenCall(storeKey);
        serviceToken = generateServiceTokenCall(encryptedKey);
        return URLEncoder.encode(serviceToken, "UTF-8");
    }

    private static String getStoreKeyCall() throws JSONException, ParseException {
        String getKeyResp;
        JSONObject strResp1;

        getKeyResp = HttpClientService
                .makeHttpPostRequest(
                        "https://" + ConstantValues.ADMIN_DOMAIN
                                + "/apiadmin/remote/skavakeystore/getKeyForToken?alias=" + ConstantValues.REQUESTOR,
                        "POST", null, null, false);
        strResp1 = new JSONObject(getKeyResp);
        return strResp1.getString("key");
    }

    private static String getEncryptedTokenCall(String key) {
        long curTime;
        List requestedServices;
        String encryptedToken;

        curTime = System.currentTimeMillis();
        requestedServices = new ArrayList();
        requestedServices.add("pimadmin");
        encryptedToken = Jwts.builder().claim("requestedServices", requestedServices).setAudience("mcadmin")
                .setExpiration(new Date(curTime + 60000)).setIssuedAt(new Date(curTime)).setIssuer("SkavaAPIAdmin")
                .signWith(SignatureAlgorithm.HS512, key).compact();
        return encryptedToken;
    }

    private static String generateServiceTokenCall(String token) throws JSONException, ParseException {
        String gettokenResp = HttpClientService.makeHttpPostRequest("https://" + ConstantValues.ADMIN_DOMAIN
                + "/apiadmin/v1/token/create?locale=en_US&token=" + token + "&requestor=" + ConstantValues.REQUESTOR,
                "POST", null, null, false);
        JSONObject respObj = new JSONObject(gettokenResp);
        LOG.debug("Service token Resp :: {}", gettokenResp);
        JSONObject tokensObj = respObj.getJSONObject("serviceTokens");
        String serviceToken = tokensObj.getString("pimadmin");
        return serviceToken;
    }

    public static String getAccessToken(String username, String pwd)
            throws JSONException, UnsupportedEncodingException, ParseException {
        String getKeyResp;
        JSONObject strResp1, adminUserObj;
        String accessToken;

        getKeyResp = HttpClientService.makeHttpPostRequest("https://" + ConstantValues.DOMAIN
                + "/corporateadmin/v1/adminuser/login?email=" + username + "&password=" + pwd, "POST", null, null,
                false);
        strResp1 = new JSONObject(getKeyResp);
        adminUserObj = strResp1.getJSONObject("adminUser");
        LOG.debug("Access token Resp :: {}", adminUserObj.getString("authTokenValue"));
        accessToken = adminUserObj.getString("authTokenValue");
        return URLEncoder.encode(accessToken, "UTF-8");
    }
}
