package com.skava.pim.jbpm.workitems;

import com.skava.util.ServerException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;

import javax.servlet.http.HttpServletRequest;

import org.drools.core.process.instance.WorkItemHandler;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;

import com.skava.model.dbbeans.Campaign;
import com.skava.model.pim.CatalogMaster;
import com.skava.model.pim.PimConstants;
import com.skava.model.pim.PimKraftConstants;
import com.skava.model.pim.PimResponse;
import com.skava.pim.helper.PIMUtil;
import com.skava.services.StreamSearchV2Service;
import com.skava.util.ReadUtil;

/**
 * <h1> GetSearchResults </h1>
 * <p>This class is used to we can directly get the products by CategoryId, ProductId and SkuId.</p>
 * @author: <u>Skava Platform Team</u>
 * @version 7.5
 * @since 6.0
 * @jbpm GetSearchResults
 */
public class UpdateKeywords implements WorkItemHandler
{

    private String SERVICE_NAME;

    private final String GETSEARCHRESULTS = "GETSEARCHRESULTS";
    public static final int SEARCH_RESPONSE_CODE = 204;

    /**
     * <p>This method execute the load products by CategoryId, ProductId and SkuId.</p>
     * 
     * @param workItem {@link org.kie.api.runtime.process.WorkItem} It contains the params which are used by the current work item.
     * @param manager {@link org.kie.api.runtime.process.WorkItemManager} It sets response of <code>GetSearchResults</code> work item.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager)
    {
        Map<String, Object> responseMap = new HashMap<>();

        try
        {

//            PIMUtil.writeLog(Level.DEBUG, SERVICE_NAME, GETSEARCHRESULTS, null, null, PimConstants.DESC_START_WORKITEM, workItem.getParameters(), PimConstants.DESC_START_WORKITEM, GETSEARCHRESULTS, workItem.getParameters());

            HashMap<String, Object> params = (HashMap<String, Object>) workItem.getParameters();

            SERVICE_NAME = ReadUtil.getString(params.get(PimConstants.SERVICE_NAME), null);

            HttpServletRequest request = (HttpServletRequest) params.get(PimConstants.PARAM_HTTPSERVLET_REQUEST);
            StreamSearchV2Service streamSearchV2Service = (StreamSearchV2Service) params.get(PimKraftConstants.PARAM_SEARCHV2_CORE_SERVICE);
            CatalogMaster catalogMaster = (CatalogMaster) params.get(PimConstants.PARAM_CATALOG_MASTER);
            String catalogId = catalogMaster == null ? "0" : catalogMaster.getCatalogId();
            String searchTerm = ReadUtil.getString(params.get(PimConstants.PARAM_SEARCH_TERM), null);
            String locale = (String) params.get(PimConstants.PARAM_LOCALE);
            Campaign campaign = (Campaign) params.get(PimConstants.PARAM_CAMPAIGN);
            long storeId = (long) params.get(PimConstants.PARAM_STORE_ID);

//            PIMUtil.writeLog(Level.DEBUG, SERVICE_NAME, GETSEARCHRESULTS, null, null, " SkavPimService - {} :request - {},  streamSearchV2Service - {},  campaign - {},  skuId - {},  productIds - {},  name - {},  brand - {},  category - {},  categoryids - {},  categorylevel1 - {10},  categorylevel2 - {11},  categorylevel3 - {12},  categorylevel4 - {13},  categorylevel5 - {14},  division - {15},  color - {16},  size1 - {17},  size2 - {18},  priceMin - {19},  priceMax - {20},  variant - {21},  searchTerm - {22},  selectedFacets - {23},  sort - {24},  group - {25},  facets - {26},  responseFormatterClass - {27},  catalogMaster - {28},  categoryMaster - {29},  offset - {30},  limit - {31},  usev2 - {32},  edismax - {33},  iszeroResult - {34},  spellcheck - {35},  personalize - {36},  locale - {37},  currentVersion - {38},  contextualParam - {39},  curateTerms - {40},  disableFacetMinCount - {41},  includeGhostProduct - {42},  segments - {43}, userPreferences - {44} ", null, GETSEARCHRESULTS, request, streamSearchV2Service, campaign, skuId, productIds, name, brand, category, categoryids, categorylevel1, categorylevel2, categorylevel3, categorylevel4, categorylevel5, division, color, size1, size2, priceMin, priceMax, variant, searchTerm, selectedFacets, sort, group, facets, responseFormatterClass, catalogMaster, categoryMaster, offset, limit, usev2, edismax, iszeroResult, spellcheck, personalize, locale, currentVersion, contextualParam, curateTerms, disableFacetMinCount, includeGhostProduct, segments, userPreferences, similarType);

            responseMap = indexKeywords(request, streamSearchV2Service, campaign, storeId, catalogId, searchTerm, locale);

//            PIMUtil.writeLog(Level.DEBUG, SERVICE_NAME, GETSEARCHRESULTS, null, null, PimConstants.DESC_RESP_MAP_WORKITEM, null, PimConstants.DESC_RESP_MAP_WORKITEM, GETSEARCHRESULTS, responseMap);

        }
        catch (ServerException se)
        {

            if (!PimConstants.knownError(se.getErrorCode()))
            {
                PIMUtil.writeLog(Level.ERROR, SERVICE_NAME, GETSEARCHRESULTS, se, PimConstants.ERRORTYPE_LOGICAL, PimConstants.DESC_SERVER_EXCEPTION, null, PimConstants.DESC_SERVER_EXCEPTION, GETSEARCHRESULTS);
            }
            responseMap = this.workItemResultBuilder(se.getErrorCode());
        }
        catch (Exception e)
        {

            PIMUtil.writeLog(Level.ERROR, SERVICE_NAME, GETSEARCHRESULTS, e, PimConstants.ERRORTYPE_INPUT, PimConstants.DESC_EXCEPTION, null, PimConstants.DESC_EXCEPTION, GETSEARCHRESULTS);

            responseMap = this.workItemResultBuilder(ServerException.ERR_UNKNOWN);
        }
        manager.completeWorkItem(workItem.getId(), responseMap);
    }

    public Map<String, Object> indexKeywords(HttpServletRequest request,
                                             StreamSearchV2Service streamSearchV2Service,
                                             Campaign campaign,
                                             long storeId,
                                             String catalogId,
                                             String searchTerm,
                                             String locale) throws Exception
    {
        Map<String, Object> responseMap = this.workItemResultBuilder(PimConstants.CODE_RESP_SUCCESS);
        if(streamSearchV2Service != null && campaign != null && searchTerm != null)
        {
            try
            {
                streamSearchV2Service.indexKeywords(storeId, campaign.getId(), catalogId, searchTerm, locale);
            }
            catch(ServerException se)
            {
                
            }
        }
        return responseMap;}

    /**
     * <p>This method construct the result map.</p>
     *
     * @param errorCode Represents the response code to return.
     * @param pimResponse {@link com.skava.model.pim.PimResponse} Represents the product loaded and response to return.
     * @param productIds {@link java.lang.String} Represents the product ids to return.
     * @return {@link java.util.Map}Returns the response map of <code>GetSearchResults</code> class.
     * <p>Below are the keys present in the output map</p>
     *  <ul>
     *  <li>PARAM_RESP_CODE - responseCode</li>
     *  <li>PARAM_PIM_RESPONSE - pimResponse </li>
     *  <li>PARAM_PRODUCT_IDS - productIds</li>
     *  </ul>
     */
    private Map<String, Object> workItemResultBuilder(int errorCode)
    {
        Map<String, Object> workItemResults = new HashMap<>();
        workItemResults.put(PimKraftConstants.PARAM_INDEX_RESP_CODE, errorCode);
        return workItemResults;
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager)
    {
        //nothing to be done while aborting
    }
}
