package com.skava.transform;

import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import com.skava.builder.interfaces.SkavaTenantContextFactory;
import com.skava.interfaces.ResponseFormatter;
import com.skava.model.com.request.SkavaCOMRequest;
import com.skava.model.transform.StreamResponseFormatter;
import com.skava.model.transform.Type;
import com.skava.services.HttpClientService;
import com.skava.util.ConfigManagerInstance;
import com.skava.util.JSONUtils;
import com.skava.util.ServerException;
import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import org.json.JSONException;

public class KraftrecipesResponseFormatter implements ResponseFormatter
{
    static SkavaLogger logger = SkavaLoggerFactory.getLogger(KraftRecipesSmartzoneHandler.class);
    private static JSONObject typeData;
    private static JSONArray sortData;
    private static JSONArray facetData;

    public KraftrecipesResponseFormatter()
    {

    }

    public KraftrecipesResponseFormatter(SkavaTenantContextFactory skavaTenantContextFactory)
    {
        try
        {
            JSONObject userPrefData = ConfigManagerInstance.getJSON("detailsmap", new JSONObject());
            typeData = (JSONObject) JSONUtils.safeGetJSONObject("typemapping", userPrefData);
            sortData = (JSONArray) JSONUtils.safeGetJSONArray("sort", userPrefData);
            facetData = (JSONArray) JSONUtils.safeGetJSONArray("facets", userPrefData);
        }
        catch (Exception e)
        {
            logger.error("Error occurred at processing typemapping", e);
        }
    }

