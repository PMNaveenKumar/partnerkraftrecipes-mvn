/*
 * 
 */
package com.skava.builder.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import com.skava.builder.interfaces.AWSCredentialsServiceBuilder;
import com.skava.builder.interfaces.AWSUtilServiceBuilder;
import com.skava.builder.interfaces.AmazonS3ClientServiceBuilder;
import com.skava.builder.interfaces.AmazonSNSClientServiceBuilder;
import com.skava.builder.interfaces.AmazonSQSClientServiceBuilder;
import com.skava.builder.interfaces.ApiTokenPropertiesServiceBuilder;
import com.skava.builder.interfaces.DBSessionManagerServiceBuilder;
import com.skava.builder.interfaces.HttpClientServiceBuilder;
import com.skava.builder.interfaces.JBPMServiceBuilder;
import com.skava.builder.interfaces.MemCacheManagerServiceBuilder;
import com.skava.builder.interfaces.MessageServiceBuilder;
import com.skava.builder.interfaces.ReCaptchaImageCaptchaServiceBuilder;
import com.skava.builder.interfaces.SharedSecretsMapServiceBuilder;
import com.skava.builder.interfaces.SimpleImageAndAudioCaptchaServiceBuilder;
import com.skava.builder.interfaces.SkavaCaptchaFactoryServiceBuilder;
import com.skava.builder.interfaces.SkavaCaptchaServiceBuilder;
import com.skava.builder.interfaces.SkavaLoggerServiceBuilder;
import com.skava.builder.interfaces.SkavaResourceBundleServiceBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.SkavaUnicolorCaptchaServiceBuilder;
import com.skava.builder.interfaces.StreamComServiceKraftBuilder;
import com.skava.builder.interfaces.StreamComUserServiceBuilder;
import com.skava.builder.interfaces.StreamUserV2MergeListnerServiceBuilder;
import com.skava.builder.interfaces.StreamUserV2ServiceBuilder;
import com.skava.builder.interfaces.URLShortenServiceBuilder;
import com.skava.builder.interfaces.UserV2MergeHandlerServiceBuilder;
import com.skava.builder.interfaces.ZookeeperManagerServiceBuilder;
import com.skava.builder.remote.interfaces.APIAdminRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.SkavaKeystoreRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.SkavaMessagingRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamCartRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamListRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamPartnerRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.URLShortenRemoteServiceBuilder;
import com.skava.cache.MemCacheManager;
import com.skava.kraft.userv2.Userv2Constants;
import com.skava.model.Tenant;
import com.skava.model.TenantThreadLocal;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;

import lombok.Getter;
import lombok.Setter;


/**
 * The Class UserTenantContextFactoryImpl.
 */
public class UserKraftTenantContextFactoryImpl extends SkavaTenantContextFactory
{
    
