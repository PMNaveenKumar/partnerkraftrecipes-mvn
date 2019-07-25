/*
 * 
 */
package com.skava.kraft.userv2;

import com.skava.util.helpers.ConfigProperties;
import com.skava.util.helpers.ConfigProperty;
import com.skava.util.helpers.DataType;
import com.skava.util.helpers.PropertyType;

/**
 * The Class Userv2Constants.
 */
@ConfigProperties(value = {
        @ConfigProperty(name = "user.loginattemptcount", description = "No of login attempts allowed", dataType = DataType.INTEGER, defaultValue="10", dynamicReload=false),
		@ConfigProperty(name = "user.encryptsaltvalue", description = "Indicates the Encryption Salt value for constructing the activation link and reset link", dataType = DataType.STRING, defaultValue="skava@infosys12345"),
		@ConfigProperty(name = "customuserauthenticator.classname", description = "Third party authenticator classname user by partners", dataType = DataType.STRING, defaultValue="com.skava.userv2.authimpl.CustomUserAuthenticator", dynamicReload=false),
		@ConfigProperty(name = "userfraudprocessorclass", description = "Fraud check implementation classname", dataType = DataType.STRING, defaultValue="By default in-build User's implementation is used for Fraud Check", dynamicReload=false),
		@ConfigProperty(name = "passwordhashalgorithm", description = "Algorithm class name which is used for hashing password", dataType = DataType.STRING, defaultValue="com.skava.userv2.hash.SkavaPwdHashAlgorithmImpl", dynamicReload=false),
		@ConfigProperty(name = "skavapwdhashworkload", description = "", dataType = DataType.INTEGER, defaultValue="12"),
		@ConfigProperty(name = "skipskavaidentitycreationonlogin", description = ""),
		@ConfigProperty(name = "skipidentitiesforphone", description = "Skips identity creation by provided phone number", dataType = DataType.STRING, allowedValues = {"TRUE", "FALSE"}, defaultValue="false", type=PropertyType.FUNCTIONAL),
		@ConfigProperty(name = "skipemailforresetpasswordvalidation", description = "Skips mail to be sent after password validation success", dataType = DataType.STRING, allowedValues = {"TRUE", "FALSE"}, defaultValue="false", type=PropertyType.FUNCTIONAL)
})
public class Userv2Constants
{

    /** The Constant EVENT_ID_USER_CREATE. */
    public static final String EVENT_ID_USER_CREATE = "100_01";
    
    /** The Constant EVENT_ID_USER_CREATE_OR_GETUSER. */
    public static final String EVENT_ID_USER_CREATE_OR_GETUSER = "100_02";
    
    /** The Constant EVENT_ID_USER_UPDATE. */
    public static final String EVENT_ID_USER_UPDATE = "100_03";
    
    /** The Constant EVENT_ID_USER_GETPROFILE. */
    public static final String EVENT_ID_USER_GETPROFILE = "100_04";
    
    /** The Constant EVENT_ID_USER_STATUS_UPDATE. */
    public static final String EVENT_ID_USER_STATUS_UPDATE = "100_05";
    
    /** The Constant EVENT_ID_USER_GET_SECURITY_QUESTIONS. */
    public static final String EVENT_ID_USER_GET_SECURITY_QUESTIONS = "100_06";
    
    /** The Constant EVENT_ID_USER_CHECK_PWD_STRENGTH. */
    public static final String EVENT_ID_USER_CHECK_PWD_STRENGTH = "100_07";
    
    /** The Constant EVENT_ID_USER_CHECK_USER_EXISTS. */
    public static final String EVENT_ID_USER_CHECK_USER_EXISTS = "100_08";
    
    /** The Constant EVENT_ID_USER_DELETE_IDENTITY. */
    public static final String EVENT_ID_USER_DELETE_IDENTITY = "100_09";
    
    /** The Constant EVENT_ID_USER_DELETE_USER. */
    public static final String EVENT_ID_USER_DELETE_USER = "100_10";
    
    /** The Constant EVENT_ID_USER_GET_TWITTER_TOKEN. */
    public static final String EVENT_ID_USER_GET_TWITTER_TOKEN = "100_11";
    
    /** The Constant EVENT_ID_USER_RESET_USER_PWD. */
    public static final String EVENT_ID_USER_RESET_USER_PWD = "100_12";
    
    /** The Constant EVENT_ID_USER_UPDATE_PWD. */
    public static final String EVENT_ID_USER_UPDATE_PWD = "100_13";
    
    /** The Constant EVENT_ID_UPDATE_STATUS. */
    public static final String EVENT_ID_UPDATE_STATUS = "100_14";
    
    /** The Constant EVENT_ID_USER_VALIDATE_SECURITY_ANSWERS. */
    public static final String EVENT_ID_USER_VALIDATE_SECURITY_ANSWERS = "100_15";
    