    public JSONObject format(Type typeConfig,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             HttpClientService httpClientService,
                             HashMap<String, List<String>> inputParams,
                             SkavaCOMRequest comRequest,
                             JSONObject rootJSON,
                             StreamResponseFormatter responseFormatterModel) throws ServerException
    {
        try
        {
            String orderFlag = getInputparamValue(inputParams, "orderchange");
            if (rootJSON != null)
            {
                if (typeConfig.getTypeFamily().equals("smartzone"))
                {
                    JSONArray products = new JSONArray();
                    if (rootJSON.has("children") && rootJSON.getJSONObject("children") != null)
                    {
                        JSONObject childobj = (JSONObject) JSONUtils.safeGetJSONObject("children", rootJSON);
                        if (rootJSON.getJSONObject("children").has("products"))
                        {
                            products = (JSONArray) JSONUtils.safeGetJSONArray("products", childobj);
                        }
                        if (rootJSON.getJSONObject("children").has("products_search"))
                        {
                            JSONArray searchProduct = (JSONArray) JSONUtils.safeGetJSONArray("products_search", childobj);
                            if (orderFlag == "true")
                            {
                                products = getProductsData(inputParams, searchProduct, products, "true");
                            }
                            else
                            {
                                products = getProductsData(inputParams, products, searchProduct, "false");
                            }

                            childobj.remove("products_search");
                            childobj.put("products", products);
                            if (rootJSON.has("responseCode") && rootJSON.has("responseMessage"))
                            {
                                rootJSON.put("responseCode", "0");
                                rootJSON.put("responseMessage", "Success");
                            }
                        }
                        if (rootJSON.getJSONObject("children").has("products_default"))
                        {
                            JSONArray defaultProduct = (JSONArray) JSONUtils.safeGetJSONArray("products_default", childobj);
                            products = getProductsData(inputParams, products, defaultProduct, "true");

                            childobj.remove("products_default");
                            childobj.put("products", products);
                            if (rootJSON.has("responseCode") && rootJSON.has("responseMessage"))
                            {
                                rootJSON.put("responseCode", "0");
                                rootJSON.put("responseMessage", "Success");
                            }
                        }
                        if (childobj.has("products") && childobj.getJSONArray("products") != null)
                        {
                            JSONArray productobj = JSONUtils.safeGetJSONArray("products", childobj);
                            if (productobj != null && productobj.length() > 0)
                            {
                                for (int indx = 0; indx < productobj.length(); indx++)
                                {
                                    Object obj = JSONUtils.safeGetJSONObject("properties.iteminfo", productobj.getJSONObject(indx));
                                    if (obj instanceof JSONObject)
                                    {
                                        JSONObject itemObj = (JSONObject) obj;
                                        String typeStr = JSONUtils.safeGetStringValue(itemObj, "itemtype", null);
                                        if (typeStr != null && typeStr.length() > 0 && typeData != null && typeData.has(typeStr) && typeData.get(typeStr) != null)
                                        {
                                            itemObj.put("itemtype", typeData.get(typeStr));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rootJSON.has("properties"))
                    {
                        rootJSON.remove("properties");
                    }
                    return rootJSON;
                }
                if (typeConfig.getIdPattern().toString().equals("similarsearch"))
                {
                    JSONArray products = null;
                    int productlimt = 0;
                    if (rootJSON.has("children") && rootJSON.getJSONObject("children") != null)
                    {
                        JSONObject childobj = (JSONObject) JSONUtils.safeGetJSONObject("children", rootJSON);
                        if (rootJSON.getJSONObject("children").has("products_search"))
                        {
                            if (rootJSON.getJSONObject("children").has("products"))
                            {
                                products = (JSONArray) JSONUtils.safeGetJSONArray("products", childobj);
                            }
                            JSONArray searchProduct = (JSONArray) JSONUtils.safeGetJSONArray("products_search", childobj);
                            int limit = Integer.parseInt(getInputparamValue(inputParams, "limit"));

                            if (products != null && products.length() > 0)
                            {
                                productlimt = products.length();
                            }
                            int totalLimit = ((limit - productlimt) > searchProduct.length()) ? searchProduct.length() : (limit - productlimt);
                            if (searchProduct != null && searchProduct.length() > 0 && productlimt < limit)
                            {
                                if (products == null)
                                {
                                    products = new JSONArray();
                                }
                                for (int k = 0; k < totalLimit; k++)
                                {
                                    products.put(searchProduct.getJSONObject(k));
                                }
                            }
                            childobj.remove("products_search");
                            childobj.put("products", products);
                            if (rootJSON.has("responseCode") && rootJSON.has("responseMessage"))
                            {
                                rootJSON.put("responseCode", "0");
                                rootJSON.put("responseMessage", "Success");
                            }
                        }
                        if (childobj.has("products") && childobj.getJSONArray("products") != null)
                        {
                            JSONArray productobj = JSONUtils.safeGetJSONArray("products", childobj);
                            if (productobj != null && productobj.length() > 0)
                            {
                                for (int indx = 0; indx < productobj.length(); indx++)
                                {
                                    Object obj = JSONUtils.safeGetJSONObject("properties.iteminfo", productobj.getJSONObject(indx));
                                    if (obj instanceof JSONObject)
                                    {
                                        JSONObject itemObj = (JSONObject) obj;
                                        String typeStr = JSONUtils.safeGetStringValue(itemObj, "itemtype", null);
                                        if (typeStr != null && typeStr.length() > 0 && typeData != null && typeData.has(typeStr) && typeData.get(typeStr) != null)
                                        {
                                            itemObj.put("itemtype", typeData.get(typeStr));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return rootJSON;
                }
                if (typeConfig.getTypeFamily().equals("product"))
                {
                    if ((rootJSON.has("children") && rootJSON.getJSONObject("children").has("skus")))
                    {
                        JSONArray categories = JSONUtils.safeGetJSONArray("skus", (JSONObject) JSONUtils.safeGetJSONObject("children", rootJSON));
                        List<JSONObject> list = new LinkedList<JSONObject>();
                        List<JSONObject> Unsortedlist = new LinkedList<JSONObject>();
                        JSONArray toRet = new JSONArray();
                        boolean lengthFlag = false;
                        for (int i = 0; i < categories.length(); i++)
                        {
                            lengthFlag = (i == (categories.length() - 1)) ? true : false;
                            toRet = sortData(categories, i, list, Unsortedlist, lengthFlag);
                        }
                        if (toRet != null && toRet.length() > 0)
                        {
                            JSONUtils.removePropertyByPath("children.skus", rootJSON);
                            JSONObject jsonchildren = rootJSON.getJSONObject("children");
                            jsonchildren.put("skus", toRet);
                        }
                    }
                    if ((rootJSON.has("properties") && rootJSON.getJSONObject("properties").has("iteminfo")))
                    {
                        JSONObject properties = rootJSON.getJSONObject("properties");
                        JSONObject iteminfo = properties.getJSONObject("iteminfo");
                        if (iteminfo.has("specifications"))
                        {
                            JSONArray specsArr = (JSONArray) JSONUtils.getJSONByPath("specifications", iteminfo);
                            if (specsArr != null && specsArr.length() > 0)
                            {
                                for (int indx = 0; indx < specsArr.length(); indx++)
                                {
                                    if (specsArr.get(indx) != null)
                                    {
                                        JSONObject getParams = specsArr.getJSONObject(indx);
                                        String name = JSONUtils.safeGetStringValue(getParams, "name", null);
                                        if (name != null && name.equals("nutritionitems"))
                                        {
                                            JSONArray getparamsList = JSONUtils.safeGetJSONArray("params", getParams);
                                            specsArr = nutritionData(getparamsList);
                                            iteminfo.put("specifications", specsArr);
                                        }
                                    }
                                }
                            }
                        }
                        if (iteminfo.has("additionalimages"))
                        {
                            JSONArray addiImgArr = (JSONArray) JSONUtils.getJSONByPath("additionalimages", iteminfo);
                            if (addiImgArr != null && addiImgArr.length() > 0)
                            {
                                JSONArray altImgObj = new JSONArray();
                                for (int indx = 0; indx < addiImgArr.length(); indx++)
                                {
                                    if (addiImgArr.get(indx) != null)
                                    {
                                        JSONObject getParams = addiImgArr.getJSONObject(indx);
                                        String label = JSONUtils.safeGetStringValue(getParams, "label", null);
                                        String value = JSONUtils.safeGetStringValue(getParams, "value", null);
                                        if (label != null && label.equals("altimages") & value != null)
                                        {
                                            String valueList[] = JSONUtils.safeGetStringValue(getParams, "value", null).split(",");
                                            if (valueList != null && valueList.length > 0)
                                            {
                                                for (int i = 0; i < valueList.length; i++)
                                                {
                                                    JSONObject altImgArr = new JSONObject();
                                                    altImgArr.put("label", "altimages");
                                                    altImgArr.put("value", valueList[i]);
                                                    altImgObj.put(altImgArr);
                                                }
                                            }
                                        }
                                        if (getParams.has("alttext"))
                                        {
                                            JSONObject alttext = getParams.getJSONObject("alttext");
                                            if (label != null && label.equals("assets") & alttext != null)
                                            {
                                                Iterator<String> keysList = alttext.keys();
                                                while (keysList != null && keysList.hasNext())
                                                {
                                                    JSONObject altImgArr = new JSONObject();
                                                    String key = keysList.next();
                                                    if (key != null)
                                                    {
                                                        JSONObject altURL = alttext.getJSONObject(key);
                                                        if (altURL != null && altURL.getString("asseturl") != null && altURL.getString("damurl") != null)
                                                        {
                                                            altImgArr.put("label", "altimages");
                                                            altImgArr.put("name", key);
                                                            altImgArr.put("image", altURL.getString("asseturl"));
                                                            altImgArr.put("value", altURL.getString("damurl"));
                                                        }
                                                    }
                                                    altImgObj.put(altImgArr);
                                                }
                                            }
                                        }
                                    }
                                }
                                iteminfo.put("additionalimages", altImgObj);
                            }
                        }
                        if (iteminfo.has("itemtype"))
                        {
                            String typeStr = JSONUtils.safeGetStringValue(iteminfo, "itemtype", null);
                            if (typeStr != null && typeStr.length() > 0 && typeData != null && typeData.has(typeStr) && typeData.get(typeStr) != null)
                            {
                                iteminfo.put("itemtype", typeData.get(typeStr));
                            }
                        }
                    }
                    if ((rootJSON.has("properties") && rootJSON.getJSONObject("properties").has("skuprops")))
                    {
                        JSONObject properties = rootJSON.getJSONObject("properties");
                        JSONObject skuprops = properties.getJSONObject("skuprops");
                        if (skuprops.has("size1"))
                        {
                            JSONArray sizeArr = (JSONArray) JSONUtils.getJSONByPath("size1", skuprops);
                            if (sizeArr != null && sizeArr.length() > 0 && sizeArr.get(0) != null)
                            {
                                JSONObject getParams = sizeArr.getJSONObject(0);
                                if (getParams.has("value"))
                                {
                                    JSONArray getvalueList = JSONUtils.safeGetJSONArray("value", getParams);
                                    sizeArr = sizevariantData(getvalueList);
                                    skuprops.put("size1", sizeArr);
                                }
                            }
                        }
                    }
                }
                if (typeConfig.getIdPattern().toString().equals("grid") || typeConfig.getIdPattern().toString().equals("search") || typeConfig.getTypeFamily().equals("productlist"))
                {
                    if (rootJSON.has("children") && rootJSON.getJSONObject("children") != null)
                    {
                        JSONObject childobj = (JSONObject) JSONUtils.safeGetJSONObject("children", rootJSON);
                        if (childobj.has("products") && childobj.getJSONArray("products") != null)
                        {
                            JSONArray productobj = JSONUtils.safeGetJSONArray("products", childobj);
                            productobj = getItemType(productobj);
                        }
                        if (childobj.has("featuredproducts") && childobj.getJSONArray("featuredproducts") != null)
                        {
                            JSONArray productobj = JSONUtils.safeGetJSONArray("featuredproducts", childobj);
                            productobj = getItemType(productobj);
                        }
                    }
                    if (rootJSON.has("properties") && rootJSON.getJSONObject("properties") != null)
                    {
                        JSONObject properties = rootJSON.getJSONObject("properties");
                        if (properties.has("state") && properties.getJSONObject("state") != null)
                        {
                            JSONArray finalSortObjects = new JSONArray();
                            JSONObject state = properties.getJSONObject("state");
                            if (state.has("sorting") && state.getJSONArray("sorting") != null && state.getJSONArray("sorting").length() > 0)
                            {
                                JSONArray sorting = JSONUtils.safeGetJSONArray("sorting", state);
                                JSONObject sortOptions = sorting.getJSONObject(0);
                                if (sortOptions.has("options") && sortOptions.getJSONArray("options") != null && sortOptions.getJSONArray("options").length() > 0)
                                {
                                    JSONArray availableSorts = sortOptions.getJSONArray("options");
                                    for (int i = 0; i < availableSorts.length(); i++)
                                    {
                                        JSONObject sortValues = availableSorts.getJSONObject(i);
                                        if (sortValues.has("value") && sortValues.getString("value") != null)
                                        {
                                            String sortLabelName = sortValues.getString("value");
                                            for (int itr = 0; itr < sortData.length(); itr++)
                                            {
                                                String sortName = (String) sortData.get(itr);
                                                if (sortName.equalsIgnoreCase(sortLabelName))
                                                {
                                                    finalSortObjects.put(sortValues);
                                                }
                                            }
                                        }
                                    }
                                    sortOptions.put("options", finalSortObjects);
                                }
                            }
                        }
                    }
                    if (rootJSON.has("facets") && rootJSON.getJSONArray("facets") != null && rootJSON.getJSONArray("facets").length() > 0)
                    {
                        JSONArray facets = rootJSON.getJSONArray("facets");
                        JSONArray facetArray = putDisplayName(facets, "name");
                        rootJSON.put("facets", facetArray);
                    }
                    if (rootJSON.has("properties") && rootJSON.getJSONObject("properties") != null && rootJSON.getJSONObject("properties").length() > 0)
                    {
                        JSONObject properties = rootJSON.getJSONObject("properties");
                        if (properties.has("state") && properties.getJSONObject("state") != null && properties.getJSONObject("state").length() > 0)
                        {
                            JSONObject state = properties.getJSONObject("state");
                            if (state.has("selectedFacets") && state.getJSONArray("selectedFacets") != null && state.getJSONArray("selectedFacets").length() > 0)
                            {
                                JSONArray facets = state.getJSONArray("selectedFacets");
                                JSONArray facetArray = putDisplayName(facets, "label");
                                state.put("selectedFacets", facetArray);
                            }
                        }
                    }
                }
                if (typeConfig.getTypeFamily().toString().equals("productdetails"))
                {
                    if (rootJSON.has("children") && rootJSON.getJSONObject("children") != null)
                    {
                        JSONObject childobj = (JSONObject) JSONUtils.safeGetJSONObject("children", rootJSON);
                        if (childobj.has("products") && childobj.getJSONArray("products") != null)
                        {
                            JSONArray productobj = JSONUtils.safeGetJSONArray("products", childobj);
                            productobj = getItemType(productobj);
                        }
                    }
                }
                if (typeConfig.getTypeFamily().equals("category") && rootJSON.has("children") && rootJSON.getJSONObject("children").has("categories"))
                {
                    sortingCategories(rootJSON);
                }
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ServerException)
            {
                throw (ServerException) t;
            }
            else
            {
                throw new ServerException(t);
            }
        }
        return rootJSON;
    }

    private JSONArray nutritionData(JSONArray getparamsList) throws Exception
    {
        JSONArray toRet = new JSONArray();
        String[] nutritionName = null, nutritionUnit = null,
                nutritionQty = null;
        for (int paramsObj = 0; paramsObj < getparamsList.length(); paramsObj++)
        {
            JSONObject getValues = getparamsList.getJSONObject(paramsObj);
            if (getValues.has("value") && getValues.get("value") != null)
            {
                String getSplitString = getValues.get("value").toString();
                if (getValues.has("label") && getValues.get("label") != null && getValues.get("label").toString().equals("nutritionitemname"))
                {
                    nutritionName = getSplitString.split(",");
                }
                if (getValues.get("label") != null && getValues.get("label").toString().equals("unit"))
                {
                    nutritionUnit = getSplitString.split(",");
                }
                if (getValues.get("label") != null && getValues.get("label").toString().equals("quantity"))
                {
                    nutritionQty = getSplitString.split(",");
                }
            }
        }
        if (nutritionName != null && nutritionName.length > 0 && nutritionUnit != null && nutritionUnit.length > 0 && nutritionQty != null && nutritionQty.length > 0)
        {
            JSONObject specnutritionObj = new JSONObject();
            specnutritionObj.put("label", "nutrition");
            JSONArray specnutritionArr = new JSONArray();
            JSONObject nutObj;
            for (int nutri = 0; nutri < nutritionName.length; nutri++)
            {
                nutObj = new JSONObject();
                nutObj.put("identifier", (nutritionName[nutri] != null && !nutritionName[nutri].isEmpty() && !(nutritionName[nutri].equalsIgnoreCase("empty"))) ? nutritionName[nutri] : "");
                nutObj.put("label", (nutritionUnit.length > nutri && nutritionUnit[nutri] != null && !nutritionUnit[nutri].isEmpty() && !(nutritionUnit[nutri].equalsIgnoreCase("empty"))) ? nutritionUnit[nutri].trim() : "");
                nutObj.put("value", (nutritionQty.length > nutri && nutritionQty[nutri] != null && !nutritionQty[nutri].isEmpty() && !(nutritionQty[nutri].equalsIgnoreCase("empty"))) ? nutritionQty[nutri].trim() : "0");
                specnutritionArr.put(nutObj);
            }
            specnutritionObj.put("additionalspecs", specnutritionArr);
            toRet.put(specnutritionObj);
        }
        return toRet;
    }

    private JSONArray sortData(JSONArray categories,
                               int k,
                               List<JSONObject> list,
                               List<JSONObject> Unsortedlist,
                               boolean lengthFlag) throws Exception
    {
        Object propobj;
        try
        {
            propobj = JSONUtils.safeGetJSONObject("properties", categories.getJSONObject(k));
            if (propobj != null && propobj instanceof JSONObject)
            {
                JSONObject propJobj = (JSONObject) propobj;
                JSONObject stateJobj = (JSONObject) JSONUtils.safeGetJSONObject("state", propJobj);
                JSONArray sorting = JSONUtils.safeGetJSONArray("sorting", stateJobj);
                if (sorting != null && sorting.length() > 0)
                {
                    JSONArray options = JSONUtils.safeGetJSONArray("options", sorting.getJSONObject(0));
                    if (options != null)
                    {
                        list.add(categories.getJSONObject(k));
                    }
                }
                else
                {
                    Unsortedlist.add(categories.getJSONObject(k));
                }
            }
            if (lengthFlag)
            {
                Collections.sort(list, new Comparator<JSONObject>()
                {
                    public int compare(JSONObject o1, JSONObject o2)
                    {
                        int obj1 = 0, obj2 = 0;
                        try
                        {
                            String obj1str = null, obj2str = null;
                            obj1str = o1.getJSONObject("properties").getJSONObject("state").getJSONArray("sorting").getJSONObject(0).getJSONArray("options").getJSONObject(0).getString("value");
                            obj1 = Integer.parseInt(obj1str);

                            obj2str = o2.getJSONObject("properties").getJSONObject("state").getJSONArray("sorting").getJSONObject(0).getJSONArray("options").getJSONObject(0).getString("value");
                            obj2 = Integer.parseInt(obj2str);
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                        return obj1 - obj2;
                    }
                });
                JSONArray sortedArray = new JSONArray();
                if (list != null)
                {
                    Iterator<JSONObject> listIterator = list.iterator();
                    while (listIterator.hasNext())
                    {
                        sortedArray.put(listIterator.next());
                    }
                }
                if (Unsortedlist != null)
                {
                    Iterator<JSONObject> listIterator = Unsortedlist.iterator();
                    while (listIterator.hasNext())
                    {
                        sortedArray.put(listIterator.next());
                    }
                }
                return sortedArray;
            }
        }
        catch (JSONException e)
        {
            throw new JSONException("Unable to parse JSON");
        }
        return null;
    }

    private String getInputparamValue(HashMap<String, List<String>> inputParams,
                                      String paramName)
    {
        if (inputParams != null && inputParams.containsKey(paramName) && inputParams.get(paramName).size() > 0) { return inputParams.get(paramName).get(0); }
        return null;
    }

    private JSONArray getProductsData(HashMap<String, List<String>> inputParams,
                                      JSONArray productsArr1,
                                      JSONArray productsArr2,
                                      String orderFlag)
    {
        try
        {
            LinkedHashSet<String> productSet = new LinkedHashSet<String>();
            LinkedHashSet<String> productSetNew = new LinkedHashSet<String>();
            if (productsArr1 != null && productsArr1.length() > 0)
            {
                for (int productsItr = 0; productsItr < productsArr1.length(); productsItr++)
                {
                    productSet.add(productsArr1.getJSONObject(productsItr).toString());
                }
            }
            if (productsArr2 != null && productsArr2.length() > 0)
            {
                for (int k = 0; k < productsArr2.length(); k++)
                {
                    productSet.add(productsArr2.getJSONObject(k).toString());
                }
            }
            if (productsArr1 != null && productsArr1.length() > 0 && inputParams.containsKey("sponsoredFlag") && orderFlag == "false")
            {
                String sponsoredFlag = getInputparamValue(inputParams, "sponsoredFlag");
                if (productSet != null && sponsoredFlag == "true")
                {
                    int setLimit = 0;
                    for (String prodObj : productSet)
                    {
                        if (prodObj != null)
                        {
                            JSONObject jObj = new JSONObject(prodObj);
                            if (setLimit < productsArr1.length())
                            {
                                if (jObj != null)
                                {
                                    setLimit = setLimit + 1;
                                    jObj.put("_name", "sponsored");
                                    productSetNew.add(jObj.toString());
                                }
                            }
                            else
                            {
                                productSetNew.add(jObj.toString());
                            }
                        }
                    }
                }
            }
            if (productSetNew != null && productSetNew.size() > 0)
            {
                productSet = productSetNew;
            }
            int totLimit = Integer.parseInt(getInputparamValue(inputParams, "limit"));
            if (productSet != null)
            {
                productsArr1 = new JSONArray();
                int setLimit = 0;
                for (String prodObj : productSet)
                {
                    if (setLimit < totLimit)
                    {
                        if (prodObj != null)
                        {
                            setLimit = setLimit + 1;
                            productsArr1.put(new JSONObject(prodObj));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error occurred at processing KraftrecipesResponseFormatter", e);
        }
        return productsArr1;

    }

    private JSONArray getItemType(JSONArray productobj)
    {
        try
        {
            if (productobj != null && productobj.length() > 0)
            {
                for (int indx = 0; indx < productobj.length(); indx++)
                {
                    Object obj = JSONUtils.safeGetJSONObject("properties.iteminfo", productobj.getJSONObject(indx));
                    if (obj instanceof JSONObject)
                    {
                        JSONObject itemObj = (JSONObject) obj;
                        String typeStr = JSONUtils.safeGetStringValue(itemObj, "itemtype", null);
                        if (typeStr != null && typeStr.length() > 0 && typeData != null && typeData.has(typeStr) && typeData.get(typeStr) != null)
                        {
                            itemObj.put("itemtype", typeData.get(typeStr));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error While Processing KraftrecipesResponseFormatter ", e);
        }
        return productobj;
    }
    
    private JSONArray sizevariantData(JSONArray getparamsList) throws Exception
    {
        JSONArray sizeArr = new JSONArray();
        String[] gtinIdentifier = null, sizeVariant = null, count = null;
        String label = null;
        for (int paramsObj = 0; paramsObj < getparamsList.length(); paramsObj++)
        {
            JSONObject getValues = getparamsList.getJSONObject(paramsObj);
            if (getValues.has("value") && getValues.get("value") != null)
            {
                String getSplitString = getValues.get("value").toString();
                if (getValues.has("label") && getValues.get("label") != null && getValues.get("label").toString().equals("defaultproductgtin"))
                {
                    gtinIdentifier = getSplitString.split(",");
                }
                if (getValues.get("label") != null && getValues.get("label").toString().equals("sizevariantoz"))
                {
                    sizeVariant = getSplitString.split(",");
                }
                if (getValues.get("label") != null && getValues.get("label").toString().equals("count"))
                {
                    count = getSplitString.split(",");
                }
                if (getValues.get("label") != null && getValues.get("label").toString().equals("declarednetcontentuomcode"))
                {
                    label = getSplitString;
                }
            }
        }
        if (gtinIdentifier != null && gtinIdentifier.length > 0 && sizeVariant != null && sizeVariant.length > 0 && count != null && count.length > 0)
        {
            JSONObject sizeObj;
            for (int nutri = 0; nutri < gtinIdentifier.length; nutri++)
            {
                sizeObj = new JSONObject();
                sizeObj.put("identifier", gtinIdentifier[nutri] != null ? gtinIdentifier[nutri] : "");
                sizeObj.put("value", (sizeVariant.length > nutri && sizeVariant[nutri] != null) ? sizeVariant[nutri].trim() : "");
                sizeObj.put("count", (count.length > nutri && count[nutri] != null) ? count[nutri].trim() : "");
                sizeObj.put("label", (label != null) ? label : "");
                sizeArr.put(sizeObj);
            }
            return sizeArr;
        }
        return sizeArr;
    }

    private JSONArray putDisplayName(JSONArray facets,
                                     String name) throws Exception
    {
        JSONArray facetArray = new JSONArray();
        for (int i = 0; i < facets.length(); i++)
        {
            JSONObject facetValue = facets.getJSONObject(i);
            if (facetValue != null && facetValue.has(name) && facetValue.getString(name) != null)
            {
                String facetName = facetValue.getString(name);
                if (facetData != null && facetData.length() > 0)
                {
                    for (int itr = 0; itr < facetData.length(); itr++)
                    {
                        JSONObject facet = (JSONObject) facetData.getJSONObject(itr);
                        if (facet != null && facet.length() > 0)
                        {
                            if (facet.has("name") && facet.getString("name") != null)
                            {
                                String fName = facet.getString("name");
                                if (fName.equalsIgnoreCase(facetName))
                                {
                                    if (facet.has("displayname") && facet.getString("displayname") != null)
                                    {
                                        facetValue.put("displayname", facet.getString("displayname"));
                                    }
                                    facetArray.put(facetValue);
                                }
                            }
                        }
                    }
                }
            }
        }
        return facetArray;
    }

    private void sortingCategories(JSONObject rootJSON) throws ServerException
    {
        try
        {
            JSONArray categories = JSONUtils.safeGetJSONArray("categories", (JSONObject) JSONUtils.safeGetJSONObject("children", rootJSON));
            List<JSONObject> list = new LinkedList<JSONObject>();
            List<JSONObject> Unsortedlist = new LinkedList<JSONObject>();
            JSONArray toRet = new JSONArray();
            boolean lengthFlag = false;
            for (int i = 0; i < categories.length(); i++)
            {
                JSONObject subchild = categories.getJSONObject(i);
                if (subchild.has("children") && subchild.getJSONObject("children").has("categories"))
                {
                    sortingCategories(subchild);
                }
                lengthFlag = (i == (categories.length() - 1)) ? true : false;
                toRet = sortData(categories, i, list, Unsortedlist, lengthFlag);
            }
            if (toRet != null && toRet.length() > 0)
            {
                JSONUtils.removePropertyByPath("children.categories", rootJSON);
                JSONObject jsonchildren = rootJSON.getJSONObject("children");
                jsonchildren.put("categories", toRet);
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ServerException)
            {
                throw (ServerException) t;
            }
            else
            {
                throw new ServerException(t);
            }
        }
    }
}
