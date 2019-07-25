package com.skava.services.userv2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.skava.model.userv2.ComUser;
import com.skava.model.userv2.ComUserIdentity;
import com.skava.model.userv2.ComUserProperties;
import com.skava.model.userv2.ComUserFindResponse;
import com.skava.model.userv2.ComUserResponse;
import com.skava.model.userv2.KraftUserLoginResponse;
import com.skava.model.userv2.UserIdentityV2;
import com.skava.model.userv2.UserPropertiesV2;
import com.skava.model.userv2.UserV2;
import com.skava.model.userv2.KraftUserSearchResponse;
import com.skava.util.ServerException;
import com.skava.util.helpers.MethodInfo;

public interface KraftUserService {
	@MethodInfo(params = { "request", "response", "user", "storeId", "locale"})
	public ComUserResponse createUser(HttpServletRequest request, HttpServletResponse response, ComUser user,
			long storeId, String locale) throws ServerException;
	
	@MethodInfo(params = { "request", "response", "user", "storeId", "locale"})
	public KraftUserLoginResponse loginUser(HttpServletRequest request, HttpServletResponse response, ComUser user,
			long storeId, String locale) throws ServerException;
	
	@MethodInfo(params = { "request", "response", "user", "storeId", "locale"})
	public ComUserResponse forgotUserName(HttpServletRequest request, HttpServletResponse response, ComUser user,
			long storeId, long messageCampaignId, String locale);
	
	/**
     * Used to gets the user identities and properties of the user.
     *
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param locale Locale in which the response message should be returned.
     * @param onlyActive Based on this boolean value, user is being loaded for an active user only.
     * @param userV2 Instance of {@link com.skava.model.userv2.UserV2} to load the properties and identities of an user.
     * @param enablePaymentRetrival Boolean value which indicates enabling/disabling of fetching the payment card related details and displaying them while loading the user profile
     * @return Instance of {@link com.skava.model.userv2.ComUserResponse} for getting user response.
     * @throws ServerException while loading properties and identities of an user.
     */
    @MethodInfo(name="getResponseUser", params = {"request", "response", "locale","onlyActive", "userV2", "enablePaymentRetrival"})
    ComUserResponse getResponseUser(HttpServletRequest request, HttpServletResponse response, String locale, boolean onlyActive, UserV2 userV2, boolean enablePaymentRetrival) throws ServerException;
    
    /**
     * Checks the users notification limit and is being used for sending mail to the respective user in send activation mail.
     *
     * @param notificationPropFromDB This param contains the last send token param timestamp followed by '~' and then followed by notification count.This notification count is configured in DB by campaign properties.
     * @param notificationCountPerDay This param indicates the user's notification count of the user which is configured in DB by the campaign properties "prop.maxnotificationperuserperday".If this particular campaign properties is not specified then it takes default notification count as 5.
     * @return true, if successful
     * @throws ServerException while checking the notification limit.
     */
    @MethodInfo(name="checkNotificationLimitExceeded", params = {"notificationPropFromDB", "notificationCountPerDay"})
    public boolean checkNotificationLimitExceeded(String notificationPropFromDB,
                                                  int notificationCountPerDay) throws ServerException;
    
    /**
     * Gets the notification property from DB and this property contains the last send token param timestamp followed by '~' and followed by total notification count.
     *
     * @param notificationPropFromDB This param contains the last send token param's timestamp followed by '~' and then followed by notification count.
     * @param notificationCountPerDay the notification count per day
     * @return The notification property of the user which contains the user's last send token param's timestamp followed by '~' and followed by total notification count
     * @throws ServerException while getting the notification property from DB
     */
    @MethodInfo(name="getNotificationPropToDB", params = {"notificationPropFromDB", "notificationCountPerDay"})
    String getNotificationPropToDB(String notificationPropFromDB,
                                   int notificationCountPerDay) throws ServerException;
    
    /**
     * Used to get the response identities of the given userid.
     *
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param locale Locale in which the response message should be returned.
     * @param identities Array of instance of {@link com.skava.model.userv2.UserIdentityV2} for getting user identities.
     * @return Instance of {@link com.skava.model.userv2.ComUserIdentity} of getting an user identities.
     * @throws ServerException while getting user identities.
     */
    @MethodInfo(name="getResponseIdentities", params = {"request", "response", "locale", "identities"})
    ComUserIdentity[] getResponseIdentities(HttpServletRequest request, HttpServletResponse response, String locale, UserIdentityV2[] identities) throws ServerException;
    

	@MethodInfo(params = { "request", "response", "storeId", "idList"})
	public KraftUserSearchResponse[] getUsersFromIdList(HttpServletRequest request, HttpServletResponse response, long storeId,
			String locale, ComUser[] user);

	@MethodInfo(params = { "request", "response", "storeId", "searchType", "searchParam", "onlyActive", "offset", "limit", "locale"})
	public KraftUserSearchResponse[] findUsers(HttpServletRequest request, 
	                                 HttpServletResponse response, 
	                                 long storeId,
	                                 String[] searchType,  
	                                 String[] searchParam,
	                                 boolean onlyActive, 
	                                 int offset,
	                                 int limit,
	                                 String locale);
	
	@MethodInfo(params = { "request", "response", "type"})
	public byte[] getCaptchaImage(HttpServletRequest request, HttpServletResponse response, String type);
	
    @MethodInfo(params = {"request", "response", "storeId", "messageCampaignId", "userEmail", "locale"})
    public ComUserResponse updateKraftUserEmail(HttpServletRequest request,
                                  HttpServletResponse response,
                                  long storeId,
                                  long messageCampaignId,
                                  String userEmail,
                                  String locale) throws ServerException;
}