    /** The Constant EVENT_ID_USER_VALIDATE_ACTIVATION_EMAIL. */
    public static final String EVENT_ID_USER_VALIDATE_ACTIVATION_EMAIL = "100_16";
    
    /** The Constant EVENT_ID_USER_VALIDATE_RESET_EMAIL. */
    public static final String EVENT_ID_USER_VALIDATE_RESET_EMAIL = "100_17";
    
    /** The Constant EVENT_ID_USER_LOGOUT. */
    public static final String EVENT_ID_USER_LOGOUT = "100_18";
    
    /** The Constant EVENT_ID_USER_VALIDATE_RESET_SMS. */
    public static final String EVENT_ID_USER_VALIDATE_RESET_SMS = "100_19";
    
    /** The Constant EVENT_ID_USER_VALIDATE_ACTIVATION_SMS. */
    public static final String EVENT_ID_USER_VALIDATE_ACTIVATION_SMS = "100_20";
    
    /** The Constant EVENT_ID_USER_SEND_ACTIVATION. */
    public static final String EVENT_ID_USER_SEND_ACTIVATION = "100_21";
    
    /** The Constant EVENT_ID_USER_IMPERSONATE_USER. */
    public static final String EVENT_ID_USER_IMPERSONATE_USER = "100_22";
    
    /** The Constant EVENT_ID_USER_SEND_OTP. */
    public static final String EVENT_ID_USER_SEND_OTP = "100_23";
    
    /** The Constant EVENT_ID_USER_LOGIN. */
    public static final String EVENT_ID_USER_LOGIN = "100_24";
    
    /** The Constant EVENT_ID_USER_GET_SEGMENT_SUGGESTIONS. */
    public static final String EVENT_ID_USER_GET_SEGMENT_SUGGESTIONS = "100_25";
    
    /** The Constant EVENT_ID_USER_CLEAR_SKAVA_USER_COOKIE. */
    public static final String EVENT_ID_USER_CLEAR_SKAVA_USER_COOKIE = "100_26";
    
    /** The Constant EVENT_ID_UNLOCK_USER. */
    public static final String EVENT_ID_UNLOCK_USER = "100_27";
    
    /** The Constant EVENT_ID_GET_USER_ID. */
    public static final String EVENT_ID_GET_USER_ID = "100_28";
    
    /** The Constant EVENT_ID_CHECK_USER_EXISTS. */
    public static final String EVENT_ID_CHECK_USER_EXISTS = "100_29";
    
    /** The Constant EVENT_ID_CREATE_USER_PROPERTIES. */
    public static final String EVENT_ID_CREATE_USER_PROPERTIES = "100_30";
    
    /** The Constant EVENT_ID_UPDATE_USER_PROPERTIES. */
    public static final String EVENT_ID_UPDATE_USER_PROPERTIES = "100_31";
    
    /** The Constant EVENT_ID_SEND_RESET_TOKEN. */
    public static final String EVENT_ID_SEND_RESET_TOKEN = "100_32";
    
    /** The Constant EVENT_ID_MERGE_USER_WITH_COOKIE_USER. */
    public static final String EVENT_ID_MERGE_USER_WITH_COOKIE_USER = "100_33";
    
    /** The Constant EVENT_ID_SEND_USER_ACTIVATION_TOKEN. */
    public static final String EVENT_ID_SEND_USER_ACTIVATION_TOKEN = "100_34";
    
    /** The Constant EVENT_ID_SEND_EMAIL_RESET_LINK. */
    public static final String EVENT_ID_SEND_EMAIL_RESET_LINK = "100_35";
    
    /** The Constant EVENT_ID_SEND_MAIL_FOR_USER_UPDATION. */
    public static final String EVENT_ID_SEND_MAIL_FOR_USER_UPDATION = "100_36";
    
    /** The Constant EVENT_ID_USER_LOOKUP. */
    public static final String EVENT_ID_USER_LOOKUP = "100_37";
        
    /** The Constant HEADER_FINGERPRINT. */
    public static final String HEADER_FINGERPRINT = "X-Skava-Fingerprint";
    
    /** The Constant SKAVA_USER_SECRET_KEY. */
    public static final String SKAVA_USER_SECRET_KEY = "skavaUserSecretKey";
    
    /** The Constant CONSTRAINT_PASSWORD_MINIMUM_LENGTH. */
    public static final int CONSTRAINT_PASSWORD_MINIMUM_LENGTH = 6;
    
    /** The Constant CONSTRAINT_PASSWORD_MAXIMUM_LENGTH. */
    public static final int CONSTRAINT_PASSWORD_MAXIMUM_LENGTH = 20;
    
