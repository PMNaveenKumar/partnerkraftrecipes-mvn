package com.skava.web.userv2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.StreamComServiceKraftBuilder;
import com.skava.model.Response;
import com.skava.model.TenantThreadLocal;
import com.skava.model.userv2.ComUser;
import com.skava.model.userv2.ComUserFindResponse;
import com.skava.model.userv2.ComUserResponse;
import com.skava.model.userv2.KraftUserLoginResponse;
import com.skava.model.userv2.KraftUserResponse;
import com.skava.model.userv2.KraftUserSearchResponse;
import com.skava.model.userv2.UserDescriptorIdentityV2;
import com.skava.model.userv2.UserDescriptorV2;
import com.skava.model.userv2.UserSecurityQuestions;
import com.skava.services.userv2.KraftUserService;
import com.skava.kraft.userv2.Userv2Constants;
import com.skava.util.ServerException;
import com.skava.util.helpers.CustomWebEditor;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Setter;

@Controller
public class UserKraftController
{
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired @Setter private SkavaTenantContextFactory skavaUserKraftContextFactory;

    @RequestMapping(value = "/kraft/ping", method = RequestMethod.GET)
    public @ResponseBody String ping()
    {
        long ts = System.currentTimeMillis();
        logger.info("Kraft User Service: responding to a ping - " + ts);
        return "Kraft User Service: responding to a ping - " + ts;
    }
    
    /**
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param storeId Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.
     * @param locale API Response and error messages will be responded in the locale mentioned in this parameter.
     * @param user Instance of {@link com.skava.model.userv2.ComUser}
     * @return
     */
    
