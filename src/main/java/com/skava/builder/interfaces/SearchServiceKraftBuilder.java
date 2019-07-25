/*******************************************************************************
 * Copyright ©2002-2014 Skava. 
 * All rights reserved.The Skava system, including 
 * without limitation, all software and other elements
 * thereof, are owned or controlled exclusively by
 * Skava and protected by copyright, patent, and 
 * other laws. Use without permission is prohibited.
 * 
 *  For further information contact Skava at info@skava.com.
 ******************************************************************************/
package com.skava.builder.interfaces;

import com.skava.interfaces.SearchService;
import com.skava.model.Tenant;
import com.skava.util.ServerException;

public interface SearchServiceKraftBuilder extends SkavaBuilder
{
    String SEARCHKRAFTSERVICE = "SearchKraftService";
    SearchService getSearchKraftService(Tenant tenant) throws ServerException;
}