    /** The Constant CARD_NUMBER_LENGTH. */
    public static final int CARD_NUMBER_LENGTH = 16;

    /** The Constant ACTIVATION_EXPIRY_MILLISECS. */
    public static final long ACTIVATION_EXPIRY_MILLISECS = 3600000L; // 1 hour
    
    /** The Constant ACTIVATION_EXPIRY_MILLISECS_OTP. */
    public static final long ACTIVATION_EXPIRY_MILLISECS_OTP = 300000L; // 5 minutes
    
    /** The Constant COMMON_EXPIRY_MILLISECS_OTP. */
    public static final long COMMON_EXPIRY_MILLISECS_OTP = 600000L; // 10 minutes
    
    /** The Constant LOGIN_ONBEHALF_TIMESPAN. */
    public static final long LOGIN_ONBEHALF_TIMESPAN = 600000L; // 10 minutes
    
    /** The Constant RESET_PASSWORD_EXPIRY_MILLISECS. */
    public static final long RESET_PASSWORD_EXPIRY_MILLISECS = 600000L;
    
    /** The Constant RESET_EXPIRY_MILLISECS. */
    public static final long RESET_EXPIRY_MILLISECS = 3600000L; // 1 hour
    
    /** The Constant RESET_EXPIRY_MILLISECS_OTP. */
    public static final long RESET_EXPIRY_MILLISECS_OTP = 300000L; // 5 minutes

    /** The Constant EMAIL_MACRO_USER_NAME. */
    public static final String EMAIL_MACRO_USER_NAME = "USERNAME";
    
    /** The Constant EMAIL_MACRO_USER_FIRST_NAME. */
    public static final String EMAIL_MACRO_USER_FIRST_NAME = "USERFIRSTNAME";
    
    /** The Constant EMAIL_MACRO_USER_LAST_NAME. */
    public static final String EMAIL_MACRO_USER_LAST_NAME = "USERLASTNAME";
    
    /** The Constant EMAIL_MACRO_RESET_PARAM. */
    public static final String EMAIL_MACRO_RESET_PARAM = "RESETPARAM";
    
    /** The Constant EMAIL_MACRO_USER_EMAIL. */
    public static final String EMAIL_MACRO_USER_EMAIL = "USEREMAIL";
    
    /** The Constant EMAIL_MACRO_ACTIVATION_PARAM. */
    public static final String EMAIL_MACRO_ACTIVATION_PARAM = "ACTIVATIONPARAM";
    
    /** The Constant EMAIL_CHANGE_MACRO_USER_NAME. */
    public static final String EMAIL_CHANGE_MACRO_USER_NAME = "USERNAME";
    
    /** The Constant EMAIL_CHANGE_OLD_EMAIL. */
    public static final String EMAIL_CHANGE_OLD_EMAIL = "USEROLDEMAIL";
    
    /** The Constant EMAIL_CHANGE_NEW_EMAIL. */
    public static final String EMAIL_CHANGE_NEW_EMAIL = "USERNEWEMAIL";

    /** The Constant OTP_MACRO_USER_NAME. */
    public static final String OTP_MACRO_USER_NAME = "USERNAME";
    
    /** The Constant OTP_MACRO_USER_PHONENUMBER. */
    public static final String OTP_MACRO_USER_PHONENUMBER = "PHONENUMBER";
    
    /** The Constant OTP_MACRO_USER_ACTIVATION_SHORTENED_URL. */
    public static final String OTP_MACRO_USER_ACTIVATION_SHORTENED_URL = "ACTIVATIONSHORTENEDURL";
    
    /** The Constant OTP_MACRO_USER_RESET_SHORTENED_URL. */
    public static final String OTP_MACRO_USER_RESET_SHORTENED_URL = "RESETSHORTENEDURL";
    
    /** The Constant OTP_MACRO_USER_CODE. */
    public static final String OTP_MACRO_USER_CODE = "CODE";
    
    /** The Constant INVALID_DECRYPT_VALUE. */
    public static final String INVALID_DECRYPT_VALUE = "CryptoUtil decrypt - Invalid Data";

    /** The Constant SEARCH_TYPE_EMAIL. */
    public static final String SEARCH_TYPE_EMAIL = "email";
    
    /** The Constant SEARCH_TYPE_PHONE. */
    public static final String SEARCH_TYPE_PHONE = "phone";
    
    /** The Constant SEARCH_TYPE_NAME. */
    public static final String SEARCH_TYPE_NAME = "name";
    
    /** The Constant SEARCH_TYPE_FNAME. */
    public static final String SEARCH_TYPE_FNAME = "fname";
    
    /** The Constant SEARCH_TYPE_LNAME. */
    public static final String SEARCH_TYPE_LNAME = "lname";
    
    /** The Constant SEARCH_TYPE_STATE. */
    public static final String SEARCH_TYPE_STATE = "state";
    
