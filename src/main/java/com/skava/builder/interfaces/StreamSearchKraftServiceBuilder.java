package com.skava.builder.interfaces;

import com.skava.builder.interfaces.SkavaBuilder;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.model.Tenant;
import com.skava.services.StreamSearchKraftService;
import com.skava.util.ServerException;

public interface StreamSearchKraftServiceBuilder extends SkavaBuilder
{
    public String STREAMSEARCHKRAFTSERVICE = "StreamSearchKraftService";

    StreamSearchKraftService getStreamSearchKraftService(Tenant tenant,
                                                         SkavaTenantContextFactory skavaTenantContextFactory) throws ServerException;
}
