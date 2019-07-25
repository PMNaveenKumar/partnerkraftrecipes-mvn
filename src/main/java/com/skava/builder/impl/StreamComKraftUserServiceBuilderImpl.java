/*
 * 
 */
package com.skava.builder.impl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;

import com.skava.builder.interfaces.AWSUtilServiceBuilder;
import com.skava.builder.interfaces.HttpClientServiceBuilder;
import com.skava.builder.interfaces.MessageServiceBuilder;
import com.skava.builder.interfaces.SkavaCaptchaFactoryServiceBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.StreamComUserServiceBuilder;
import com.skava.builder.interfaces.UserV2MergeHandlerServiceBuilder;
import com.skava.builder.remote.interfaces.SkavaMessagingRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.URLShortenRemoteServiceBuilder;
import com.skava.cache.MemCacheManager;
import com.skava.db.DBSessionManager;
import com.skava.interfaces.SkavaCaptchaFactory;
import com.skava.interfaces.UserV2MergeHandler;
import com.skava.kraft.userv2.KraftUserServiceImpl;
import com.skava.model.Tenant;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.Partner;
import com.skava.model.userv2.ComUser;
import com.skava.model.userv2.ComUserResponse;
import com.skava.services.BpmService;
import com.skava.services.HttpClientService;
import com.skava.services.SkavaMessagingService;
import com.skava.services.StreamComUserService;
import com.skava.services.URLShortenService;
import com.skava.services.userv2.KraftUserService;
import com.skava.userv2.builder.StreamComKraftUserServiceBuilder;
import com.skava.util.AWSUtil;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.CryptoUtil;
import com.skava.util.ServerException;
import com.skava.util.messageservice.MessageService;


/**
 * The Class StreamComUserServiceBuilderImpl.
 */
public class StreamComKraftUserServiceBuilderImpl implements StreamComKraftUserServiceBuilder
{
    
    /** The stream com user service map. */
    Map<String, KraftUserService> streamComUserServiceMap = new HashMap<>();
    
    
    /** The encryptsaltvalue. */
    String encryptsaltvalue;
    
    /**
     * Instantiates a new stream com user service builder impl.
     *
     * @param mimeMap the mime map
     * @param encryptsaltvalue the encryptsaltvalue
     * @param tokenizationManager the tokenization manager
     */
    StreamComKraftUserServiceBuilderImpl(String encryptsaltvalue)
    {
        this.encryptsaltvalue = encryptsaltvalue;
    }

    /* (non-Javadoc)
     * @see com.skava.builder.interfaces.StreamComUserServiceBuilder#getStreamComUserService(Tenant, SkavaTenantContextFactory)
     *
     * Gets the stream com user service.
     *
     * @param tenant Instance of {@link com.skava.model.Tenant}.
     * @param skavaTenantContextFactory Instance of {@link com.skava.builder.interfaces.SkavaTenantContextFactory}.
     * @return Instance of {@link com.skava.services.StreamComUserService}.
     * @throws ServerException while getting stream comuser.
     */

    /**
     * Gets the stream com user service from map.
     *
     * @param tenant the tenant
     * @return the stream com user service from map
     */

	@Override
	public KraftUserService getStreamComKraftService(Tenant tenant, SkavaTenantContextFactory skavaTenantContextFactory)
			throws ServerException 
	{
    	KraftUserService streamComKraftUserService = getStreamComKraftUserServiceFromMap(tenant);
        if (streamComKraftUserService == null)
        {
            synchronized (this)
            {
            	streamComKraftUserService = getStreamComKraftUserServiceFromMap(tenant);
                if (streamComKraftUserService == null)
                {
                    try
                    {
                        DBSessionManager dbSessionManager = (DBSessionManager) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.DBSESSIONMANAGER);
                        HttpClientService httpClientService = (HttpClientService) skavaTenantContextFactory.get(tenant, HttpClientServiceBuilder.HTTPCLIENTSERVICE);
                        UserV2MergeHandler handler = (UserV2MergeHandler) skavaTenantContextFactory.get(tenant, UserV2MergeHandlerServiceBuilder.USER_V2_MERGE_HANDLER_SERVICE);
                        AWSUtil awsUtil = (AWSUtil) skavaTenantContextFactory.get(tenant, AWSUtilServiceBuilder.AWSUTIL);
                        MessageSource resourceBundle = (MessageSource) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.MessageSource);
                        URLShortenService urlShortenService = (URLShortenService) skavaTenantContextFactory.get(tenant, URLShortenRemoteServiceBuilder.URLSHORTENREMOTESERVICEBUILDER);
                        SkavaMessagingService messageCampaignService = (SkavaMessagingService) skavaTenantContextFactory.get(tenant, SkavaMessagingRemoteServiceBuilder.SKAVA_MESSAGING_REMOTE_SERVICE);
                        encryptsaltvalue = ConfigManagerInstance.get(this.encryptsaltvalue);
                        SkavaCaptchaFactory skavaCaptchaFactory = (SkavaCaptchaFactory) skavaTenantContextFactory.get(tenant, SkavaCaptchaFactoryServiceBuilder.SKAVACAPTCHAFACTORY);
                        BpmService bpmService = (BpmService) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.BPMSERVICE);
                        MemCacheManager cacheManager = (MemCacheManager) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                        MessageService messageService = (MessageService) skavaTenantContextFactory.get(tenant, MessageServiceBuilder.MESSAGE_SERVICE_CLIENT);
                        
                        StreamComUserService streamComUserService = (StreamComUserService) skavaTenantContextFactory.get(tenant, StreamComUserServiceBuilder.STREAMCOMUSERSERVICE);
                        streamComKraftUserService = new KraftUserServiceImpl(streamComUserService, httpClientService, cacheManager, dbSessionManager, resourceBundle, messageCampaignService, encryptsaltvalue, bpmService, skavaCaptchaFactory, messageService);
                        streamComUserServiceMap.put(tenant.getId(), streamComKraftUserService);
                    }
                    catch (ServerException se)
                    {
                        throw (ServerException) se;
                    }
                    catch (Exception e)
                    {
                        throw new ServerException(e);
                    }
                }
            }
        }
        return streamComKraftUserService;
    }
	
    private KraftUserService getStreamComKraftUserServiceFromMap(Tenant tenant)
    {
        return streamComUserServiceMap.get(tenant.getId());
    }
}
