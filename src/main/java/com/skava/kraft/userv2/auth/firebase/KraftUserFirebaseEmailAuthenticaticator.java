package com.skava.kraft.userv2.auth.firebase;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.skava.dao.UserV2DAO;
import com.skava.db.DBSession;
import com.skava.model.Response;
import com.skava.interfaces.StreamUserV2Authenticator;
import com.skava.model.userv2.UserPropertiesV2;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.Partner;
import com.skava.model.userv2.ComUserIdentity;
import com.skava.model.userv2.ComUserProperties;
import com.skava.model.userv2.ComUserResponse;
import com.skava.model.userv2.FirebaseGetAccountInfoResponse;
import com.skava.model.userv2.FirebaseUserLoginResponse;
import com.skava.model.userv2.UserDescriptorIdentityV2;
import com.skava.model.userv2.UserIdentityV2;
import com.skava.model.userv2.UserProperties;
import com.skava.model.userv2.UserV2;
import com.skava.services.HttpClientService;
import com.skava.userv2.utils.Userv2Util;
import com.skava.util.AWSUtil;
import com.skava.util.CryptoUtil;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.helpers.MimeMap;

public class KraftUserFirebaseEmailAuthenticaticator implements StreamUserV2Authenticator {

	private SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());
	private String STATUS_ACTIVE = "0";
	private String STATUS_INACTIVE = "1";
	
	
	@Override
	public String authenticate(DBSession session, HttpServletRequest request, HttpServletResponse response,
			Partner partner, Campaign campaign, UserDescriptorIdentityV2 identity, HashMap<String, String> userMap,
			UserProperties userProperties, HttpClientService httpClientService, AWSUtil awsUtil, MimeMap mimeMap,
			CryptoUtil cryptoUtil, Locale localeObj) throws ServerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String authenticate(DBSession session, HttpServletRequest request, HttpServletResponse response,
			Partner partner, Campaign campaign, ComUserIdentity identity, HashMap<String, String> userMap,
			ComUserProperties userProperties, HttpClientService httpClientService, AWSUtil awsUtil, MimeMap mimeMap,
			CryptoUtil cryptoUtil, Locale localeObj)
			throws ServerException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        String skavaUserId = null;
        String userEmailId = null;
        String authToken = null;
        String userName = null;
        String userPassword = null;
        String userUid = null;
        UserIdentityV2 userIdentity = null;
        
        String identityValue = ReadUtil.getString(identity.getValue(), partner.getName());
        
        if((userProperties.getUserEmail() != null && !userProperties.getUserEmail().isEmpty()) || (userProperties.getUniqueUserName() != null && !userProperties.getUniqueUserName().isEmpty()) || (userProperties.getUserPassword() != null && !userProperties.getUserPassword().isEmpty())) {
        	
        	if(userProperties.getUserPassword() != null && !userProperties.getUserPassword().isEmpty()) {
        		userPassword = userProperties.getUserPassword();
        	} else {
        		throw new ServerException(ComUserResponse.RESP_INVALID_PASSWORD);
        	}
        
	        if(userProperties.getUserEmail() != null) {
	        	
	        	if(!userProperties.getUserEmail().isEmpty()) {
	        		userEmailId = userProperties.getUserEmail().toLowerCase(); 
	        	} else {
	        		throw new ServerException(ComUserResponse.RESP_INVALID_EMAIL_ID);
	        	}
	        	
	        } else if(userProperties.getUniqueUserName() != null) {
	        	
	        	if(!userProperties.getUniqueUserName().isEmpty()) {
	        		userName = userProperties.getUniqueUserName();
	        	} else {
	        		throw new ServerException(ComUserResponse.RESP_INVALID_USERUNIQUENAME);
	        	}
	        	
	        	userIdentity = Userv2Util.loadIdentityByTypeValusUserIdAndCampaignId(session, cryptoUtil, identity.getType(), identityValue, userName, campaign.getId());
	        	
	        	if(userIdentity == null) {
	        		throw new ServerException(ComUserResponse.RESP_INVALID_USER);
	        	}
	        	
	            UserV2 userByName = Userv2Util.loadUserv2(session, cryptoUtil, userIdentity.getSkavaUserId(), partner.getId(), campaign.getId(), false);
	            
	            if(userByName == null) {
	            	throw new ServerException(ComUserResponse.RESP_INVALID_USER);
	            }
	            
	            for(UserPropertiesV2 userPropertiesV2 : userByName.getProperties()) {
	            	if((UserPropertiesV2.PROP_USER_EMAIL).equals(userPropertiesV2.getName())) {
	            		userEmailId = userPropertiesV2.getValue().toLowerCase();
	            		break;
	            	}
	            }
	        }
	        
	        userIdentity = Userv2Util.loadIdentityByTypeValusUserIdAndCampaignId(session, cryptoUtil, identity.getType(), identityValue, userEmailId, campaign.getId());
	        
	        if(userIdentity == null) {
        		throw new ServerException(ComUserResponse.RESP_INVALID_USER);
        	}
	        
	        FirebaseUserLoginResponse firebaseUserLoginResponse = KraftFirebaseUtil.getUserLoginInfo(request, httpClientService, userEmailId, userPassword);
	        
	        if(firebaseUserLoginResponse == null || firebaseUserLoginResponse.getResponseCode() != Response.RESPONSE_SUCCESS) {
	        	throw new ServerException(ComUserResponse.RESP_ERROR_FIREBASE_AUTHENTICATION);
	        } else {
	        
	        	userUid = firebaseUserLoginResponse.getLocalId();
	        }
        }	
        else {
        	
        	if(userProperties.getAuthToken() != null && !userProperties.getAuthToken().isEmpty()) {
        		authToken = userProperties.getAuthToken();
        		
        	} else {
        		throw new ServerException(ComUserResponse.RESP_INVALID_AUTHTOKEN);
        	}
        	
        	FirebaseGetAccountInfoResponse firebaseGetAccountInfoResponse = KraftFirebaseUtil.getAccountInfo(request, httpClientService, authToken);
        	
        	if(firebaseGetAccountInfoResponse == null || firebaseGetAccountInfoResponse.getResponseCode() != Response.RESPONSE_SUCCESS) {
	        	throw new ServerException(ComUserResponse.RESP_ERROR_FIREBASE_AUTHENTICATION);
	        } else {
	        	userUid = firebaseGetAccountInfoResponse.getUsers()[0].getLocalId();
	        }
        	
        	userIdentity = Userv2Util.loadIdentityByTypeValusUserIdAndCampaignId(session, cryptoUtil, identity.getType(), identityValue, userUid, campaign.getId());
        	
        	if(userIdentity == null) {
        		throw new ServerException(ComUserResponse.RESP_INVALID_USER);
        	}
	    }
            
        if (userUid != null)
        {
            if (userIdentity != null && userIdentity.getId() > 0)
            {
                skavaUserId = String.valueOf(userIdentity.getSkavaUserId()) + "~" + RET_PARAM_LOGIN;
                UserV2 user = (new UserV2DAO()).loadUserV2ById(session, userIdentity.getSkavaUserId(), partner.getId(), campaign.getId());
                if (user != null && user.getId() > 0)
                {
                    if (user.getStatus() == UserV2.STATUS_IN_ACTIVE) { throw new ServerException(ComUserResponse.RESP_LOGIN_INACTIVE_USER); }
                }
                else
                {
                    throw new ServerException(ComUserResponse.RESP_INVALID_USER);
                }
            }
        }
        else
        {
            throw new ServerException(ComUserResponse.RESP_INVALID_PROFILE_ID);
        }
        return skavaUserId;
	}
}