    /** The logger. */
    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());

    /** The Constant SERVEREXCEPTION_NULLPOINTER. */
    private static final String SERVEREXCEPTION_NULLPOINTER = "Null Pointer Exception Occurred While Processing on SkavaTenantContextFactoryImpl.";

    /** The skava tenant context factory list. */
    @Setter @Getter public List<Object> skavaTenantContextFactoryList;

    /** The zookeeper manager service builder. */
    /* for code readability please put all *ServiceBuilder instance objects below */
    static ZookeeperManagerServiceBuilder zookeeperManagerServiceBuilder;
    
    /** The skava resource bundle service builder. */
    SkavaResourceBundleServiceBuilder skavaResourceBundleServiceBuilder;
    
    /** The skava logger service builder. */
    SkavaLoggerServiceBuilder skavaLoggerServiceBuilder;
    
    /** The mem cache manager service builder. */
    MemCacheManagerServiceBuilder memCacheManagerServiceBuilder;
    
    /** The db session manager service builder. */
    DBSessionManagerServiceBuilder dbSessionManagerServiceBuilder;
    
    /** The jbpm service builder. */
    JBPMServiceBuilder jbpmServiceBuilder;
    
    /** The api token properties service builder. */
    ApiTokenPropertiesServiceBuilder apiTokenPropertiesServiceBuilder;
    
    /** The http client service builder. */
    HttpClientServiceBuilder httpClientServiceBuilder;
    
    /** The stream user V 2 merge listner service builder. */
    StreamUserV2MergeListnerServiceBuilder streamUserV2MergeListnerServiceBuilder;
    
    /** The user V 2 merge handler service builder. */
    UserV2MergeHandlerServiceBuilder userV2MergeHandlerServiceBuilder;
    
    /** The skava captcha factory service builder. */
    SkavaCaptchaFactoryServiceBuilder skavaCaptchaFactoryServiceBuilder;
    
    /** The amazon S 3 client service builder. */
    AmazonS3ClientServiceBuilder amazonS3ClientServiceBuilder;
    
    /** The aws credentials service builder. */
    AWSCredentialsServiceBuilder awsCredentialsServiceBuilder;
    
    /** The amazon SQS client service builder. */
    AmazonSQSClientServiceBuilder amazonSQSClientServiceBuilder;
    
    /** The amazon SNS client service builder. */
    AmazonSNSClientServiceBuilder amazonSNSClientServiceBuilder;
    
    /** The aws util service builder. */
    AWSUtilServiceBuilder awsUtilServiceBuilder;
    
    /** The simple image and audio captcha service builder. */
    SimpleImageAndAudioCaptchaServiceBuilder simpleImageAndAudioCaptchaServiceBuilder;
    
    /** The re captcha image captcha service builder. */
    ReCaptchaImageCaptchaServiceBuilder reCaptchaImageCaptchaServiceBuilder;
    
    /** The skava captcha service builder. */
    SkavaCaptchaServiceBuilder skavaCaptchaServiceBuilder;
    
    /** The skava unicolor captcha service builder. */
    SkavaUnicolorCaptchaServiceBuilder skavaUnicolorCaptchaServiceBuilder;
    
    /** The stream com user service builder. */
    StreamComUserServiceBuilder streamComUserServiceBuilder;
    
    
    /** The stream user V 2 service builder. */
    StreamUserV2ServiceBuilder streamUserV2ServiceBuilder;
    
    /** The shared secrets map service builder. */
    SharedSecretsMapServiceBuilder sharedSecretsMapServiceBuilder;
    MessageServiceBuilder messageServiceBuilder;

    /** The skava keystore remote service builder. */
    /* for code readability please put all *RemoteServiceBuilder instance objects below */
    SkavaKeystoreRemoteServiceBuilder skavaKeystoreRemoteServiceBuilder;
    
    /** The api admin remote service builder. */
    APIAdminRemoteServiceBuilder apiAdminRemoteServiceBuilder;
    
    /** The skava messaging remote service builder. */
    SkavaMessagingRemoteServiceBuilder skavaMessagingRemoteServiceBuilder;
    
    /** The url shorten remote service builder. */
    URLShortenRemoteServiceBuilder urlShortenRemoteServiceBuilder;
    
    /** The stream list remote service builder. */
    StreamListRemoteServiceBuilder streamListRemoteServiceBuilder;
    
    /** The stream cart remote service builder. */
    StreamCartRemoteServiceBuilder streamCartRemoteServiceBuilder;
    
    /** The stream partner remote service builder. */
    StreamPartnerRemoteServiceBuilder streamPartnerRemoteServiceBuilder;
    
    /** The stream com user kraft service builder. */
    StreamComServiceKraftBuilder streamComServiceKraftBuilder;
    
    /** The exec builder method. */
    private static Set<String> execBuilderMethod = new LinkedHashSet<>();

    /**
     * Instantiates a new user tenant context factory impl.
     *
     * @param userTenantContextFactory the skava tenant context factory list
     * @throws Exception the exception
     */
    public UserKraftTenantContextFactoryImpl(UserTenantContextFactoryImpl userTenantContextFactory, StreamComKraftUserServiceBuilderImpl streamComServiceKraftBuilder) throws Exception
    {
        super(zookeeperManagerServiceBuilder);
        this.skavaTenantContextFactoryList = userTenantContextFactory.getSkavaTenantContextFactoryList();
        this.skavaTenantContextFactoryList.add(streamComServiceKraftBuilder);
        initAllSpringBuilders();
    }

    /**
     * Inits the all spring builders.
     *
     * @throws ServerException the server exception
     */
    void initAllSpringBuilders() throws ServerException
    {
        if (skavaTenantContextFactoryList != null && !skavaTenantContextFactoryList.isEmpty())
        {
            for (Object object : skavaTenantContextFactoryList)
            {
                initRemoteBuilder(object); // Note: Don't if check *RemoteServiceBuilder builders below. for code readability we moved to initRemoteBuilder method.

                if (object instanceof ZookeeperManagerServiceBuilder)
                {
                    zookeeperManagerServiceBuilder = (ZookeeperManagerServiceBuilder) object;
                }
                else if (object instanceof SkavaLoggerServiceBuilder)
                {
                    skavaLoggerServiceBuilder = (SkavaLoggerServiceBuilder) object;
                    skavaLoggerServiceBuilder.reInitializeLog4j2(zookeeperManagerServiceBuilder);
                }
                else if (object instanceof SkavaResourceBundleServiceBuilder)
                {
                    skavaResourceBundleServiceBuilder = (SkavaResourceBundleServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.MessageSource);
                }
                else if (object instanceof MemCacheManagerServiceBuilder)
                {
                    memCacheManagerServiceBuilder = (MemCacheManagerServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                }
                else if (object instanceof DBSessionManagerServiceBuilder)
                {
                    dbSessionManagerServiceBuilder = (DBSessionManagerServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.DBSESSIONMANAGER);
                }
                else if (object instanceof HttpClientServiceBuilder)
                {
                    httpClientServiceBuilder = (HttpClientServiceBuilder) object;
                    execBuilderMethod.add(HttpClientServiceBuilder.HTTPCLIENTSERVICE);
                }
                else if (object instanceof JBPMServiceBuilder)
                {
                    jbpmServiceBuilder = (JBPMServiceBuilder) object;
                    execBuilderMethod.add(SkavaTenantContextFactory.BPMSERVICE);
                }
                else if (object instanceof ApiTokenPropertiesServiceBuilder)
                {
                    apiTokenPropertiesServiceBuilder = (ApiTokenPropertiesServiceBuilder) object;
                }
                else if (object instanceof AWSUtilServiceBuilder)
                {
                    awsUtilServiceBuilder = (AWSUtilServiceBuilder) object;
                }
                else if (object instanceof AmazonS3ClientServiceBuilder)
                {
                    amazonS3ClientServiceBuilder = (AmazonS3ClientServiceBuilder) object;
                }
                else if (object instanceof AWSCredentialsServiceBuilder)
                {
                    awsCredentialsServiceBuilder = (AWSCredentialsServiceBuilder) object;
                }
                else if (object instanceof AmazonSQSClientServiceBuilder)
                {
                    amazonSQSClientServiceBuilder = (AmazonSQSClientServiceBuilder) object;
                }
                else if (object instanceof AmazonSNSClientServiceBuilder)
                {
                    amazonSNSClientServiceBuilder = (AmazonSNSClientServiceBuilder) object;
                }
                else if (object instanceof StreamUserV2MergeListnerServiceBuilder)
                {
                    streamUserV2MergeListnerServiceBuilder = (StreamUserV2MergeListnerServiceBuilder) object;
                    execBuilderMethod.add(StreamUserV2MergeListnerServiceBuilder.STREAM_USER_V2_MERGE_LISTNER_SERVICE);
                }
                else if (object instanceof SkavaCaptchaFactoryServiceBuilder)
                {
                    skavaCaptchaFactoryServiceBuilder = (SkavaCaptchaFactoryServiceBuilder) object;
                }
                else if (object instanceof UserV2MergeHandlerServiceBuilder)
                {
                    userV2MergeHandlerServiceBuilder = (UserV2MergeHandlerServiceBuilder) object;
                    execBuilderMethod.add(UserV2MergeHandlerServiceBuilder.USER_V2_MERGE_HANDLER_SERVICE);
                }
                else if (object instanceof SimpleImageAndAudioCaptchaServiceBuilder)
                {
                    simpleImageAndAudioCaptchaServiceBuilder = (SimpleImageAndAudioCaptchaServiceBuilder) object;
                }
                else if (object instanceof ReCaptchaImageCaptchaServiceBuilder)
                {
                    reCaptchaImageCaptchaServiceBuilder = (ReCaptchaImageCaptchaServiceBuilder) object;
                }
                else if (object instanceof SkavaCaptchaServiceBuilder)
                {
                    skavaCaptchaServiceBuilder = (SkavaCaptchaServiceBuilder) object;
                }
                else if (object instanceof SkavaUnicolorCaptchaServiceBuilder)
                {
                    skavaUnicolorCaptchaServiceBuilder = (SkavaUnicolorCaptchaServiceBuilder) object;
                }
                else if (object instanceof SharedSecretsMapServiceBuilder)
                {
                    sharedSecretsMapServiceBuilder = (SharedSecretsMapServiceBuilder) object;
                }
                else if (object instanceof StreamComUserServiceBuilder)
                {
                    streamComUserServiceBuilder = (StreamComUserServiceBuilder) object;
                    execBuilderMethod.add(StreamComUserServiceBuilder.STREAMCOMUSERSERVICE);
                }
                else if (object instanceof MessageServiceBuilder)
                {
                    messageServiceBuilder = (MessageServiceBuilder) object;
                }
                else if (object instanceof StreamComServiceKraftBuilder)
                {
                	streamComServiceKraftBuilder = (StreamComServiceKraftBuilder) object;
                    execBuilderMethod.add(StreamComServiceKraftBuilder.KRAFTUSERSERVICE);
                }
            }
            putExecBuilderIntoMap();
        }
    }

    /**
     * Put exec builder into map.
     *
     * @throws ServerException the server exception
     */
    private void putExecBuilderIntoMap() throws ServerException
    {
    	Map<String, Object> requiredParams = new HashMap<>();
        Set<Tenant> tenants = zookeeperManagerServiceBuilder.getTenants();
        if (!execBuilderMethod.isEmpty() && tenants != null && !tenants.isEmpty())
        {
            Class<? extends UserKraftTenantContextFactoryImpl> currentClass = this.getClass();
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            Map<String, Method> currentClassMethods = new HashMap<>();
            for (Method invokeMethod : declaredMethods)
            {
                currentClassMethods.put(invokeMethod.getName(), invokeMethod);
            }
            for (Tenant tenant : tenants)
            {
                String execMethodTemp = null;
                try
                {
                    TenantThreadLocal.THREAD_TENANT.set(tenant);
                    for (String execMethod : execBuilderMethod)
                    {
                        execMethodTemp = execMethod;
                        Method invokeMethod = currentClassMethods.get("get");
                        if (invokeMethod != null)
                        {
                            invokeMethod.invoke(this, tenant, execMethod);
                        }
                        else
                        {
                        	requiredParams.put("execMethod", execMethod);
                        	logger.writeLog(Level.INFO, null, null, this.getClass().getSimpleName() , Thread.currentThread().getStackTrace()[1].getMethodName(), 0, execMethod, Userv2Constants.ERRORTYPE_INPUT, "No such builder method {} found", requiredParams, true, null);
                        }
                    }
                }
                catch (Exception e)
                {
                    String errorMsg = "Exception @putExecBuilderIntoMap tenant Id : " + tenant.getId() + ", execBuilderMethod : " + execMethodTemp;
                    logger.writeLog(Level.ERROR, null, null, this.getClass().getSimpleName() , Thread.currentThread().getStackTrace()[1].getMethodName(), 0, null, Userv2Constants.ERRORTYPE_INPUT, "Exception @putExecBuilderIntoMap tenant Id", null, true, e);
                    System.err.println(errorMsg + e.getMessage());
                    throw new ServerException(e);
                }
                finally
                {
                    TenantThreadLocal.THREAD_TENANT.remove();
                }
            }
        }
    }

    /**
     * Inits the remote builder.
     *
     * @param object the object
     */
    private void initRemoteBuilder(Object object)
    {
        if (object instanceof APIAdminRemoteServiceBuilder)
        {
            apiAdminRemoteServiceBuilder = (APIAdminRemoteServiceBuilder) object;
            execBuilderMethod.add(APIAdminRemoteServiceBuilder.API_ADMIN_REMOTE_SERVICE);
        }
        else if (object instanceof SkavaKeystoreRemoteServiceBuilder)
        {
            skavaKeystoreRemoteServiceBuilder = (SkavaKeystoreRemoteServiceBuilder) object;
            execBuilderMethod.add(SkavaTenantContextFactory.SKAVA_KEYSTORE_REMOTE_SERVICE);
        }
        else if (object instanceof SkavaMessagingRemoteServiceBuilder)
        {
            skavaMessagingRemoteServiceBuilder = (SkavaMessagingRemoteServiceBuilder) object;
            execBuilderMethod.add(SkavaMessagingRemoteServiceBuilder.SKAVA_MESSAGING_REMOTE_SERVICE);
        }
        else if (object instanceof StreamPartnerRemoteServiceBuilder)
        {
            streamPartnerRemoteServiceBuilder = (StreamPartnerRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamPartnerRemoteServiceBuilder.STREAM_PARTNER_REMOTE_SERVICE);
        }
        else if (object instanceof StreamCartRemoteServiceBuilder)
        {
            streamCartRemoteServiceBuilder = (StreamCartRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamCartRemoteServiceBuilder.STREAM_CART_REMOTE_SERVICE);
        }
        else if (object instanceof StreamListRemoteServiceBuilder)
        {
            streamListRemoteServiceBuilder = (StreamListRemoteServiceBuilder) object;
            execBuilderMethod.add(StreamListRemoteServiceBuilder.STREAM_LIST_REMOTE_SERVICE);
        }
        else if (object instanceof URLShortenRemoteServiceBuilder)
        {
            urlShortenRemoteServiceBuilder = (URLShortenRemoteServiceBuilder) object;
            execBuilderMethod.add(URLShortenRemoteServiceBuilder.URLSHORTENREMOTESERVICEBUILDER);
        }
    }

    /**
     * Null check.
     *
     * @param obj the obj
     * @param tenant the tenant
     * @throws ServerException the server exception
     */
    void nullCheck(Object obj, Tenant tenant) throws ServerException
    {
        if (obj == null) { throw new ServerException(SERVEREXCEPTION_NULLPOINTER); }
        if (tenant == null) { throw new ServerException(SERVEREXCEPTION_NULLPOINTER); }
    }

    /**
     * Used to get tenant.
     *
     * @param tenant Instance of {@link com.skava.model.Tenant}.
     * @param builderName Indicates the builder name which is used to get value for the given tenant name.
     * @return the object
     * @throws ServerException while getting value for builder name.
     */
    @Override
    public Object get(Tenant tenant, String builderName) throws ServerException
    {
        Object toRet = null;
        if (builderName != null)
        {
            if (builderName.equals(SkavaTenantContextFactory.DBSESSIONMANAGER))
            {
                nullCheck(dbSessionManagerServiceBuilder, tenant);
                MemCacheManager memCacheManager = (MemCacheManager) get(tenant, SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                toRet = dbSessionManagerServiceBuilder.getDBSessionManager(tenant, memCacheManager);
            }
            else if (builderName.equals(HttpClientServiceBuilder.HTTPCLIENTSERVICE))
            {
            	nullCheck(httpClientServiceBuilder, tenant);
            	toRet = httpClientServiceBuilder.getHttpClientService(tenant, HttpClientServiceBuilder.HTTPCLIENTSERVICE);
            }
            
            else if (builderName.equals(UserV2MergeHandlerServiceBuilder.USER_V2_MERGE_HANDLER_SERVICE))
            {
                nullCheck(userV2MergeHandlerServiceBuilder, tenant);
                toRet = userV2MergeHandlerServiceBuilder.getUserV2MergeHandler(tenant, this);
            }
            else if (builderName.equals(AWSUtilServiceBuilder.AWSUTIL))
            {
            	nullCheck(this.awsUtilServiceBuilder, tenant);
            	toRet = awsUtilServiceBuilder.getAWSUtil(tenant, this);
            }
            else if (builderName.equals(SkavaTenantContextFactory.MessageSource))
            {
            	nullCheck(this.skavaResourceBundleServiceBuilder, tenant);
            	toRet = skavaResourceBundleServiceBuilder.get(tenant);
            }
            else if (builderName.equals(URLShortenServiceBuilder.URLSHORTENSERVICE))
            {
                nullCheck(urlShortenRemoteServiceBuilder, tenant);
                toRet = urlShortenRemoteServiceBuilder.getURLShortenRemoteService(tenant, this);
            }
            else if (builderName.equals(SkavaMessagingRemoteServiceBuilder.SKAVA_MESSAGING_REMOTE_SERVICE))
            {
                nullCheck(skavaMessagingRemoteServiceBuilder, tenant);
                toRet = skavaMessagingRemoteServiceBuilder.getSkavaMessagingRemoteService(tenant, this);
            }
            else if (builderName.equals(SkavaCaptchaFactoryServiceBuilder.SKAVACAPTCHAFACTORY))
            {
            	nullCheck(this.skavaCaptchaFactoryServiceBuilder, tenant);
            	toRet = skavaCaptchaFactoryServiceBuilder.getSkavaCaptchaFactory(tenant, this);
            }
            else if (builderName.equals(SkavaTenantContextFactory.BPMSERVICE))
            {
            	nullCheck(this.jbpmServiceBuilder, tenant);
            	toRet = jbpmServiceBuilder.getBpmService(tenant);
            }
            else if (builderName.equals(SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE))
            {
            	nullCheck(memCacheManagerServiceBuilder, tenant);
            	toRet = memCacheManagerServiceBuilder.getMemCacheManagerService(tenant);
            }
            else if (builderName.equals(MessageServiceBuilder.MESSAGE_SERVICE_CLIENT))
            {
                nullCheck(messageServiceBuilder, tenant);
                toRet = messageServiceBuilder.getMessageService(tenant, this);
            }
            else if (builderName.equals(StreamComUserServiceBuilder.STREAMCOMUSERSERVICE))
            {
                nullCheck(this.streamComUserServiceBuilder, tenant);
                return this.streamComUserServiceBuilder.getStreamComUserService(tenant, this);
            }
            else if (builderName.equals(HttpClientServiceBuilder.HTTPCLIENTSERVICE))
            {
                nullCheck(httpClientServiceBuilder, tenant);
                toRet = httpClientServiceBuilder.getHttpClientService(tenant, HttpClientServiceBuilder.HTTPCLIENTSERVICE);
            }
            else if (builderName.equals(ApiTokenPropertiesServiceBuilder.API_TOKEN_PROPERTIES))
            {
                nullCheck(apiTokenPropertiesServiceBuilder, tenant);
                toRet = apiTokenPropertiesServiceBuilder.getAPITokenProperties(tenant);
            }
            else if (builderName.equals(SharedSecretsMapServiceBuilder.SHAREDSECRETSHASHMAP))
            {
                nullCheck(sharedSecretsMapServiceBuilder, tenant);
                toRet = sharedSecretsMapServiceBuilder.getSharedSecretsHashMap(tenant);
            }
            else if (builderName.equals(SkavaTenantContextFactory.SKAVA_KEYSTORE_REMOTE_SERVICE))
            {
                nullCheck(skavaKeystoreRemoteServiceBuilder, tenant);
                toRet = skavaKeystoreRemoteServiceBuilder.getSkavaKeystoreRemoteService(tenant, this);
            }
            
            else if (builderName.equals(StreamComServiceKraftBuilder.KRAFTUSERSERVICE))
            {
                nullCheck(streamComServiceKraftBuilder, tenant);
                toRet = streamComServiceKraftBuilder.getStreamComKraftService(tenant, this);
            }
            
            
            /*
             * END
             * */
            
            else if (builderName.equals(SkavaTenantContextFactory.APITOKENPROPERTIES))
            {
                nullCheck(apiTokenPropertiesServiceBuilder, tenant);
                toRet = apiTokenPropertiesServiceBuilder.getAPITokenProperties(tenant);
            }
            else if (builderName.equals(StreamUserV2MergeListnerServiceBuilder.STREAM_USER_V2_MERGE_LISTNER_SERVICE))
            {
                nullCheck(streamUserV2MergeListnerServiceBuilder, tenant);
                toRet = streamUserV2MergeListnerServiceBuilder.getStreamUserV2MergeListnerService(tenant, this);
            }
            else if (builderName.equals(AmazonS3ClientServiceBuilder.AMAZON_S3_CLIENT))
            {
                nullCheck(amazonS3ClientServiceBuilder, tenant);
                toRet = amazonS3ClientServiceBuilder.getAmazonS3Client(tenant, this);
            }
            else if (builderName.equals(AWSCredentialsServiceBuilder.AWS_CREDEBTIALS))
            {
                nullCheck(awsCredentialsServiceBuilder, tenant);
                toRet = awsCredentialsServiceBuilder.getAWSCredentials(tenant);
            }
            else if (builderName.equals(AmazonSQSClientServiceBuilder.AMAZON_SQS_CLIENT))
            {
                nullCheck(amazonSQSClientServiceBuilder, tenant);
                toRet = amazonSQSClientServiceBuilder.getAmazonSQSClient(tenant, this);
            }
            else if (builderName.equals(AmazonSNSClientServiceBuilder.AMAZON_SNS_CLIENT))
            {
                nullCheck(amazonSNSClientServiceBuilder, tenant);
                toRet = amazonSNSClientServiceBuilder.getAmazonSNSClient(tenant, this);
            }
            else if (builderName.equals(SimpleImageAndAudioCaptchaServiceBuilder.SIMPLEIMAGEANDAUDIOCAPTCHASERVICE))
            {
                nullCheck(simpleImageAndAudioCaptchaServiceBuilder, tenant);
                toRet = simpleImageAndAudioCaptchaServiceBuilder.getSimpleImageAndAudioCaptchaService(tenant, this);
            }
            else if (builderName.equals(ReCaptchaImageCaptchaServiceBuilder.RECAPTCHAIMAGECAPTCHASERVICE))
            {
                nullCheck(reCaptchaImageCaptchaServiceBuilder, tenant);
                toRet = reCaptchaImageCaptchaServiceBuilder.getReCaptchaImageCaptchaService(tenant, this);
            }
            else if (builderName.equals(SkavaCaptchaServiceBuilder.SKAVACAPTCHASERVICE))
            {
                nullCheck(skavaCaptchaServiceBuilder, tenant);
                toRet = skavaCaptchaServiceBuilder.getSkavaCaptchaService(tenant, this);
            }
            else if (builderName.equals(SkavaUnicolorCaptchaServiceBuilder.SKAVAUNICOLORCAPTCHASERVICE))
            {
                nullCheck(skavaUnicolorCaptchaServiceBuilder, tenant);
                toRet = skavaUnicolorCaptchaServiceBuilder.getSkavaUnicolorCaptchaService(tenant, this);
            }
            else if (builderName.equals(StreamComUserServiceBuilder.STREAMCOMUSERSERVICE))
            {
                nullCheck(streamComUserServiceBuilder, tenant);
                toRet = streamComUserServiceBuilder.getStreamComUserService(tenant, this);
            }
            else if (builderName.equals(SharedSecretsMapServiceBuilder.SHAREDSECRETSHASHMAP))
            {
                nullCheck(sharedSecretsMapServiceBuilder, tenant);
                toRet = sharedSecretsMapServiceBuilder.getSharedSecretsHashMap(tenant);
            }
            else if (builderName.equals(SkavaTenantContextFactory.SKAVA_KEYSTORE_REMOTE_SERVICE))
            {
                nullCheck(skavaKeystoreRemoteServiceBuilder, tenant);
                toRet = skavaKeystoreRemoteServiceBuilder.getSkavaKeystoreRemoteService(tenant, this);
            }
            else if (builderName.equals(APIAdminRemoteServiceBuilder.API_ADMIN_REMOTE_SERVICE))
            {
                nullCheck(apiAdminRemoteServiceBuilder, tenant);
                toRet = apiAdminRemoteServiceBuilder.getAPIAdminRemoteService(tenant, this);
            }
            else if (builderName.equals(StreamListRemoteServiceBuilder.STREAM_LIST_REMOTE_SERVICE))
            {
                nullCheck(streamListRemoteServiceBuilder, tenant);
                toRet = streamListRemoteServiceBuilder.getStreamListRemoteService(tenant, this);
            }
            else if (builderName.equals(StreamCartRemoteServiceBuilder.STREAM_CART_REMOTE_SERVICE))
            {
                nullCheck(streamCartRemoteServiceBuilder, tenant);
                toRet = streamCartRemoteServiceBuilder.getStreamCartRemoteService(tenant, this);
            }
        }
        return toRet;
    }
}