    /** The Constant SEARCH_TYPE_CITY. */
    public static final String SEARCH_TYPE_CITY = "city";
    
    /** The Constant SEARCH_TYPE_USERID. */
    public static final String SEARCH_TYPE_USERID = "userid";

    /** The Constant STREAMCOM_VERSION. */
    public static final String STREAMCOM_VERSION = "v7";
    
    /** The Constant COOKIE_VALUE_SPLITTER. */
    public static final String COOKIE_VALUE_SPLITTER = "~";
    
    /** The Constant COOKIE_SPLITTER_LENGTH. */
    public static final int COOKIE_SPLITTER_LENGTH = 4;
    
    /** The Constant COOKIE_CUST_JOURNAL_ADMIN_USER. */
    public static final String COOKIE_CUST_JOURNAL_ADMIN_USER = "ckcjeadmin";
    
    /** The Constant COOKIE_CUST_JOURNAL_END_USER. */
    public static final String COOKIE_CUST_JOURNAL_END_USER = "ckcjeu";
    
    /** The Constant COOKIE_CUST_JOURNAL_END_USER_STATUS. */
    public static final String COOKIE_CUST_JOURNAL_END_USER_STATUS = "ckcjeustat";
    
    /** The Constant COOKIE_STATUS_ACTIVE. */
    public static final int COOKIE_STATUS_ACTIVE = 1;
    
    /** The Constant COOKIE_STATUS_NOT_ACTIVE. */
    public static final int COOKIE_STATUS_NOT_ACTIVE = 0;
    
    /** The Constant COOKIE_SECURE. */
    public static final String COOKIE_SECURE = "https";
    
    /** The Constant STRING_USER_RETRIVE_CARD. */
    public static final String STRING_USER_RETRIVE_CARD = "user_retrieve_card";
    
    /** The Constant STRING_AN_ERROR_HAS_OCCURRED. */
    public static final String STRING_AN_ERROR_HAS_OCCURRED = "An error has occurred.";

    /** The Constant TWO_YEAR_IN_SECONDS. */
    public static final int TWO_YEAR_IN_SECONDS = 63072000; // Two years
    
    /** The Constant ONEHOUR_IN_SECONDS. */
    public static final int ONEHOUR_IN_SECONDS = 3600; // 1 hour
    
    /** The Constant ONE_MONTH_IN_SECONDS. */
    public static final int ONE_MONTH_IN_SECONDS = 2592000; // one month
    
    /** The Constant LOGIN_EXPIRY_THRESHOLD_DEFAULT. */
    public static final long LOGIN_EXPIRY_THRESHOLD_DEFAULT = 300000L;

    /** The Constant DEFAULT_ENCRYPTION_SALE_VALUE. */
    public static final String DEFAULT_ENCRYPTION_SALE_VALUE = "SK@V@SALT1234567";
    
    /** The Constant DEFAULT_NOTIFICATION_PER_USER_PER_DAY. */
    public static final int DEFAULT_NOTIFICATION_PER_USER_PER_DAY = 5;
    
    /** The Constant UNLOCK_COUNT_PER_USER. */
    public static final int UNLOCK_COUNT_PER_USER = 3;

    /** The Constant NOTIFICATION_TIME_FORMAT. */
    public static final String NOTIFICATION_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    /** The Constant NOTIFICATION_TIME_ZONE. */
    public static final String NOTIFICATION_TIME_ZONE = "GMT";

    /** The Constant RET_PARAM_LOGIN. */
    public static final String RET_PARAM_LOGIN = "LOGIN";
    
    /** The Constant RET_PARAM_CREATE. */
    public static final String RET_PARAM_CREATE = "CREATE";
    
    /** The Constant RET_PARAM_UPDATE. */
    public static final String RET_PARAM_UPDATE = "UPDATE";

    /** The Constant DEFAULT_PASSWORD_VALIDATOR_CLASS. */
    public static final String DEFAULT_PASSWORD_VALIDATOR_CLASS = "com.skava.userv2.authimpl.DefaultPasswordValidatorImpl";
    
    /** The Constant PARAM_CAMPAIGN_ID. */
    public static final String PARAM_CAMPAIGN_ID = "campaignId";
    
    /** The Constant PARAM_STORE_ID. */
    public static final String PARAM_STORE_ID = "storeId";
    
    /** The Constant PARAM_TYPE_USER. */
    public static final String PARAM_TYPE_USER = "user";
    
    /** The Constant PARAM_MESSAGE_CAMPAIGN_ID. */
    public static final String PARAM_MESSAGE_CAMPAIGN_ID = "messageCampaignId";
    
    /** The Constant PARAM_VALIDATION_METHOD. */
    public static final String PARAM_VALIDATION_METHOD = "validationMethod";
    
