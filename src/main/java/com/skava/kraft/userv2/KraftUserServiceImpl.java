package com.skava.kraft.userv2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.lang.StringBuilder;
import java.util.regex.Pattern;
import java.awt.Color;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.LocaleUtils;
import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.cache.MemCacheManager;
import com.skava.db.DBSessionManager;
import com.skava.interfaces.SkavaCaptchaFactory;
import com.skava.kraft.userv2.auth.firebase.KraftFirebaseUtil;
import com.skava.model.Response;
import com.skava.model.userv2.ComUser;
import com.skava.model.userv2.ComUserIdentity;
import com.skava.model.userv2.ComUserProperties;
import com.skava.model.userv2.ComUserFindResponse;
import com.skava.model.userv2.ComUserResponse;
import com.skava.model.userv2.FirebaseGetAccountInfoResponse;
import com.skava.model.userv2.KraftUserLoginResponse;
import com.skava.model.userv2.KraftUserSearchResponse;
import com.skava.model.userv2.UserIdentityV2;
import com.skava.model.userv2.UserPropertiesV2;
import com.skava.model.userv2.UserV2;
import com.skava.services.BpmService;
import com.skava.services.HttpClientService;
import com.skava.services.SkavaMessagingService;
import com.skava.services.StreamComUserService;
import com.skava.services.StreamUserV2Service;
import com.skava.services.userv2.KraftUserService;
import com.skava.util.CryptoUtil;
import com.skava.util.KeystoreServiceUtil;
import com.skava.services.SkavaKeystoreService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.skava.model.email.MacroDescriptor;
import com.skava.model.email.MessagingDescriptor;
import com.skava.kraft.userv2.Userv2Constants;
import com.skava.userv2.utils.Userv2Util;
import com.skava.util.ServerException;
import com.skava.util.messageservice.MessageService;
import com.skava.util.DateTimeUtil;
import org.apache.commons.lang.StringUtils;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.Partner;
import com.skava.model.dbbeans.MessageCampaigns;
import com.skava.util.CampaignUtil;
import com.skava.db.DBSession;
import com.skava.dao.PartnerDAO;
import com.skava.dao.UserIdentityV2DAO;
import com.skava.dao.UserPropertiesV2DAO;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import com.skava.util.ReadUtil;
import com.skava.util.ConfigManagerInstance;
import com.skava.kraft.userv2.captcha.CaptchaGenerator;
import lombok.Getter;
import lombok.Getter;
import lombok.Setter;

public class KraftUserServiceImpl implements KraftUserService {

	SkavaLogger logger = SkavaLoggerFactory.getLogger(StreamUserV2Service.class);
    
    @Setter private StreamComUserService streamComUserService;
    @Setter private HttpClientService httpClientService;
    @Setter private MemCacheManager cacheManager;
    @Setter private DBSessionManager dbSessionManager;
    @Setter private MessageSource resourceBundle;
    @Setter private SkavaMessagingService messageCampaignService;
    @Setter private String encryptsaltvalue;
    @Setter private BpmService bpmService;
    @Setter private SkavaCaptchaFactory skavaCaptchaFactory;
    @Setter private MessageService messageService;
    private CryptoUtil cryptoUtil;
    
    /** Partner related information. */
    private Partner partner = null;
    
    /** Campaign related information. */
    private Campaign campaign = null;
    
    /** Mwssage Campaign related information. */
    private MessageCampaigns messageCampaign = null;
    
    private Locale localeObj = null;
    
    private int loginRequestCount = 0;
    
	private CaptchaGenerator captchaGenerator;
	
    @Autowired @Setter private SkavaTenantContextFactory skavaKraftContextFactory;
    
	public KraftUserServiceImpl(StreamComUserService streamComUserService, HttpClientService httpClientService,
			MemCacheManager cacheManager, DBSessionManager dbSessionManager, MessageSource resourceBundle,
			SkavaMessagingService messageCampaignService, String encryptsaltvalue, BpmService bpmService,
			SkavaCaptchaFactory skavaCaptchaFactory, MessageService messageService) {
		        this.streamComUserService = streamComUserService;
		        this.httpClientService = httpClientService;
		        this.cacheManager = cacheManager;
		        this.dbSessionManager = dbSessionManager;
		        this.resourceBundle = resourceBundle;
		        this.messageCampaignService = messageCampaignService;
		        this.encryptsaltvalue = encryptsaltvalue;
		        this.bpmService = bpmService;
		        this.skavaCaptchaFactory = skavaCaptchaFactory;
		        this.messageService = messageService;
	}


