package com.skava.builder.interfaces;

import com.skava.model.Tenant;
import com.skava.services.StreamSearchV2KraftService;
import com.skava.util.ServerException;

public interface StreamSearchV2KraftServiceBuilder extends SkavaBuilder
{
    String STREAMSEARCHV2KRAFTSERVICE = "StreamSearchV2KraftService";

    StreamSearchV2KraftService getStreamSearchV2KraftService(Tenant tenant,
                                                             SkavaTenantContextFactory skavaTenantContextFactory) throws ServerException;
}