    /** The Constant PARAM_PARTNER. */
    public static final String PARAM_PARTNER = "partner";
    
    /** The Constant PARAM_LOCALE. */
    public static final String PARAM_LOCALE = "locale";
    
    /** The Constant PARAM_USER. */
    public static final String PARAM_USER = "user";
    
    /** The Constant PARAM_MESSAGING_DESCRIPTOR. */
    public static final String PARAM_MESSAGING_DESCRIPTOR = "messagingDescriptor";
    
    /** The Constant PARAM_ENABLE_PAYMENT_RETRIVAL. */
    public static final String PARAM_ENABLE_PAYMENT_RETRIVAL = "enablePaymentRetrival";
    
    /** The Constant PARAM_CV. */
    public static final String PARAM_CV = "cv";
    
    /** The Constant PARAM_CID. */
    public static final String PARAM_CID = "cid";
    
    /** The Constant PARAM_IS_SUPERADMIN. */
    public static final String PARAM_IS_SUPERADMIN = "isSuperAdmin";
    
    /** The Constant PARAM_USER_ID. */
    public static final String PARAM_USER_ID = "userId";
    
    /** The Constant PARAM_OTP_FUNCTION. */
    public static final String PARAM_OTP_FUNCTION = "otpFunction";
    
    /** The Constant PARAM_NOTIFICATION_METHOD. */
    public static final String PARAM_NOTIFICATION_METHOD = "notificationMethod";
    
    /** The Constant PARAM_CHANNEL. */
    public static final String PARAM_CHANNEL = "channel";
    
    /** The Constant PARAM_SEGMENTS. */
    public static final String PARAM_SEGMENTS = "segments";
    
    /** The Constant PARAM_OTP. */
    public static final String PARAM_OTP = "otp";
    
    /** The Constant PARAM_ACTIVATION_PARAM. */
    public static final String PARAM_ACTIVATION_PARAM = "activationParam";
    
    /** The Constant PARAM_RESETPARAM. */
    public static final String PARAM_RESETPARAM = "resetParam";
    
    /** The Constant PARAM_ADMINDESC. */
    public static final String PARAM_ADMINDESC = "admindesc";
    
    /** The Constant PARAM_PWD. */
    public static final String PARAM_PWD = "password";
    
    /** The Constant PARAM_RESET_PARAM_TYPE. */
    public static final String PARAM_RESET_PARAM_TYPE = "resetparamType";
    
    /** The Constant PARAM_PARTNER_ID. */
    public static final String PARAM_PARTNER_ID = "partnerId";
    
    /** The Constant PARAM_CACHE_NAME. */
    public static final String PARAM_CACHE_NAME = "cacheName";
    
    /** The Constant PARAM_SEND_WELCOME_EMAIL. */
    public static final String PARAM_SEND_WELCOME_EMAIL = "sendWelcomeEmail";
    
    /** The Constant PARAM_PROMOCODE. */
    public static final String PARAM_PROMOCODE = "promocode";
    
    /** The Constant PARAM_PROMO_CAMPAIGN. */
    public static final String PARAM_PROMO_CAMPAIGN = "promocampaign";
    
    /** The Constant PARAM_SKIP_VALIDATIONS. */
    public static final String PARAM_SKIP_VALIDATIONS = "skipValidations";
    
    /** The Constant PARAM_SKIP_PASSWORD. */
    public static final String PARAM_SKIP_PASSWORD = "skipPassword";
    
    /** The Constant PARAM_LINK_BADDR_TO_CARD. */
    public static final String PARAM_LINK_BADDR_TO_CARD = "skipPassword";
    
    /** The Constant PARAM_RESET_METHOD. */
    public static final String PARAM_RESET_METHOD = "resetMethod";
    
    /** The Constant PARAM_IDENTITY_ID. */
    public static final String PARAM_IDENTITY_ID = "identityId";
    
    /** The Constant PARAM_TYPE. */
    public static final String PARAM_TYPE = "type";
    
    /** The Constant PARAM_ACCESS_TOKEN. */
    public static final String PARAM_ACCESS_TOKEN = "accessToken";
    
    /** The Constant PARAM_STATUS. */
    public static final String PARAM_STATUS = "status";
    
    /** The Constant PARAM_SEND_ACTIVATION. */
    public static final String PARAM_SEND_ACTIVATION = "sendActivation";
    
    /** The Constant PARAM_SEARCH_TYPE. */
    public static final String PARAM_SEARCH_TYPE = "searchType";
    
    /** The Constant PARAM_SEARCH_PARAM. */
    public static final String PARAM_SEARCH_PARAM = "searchParam";
    
    /** The Constant PARAM_ONLY_ACTIVE. */
    public static final String PARAM_ONLY_ACTIVE = "onlyActive";
    
