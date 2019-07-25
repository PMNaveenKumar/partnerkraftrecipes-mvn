package com.skava.builder.remote.interfaces;

import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.model.Tenant;
import com.skava.services.StreamSearchV2KraftService;
import com.skava.util.ServerException;

public interface StreamSearchV2KraftRemoteServiceBuilder
{
    String STREAM_SEARCH_V2_KRAFT_REMOTE_SERVICE = "StreamSearchV2KraftRemoteService";

    public StreamSearchV2KraftService getStreamSearchV2RemoteServiceKraft(Tenant tenant,
                                                                          SkavaTenantContextFactory skavaTenantContextFactory) throws ServerException;
}