    @ApiOperation(value = "Create an User", notes = "This service is used to create a user or register a new user. User is created based on the provided identity information and properties. User's identity are userName and uniqueUserId by default which are validated and uniqueness of the user is maintained.The authoken is obtained through the call which is then used to retrive the uniqueUserId and emailof the user.\n" +"\n"+"The JSON format of the user property is\n"+"\n" +"<pre style=\"overflow-y:scroll;height:300px;\"> {\r\n" + 
    		"  \"userIdentities\": [\r\n" + 
    		"    {\r\n" + 
    		"      \"type\": 5,\r\n" + 
    		"      \"value\": \"kraft\"\r\n" + 
    		"    }\r\n" + 
    		"  ],\r\n" + 
    		"  \"userProperties\": {\r\n" + 
    		"    \"authToken\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IlFEQXl2QSJ9\",\r\n" + 
    		"    \"userEmail\": \"skavauser@skava.com\",\r\n" + 
    		"    \"uniqueUserName\": \"skavaUser\"\r\n" + 
    		"    \"userPreferences\": {\r\n" + 
    		"      \"resetMethod\": 4\r\n" + 
    		"    }\r\n" + 
    		"  }\r\n" + 
    		"} </pre>\n"+ "\n" +
    		"Here \n"+"<b>userIdentities</b> - It refers to an user's identities model object. It is an array object which contains each identities as one useridentities array object. It also allows single sign-on by extending the default authenticator. It is used to login an user with the help of user identities. \n"+"\n"+"<b>userproperties</b> - It refers to an user's properties model object which contains user properties.(i.e) Username, Useremail, Userfirstname, Userlastname, Usercity, Userstate, Userzipcode, Userbillingaddress, Usershippingaddress, Userpaymentcards, Userpreferences, Usersegments, Usertimezone, Usersecurityquestions etc. (Mandatory field for creating an user either userEmail or userPhoneNumber must be given).\n"+"\n"+"<b>uniqueUserName</b> - (String) It refers to unique user name of the registrant which he can use for the purpose of logging in.\n"+"\n"+"<b>authToken</b> - (String)It refers to the authtoken that is provided by the frontend after validation of the user.\n"+"\n"+"<b>userName</b> - (String)It refers to the name of the user. Eg:testUserName. There are no specific validations on the length or range for this field. Input value to this field as null or an empty string, this will be updated as null or empty.\n"+"\n"+"<b>userEmail</b> - (String)It refers to the email of the user. An identity with user's email will be created when a user is created. This identity is later used for logging in the user with email as an identity.There are no specific validations on the length or range for this field. Eg: testemail@skava.com. Input value to this field as null or an empty string will be updated as null or empty. If useremail must be given to create an user, otherwise it shows error message as <b>\"Mandatory Properties Missing\"</b>.\n"+"\n", response=ComUserResponse.class, protocols="https")
    @RequestMapping(value = "/kraft/user/create", method = RequestMethod.POST)
    @ResponseBody 
    public KraftUserResponse createKraftUser(HttpServletRequest request,
            HttpServletResponse response,
            @ApiParam(value = "Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_STORE_ID) long storeId,
            @ApiParam(value = "API Response and error messages will be responded in the locale mentioned in this parameter. ", required = false) @RequestParam(value = Userv2Constants.PARAM_LOCALE, defaultValue = Userv2Constants.LOCALE_EN_US) String locale,
            @ApiParam(value = "Indicates the user object that is used for creating a user. The description of the userobject will be specified in API description.", required = true) @RequestParam(value = Userv2Constants.PARAM_USER) ComUser user)
    {
    	KraftUserResponse toRet = null;
    	ComUserResponse resp = new ComUserResponse();
        try
        {
              resp = ((KraftUserService) skavaUserKraftContextFactory.get(TenantThreadLocal.get(), StreamComServiceKraftBuilder.KRAFTUSERSERVICE)).createUser(request, response, user, storeId, locale);

              if(resp.getResponseCode() == Response.RESPONSE_SUCCESS)
              {
            	  toRet = new KraftUserResponse(resp.getResponseCode(), resp.getResponseMessage());
              }
              else
              {
            	  toRet = new KraftUserResponse(resp.getResponseCode(), resp.getResponseMessage());  
              }
        }
        catch (ServerException t)
        {
            logger.error("KraftUserController error in loginKraftUser", t);
            toRet = new KraftUserResponse(t.getErrorCode(), t.getMessage());
        }
        return toRet;
    }
    
    /**
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param storeId Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.
     * @param locale locale API Response and error messages will be responded in the locale mentioned in this parameter.
     * @param user Instance of {@link com.skava.model.userv2.ComUser}
     * @return
     */
    
    @ApiOperation(value = "Login an User", notes = "Login service gets credentials from the user, validate them, create a active session and send response with user session details.Login can take place either through social networks or using email or uniqueusername which is given by the user when he registers to the site.\n"+"\n"+"The JSON format of the user property is\n"+"\n" +"<pre style=\"overflow-y:scroll;height:300px;\"> {\r\n" + 
    		"  \"userIdentities\": [\r\n" + 
    		"    {\r\n" + 
    		"      \"type\": 5,\r\n" + 
    		"      \"value\": \"kraft\"\r\n" + 
    		"    }\r\n" + 
    		"  ],\r\n" + 
    		"  \"userProperties\": {\r\n" + 
    		"    \"authToken\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IlFEQXl2QSJ9\",\r\n" + 
    		"    \"userEmail\": \"skavauser@skava.com\",\r\n" + 
    		"    \"uniqueUserName\": \"skavaUser\"\r\n" + 
    		"    \"userPreferences\": {\r\n" + 
    		"      \"resetMethod\": 4\r\n" + 
    		"    }\r\n" + 
    		"  }\r\n" + 
    		"} </pre>\n"+ "\n" +
    		"Here \n"+"<b>userIdentities</b> - It refers to an user's identities model object. It is an array object which contains each identities as one useridentities array object. It also allows single sign-on by extending the default authenticator. It is used to login an user with the help of user identities. \n"+"\n"+"<b>userproperties</b> - It refers to an user's properties model object which contains user properties.(i.e) Username, Useremail, Userfirstname, Userlastname, Usercity, Userstate, Userzipcode, Userbillingaddress, Usershippingaddress, Userpaymentcards, Userpreferences, Usersegments, Usertimezone, Usersecurityquestions etc. (Mandatory field for creating an user either userEmail or userPhoneNumber must be given).\n"+"\n"+"<b>uniqueUserName</b> - (String) It refers to unique user name of the registrant which he can use for the purpose of logging in.\n"+"\n"+"<b>authToken</b> - (String)It refers to the authtoken that is provided by the frontend after validation of the user.\n"+"\n"+"<b>userName</b> - (String)It refers to the name of the user. Eg:testUserName. There are no specific validations on the length or range for this field. Input value to this field as null or an empty string, this will be updated as null or empty.\n"+"\n"+"<b>userEmail</b> - (String)It refers to the email of the user. An identity with user's email will be created when a user is created. This identity is later used for logging in the user with email as an identity.There are no specific validations on the length or range for this field. Eg: testemail@skava.com. Input value to this field as null or an empty string will be updated as null or empty. If useremail must be given to create an user, otherwise it shows error message as <b>\"Mandatory Properties Missing\"</b>.\n"+"\n", response=ComUserResponse.class, protocols="https" )
    @RequestMapping(value = "/kraft/user/login", method = RequestMethod.POST)
    @ResponseBody 
    public KraftUserLoginResponse loginKraftUser(HttpServletRequest request,
          HttpServletResponse response,
          @ApiParam(value = "Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_STORE_ID) long storeId,
          @ApiParam(value = "API Response and error messages will be responded in the locale mentioned in this parameter. ", required = false) @RequestParam(value = Userv2Constants.PARAM_LOCALE, defaultValue = Userv2Constants.LOCALE_EN_US) String locale,
          @ApiParam(value = "Indicates the comuser object that is used for login user. The description of the userobject will be specified in the API description." , required = true) @RequestParam(value = Userv2Constants.PARAM_USER) ComUser user)

    {
    	KraftUserLoginResponse toReturn = null;
    	ComUserResponse comUserResponse = new ComUserResponse();
        try
        {
        	toReturn = ((KraftUserService) skavaUserKraftContextFactory.get(TenantThreadLocal.get(), StreamComServiceKraftBuilder.KRAFTUSERSERVICE)).loginUser(request, response, user, storeId, locale);
            
        }
        catch (ServerException e)
        {
            logger.error("KraftUserController error in loginKraftUser", e);
            toReturn = new KraftUserLoginResponse(e.getErrorCode(), e.getMessage());
        }
        return toReturn;
    }
    
    /**
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param storeId Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.
     * @param locale locale API Response and error messages will be responded in the locale mentioned in this parameter.
     * @param user Instance of {@link com.skava.model.userv2.ComUser}
     * @param messageCampaignId Indicates the messaging campaign Id that is required for messaging / mailing services. It is needed for the mailing service to determine the mail content which is configured as per specific calls and scenarios. The default value is 0 .It's minimum value is 0 and has no maximum value.
     * @return
     */
    @ApiOperation(value = "forgotUsername ", notes = "This service is used for resetting a password based on reset method through Email/Sms/SecuirtyQuestions/Dynamic.The resetMethod will be 1 for Email, 2 for SMS, 3 for SecurityQuestions and 4 for Dynamic. While creation of user if reset method is selected as 4 (i.e dynamic) ,in resetPassword call we can choose any one of these above three reset methods(i.e) EMAIL/SMS/Securityquestions.  While resetting by email/sms, userId will be useremail and userphonenumber. The reset param or reset otp will be sent to the corresponding useremail or userphonenumber. An user password is resetted using reset token or reset otp through Validateresetemail call or Validateresetsms call. If user choose to reset an user password through security questions, then user needs to give all the security questions and answers that given while creation of user. In this case security questions are validated by validatesecurityanswers call and then the user password is resetted.\n"+"\n"+" The JSON format of resetpassword by EMAIL is\n"+"\n"+"<pre style=\"overflow-y:scroll;height:300px;\"> {\n  'userIdentities': [\n\t\t{\n\t\t'type':0,\n\t\t'value':'skava', \n\t\t'userId':'test@skava.com'\n\t\t}\n\t  ]\n }</pre>\n"+"\n"+"The JSON format of resetpassword by SMS is\n"+"\n"+"<pre style=\"overflow-y:scroll;height:300px;\"> \n {\n  'userIdentities': [\n\t\t{\n\t\t'type':0,\n\t\t'value':'skava', \n\t\t'userId':'9798989797'\n\t\t}\n\t  ]\n }</pre>\n"+"\n"+"The JSON format of resetpassword by SecurityQuestions is\n"+"\n"+"<pre style=\"overflow-y:scroll;height:300px;\">\n {\n  'userIdentities': [\n\t\t{\n\t\t'type':0,\n\t\t'value':'skava', \n\t\t'userId':'test@skava.com'\n\t\t}\n\t  ],\n 'userProperties':\n\t\t{\n\t\t'securityQuestions': [\n\t\t\t{\n\t\t\t'question':'testquestion', \n\t\t\t'answer': 'testanswer'\n\t\t\t}\n\t\t  ],\n\t\t'newPassword': 'Skava@15'\n\t\t}\n }</pre>\n"+"\n"+" Here <b>userIdentities</b> - It refers to an user's identities model object. It is an array object which contains each identities as one useridentities array object. Identities will be userEmail, userPhoneNumber and userProfileId(corresponds to googleId, FacebookId, TwitterId or Id from Third party authenticator). An user which contains one user identities in all each types(i.e)SKAVA, FACEBOOK, GOOGLE, TWITTER, CUSTOM. User is defined based on this identity type value (i.e)SKAVA user or FACEBOOK user or GOOGLE user or TWITTER user or CUSTOM user. It also allows single sign-on by extending the default authenticator. It is used to login an user with the help of user identities.\n"+"\n"+"<b>type</b> - (Integer)(Mandatory)It refers to the numeric representation of the identity type. It's possible values are 0,2,3,4,5. Identity type such 0,2,3,4 and 5 holds the authentication type value as SKAVA, FACEBOOK, GOOGLE, TWITTER and CUSTOM respectively.Each user must have atleast one user identities. \n"+"\n"+"<b>value</b> - (String)It refers to the name of the identity. Eg:SKAVA,GOOGLE,FACEBOOK,TWITTER,CUSTOM. There are no specific validations on the length or range for this field.\n"+"\n"+"<b>channel</b> - (String)Channel indicates the mode for accessing user micro service. Channel may be web/mobile/tablet.\n"+"\n"+"<b>userId</b> - (String)(Mandatory)It refers to the userId of the user. (Mandatory field for resetpassword call by Email and SMS) userEmail, userPhoneNumber and userProfileId are created as each identity for each user. While resetting by email/sms, userId will be useremail and userphonenumber. The reset param or reset otp will be sent to the corresponding useremail or userphonenumber. An user password is resetted using reset token or reset otp through Validateresetemail call or Validateresetsms call. \n"+"\n"+"<b>userproperties</b> - It refers to an user's properties model object which contains user properties.(i.e) Username, Useremail, Userphonenumber, Userfirstname, Userlastname, Usercity, Userstate, Userzipcode, Userbillingaddress, Usershippingaddress, Userpaymentcards, Userpreferences, Usersegments, Usertimezone, Usersecurityquestions etc. \n"+"\n"+"<b>securityQuestions</b> - It refers to an user's Security questions model object which contains user's security questions and answers.It is an array object which is being used for resetting password using security questions. Input value to this field as null, null value is updated. Input value to this field as an empty[], it shows an error message as <b>\"Invalid Security Question And Answer\"</b>. Security question object must contain both question and answer, otherwise it shows an error message as as <b>\"Invalid Security Question And Answer\"</b>. \n"+"\n"+"<b>question</b> - (String)It refers to the security questions of the user.There are no specific validations on the length or range for this field.\n"+"\n"+"<b>answer</b> - (String)It refers to the security answers of the user.There are no specific validations on the length or range for this field.\n"+"\n"+"<b>newPassword</b> - (String)The secret key which user provides for his login authentication by email / phone. The user password is send to checkpasswordstrength call to check the given user password is valid or not. The password contain 1 Uppercase, 1 Special Character and numeric value. The password validation is configurable through campaignProperties in db.Password validating Campaignproperties is \"prop.passwordvalidatorclassname\" and password validatorconfig is \"prop.user.passwordvalidatorconfig\". Eg:Test@123", response = ComUserResponse.class, protocols = "https")
    @RequestMapping(value = "/kraft/user/forgotUsername", method = RequestMethod.POST)
    @ResponseBody 
    public KraftUserResponse forgotUserNameKraftUser(HttpServletRequest request,
    													HttpServletResponse response,
														@ApiParam(value = "Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_STORE_ID) long storeId,
														@ApiParam(value = "API Response and error messages will be responded in the locale mentioned in this parameter. ", required = false) @RequestParam(value = Userv2Constants.PARAM_LOCALE, defaultValue = Userv2Constants.LOCALE_EN_US) String locale,
														@ApiParam(value = "Indicates the comuser object that is used for login user. The description of the userobject will be specified in the API description." , required = true) @RequestParam(value = Userv2Constants.PARAM_USER) ComUser user,
														@ApiParam(value = "Indicates the messaging campaign Id that is required for messaging / mailing services. It is needed for the mailing service to determine the mail content which is configured as per specific calls and scenarios. The default value is 0 .It's minimum value is 0 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_MESSAGE_CAMPAIGN_ID, required = false, defaultValue = "0") long messageCampaignId)

    {
    	KraftUserResponse toReturn = null;
    	ComUserResponse comUserResponse = new ComUserResponse();
        try
        {
        	comUserResponse = ((KraftUserService) skavaUserKraftContextFactory.get(TenantThreadLocal.get(), StreamComServiceKraftBuilder.KRAFTUSERSERVICE)).forgotUserName(request, response, user, storeId, messageCampaignId, locale);
            
            if(comUserResponse.getResponseCode() == Response.RESPONSE_SUCCESS)
            {
            	toReturn = new KraftUserResponse(comUserResponse.getResponseCode(), comUserResponse.getResponseMessage());
            }
            else
            {
            	toReturn = new KraftUserResponse(comUserResponse.getResponseCode(), comUserResponse.getResponseMessage());  
            }
        }
        catch (ServerException e)
        {
            logger.error("KraftUserController error in forgotUserNameKraftUser", e);
            toReturn = new KraftUserResponse(e.getErrorCode(), e.getMessage());
        }
        return toReturn;
    }

    @RequestMapping(value = "/kraft/user/find", method = RequestMethod.POST)
    @ResponseBody 
    public KraftUserSearchResponse[] findKraftUsers(HttpServletRequest request,
                                            HttpServletResponse response,
                                            @RequestParam(value = "storeId") long storeId,
                                            @RequestParam(value = "searchType") String[] searchType,
                                            @RequestParam(value = "searchParam") String[] searchParam,
                                            @RequestParam(value = "onlyActive", defaultValue = "false") boolean onlyActive,
                                            @RequestParam(value = "offset", defaultValue = "0") int offset,
                                            @RequestParam(value = "limit", defaultValue = "10") int limit,
                                            @RequestParam(value = "locale", defaultValue = "en_US") String locale)
    
    {
        KraftUserSearchResponse[] toReturn = new KraftUserSearchResponse[1];
        try
        {
            toReturn = ((KraftUserService) skavaUserKraftContextFactory.get(TenantThreadLocal.get(), StreamComServiceKraftBuilder.KRAFTUSERSERVICE)).findUsers(request, response, storeId, searchType, searchParam, onlyActive, offset, limit, locale);
        }
        catch (Exception t)
        {
            logger.error("AimiaALPEUserController error in getMemberStatus", t);
        }
        return toReturn;
    }
    
    /**
     * @param request Provide request information for HTTP servlets.
     * @param response Provide HTTP-specific functionality in sending a response.
     * @param storeId Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.
     * @param locale locale API Response and error messages will be responded in the locale mentioned in this parameter.
     * @param user Instance of {@link com.skava.model.userv2.ComUser}
     * @return
     * @throws ServerException
     */
    @ApiOperation(value = "loadUserById", notes = "This service is used to search all the users using the user ids provided, uniqueUserName, userprofilephoto, uniqueuserid will be returned to the user\n"+"\n"+"The JSON format of the user  is\n"+"\n" +"<pre style=\"overflow-y:scroll;height:300px;\">\r\n" + "[{\"id\":119},{\"id\":120},{\"id\":107},{\"id\":101}]"+"} </pre>\n"+ "\n" +
    		"Here \n"+"<b>id</b> - It refers to an array of comuser objects with ids which is used to fetch the userinformation from the table\n"+"\n"+"<b>id</b> - Unique id of the user\n"+"\n",response=ComUserResponse.class, protocols="https")
    @RequestMapping(value = "/kraft/user/loadUserById", method = RequestMethod.POST)
    @ResponseBody 
    public KraftUserSearchResponse[] loadUserById(HttpServletRequest request,
    													HttpServletResponse response,
														@ApiParam(value = "Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_STORE_ID) long storeId,
														@ApiParam(value = "API Response and error messages will be responded in the locale mentioned in this parameter. ", required = false) @RequestParam(value = Userv2Constants.PARAM_LOCALE, defaultValue = Userv2Constants.LOCALE_EN_US) String locale,
														@ApiParam(value = "Indicates the Comuser array of all the user ids" , required = true) @RequestParam(value = Userv2Constants.PARAM_USER) ComUser[] user) throws ServerException

    {
    	KraftUserSearchResponse[] toReturn = null;
		try 
		{
			toReturn = ((KraftUserService) skavaUserKraftContextFactory.get(TenantThreadLocal.get(), StreamComServiceKraftBuilder.KRAFTUSERSERVICE)).getUsersFromIdList(request, response, storeId, locale, user);
		}
		catch (ServerException e)
		{
			logger.error("KraftUserController error in loadUserById", e);
			toReturn[0].setResponseCode(e.getErrorCode());
			toReturn[0].setResponseMessage(e.getMessage());
		}
		return toReturn;
    }
    
    @RequestMapping(value = "/kraft/user/captchaimage", method = RequestMethod.POST)
    @ResponseBody 
    public byte[] getCaptchaImage(HttpServletRequest request, 
    								HttpServletResponse response, 
    								@ApiParam(value = "Indicates whether the type is Create user or Login. It holds String value. It's minimum value is 1 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_TYPE) String type)

    {
    	KraftUserResponse toReturn = null;
    	byte[] captchaImage = null;
        try
        {
        	captchaImage = ((KraftUserService) skavaUserKraftContextFactory.get(TenantThreadLocal.get(), StreamComServiceKraftBuilder.KRAFTUSERSERVICE)).getCaptchaImage(request, response, type);
            
        }
        catch (ServerException e)
        {
            logger.error("KraftUserController error in getCaptchaImage", e);
        }
        return captchaImage;
    }
    
    
    /**
     * @param request
     * @param response
     * @param storeId Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.
     * @param messageCampaignId Indicates the messaging campaign Id that is required for messaging / mailing services. It is needed for the mailing service to determine the mail content which is configured as per specific calls and scenarios. The default value is 0 .It's minimum value is 0 and has no maximum value.
     * @param userEmail Indicates the email of the user to be updated.
     * @param locale API Response and error messages will be responded in the locale mentioned in this parameter. 
     * @return
     */
    @ApiOperation(value = "updateUserEmail", notes = "This service is user to update email of a kraft user.", response = ComUserResponse.class, protocols = "https")
    @RequestMapping(value = "/kraft/user/updateUserEmail", method = RequestMethod.POST)
    @ResponseBody 
    public KraftUserResponse updateUserEmail(HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        @ApiParam(value = "Indicates the storeId is the primary key identifier in the campaign table where value of the column 'servicetype' is 'user'. It holds long value. It's minimum value is 1 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_STORE_ID) long storeId,
                                                        @ApiParam(value = "Indicates the messaging campaign Id that is required for messaging / mailing services. It is needed for the mailing service to determine the mail content which is configured as per specific calls and scenarios. The default value is 0 .It's minimum value is 0 and has no maximum value.", required = true) @RequestParam(value = Userv2Constants.PARAM_MESSAGE_CAMPAIGN_ID, required = false, defaultValue = "0") long messageCampaignId,
                                                        @ApiParam(value = "Indicates the email of the user to be updated. ", required = false) @RequestParam(value = "userEmail", required = true) String userEmail,
                                                        @ApiParam(value = "API Response and error messages will be responded in the locale mentioned in this parameter. ", required = false) @RequestParam(value = Userv2Constants.PARAM_LOCALE, defaultValue = Userv2Constants.LOCALE_EN_US) String locale)

    {
        KraftUserResponse toReturn = null;
        ComUserResponse comUserResponse = new ComUserResponse();
        try
        {
            comUserResponse = ((KraftUserService) skavaUserKraftContextFactory.get(TenantThreadLocal.get(), StreamComServiceKraftBuilder.KRAFTUSERSERVICE)).updateKraftUserEmail(request, response, storeId, messageCampaignId, userEmail, locale);
            
            if(comUserResponse.getResponseCode() == Response.RESPONSE_SUCCESS)
            {
                toReturn = new KraftUserResponse(comUserResponse.getResponseCode(), comUserResponse.getResponseMessage());
            }
            else
            {
                toReturn = new KraftUserResponse(comUserResponse.getResponseCode(), comUserResponse.getResponseMessage());  
            }
        }
        catch (ServerException e)
        {
            logger.error("KraftUserController error in forgotUserNameKraftUser", e);
            toReturn = new KraftUserResponse(e.getErrorCode(), e.getMessage());
        }
        return toReturn;
    }
    
    
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
    	binder.registerCustomEditor(ComUser[].class, new CustomWebEditor<ComUser[]>(ComUser[].class));
        binder.registerCustomEditor(ComUser.class, new CustomWebEditor<ComUser>(ComUser.class));
        binder.registerCustomEditor(UserDescriptorV2.class, new CustomWebEditor<UserDescriptorV2>(UserDescriptorV2.class));
        binder.registerCustomEditor(UserDescriptorIdentityV2.class, new CustomWebEditor<UserDescriptorIdentityV2>(UserDescriptorIdentityV2.class));
        binder.registerCustomEditor(UserSecurityQuestions.class, new CustomWebEditor<UserSecurityQuestions>(UserSecurityQuestions.class));
    }
}