    /** The Constant PARAM_OFFSET. */
    public static final String PARAM_OFFSET = "offset";
    
    /** The Constant PARAM_LIMIT. */
    public static final String PARAM_LIMIT = "limit";
    
    /** The Constant PARAM_FROM_TIME_STAMP. */
    public static final String PARAM_FROM_TIME_STAMP = "fromtimestamp";
    
    /** The Constant PARAM_TO_TIME_STAMP. */
    public static final String PARAM_TO_TIME_STAMP = "totimestamp";
    
    /** The Constant PARAM_SKIP_GUEST_USERS. */
    public static final String PARAM_SKIP_GUEST_USERS = "skipGuestUsers";
    
    /** The Constant LOCALE_EN_US. */
    public static final String LOCALE_EN_US = "en_US";
    
    /** The Constant USER_SESION_LOAD_MAX_LIMIT. */
    public static final long USER_SESION_LOAD_MAX_LIMIT = 5000L;
    
    /** The Constant SKIP_SKAVAIDENTIY_CRAETION_ON_LOGIN. */
    public static final String SKIP_SKAVAIDENTIY_CRAETION_ON_LOGIN = "skipskavaidentitycreationonlogin";
    
    public static final String EVENTID_API_AUTHENTICATE = "AUTHENTICATE";
    
    public static final String EVENTID_API_CREATEORGETUSER = "CREATEORGETUSER";
    
    public static final String EVENTID_API_CREATEORGETUSERADMINUSER = "CREATEORGETADMINUSER";
    
    public static final String EVENTID_API_CREATEUSER = "CREATEUSER";
    
    public static final String EVENTID_API_LOGIN = "LOGIN";
    
    public static final String EVENTID_API_GETPROFILE = "GETPROFILE";
    
    public static final String EVENTID_API_UPDATEUSER = "UPDATEUSER";
    
    public static final String EVENTID_API_RESETPASSEORD = "RESETPASSWORD";
    
    public static final String EVENTID_API_CHECKUSEREXIST = "CHECKUSEREXIST";
    
    public static final String EVENTID_API_GETSEQURITYQUESTIONS = "GETSEQURITYQUESTION";
    
    public static final String EVENTID_API_UPDATEPASSWORD = "UPDATEPASSWORD";
    
    public static final String EVENTID_API_VALIDATESEQURITYANSWERS = "VALIDATESEQURITYANSWERS";
    
    public static final String EVENTID_API_DELETEUSER = "DELETEUSER";
    
    public static final String EVENTID_API_DELETEIDENTITY = "DELETEIDENTITY";
    
    public static final String EVENTID_API_CHECKPASSWORDSTRENGTH = "CHECKPASSWORDSTRENGTH";
    
    public static final String EVENTID_API_SENDACTIVATION = "SENDACTIVATION";
    
    public static final String EVENTID_API_VALIDATEACTIVATIONEMAIL = "VALIDATEACTIVATIONEMAIL";
    
    public static final String EVENTID_API_VALIDATERESETEMAIL = "VALIDATERESETEMAIL";
    
    public static final String EVENTID_API_VALIDATEACTIVATIONSMS = "VALIDATEACTIVATIONSMS";
    
    public static final String EVENTID_API_VALIDATERESETSMS = "VALIDATERESETSMS";
    
    public static final String EVENTID_API_IMPERSONATEUSER = "IMPERSONATEUSER";
    
    public static final String EVENTID_API_GETSEGMENTSUGGESTIONS = "GETSEGMENTSUGGESIONS";
    
    public static final String EVENTID_API_SENDONETIMEPASSWORD = "SENDONETIMEPASSWORD";
    
    public static final String EVENTID_API_LOGOUT = "LOGOUT";
    
    public static final String EVENTID_API_CLEARSKAVAUSERCOOKIE = "CLEARSKAVAUSERCOOKIE";
    
    public static final String EVENTID_API_UNLOCKUSER = "UNLOCKUSER";
    
    public static final String EVENTID_API_PRIVILEGECREATE= "PRIVILEGECREATE";
    
    public static final String EVENTID_API_PRIVILEGEUPDATE= "PRIVILEGEUPDATE";
    
    public static final String EVENTID_API_PRIVILEGESETCREATE = "PRIVILEGESETCREATE";
    
    public static final String EVENTID_API_PRIVILEGESETUPDATE = "PRIVILEGESETUPDATE";
    
    public static final String EVENTID_API_CHECKPRIVILEGE = "CHECKPRIVILEGE";
    
    public static final String EVENTID_API_CHECKPRIVILEGESET = "CHECKPRIVILEGESET";
    
    public static final String EVENTID_API_ROLECREATE = "ROLECREATE";
    
