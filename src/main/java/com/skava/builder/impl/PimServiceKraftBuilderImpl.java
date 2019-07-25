package com.skava.builder.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.springframework.context.MessageSource;

import com.skava.builder.interfaces.PimServiceKraftBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.remote.interfaces.PromotionRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamComUserRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamInventoryUserRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamSearchV2KraftRemoteServiceBuilder;
import com.skava.builder.remote.interfaces.StreamSearchV2RemoteServiceBuilder;
import com.skava.cache.MemCacheManager;
import com.skava.db.DBSessionManager;
import com.skava.model.Tenant;
import com.skava.model.pim.PimConstants;
import com.skava.pim.PimServicesImplKraft;
import com.skava.pim.helper.PIMUtil;
import com.skava.services.BpmService;
import com.skava.services.PimServiceKraft;
import com.skava.services.PromotionService;
import com.skava.services.StreamComUserService;
import com.skava.services.StreamInventoryUserService;
import com.skava.services.StreamSearchV2KraftService;
import com.skava.services.StreamSearchV2Service;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.ServerException;

public class PimServiceKraftBuilderImpl implements PimServiceKraftBuilder
{
    private static final String PIMSERVICEBUILDERIMPL = "PIMSERVICEBUILDERIMPL";

	Map<String, PimServiceKraft> pimServiceKraftMap = new HashMap<>();

    String cryptoutilKeyPrefix;
    String allowPreviewKey;

    PimServiceKraftBuilderImpl(String cryptoutilKeyPrefix, String allowPreview)
    {
        this.cryptoutilKeyPrefix = cryptoutilKeyPrefix;
        this.allowPreviewKey = allowPreview;
    }

    @Override
    public PimServiceKraft getPimServiceKraft(Tenant tenant,
                                    SkavaTenantContextFactory skavaServiceTenantContextBuilder) throws ServerException
    {
        PimServiceKraft pimServiceKraft = getPimServiceKraftFromMap(tenant);
        if (pimServiceKraft == null)
        {
            synchronized (this)
            {
                pimServiceKraft = getPimServiceKraftFromMap(tenant);
                if (pimServiceKraft == null)
                {
                    try
                    {
                        String prefix = ConfigManagerInstance.get(tenant, this.cryptoutilKeyPrefix);
                        DBSessionManager dbSessionManager = (DBSessionManager) skavaServiceTenantContextBuilder.get(tenant, SkavaTenantContextFactory.DBSESSIONMANAGER);
                        StreamSearchV2KraftService streamSearchV2KraftService = (StreamSearchV2KraftService) skavaServiceTenantContextBuilder.get(tenant, StreamSearchV2KraftRemoteServiceBuilder.STREAM_SEARCH_V2_KRAFT_REMOTE_SERVICE);
                        StreamSearchV2Service streamSearchV2Service = (StreamSearchV2Service) skavaServiceTenantContextBuilder.get(tenant, StreamSearchV2RemoteServiceBuilder.STREAM_SEARCH_V2_REMOTE_SERVICE);
                        StreamInventoryUserService streamInventoryUserService = (StreamInventoryUserService) skavaServiceTenantContextBuilder.get(tenant, StreamInventoryUserRemoteServiceBuilder.STREAM_INVENTORY_USER_REMOTE_SERVICE);
                        MemCacheManager cacheManager = (MemCacheManager) skavaServiceTenantContextBuilder.get(tenant, SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                        MessageSource resourceBundle = (MessageSource) skavaServiceTenantContextBuilder.get(tenant, SkavaTenantContextFactory.MessageSource);
                        BpmService bpmService = (BpmService) skavaServiceTenantContextBuilder.get(tenant, SkavaTenantContextFactory.BPMSERVICE);
                        PromotionService promotionService = (PromotionService) skavaServiceTenantContextBuilder.get(tenant, PromotionRemoteServiceBuilder.PROMOTION_REMOTE_SERVICE);
                        StreamComUserService streamComUserService = (StreamComUserService) skavaServiceTenantContextBuilder.get(tenant, StreamComUserRemoteServiceBuilder.STREAM_COM_USER_REMOTE_SERVICE);

                        boolean allowPreview = ConfigManagerInstance.getBoolean(tenant, this.allowPreviewKey, false);
                        pimServiceKraft = new PimServicesImplKraft(dbSessionManager, streamSearchV2KraftService, streamSearchV2Service, streamInventoryUserService, cacheManager, resourceBundle, prefix, promotionService, bpmService, streamComUserService, allowPreview);
                        pimServiceKraftMap.put(tenant.getId(), pimServiceKraft);
                    }
                    catch (ServerException se)
                    {
                    	PIMUtil.writeLog(Level.ERROR, PIMUtil.getCallingMethodName(2), PIMSERVICEBUILDERIMPL, se, PimConstants.ERRORTYPE_LOGICAL, PimConstants.DESC_SERVER_EXCEPTION+" @getPimService()", null, PIMSERVICEBUILDERIMPL);
                        throw (ServerException) se;
                    }
                    catch (Exception e)
                    {
                    	PIMUtil.writeLog(Level.ERROR, PIMUtil.getCallingMethodName(2), PIMSERVICEBUILDERIMPL, e, PimConstants.ERRORTYPE_LOGICAL, PimConstants.DESC_EXCEPTION+" @getPimService()" , null, PIMSERVICEBUILDERIMPL);
                        throw new ServerException(e);
                    }
                    
                }
            }
        }
        return pimServiceKraft;
    }

    private PimServiceKraft getPimServiceKraftFromMap(Tenant tenant)
    {
        return pimServiceKraftMap.get(tenant.getId());
    }
}