	@Override
	public ComUserResponse createUser(HttpServletRequest request, HttpServletResponse response, ComUser user,
			long storeId, String locale) throws ServerException {
	    
		this.localeObj = getLocaleObj(locale);
	    ComUserResponse toRet = new ComUserResponse();
	    toRet.setResponseCode(Response.RESPONSE_CODE_SCALE_AUTHENTICATION_FAILED);
	    String userEmailID = user.getUserProperties().getUserEmail().toLowerCase();
	    String authToken  = user.getUserProperties().getAuthToken();
	    String uniqueUsername = user.getUserProperties().getUniqueUserName();
			
		FirebaseGetAccountInfoResponse resp = KraftFirebaseUtil.getAccountInfo(request, httpClientService, authToken);
		
		if(resp.getResponseCode() == Response.RESPONSE_SUCCESS)
		{
			String firebaseEmail = resp.getUsers()[0].getEmail();
			String firebaseLocalId = resp.getUsers()[0].getLocalId();
			String providerId = resp.getUsers()[0].getProviderUserInfo()[0].getProviderId();
			
			try 
			{
				firebaseUserCreationPrecheck(userEmailID, uniqueUsername, firebaseEmail, firebaseLocalId, providerId);
				user.getUserProperties().setUniqueUserId(firebaseLocalId);
				toRet = streamComUserService.createUserbpm(request, response,"v7", storeId ,1 ,1 ,false ,null ,null ,null ,null, (long)0, true ,user ,locale ,true);
			}
			catch (ServerException e) 
			{
				toRet.setResponseMessage(e.getMessage());
				toRet.setResponseCode(e.getErrorCode());
			}
		}
		else
		{
			toRet.setResponseCode(ComUserResponse.RESP_ERROR_FIREBASE_AUTHENTICATION);
			toRet.setResponseMessage(ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_ERROR_FIREBASE_AUTHENTICATION, this.localeObj));
		}
		return toRet;
	}

	@Override
	public KraftUserLoginResponse loginUser(HttpServletRequest request, HttpServletResponse response, ComUser user, long storeId, String locale) throws ServerException{
		
		ComUserResponse toRet = new ComUserResponse();
		KraftUserLoginResponse kraftUserLoginResponse = new KraftUserLoginResponse();
		int maxLoginCount = ReadUtil.getInt(ConfigManagerInstance.get(Userv2Constants.ZOOKEEPER_PROP_MAX_LOGIN_ATTEMPT_COUNT), 3);
		toRet.setResponseCode(Response.RESPONSE_CODE_SCALE_AUTHENTICATION_FAILED);
		
		loginRequestCount++;               
		try 
		{
		    this.localeObj = getLocaleObj(locale);
			toRet = streamComUserService.loginBpm(request, response, storeId, 1, user, locale, null, null, (long)0);
		} 
		catch (ServerException e) 
		{
			kraftUserLoginResponse.setResponseCode(e.getErrorCode());
			kraftUserLoginResponse.setResponseMessage(e.getMessage());
		}
		
		if(loginRequestCount > maxLoginCount && user.getUserProperties().getCaptchaText() != null) {
			
			String captchaText = null;
			String userCaptchaText = user.getUserProperties().getCaptchaText();
			boolean isCaptchaTextEqual = captchaText.equals(userCaptchaText);
				
			if(isCaptchaTextEqual) {
				try 
				{
					toRet = streamComUserService.loginBpm(request, response, storeId, 1, user, locale, null, null, (long)0);
				} 
				catch (ServerException e) 
				{
					kraftUserLoginResponse.setResponseCode(e.getErrorCode());
					kraftUserLoginResponse.setResponseMessage(e.getMessage());
				}
			} else {
				kraftUserLoginResponse.setResponseCode(ComUserResponse.RESP_USER_INVALID_CAPTCHA);
				kraftUserLoginResponse.setResponseMessage(toRet.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_USER_INVALID_CAPTCHA, this.localeObj));
				kraftUserLoginResponse.setLoginmaxcountreached(true);
				return kraftUserLoginResponse;
			}
		}
		if(toRet.getResponseCode() == Response.RESPONSE_SUCCESS)
        {	
			loginRequestCount = 0;
			kraftUserLoginResponse.setResponseCode(toRet.getResponseCode());
			kraftUserLoginResponse.setResponseMessage(toRet.getResponseMessage());
        }
        else
        {	
        	kraftUserLoginResponse.setResponseCode(toRet.getResponseCode());
			kraftUserLoginResponse.setResponseMessage(toRet.getResponseMessage());
			if(loginRequestCount >= maxLoginCount) {
				kraftUserLoginResponse.setLoginmaxcountreached(true);
			}
        }
		return kraftUserLoginResponse;
	}
	
	@Override
	public ComUserResponse forgotUserName(HttpServletRequest request, HttpServletResponse response, ComUser user, long storeId, long messageCampaignId, String locale) {
		
		ComUserResponse toRet = new ComUserResponse();
		toRet.setResponseCode(Response.RESPONSE_CODE_SCALE_AUTHENTICATION_FAILED);
		
		UserIdentityV2 userIdentity = null;
		UserV2 userData = null;
		DBSession session = dbSessionManager.getReadOnlyDBSession();
		String userEmailId = user.getUserProperties().getUserEmail().toLowerCase();
		int identityType = user.getUserIdentities()[0].getType();
        String identityValue = user.getUserIdentities()[0].getValue();
        String userName = null;
        
		try 
		{	
			this.localeObj = getLocaleObj(locale);
			userPrevalidation(dbSessionManager, streamComUserService, storeId, messageCampaignId);
			if(!StringUtils.isEmpty(userEmailId)) { 
				userIdentity = Userv2Util.loadIdentityByTypeValusUserIdAndCampaignId(session, this.getCryptoUtil(), identityType, identityValue, userEmailId, campaign.getId());
				if(userIdentity == null) {
					throw new ServerException(ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, this.localeObj));
				}
				userData = Userv2Util.loadUserv2(session, this.getCryptoUtil(), userIdentity.getSkavaUserId(), partner.getId(), campaign.getId(), false);
				if(userData != null) {
					for(UserPropertiesV2 userPropertiesV2 : userData.getProperties()) {
						if((UserPropertiesV2.PROP_UNIQUE_NAME).equals(userPropertiesV2.getName())) {
		                    userName = userPropertiesV2.getValue();
		                    break;
		            	}
					}
					if(StringUtils.isEmpty(userName)) {
						throw new ServerException(ComUserResponse.RESP_INVALID_USER_NAME, ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_INVALID_USER_NAME, this.localeObj));
					}
					
					if (campaign != null && messageCampaign != null && user.getUserProperties() != null && user.getUserProperties().getUserEmail() != null)
                    {
					    toRet = sendForgotUserNameMail(request, response, locale, campaign, storeId, messageCampaign, partner, 0, session, userData, user.getId(), user.getUserProperties().getUserEmail());
                    }
				}
			} else {
				throw new ServerException(ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, this.localeObj));
			}
		}
		catch (ServerException e) 
		{
			toRet.setResponseMessage(e.getMessage());
			toRet.setResponseCode(e.getErrorCode());
	        if (session != null)
	        {
	        	session.endSession(e);
	        }
	    }
	    finally
	    {
	        if (session != null)
	        {
	        	session.endSession();
	        	session = null;
	        }
	    }
		return toRet;
	}
	
	@Override
	public byte[] getCaptchaImage(HttpServletRequest request, HttpServletResponse response, String type) {
		
		byte[] captchaImage = null;
		try 
		{
			captchaGenerator = new CaptchaGenerator(5);
			captchaGenerator.setBackgroundColor(Color.BLUE);
			captchaGenerator.setTextColor(Color.RED);
			captchaGenerator.drawRandomCharacters();
			captchaGenerator.save();
			captchaImage = captchaGenerator.getImage();
			String[] captchaText = captchaGenerator.getCharacters();
			StringBuilder builder = new StringBuilder(5);
			for(String s : captchaText) {
			    builder.append(s);
			}
			String captcha = builder.toString();
		} catch(Exception e) {
			
		}
		return captchaImage;
	}
	
	@Override
    public ComUserResponse updateKraftUserEmail(HttpServletRequest request,
                                         HttpServletResponse response,
                                         long storeId,
                                         long messageCampaignId,
                                         String userEmail,
                                         String locale) throws ServerException
    {
        ComUserResponse toRet = new ComUserResponse();
        
        ComUser userToUpdate = new ComUser();
        
        String identityValue = ReadUtil.getString(ConfigManagerInstance.get(Userv2Constants.KRAFTRECIPE_IDENTITY_VALUE), "kraft");
        ComUserIdentity userIdentity = new ComUserIdentity();
        ComUserIdentity[] userIdentities = new ComUserIdentity[1];
        userIdentities[0] = userIdentity;
        ComUserProperties userprops = new ComUserProperties(); 
        ComUserResponse comUserFromDb = null;
        ComUserProperties comUserPropsFromDb = null;
        ComUserIdentity[] comUserIdentitiesFromDb = null;
        UserV2 userv2ForUpdation = null;
        UserIdentityV2 userIdentityv2 = null;
        String userEmailFromDB = null;
        DBSession dbSession = dbSessionManager.getReadOnlyDBSession();
        
        userprops.setUserEmail(userEmail);
        localeObj = getLocaleObj(locale);
        userToUpdate.setUserIdentities(userIdentities);
        userToUpdate.setUserProperties(userprops);
        
        try 
        {	
        	userPrevalidation(dbSessionManager, streamComUserService, storeId, messageCampaignId);
            if(partner != null && campaign != null) {
            	 userv2ForUpdation = streamComUserService.getSkavaUserV2FromCookie(dbSession, request, response, partner, campaign, false);
            }
            if(userv2ForUpdation != null) {
           	 comUserFromDb = getResponseUser(request, response, localeObj.toString(), true, userv2ForUpdation, false);
            }
            if(comUserFromDb != null) {
            	comUserIdentitiesFromDb = comUserFromDb.getUserIdentities();
            	comUserPropsFromDb = comUserFromDb.getUserProperties();
       		}
            //XXX: In order to update email, prop.allowupdateemail in campaign properties in need to be set to true.
            toRet = streamComUserService.updateUserBpm(request, response, storeId, messageCampaignId, userToUpdate, false, locale);
            if(toRet.getResponseCode() == ComUserResponse.CODE_RESP_SUCCESS) {
    		
            	String userPropertyEmail = comUserPropsFromDb.getUserEmail();
            		
	            for (ComUserIdentity identityUpdated : comUserIdentitiesFromDb) {
	            	if(userPropertyEmail.equals(identityUpdated.getUserId())) {
	            		userEmailFromDB = identityUpdated.getUserId();
	            		break;
	            	}
	            }
	            userIdentityv2 = Userv2Util.loadIdentityByTypeValusUserId(dbSession, this.getCryptoUtil(), UserIdentityV2.TYPE_CUSTOM, identityValue, userEmailFromDB, partner.getId(), campaign.getId());
	            if (userIdentityv2 != null && userIdentityv2.getId() > 0)
	            {
	                if (!userEmail.equals(userIdentityv2.getUserId()))
	                {
	                	dbSession = dbSessionManager.getReadWriteDBSession();
	                    (new UserIdentityV2DAO()).updateUserId(dbSession, this.getCryptoUtil(), userEmail, userIdentityv2.getId());
	                }
	            }
            }
        } 
        catch (ServerException e) 
        {
            toRet.setResponseMessage(e.getMessage());
            toRet.setResponseCode(e.getErrorCode());
        }
        finally
        {
            if (dbSession != null)
            {
                dbSession.endSession();
                dbSession = null;
            }
        }
        
        return toRet;
    }

	private Locale getLocaleObj(String locale) throws ServerException{
		Locale localeObj = null;
		try
		{
			localeObj = LocaleUtils.toLocale(locale);
		}
		catch(Exception e)
		{
			throw new ServerException(ComUserResponse.ERR_UNKNOWN);
		}
        return localeObj; 
	}


	private void firebaseUserCreationPrecheck(String userEmailID, String uniqueUsername, String firebaseEmail,
			String firebaseLocalId, String providerId) throws ServerException 
	{
 		if(userEmailID == null || userEmailID.length()<=0)
		{
			throw new ServerException(ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, this.localeObj));
		}
		if(firebaseEmail!=null && firebaseEmail.length()>0)
		{
			if(!firebaseEmail.equals(userEmailID))
			{
				throw new ServerException(ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_INVALID_EMAIL_ADDRESS, this.localeObj));
			}
		}
		if(providerId.equals("password"))
		{
			if(uniqueUsername==null || uniqueUsername.length()<=0)
			{
				throw new ServerException(ComUserResponse.RESP_MANDATORY_PROPERTY_MISSING, ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_MANDATORY_PROPERTY_MISSING, this.localeObj));
			}
		}
		if(firebaseLocalId == null || firebaseLocalId.length()<=0)
		{
			throw new ServerException(ComUserResponse.RESP_INVALID_USER_ID, ComUserResponse.getMessage(this.resourceBundle, (int)ComUserResponse.RESP_INVALID_USER_ID, this.localeObj));
		}
				}

	/**
     * Creates the Crypto util object using the
     * shared secret key from API Admin service.
     * @return CryptoUtil
     */
    private CryptoUtil getCryptoUtil()
    {
        synchronized (this)
        {
        	if (this.cryptoUtil == null)
        	{
                SecretKeySpec secertKey = KeystoreServiceUtil.getKeySpec(SkavaKeystoreService.SKAVA_USER_SECRET_KEY);
                if (secertKey != null)
                {
                    this.cryptoUtil = new CryptoUtil(secertKey);
                }
        	}
        }
        return this.cryptoUtil;
    }
    
    public void userPrevalidation(DBSessionManager dbSessionManager,
            										StreamComUserService comUserService,
    												long storeId,
    												long messageCampaignId) throws ServerException
    {
    
    	DBSession dbSession = null;

	    try
	    {
	        dbSession = dbSessionManager.getReadOnlyDBSession();
	        
	        if(!isValideStoreId(storeId))
	        {
	            throw new ServerException(ComUserResponse.RESP_INVALID_STOREID);
	        }
	        
	        long campaignId = CampaignUtil.getCampaignIdByStoreId(dbSession, storeId, Userv2Constants.PARAM_TYPE_USER);
	        
	        campaignId = campaignId == 0 ? storeId : campaignId;
	        
	        campaign = validateCampaign(comUserService, campaignId, dbSession);
	        
	        partner = validatePartner(comUserService, campaign.getPartnerid(), dbSession);
	        
	        messageCampaign = validateCommunicationCampaign(comUserService, messageCampaignId, dbSession);
	        
	    }
	    catch (ServerException se)
	    {
	        if (dbSession != null)
	        {
	            dbSession.endSession(se);
	        }
	    }
	    finally
	    {
	        if (dbSession != null)
	        {
	            dbSession.endSession();
	            dbSession = null;
	        }
	    }
	}
    
    /**
     * Validate campaign.
     *
     * @param comUserService the com user service
     * @param campaignId the campaign id
     * @param partnerId the partner id
     * @param dbSession the db session
     * @return the campaign
     * @throws ServerException the server exception
     */
    private Campaign validateCampaign(StreamComUserService comUserService,
                                      long campaignId,
                                      DBSession dbSession) throws ServerException
    {
        Campaign campaign = null;

        if (campaignId > 0)
        {
            campaign = comUserService.loadCampaignById(dbSession, campaignId);

            if (isInvalidCampaign(campaign)) { throw new ServerException(ComUserResponse.RESP_INVALID_CAMPAIGN); }
        }

        return campaign;
    }
    
    /**
     * Checks if is invalid campaign.
     *
     * @param campaign the campaign
     * @return true, if is invalid campaign
     */
    private boolean isInvalidCampaign(Campaign campaign)
    {
        return campaign == null || campaign.getId() <= 0 || !campaign.isActive();
    }
    
    /**
     * Validate partner.
     *
     * @param comUserService the com user service
     * @param partnerId the partner Id
     * @param dbSession the db session
     * @return the partner
     * @throws ServerException the server exception
     */
    private Partner validatePartner(StreamComUserService comUserService,
                                    long partnerId,
                                    DBSession dbSession) throws ServerException
    {
    	Partner partner = null;
        if(partnerId > 0 )
        {
             partner = (new PartnerDAO()).load(dbSession, partnerId);

             if (isInvalidPartner(partner)) { throw new ServerException(ComUserResponse.RESP_INVALID_PARTNER); }
        }
        return partner;
    }
    
    private boolean isValideStoreId(long storeId)
    {
        return storeId > 0 ? true : false; 
    }
    
    /**
     * Checks if it is invalid partner.
     *
     * @param partner the partner
     * @return true, if is invalid partner
     */
    private boolean isInvalidPartner(Partner partner)
    {
        return partner == null || partner.getId() <= 0;
    }
    
    /**
     * Validate communication campaign.
     *
     * @param comUserService the com user service
     * @param messageCampaignId the message campaign id
     * @param dbSession the db session
     * @return the message campaigns
     * @throws ServerException the server exception
     */
    private MessageCampaigns validateCommunicationCampaign(StreamComUserService comUserService,
                                                           long messageCampaignId,
                                                           DBSession dbSession) throws ServerException
    {
        MessageCampaigns messageCampaign = null;

        if (messageCampaignId > 0)
        {
            messageCampaign = comUserService.loadMessageCampaignById(dbSession, messageCampaignId);

            if (isInvalidCommunicationCampaign(messageCampaign)) { throw new ServerException(ComUserResponse.RESP_INVALID_MESSAGE_CAMPAIGN); }
        }
        return messageCampaign;
    }

    /**
     * Checks if is invalid communication campaign.
     *
     * @param messageCampaign the message campaign
     * @return true, if is invalid communication campaign
     */
    private boolean isInvalidCommunicationCampaign(MessageCampaigns messageCampaign)
    {
        return messageCampaign == null || messageCampaign.getId() <= 0;
    }
    
    /**
     * Send forgot user name mail.
     *
     * @param request the request
     * @param response the response
     * @param locale the locale
     * @param campaign the campaign
     * @param messageCampaign the message campaign
     * @param partner the partner
     * @param responseCode the response code
     * @param dbSession the db session
     * @param userV2 the user V 2
     * @param userId the user id
     * @param userEmail the user email
     */
    private ComUserResponse sendForgotUserNameMail(HttpServletRequest request,
                                 HttpServletResponse response,
                                 String locale,
                                 Campaign campaign,
                                 long storeId,
                                 MessageCampaigns messageCampaign,
                                 Partner partner,
                                 int responseCode,
                                 DBSession dbSession,
                                 UserV2 userV2,
                                 long userId,
                                 String userEmail)
    {
        ComUserResponse toRet = new ComUserResponse();
        responseCode = Response.RESPONSE_FAILED;
        try
        {
        	dbSession = dbSessionManager.getReadWriteDBSession(); 
            ComUserResponse comUserResp = getResponseUser(request, response, locale, true, userV2, false);
            responseCode = sendForgotUserNameMail(campaign, storeId, messageCampaign, partner, 0, dbSession, comUserResp, comUserResp.getId(), comUserResp.getUserProperties().getUserEmail());
            if(responseCode == 7033)
            {
                toRet.setResponseCode(responseCode);
                toRet.setResponseMessage("Forgot User Name Mail Sent Successfully");
                //TODO: need to be accessed via messageBundle
            }
            else
            {
                toRet.setResponseCode(responseCode);
                toRet.setResponseMessage("ForgotUserName Failed");
            }
        }
        catch (Exception e)
        {
        	logger.writeLog(Level.INFO, null, null, this.getClass().getSimpleName() , Thread.currentThread().getStackTrace()[1].getMethodName(), 0, null, Userv2Constants.ERRORTYPE_INPUT, "Exception in sending Welcome Mail", null, true, e);
            logger.writeLog(Level.ERROR, null, null, this.getClass().getSimpleName() , Thread.currentThread().getStackTrace()[1].getMethodName(), 0, null, Userv2Constants.ERRORTYPE_INPUT, "An error has occurred", null, true, e);
            if (dbSession != null)
            {
                dbSession.rollbackSession(e);
                dbSession = null;
            }
        }
        finally
        {
            if (dbSession != null)
            {
                dbSession.endSession();
                dbSession = null;
            }
        }
        return toRet;
    }

    /**
     * Send forgot user name mail.
     *
     * @param campaign the campaign
     * @param messageCampaign the message campaign
     * @param partner the partner
     * @param responseCode the response code
     * @param dbSession the db session
     * @param comResp the com resp
     * @param userId the user id
     * @param userEmail the user email
     * @return the int
     * @throws JSONException the JSON exception
     * @throws ServerException the server exception
     */
    private int sendForgotUserNameMail(Campaign campaign,
                                long storeId,
                                MessageCampaigns messageCampaign,
                                Partner partner,
                                int responseCode,
                                DBSession dbSession,
                                ComUserResponse comResp,
                                long userId,
                                String userEmail) throws JSONException, ServerException
    {
        String userName = comResp.getUserProperties().getUniqueUserName();
        String userFirstName = comResp.getUserProperties().getUserFirstName();
        String userLastName = comResp.getUserProperties().getUserLastName();

        userName = (userName != null ? userName : (userFirstName != null ? userFirstName : "user"));

        MessagingDescriptor messagingDescriptor = new MessagingDescriptor();
        MacroDescriptor macroDescriptor = new MacroDescriptor();

        HashMap<String, String> macrosTemp = new HashMap<String, String>();
        String customUserMacros = campaign.getProperty(CampaignProperties.PROP_CUSTOM_USER_MACROS);
        if (customUserMacros != null)
        {
            JSONObject customUserMacrosJSON = new JSONObject(customUserMacros);
            if (customUserMacrosJSON != null && customUserMacrosJSON.length() > 0)
            {
                String userNameCustom = customUserMacrosJSON.getString(Userv2Constants.EMAIL_MACRO_USER_NAME);
                String userFirstNameCustom = customUserMacrosJSON.getString(Userv2Constants.EMAIL_MACRO_USER_FIRST_NAME);
                String userLastNameCustom = customUserMacrosJSON.getString(Userv2Constants.EMAIL_MACRO_USER_LAST_NAME);
                String userEmailCustom = customUserMacrosJSON.getString(Userv2Constants.EMAIL_MACRO_USER_EMAIL);
                macrosTemp.put(userNameCustom, userName);
                macrosTemp.put(userFirstNameCustom, userFirstName);
                macrosTemp.put(userLastNameCustom, userLastName);
                macrosTemp.put(userEmailCustom, userEmail);
            }
        }
        else
        {
            macrosTemp.put(Userv2Constants.EMAIL_MACRO_USER_NAME, userName);
            macrosTemp.put(Userv2Constants.EMAIL_MACRO_USER_EMAIL, userEmail);

            HashMap<String, String> macroLengthConfig = new HashMap<String, String>();
            macroLengthConfig.put(Userv2Constants.EMAIL_MACRO_USER_NAME, String.valueOf(userName.length()));
            macroLengthConfig.put(Userv2Constants.EMAIL_MACRO_USER_EMAIL, String.valueOf(userEmail.length()));
            if (userFirstName != null)
            {
                macrosTemp.put(Userv2Constants.EMAIL_MACRO_USER_FIRST_NAME, userFirstName);
                macroLengthConfig.put(Userv2Constants.EMAIL_MACRO_USER_FIRST_NAME, String.valueOf(userFirstName.length()));
            }
            if (userLastName != null)
            {
                macrosTemp.put(Userv2Constants.EMAIL_MACRO_USER_LAST_NAME, userLastName);
                macroLengthConfig.put(Userv2Constants.EMAIL_MACRO_USER_LAST_NAME, String.valueOf(userLastName.length()));
            }

            messagingDescriptor.setMacroLengthConfig(macroLengthConfig);
        }

        macroDescriptor.setDefaultMacro(macrosTemp);
        macroDescriptor.setDefaultOnly(true);
        messagingDescriptor.setMacroData(macroDescriptor);
        messagingDescriptor.setRecipients(userEmail);

        int notificationCountPerDay = ReadUtil.getInt(campaign.getProperty(CampaignProperties.PROP_MAX_NOTIFICATION_PER_USER_PER_DAY), Userv2Constants.DEFAULT_NOTIFICATION_PER_USER_PER_DAY);
        String notificationPropFromDB = comResp.getUserProperties().getNotificationCount();
        boolean isLimitExceeded = checkNotificationLimitExceeded(notificationPropFromDB, notificationCountPerDay);
        if (!isLimitExceeded)
        {
            Response responseJSON = messageCampaignService.sendMail(storeId, messageCampaign.getId(), messagingDescriptor);
            if (responseJSON != null && responseJSON.getResponseCode() == 0)
            {
                String notificationProperty = getNotificationPropToDB(notificationPropFromDB, notificationCountPerDay);
                if (notificationPropFromDB == null)
                {
                    (new UserPropertiesV2DAO()).createWithoutEncryption(dbSession, userId, 0, UserPropertiesV2.PROP_USER_NOTIFICATIONS_COUNT, notificationProperty);
                }
                else
                {
                    (new UserPropertiesV2DAO()).updateWithoutEncryption(dbSession, notificationProperty, userId, 0, UserPropertiesV2.PROP_USER_NOTIFICATIONS_COUNT);
                }
                responseCode = ComUserResponse.RESP_ACTIVATION_WELCOME_EMAIL_SUCCESS;
            }
            else
            {
                throw new ServerException(ComUserResponse.RESP_ACTIVATION_WELCOME_EMAIL_FAILED);
            }
        }
        else
        {
            throw new ServerException(ComUserResponse.RESP_NOTIFICATION_LIMIT_EXCEEDED);
        }
        return responseCode;
    }
    
    /* 
     * Used to gets the user identities and properties of the user.
     *
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param locale Locale in which the response message should be returned.
     * @param onlyActive Based on this boolean value, user is being loaded for an active user only.
     * @param userV2 Instance of {@link com.skava.model.UserV2} to load the properties and identities of an user.
     * @param enablePaymentRetrival Boolean value which indicates enabling/disabling of fetching the payment card related details and displaying them while loading the user profile
     * @return Instance of {@link com.skava.model.userv2.ComUserResponse} for getting user response.
     * @throws ServerException while loading properties and identities of an user.
     */
    @Override
    public ComUserResponse getResponseUser(HttpServletRequest request,
                                           HttpServletResponse response,
                                           String locale,
                                           boolean onlyActive,
                                           UserV2 userV2,
                                           boolean enablePaymentRetrival) throws ServerException
    {
        return getResponseUser(request, response, locale, onlyActive, userV2, false, enablePaymentRetrival);
    }
    
    /**
     * Gets the response user.
     *
     * @param request the request
     * @param response the response
     * @param locale the locale
     * @param onlyActive the only active
     * @param userV2 the user V 2
     * @param returnFullCardVal the return full card val
     * @param enablePaymentRetrival the enable payment retrival
     * @return the response user
     * @throws ServerException the server exception
     */
    private ComUserResponse getResponseUser(HttpServletRequest request,
                                            HttpServletResponse response,
                                            String locale,
                                            boolean onlyActive,
                                            UserV2 userV2,
                                            boolean returnFullCardVal,
                                            boolean enablePaymentRetrival) throws ServerException
    {
        ComUserResponse userResponse = null;
        if (userV2 != null && Userv2Util.isActiveUser(onlyActive, userV2))
        {
            userResponse = new ComUserResponse();
            userResponse.setId(userV2.getId());
            userResponse.setCreatedTime(userV2.getCreatedTime());
            userResponse.setStatus(userV2.getStatus());
            userResponse.setGuestUser(userV2.isGuestUser());
            userResponse.setCampaignId(userV2.getCampaignId());
            if (userV2.getIdentities() != null)
            {
                ComUserIdentity[] userIdentities = getResponseIdentities(request, response, locale, userV2.getIdentities());
                userResponse.setUserIdentities(userIdentities);
            }
            if (userV2.getProperties() != null)
            {
                ComUserProperties userProperties = Userv2Util.getResponseProperties(request, response, this.cryptoUtil, this.dbSessionManager, null, this.cacheManager, locale, userV2.getId(), userV2.getProperties(), returnFullCardVal, enablePaymentRetrival);
                userResponse.setUserProperties(userProperties);

                userResponse.setUserSegments(Userv2Util.getUserSegmentDetailsEx(userV2.getProperties()));
            }
        }
        return userResponse;
    }
    
    /*
     * Checks the users notification limit and is being used for sending mail to the respective user in send activation mail.
     *
     * @param notificationPropFromDB This param contains the last send token param timestamp followed by '~' and then followed by notification count.This notification count is configured in DB by campaign properties.
     * @param notificationCountPerDay This param indicates the user's notification count of the user which is configured in DB by the campaign properties "prop.maxnotificationperuserperday".If this particular campaign properties is not specified then it takes default notification count as 5.
     * @return true, if successful
     * @throws ServerException while checking the notification limit.
     */
    @Override
    public boolean checkNotificationLimitExceeded(String notificationPropFromDB,
                                                  int notificationCountPerDay) throws ServerException
    {
        boolean isLimitExceeded = false;
        if (notificationPropFromDB != null)
        {
            String[] data = notificationPropFromDB.split("~");

            String dateFromDB = ReadUtil.getString(data[0], null);
            int countFromDB = ReadUtil.getInt(data[1], 0);
            String dateToday = DateTimeUtil.getCurrentDate(ComUserResponse.NOTIFICATION_TIME_FORMAT, ComUserResponse.NOTIFICATION_TIME_ZONE);

            int daysDifference = DateTimeUtil.getNumDaysBetween(dateFromDB, dateToday, ComUserResponse.NOTIFICATION_TIME_FORMAT);
            if (daysDifference < 1 && countFromDB >= notificationCountPerDay)
            {
                isLimitExceeded = true;
            }
        }
        return isLimitExceeded;
    }
    
    /* 
     * Gets the notification property from DB and this property contains the last send token param timestamp followed by '~' and followed by total notification count.
     *
     * @param notificationPropFromDB This param contains the last send token param's timestamp followed by '~' and then followed by notification count.
     * @param notificationCountPerDay the notification count per day
     * @return The notification property of the user which contains the user's last send token param's timestamp followed by '~' and followed by total notification count
     * @throws ServerException while getting the notification property from DB
     */
    @Override
    public String getNotificationPropToDB(String notificationPropFromDB,
                                          int notificationCountPerDay) throws ServerException
    {
        int countToDB = 0;
        String dateToday = DateTimeUtil.getCurrentDate(ComUserResponse.NOTIFICATION_TIME_FORMAT, ComUserResponse.NOTIFICATION_TIME_ZONE);
        String dateToDB = dateToday;

        if (notificationPropFromDB != null)
        {
            String[] data = notificationPropFromDB.split("~");
            dateToDB = ReadUtil.getString(data[0], dateToday);
            countToDB = ReadUtil.getInt(data[1], 0);
        }

        int daysDifference = DateTimeUtil.getNumDaysBetween(dateToDB, dateToday, ComUserResponse.NOTIFICATION_TIME_FORMAT);
        if (daysDifference >= 1)
        {
            dateToDB = dateToday;
            countToDB = 0;
        }
        countToDB = countToDB + 1;
        return dateToDB + "~" + String.valueOf(countToDB);

    }
    
    /* 
     * Used to get the response properties for the given userid.
     *
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param locale Locale in which the response message should be returned.
     * @param userId Indicates the id of the user for which user the properties is being loaded.
     * @param props Array of instance of {@link com.skava.model.userv2.UserPropertiesV2} which contains user properties.
     * @param enablePaymentRetrival Boolean value which indicates enabling/disabling of fetching the payment card related details and displaying them while loading the user profile
     * @return Instance of {@link com.skava.model.userv2.ComUserProperties} of getting an user properties.
     * @throws ServerException while getting user properties.
     */
    @Override
    public ComUserIdentity[] getResponseIdentities(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   String locale,
                                                   UserIdentityV2[] identities) throws ServerException
    {
        ComUserIdentity[] comIdentities = new ComUserIdentity[identities.length];
        for (int i = 0; i < identities.length; i++)
        {
            comIdentities[i] = new ComUserIdentity();
            comIdentities[i].setId(identities[i].getId());
            comIdentities[i].setType(identities[i].getType());
            comIdentities[i].setValue(identities[i].getValue());
            comIdentities[i].setUserId(identities[i].getUserId());
            comIdentities[i].setSkavaUserId(identities[i].getSkavaUserId());
            comIdentities[i].setPartnerId(identities[i].getPartnerId());
            comIdentities[i].setChannel(identities[i].getChannel());
            comIdentities[i].setLastLoggedinTime(identities[i].getLastLoggedinTime());
            comIdentities[i].setCreatedTime(identities[i].getCreatedTime());
        }
        return comIdentities;
    }
    
	private static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
		return objectMapper;
	}


	@Override
	public KraftUserSearchResponse[] getUsersFromIdList(HttpServletRequest request, HttpServletResponse response, long storeId,
			String locale, ComUser[] user) {
			ComUserResponse[] userInfo = null;
			KraftUserSearchResponse[] toRet = null;
				try 
				{
					userPrevalidation(dbSessionManager, streamComUserService, storeId, 0);
					userInfo = streamComUserService.getAllUsersFromIdList(request, response, storeId, locale, user, partner.getId(), campaign.getId());
					if(userInfo.length > 0)
					{
					toRet = new KraftUserSearchResponse[userInfo.length];
					for(int i=0; i<userInfo.length;i++)
					{
						KraftUserSearchResponse resp = new KraftUserSearchResponse();
						if(userInfo[i].getUserProperties().getUniqueUserName()!= null) 
						{ 
							resp.setUniqueUserName(userInfo[i].getUserProperties().getUniqueUserName());
						} 
						if(userInfo[i].getUserProperties().getUserFirstName()!= null)
						{
							resp.setUserFirstName(userInfo[i].getUserProperties().getUserFirstName());
						}
						if(userInfo[i].getUserProperties().getUserLastName()!= null)
						{
								resp.setUserLastName(userInfo[i].getUserProperties().getUserLastName());
						}
						if(userInfo[i].getUserProperties().getUserEmail()!= null)
						{
								resp.setUserEmail(userInfo[i].getUserProperties().getUserEmail());
						}
							if(userInfo[i].getId()> 0L)
							{
								resp.setSkavaUserId(userInfo[i].getId());
							}
						if(resp!=null)
						{
							toRet[i] = resp;	
						}
					}
				}
					else 
					{
						toRet[0].setResponseCode(ComUserResponse.RESP_SEARCH_USER_NODATA);
					}
				}
				catch (ServerException e) 
				{
					toRet[0].setResponseMessage(e.getMessage());
					toRet[0].setResponseCode(e.getErrorCode());
				}
				return toRet;
	}

    @Override
    public KraftUserSearchResponse[] findUsers(HttpServletRequest request,
                                     HttpServletResponse response,
                                     long storeId,
                                     String[] searchType,
                                     String[] searchParam,
                                     boolean onlyActive,
                                     int offset,
                                     int limit,
                                     String locale)
    {
        KraftUserSearchResponse[] searchResp = new KraftUserSearchResponse[1];
        ComUserFindResponse toRet = new ComUserFindResponse();
        toRet.setResponseCode(ComUserResponse.RESP_SEARCH_USER_NODATA);
        
        try
        {
            toRet = this.streamComUserService.findUserBpm(request, response, storeId, searchType, searchParam, offset, limit, onlyActive,locale);
            
            if(toRet != null && toRet.getResponseCode() == ComUserResponse.CODE_RESP_SUCCESS)
            {
                ComUserResponse[] searchResult = toRet.getUserResponse();
                searchResp = new KraftUserSearchResponse[toRet.getUserResponse().length];
                
                if(searchResult != null && searchResult.length > 0)
                {
                    for(int idx = 0; idx < searchResult.length; idx++)
                    {
                     
                        if(searchResult[idx] != null)
                        {
                            searchResp[idx] = new KraftUserSearchResponse();
                            if(searchResult[idx].getUserProperties().getUserFirstName() != null)
                            {
                                searchResp[idx].setUserFirstName(searchResult[idx].getUserProperties().getUserFirstName());
                            }
                            if(searchResult[idx].getUserProperties().getUserLastName() != null)
                            {
                                searchResp[idx].setUserLastName(searchResult[idx].getUserProperties().getUserLastName());
                            }
                            if(searchResult[idx].getUserProperties().getUniqueUserName() != null)
                            {
                                searchResp[idx].setUniqueUserName(searchResult[idx].getUserProperties().getUniqueUserName());
                            }
                            if(searchResult[idx].getId() > 0L)
                            {
                                searchResp[idx].setSkavaUserId(searchResult[idx].getId());
                            }
                            /*if(searchResult[idx].getUserProperties().getUserEmail() != null)
                            {
                                searchResp[idx].setUserEmail(searchResult[idx].getUserProperties().getUserEmail());
                            }*/
                        }
                    }
                }
            }
            else
            {
                searchResp[0] = new KraftUserSearchResponse();
                searchResp[0].setResponseMessage(toRet.getResponseMessage());
                searchResp[0].setResponseCode(toRet.getResponseCode());
            }
        }
        catch(ServerException e)
        {
            searchResp[0] = new KraftUserSearchResponse();
            searchResp[0].setResponseMessage(e.getMessage());
            searchResp[0].setResponseCode(e.getErrorCode());
        }
        return searchResp;
    }
    
    
    
}