    public static final String EVENTID_API_ROLEUPDATE = "ROLEUPDATE";
    
    public static final String EVENTID_API_FINDUSER = "FINDUSER";
    
    public static final String EVENTID_API_LOADUSER = "LOADUSER";
    
    public static final String EVENTID_API_LOGINONBEHALFOF = "LOGINONBEHALFOF";
    
    public static final String EVENTID_API_LOGOUTONBEHALFOF = "LOGOUTONBEHALFOF";
    
    public static final String EVENTID_API_UPDATEUSERSTATUS = "UPDATEUSERSTATUS";
    
    public static final String EVENTID_API_GETALLBYUSERSTIMESTAMP = "GETALLBYUSERSTIMESTAMP";
    
    public static final String EVENTID_API_GETALLBYUSERSESSIONS = "GETALLBYUSERSESSIONS";
    
    public static final String EVENTID_API_CLEARALLCACHE = "CLEARALLCACHE";
    
    public static final String EVENTID_API_CHECKSOFTLOGINSTATE = "CHECKSOFTLOGINSTATE";
    
    public static final String EVENTID_API_CREATEPRIVILEGE = "CREATEPRIVILEGE";
    
    public static final String EVENTID_API_CREATEPRIVILEGESET = "CREATEPRIVILEGESET";
    
    public static final String EVENTID_API_CREATEROLE = "CREATEROLE";
    
    public static final String EVENTID_API_GETTWITTERTOKEN = "GETWITTERTOKEN";
    
    public static final String EVENTID_API_SENDACTIVATIONMAIL = "SENDACTIVATIONMAIL";
    
    public static final String EVENTID_API_SENDUPDATEMAIL = "SENDUPDATEMAIL";
    
    public static final String EVENTID_API_UPDATEPRIVILEGE = "UPDATEPRIVILEGE";
    
    public static final String EVENTID_API_UPDATEPRIVILEGESET = "UPDATEPRIVILEGESET";
    
    public static final String EVENTID_API_UPDATEROLE = "UPDATEROLE";
    
    public static final String EVENTID_API_USERLOOKUP = "USERLOOKUP";
    
    public static final String EVENTID_API_USERPREVALIDATION = "USERPREVALIDATION";
    
    public static final String EVENTID_API_VALIDATECAPTCHA= "VALIDATECAPTCHA";
    
    
    public static final String EVENTID_WORKITEM_GETPROFILE = "GETPROFILE";
    
    public static final String EVENTID_WORKITEM_USERPREVALIDATION = "USERPREVALIDATION";
    
    public static final String EVENTID_WORKITEM_AUTHENTICATEUSER = "AUTHENTICATEUser";
    
    public static final String EVENTID_WORKITEM_CHECKPASSWORDSTRENGTH = "CHECKPASSWORDSTRENGTH";
    
    public static final String EVENTID_WORKITEM_CHECKPRIVILEGE = "CHECKPRIVILEGE";
    
    public static final String EVENTID_WORKITEM_CHECKPRIVILEGESET = "CHECKPRIVILEGESET";
    
    public static final String EVENTID_WORKITEM_CHECKSOFTLOGINSTATE = "CHECKSOFTLOGINSTATE";
    
    public static final String EVENTID_WORKITEM_CHECKUSEREXISTS = "CHECKUSEREXISTS";
    
    public static final String EVENTID_WORKITEM_CLEARSKAVAUSERCOOKIE = "CLEARSKAVEUSERCOOKIE";
    
    public static final String EVENTID_WORKITEM_CREATEORGETUSER = "CREATEORGETUSER";
    
    public static final String EVENTID_WORKITEM_CREATEPRIVILEGE = "CREATEPRIVILEGE";
    
    public static final String EVENTID_WORKITEM_CREATEPRIVILEGESET = "CREATEPRIVILEGESET";
    
    public static final String EVENTID_WORKITEM_CREATEROLE= "CREATEROLE";
    
    public static final String EVENTID_WORKITEM_CREATEUSER = "CREATEUSER";
    
    public static final String EVENTID_WORKITEM_DELETEIDENTITY = "DELETEIDENTITY";
    
    public static final String EVENTID_WORKITEM_DELETEUSER = "DELETEUSER";
    
    public static final String EVENTID_WORKITEM_GETLOCALIZEDMESSAGE = "GETLOCALIZEDMESSAGE";
    
    public static final String EVENTID_WORKITEM_GETSECURITYQUESTIONS = "GETSECURITYQUESTIONS";
    
    public static final String EVENTID_WORKITEM_GETSEGMENTSUGGESTIONS = "GETSEGMENTSUGGESTIONS";
    
    public static final String EVENTID_WORKITEM_GETTWITTERTOKEN = "GETTWITTERTOKEN";
    
