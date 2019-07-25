
package com.skava.builder.remote.impl;

import java.util.HashMap;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.builder.remote.interfaces.StreamSearchV2KraftRemoteServiceBuilder;
import com.skava.model.Tenant;
import com.skava.util.ServerException;

public class StreamSearchV2KraftRemoteServiceBuilderImpl
    implements StreamSearchV2KraftRemoteServiceBuilder
{

    private String serverName;
    private String apiTokenSecureEnabled;
    private String requestor;
    private String remoteClassName;
    private HashMap<String, com.skava.services.StreamSearchV2KraftService> remoteServiceBuilderMap = new HashMap<String, com.skava.services.StreamSearchV2KraftService>();

    public StreamSearchV2KraftRemoteServiceBuilderImpl(String serverName, String apiTokenSecureEnabled, String requestor) {
        this(serverName, apiTokenSecureEnabled, requestor, "com.skava.remoteservices.StreamSearchV2KraftRemoteImpl");
    }

    public StreamSearchV2KraftRemoteServiceBuilderImpl(String serverName, String apiTokenSecureEnabled, String requestor, String remoteClassName) {
        this.serverName = serverName;
        this.apiTokenSecureEnabled = apiTokenSecureEnabled;
        this.requestor = requestor;
        this.remoteClassName = remoteClassName;
    }

    private com.skava.services.StreamSearchV2KraftService getStreamSearchV2KraftRemoteServiceFromMap(Tenant tenant) {
        return remoteServiceBuilderMap.get(tenant.getId());
    }

    public com.skava.services.StreamSearchV2KraftService getStreamSearchV2RemoteServiceKraft(Tenant tenant, SkavaTenantContextFactory skavaTenantContextFactory)
        throws ServerException
    {
        com.skava.services.StreamSearchV2KraftService toRet = getStreamSearchV2KraftRemoteServiceFromMap(tenant);
        if (toRet == null)
        {
            synchronized (this)
            {
                toRet = getStreamSearchV2KraftRemoteServiceFromMap(tenant);
                if (toRet == null)
                {
                    try
                    {
                        com.skava.cache.MemCacheManager memCacheManagerService = (com.skava.cache.MemCacheManager) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.MEMCACHEMANAGERSERVICE);
                        com.skava.services.HttpClientService httpClientService = (com.skava.services.HttpClientService) skavaTenantContextFactory.get(tenant, com.skava.builder.interfaces.HttpClientServiceBuilder.HTTPCLIENTSERVICE);
                        com.skava.model.apitoken.APITokenProperties apiTokenProperties = (com.skava.model.apitoken.APITokenProperties) skavaTenantContextFactory.get(tenant, SkavaTenantContextFactory.APITOKENPROPERTIES);
                        String serverName = com.skava.util.ConfigManagerInstance.get(tenant, this.serverName);
                        boolean isApiTokenSecureEnabled = "remoteurl.apiTokenSecureEnabled".equals(apiTokenSecureEnabled) ? com.skava.util.ConfigManagerInstance.getBoolean(tenant, "remoteurl.apiTokenSecureEnabled", false): Boolean.valueOf(apiTokenSecureEnabled);
                        String requestor = this.requestor;
                        java.lang.reflect.Constructor<?> constructor = Class.forName(remoteClassName).getConstructor(com.skava.cache.MemCacheManager.class, com.skava.services.HttpClientService.class, com.skava.services.APIAdminService.class, com.skava.model.apitoken.APITokenProperties.class, String.class, boolean.class, String.class);
                        com.skava.services.APIAdminService apiAdminRemoteService = (com.skava.services.APIAdminService) skavaTenantContextFactory.get(tenant, com.skava.builder.interfaces.SkavaTenantContextFactory.API_ADMIN_REMOTE_SERVICE);
                        toRet = (com.skava.services.StreamSearchV2KraftService) constructor.newInstance(memCacheManagerService, httpClientService, apiAdminRemoteService, apiTokenProperties, serverName, isApiTokenSecureEnabled, requestor);
                        remoteServiceBuilderMap.put(tenant.getId(), toRet);
                    }
                    catch (ServerException e)
                    {
                        throw (ServerException) e;
                    }
                    catch (Exception e)
                    {
                        throw new ServerException(e);
                    }
                }
            }
        }
        return toRet;
    }

}
