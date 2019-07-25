package com.skava.builder.impl;

import java.util.HashMap;
import java.util.Map;

import com.skava.builder.interfaces.HttpClientServiceBuilder;
import com.skava.builder.interfaces.SearchServiceKraftBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.interfaces.StreamSearchKraftServiceBuilder;
import com.skava.cache.MemCacheManager;
import com.skava.db.DBSessionManager;
import com.skava.interfaces.SearchService;
import com.skava.model.Tenant;
import com.skava.searchv2.StreamSearchKraftServiceImpl;
import com.skava.services.HttpClientService;
import com.skava.services.StreamSearchKraftService;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.ServerException;

public class StreamSearchKraftServiceBuilderImpl implements StreamSearchKraftServiceBuilder
{
    private Map<String, StreamSearchKraftService> streamSearchKraftServiceMap = new HashMap<>();

    private String searchConfigPathV2;

    public StreamSearchKraftServiceBuilderImpl(String searchConfigPathV2)
    {
        this.searchConfigPathV2 = searchConfigPathV2;
    }

    @Override
    public StreamSearchKraftService getStreamSearchKraftService(Tenant tenant,
                                                                SkavaTenantContextFactory skavaTenantContextFactory) throws ServerException
    {
        StreamSearchKraftService streamSearchService = getStreamSearchKraftServiceFromMap(tenant);
        if (streamSearchService == null)
        {
            synchronized (this)
            {
                streamSearchService = getStreamSearchKraftServiceFromMap(tenant);
                if (streamSearchService == null)
                {
                    try
                    {
                        String searchConfigV2PathValue = ConfigManagerInstance.get(tenant, searchConfigPathV2);
                        DBSessionManager dbSessionManager = (DBSessionManager) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.DBSESSIONMANAGER);
                        MemCacheManager memCacheManager = (MemCacheManager) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                        HttpClientService httpClientService = (HttpClientService) skavaTenantContextFactory.get(tenant, HttpClientServiceBuilder.HTTPCLIENTSERVICE);
                        SearchService searchService = (SearchService) skavaTenantContextFactory.get(tenant, SearchServiceKraftBuilder.SEARCHKRAFTSERVICE);
                        streamSearchService = new StreamSearchKraftServiceImpl(dbSessionManager, memCacheManager, httpClientService, searchConfigV2PathValue, searchService);
                        streamSearchKraftServiceMap.put(tenant.getId(), streamSearchService);
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

        return streamSearchService;
    }

    StreamSearchKraftService getStreamSearchKraftServiceFromMap(Tenant tenant)
    {
        return streamSearchKraftServiceMap.get(tenant.getId());
    }
}