    public static final String EVENTID_WORKITEM_IMPERSONATEUSER = "IMPERSONATEUSER";
    
    public static final String EVENTID_WORKITEM_LOGINUSER = "LOGINUSER";
    
    public static final String EVENTID_WORKITEM_RESETPASSWORD = "RESETPASSWORD";
    
    public static final String EVENTID_WORKITEM_SENDACTIVATION = "SENDACTIVATION";
    
    public static final String EVENTID_WORKITEM_SENDACTIVATIONMAIL = "SENDACTIVATIONMAIL";
    
    public static final String EVENTID_WORKITEM_SENDONETIMEPASSWORD = "SENDONETIMEPASSWORD";
  
    public static final String EVENTID_WORKITEM_SENDUPDATEEMAIL = "SENDUPDATEEMAIL";
    
    public static final String EVENTID_WORKITEM_UNLOCKUSER = "UNLOCKUSER";
    
    public static final String EVENTID_WORKITEM_UPDATEPASSWORD = "UPDATEPASSWORD";
    
    public static final String EVENTID_WORKITEM_UPDATEPRIVILEGE = "UPDATEPRIVILEGE";
    
    public static final String EVENTID_WORKITEM_UPDATEPRIVILEGESET = "UPDATEPRIVILEGESET";
    
    public static final String EVENTID_WORKITEM_UPDATEROLE = "UPDATEROLE";
    
    public static final String EVENTID_WORKITEM_UPDATESTATUS = "UPDATESTATUS";
    
    public static final String EVENTID_WORKITEM_UPDATEUSER = "UPDATEUSER";
    
    public static final String EVENTID_WORKITEM_USERLOOKUP = "USERLOOKUP";
    
    public static final String EVENTID_WORKITEM_VALIDATEACTIVATIONMAIL = "VALIDATEACTIVATIONMAIL";
    
    public static final String EVENTID_WORKITEM_VALIDATEACTIVATIONSMS = "VALIDATEACTIVATIONSMS";
    
    public static final String EVENTID_WORKITEM_VALIDATECAPTCHA = "VALIDATECAPTCHA";
    
    public static final String EVENTID_WORKITEM_VALIDATERESETEMAIL = "VALIDATERESETEMAIL";
    
    public static final String EVENTID_WORKITEM_VALIDATERESETSMS = "VALIDATERESETSMS";
    
    public static final String EVENTID_WORKITEM_VALIDATESECURITYANSWERS = "VALIDATESECURITYANSWERS";
    
    public static final String EVENTID_WORKITEM_FINDUSER = "FINDUSER";
    
    public static final String EVENTID_WORKITEM_GETALLUSERSESSIONS = "GETALLUSERSESSIONS";
    
    public static final String EVENTID_WORKITEM_LOADUSER = "LOADUSER";
    
    public static final String EVENTID_WORKITEM_LOGINONBEHALFOF= "LOGINONBEHALFOF";
    
    public static final String EVENTID_WORKITEM_LOGOUTONBEHALFOF= "LOGOUTONBEHALFOF";
    
    /** The Constant ERRORTYPE_CONFIGURATION. */
    //Error Logger
    public static final String ERRORTYPE_CONFIGURATION = "configuration error";
    
    /** The Constant ERRORTYPE_INPUT. */
    public static final String ERRORTYPE_INPUT = "input error";
    
    /** The Constant ERRORTYPE_LOGICAL. */
    public static final String ERRORTYPE_LOGICAL = "logical error";
    
    /** The Constant PARAM_USERFRAUD_PROCESSOR. */
    public static final String PARAM_USERFRAUD_PROCESSOR = "userfraudprocessor";
    
    /** The Constant TYPE_CREATE_USER. */
    public static final String TYPE_CREATE_USER = "createuser";
    
    /** The Constant TYPE_LOGIN_USER. */
    public static final String TYPE_LOGIN_USER = "loginuser";
    
    /** The Constant CREATE_USER_CAPTCHATEXT. */
    public static final String CREATE_USER_CAPTCHATEXT = "createuser_captchatext";
    
    /** The Constant LOGIN_USER_CAPTCHATEXT. */
    public static final String LOGIN_USER_CAPTCHATEXT = "loginuser_captchatext";
    
    /** The Constant USER_LOGIN_ATTEMPT_COUNT. */
    public static final String USER_LOGIN_ATTEMPT_COUNT = "userloginattemptcount";
    
    /** The Constant USER_LOGIN_ATTEMPT_COUNT. */
    public static final String ZOOKEEPER_PROP_MAX_LOGIN_ATTEMPT_COUNT = "kraft.maxloginattempt";
    
    /** The Constant KRAFTRECIPE_IDENTITY_VALUE. */
    public static final String KRAFTRECIPE_IDENTITY_VALUE = "kraftrecipe.identityvalue";
}
