package com.skava.index.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.skava.util.SkavaLogger;
import com.skava.util.SkavaLoggerFactory;

import com.skava.index.message.SearchProductIndexMessage;
import com.skava.interfaces.SolrDocumentInterface;
import com.skava.model.TenantThreadLocal;
import com.skava.model.dbbeans.Campaign;
import com.skava.model.dbbeans.CampaignProperties;
import com.skava.model.dbbeans.StreamV2COMPrice;
import com.skava.model.dbbeans.StreamV2COMSize;
import com.skava.model.jmq.JMQMessage;
import com.skava.model.jmq.JMQProductMessage;
import com.skava.model.pim.CategoryMaster;
import com.skava.model.pim.DataType;
import com.skava.model.pim.FacetMaster;
import com.skava.model.pim.FieldType;
import com.skava.model.pim.PIMItemProperty;
import com.skava.model.pim.ProductCategory;
import com.skava.model.pim.ProductMaster;
import com.skava.model.pim.SkuMaster;
import com.skava.model.pim.CatalogCategory;
import com.skava.services.StreamSearchService;
import com.skava.util.CastUtil;
import com.skava.util.JSONUtils;
import com.skava.util.ReadUtil;
import com.skava.util.ServerException;
import com.skava.util.StringUtil;

import lombok.Getter;
import lombok.Setter;

public class PimKraftSolrDocument implements SolrDocumentInterface
{
    private static final String FACETNAME = "name";

    Campaign pimCampaign;
    Campaign searchCampaign;
    ProductMaster product;
    SkavaLogger logger = SkavaLoggerFactory.getLogger(getClass());
    String locale;
    HashMap<String, FacetMaster> facetMap;
    HashMap<String, String> dataMap;
    Map<String, CategoryMaster> categoryMap;
    Map<String, CatalogCategory> catalogCategoryMap;
    List<String> catalogIdsWithCategoryMap;
    long storeId;

    public void init(Campaign pimCampaign,
                     Campaign searchCampaign,
                     long storeId,
                     ProductMaster product,
                     String locale,
                     HashMap<String, FacetMaster> facetMap,
                     HashMap<String, String> dataMap,
                     Map<String, CategoryMaster> categoryMap,
                     Map<String, CatalogCategory> catalogCategoryMap,
                     List<String> catalogIdsWithCategoryMap)
    {
        this.pimCampaign = pimCampaign;
        this.searchCampaign = searchCampaign;
        this.storeId = storeId;
        this.product = product;
        this.locale = locale;
        this.facetMap = facetMap;
        this.dataMap = dataMap;
        this.categoryMap = categoryMap;
        this.catalogCategoryMap = catalogCategoryMap;
        this.catalogIdsWithCategoryMap = catalogIdsWithCategoryMap;
    }

    public ArrayList<JMQMessage> getSolrIndexMessages() throws ServerException
    {

        ArrayList<JMQMessage> toRet = null;
        try
        {
        if (product != null)
        {
            logger.info("getSolrIndexMessages" + product.getProductId());
            Set<String> categoryIds = new LinkedHashSet<>();
            Set<String> categoryNames = new LinkedHashSet<>();
            List<String> categoryLevel1 = new ArrayList<>();
            List<String> categoryLevel2 = new ArrayList<>();
            List<String> categoryLevel3 = new ArrayList<>();
            List<String> categoryLevel4 = new ArrayList<>();
            List<String> categoryLevel5 = new ArrayList<>();
            if (product.getCategories() != null)
            {
                for (ProductCategory productCategory : product.getCategories())
                {
                    if (productCategory != null && productCategory.getCategoryId() != null)
                    {
                            addCategory(pimCampaign, productCategory.getCategoryId(), categoryIds, categoryNames, categoryMap, categoryLevel1, categoryLevel2, categoryLevel3, categoryLevel4, categoryLevel5);
                    }
                }
            }
            if (product.getDefaultParentCategoryId() != null && !categoryIds.contains(product.getDefaultParentCategoryId()))
            {
                    addCategory(pimCampaign, product.getDefaultParentCategoryId(), categoryIds, categoryNames, categoryMap, categoryLevel1, categoryLevel2, categoryLevel3, categoryLevel4, categoryLevel5);
            }
            String[] categoryIdsArray = categoryIds.isEmpty() ? null : categoryIds.toArray(new String[categoryIds.size()]);
            String[] categoryNamesArray = categoryNames.isEmpty() ? null : categoryNames.toArray(new String[categoryNames.size()]);
            String[] categoryLevel1Array = categoryLevel1.isEmpty() ? null : categoryLevel1.toArray(new String[categoryLevel1.size()]);
            String[] categoryLevel2Array = categoryLevel2.isEmpty() ? null : categoryLevel2.toArray(new String[categoryLevel2.size()]);
            String[] categoryLevel3Array = categoryLevel3.isEmpty() ? null : categoryLevel3.toArray(new String[categoryLevel3.size()]);
            String[] categoryLevel4Array = categoryLevel4.isEmpty() ? null : categoryLevel4.toArray(new String[categoryLevel4.size()]);
            String[] categoryLevel5Array = categoryLevel5.isEmpty() ? null : categoryLevel5.toArray(new String[categoryLevel5.size()]);

            if (product.getCatalogSkuMap() != null && !product.getCatalogSkuMap().isEmpty())
            {
                Map<String, HashSet<SkuMaster>> catalogSkuMap = product.getCatalogSkuMap();
                Map<String, HashSet<ProductMaster>> catalogProductMap = product.getCatalogProductMap();
                Set<String> catalogIds = catalogSkuMap.keySet();
                for (String catalogId : catalogIds)
                {
                    HashSet<SkuMaster> skuSet = catalogSkuMap.get(catalogId);
                    if (skuSet != null && !skuSet.isEmpty())
                    {
                        ArrayList<String> skuids = new ArrayList<>();
                        for (SkuMaster sku : skuSet)
                        {
                            skuids.add(sku.getSkuId());
                        }
                        for (SkuMaster skuMaster : skuSet)
                        {
                            if (toRet == null)
                            {
                                toRet = new ArrayList<>();
                            }
                            if (catalogProductMap != null && catalogProductMap.containsKey(catalogId))
                            {
                                HashSet<ProductMaster> catalogProductSet = catalogProductMap.get(catalogId);
                                Set<String> catalogCategoryIds = null;
                                Set<String> catalogCategoryNames = null;
                                List<String> catalogCategoryLevel1 = null;
                                List<String> catalogCategoryLevel2 = null;
                                List<String> catalogCategoryLevel3 = null;
                                List<String> catalogCategoryLevel4 = null;
                                List<String> catalogCategoryLevel5 = null;
                                for (ProductMaster catalogProduct : catalogProductSet)
                                {
                                    SearchProductIndexMessage indexMessage = null;
                                    JMQProductMessage productMessage = null;
                                    if(catalogIdsWithCategoryMap != null && catalogIdsWithCategoryMap.contains(catalogId) && catalogCategoryMap != null)
                                    {
                                        catalogCategoryIds = new LinkedHashSet<>();
                                        catalogCategoryNames = new LinkedHashSet<>();
                                        catalogCategoryLevel1 = new ArrayList<>();
                                        catalogCategoryLevel2 = new ArrayList<>();
                                        catalogCategoryLevel3 = new ArrayList<>();
                                        catalogCategoryLevel4 = new ArrayList<>();
                                        catalogCategoryLevel5 = new ArrayList<>();
                                        if (catalogProduct.getCategories() != null)
                                        {
                                            for (ProductCategory productCategory : catalogProduct.getCategories())
                                            {
                                                if (productCategory != null && productCategory.getCategoryId() != null && productCategory.getStatus() == ProductCategory.STATUS_ACTIVE)
                                                {
                                                        addcatalogCategory(pimCampaign, catalogId + SolrDocumentInterface.DELIMITER + productCategory.getCategoryId(), catalogCategoryMap, catalogCategoryIds, catalogCategoryNames, catalogCategoryLevel1, catalogCategoryLevel2, catalogCategoryLevel3, catalogCategoryLevel4, catalogCategoryLevel5);
                                                }
                                            }
                                        }
                                        if (catalogProduct.getDefaultParentCategoryId() != null && !catalogCategoryIds.contains(catalogProduct.getDefaultParentCategoryId()))
                                        {
                                                addcatalogCategory(pimCampaign, catalogId + "_" + catalogProduct.getDefaultParentCategoryId(), catalogCategoryMap, catalogCategoryIds, catalogCategoryNames, catalogCategoryLevel1, catalogCategoryLevel2, catalogCategoryLevel3, catalogCategoryLevel4, catalogCategoryLevel5);
                                        }
                                        String[] catalogCategoryIdsArray = catalogCategoryIds.isEmpty() ? null : catalogCategoryIds.toArray(new String[catalogCategoryIds.size()]);
                                        String[] catalogCategoryNamesArray = catalogCategoryNames.isEmpty() ? null : catalogCategoryNames.toArray(new String[catalogCategoryNames.size()]);
                                        String[] catalogCategoryLevel1Array = catalogCategoryLevel1.isEmpty() ? null : catalogCategoryLevel1.toArray(new String[catalogCategoryLevel1.size()]);
                                        String[] catalogCategoryLevel2Array = catalogCategoryLevel2.isEmpty() ? null : catalogCategoryLevel2.toArray(new String[catalogCategoryLevel2.size()]);
                                        String[] catalogCategoryLevel3Array = catalogCategoryLevel3.isEmpty() ? null : catalogCategoryLevel3.toArray(new String[catalogCategoryLevel3.size()]);
                                        String[] catalogCategoryLevel4Array = catalogCategoryLevel4.isEmpty() ? null : catalogCategoryLevel4.toArray(new String[catalogCategoryLevel4.size()]);
                                        String[] catalogCategoryLevel5Array = catalogCategoryLevel5.isEmpty() ? null : catalogCategoryLevel5.toArray(new String[catalogCategoryLevel5.size()]);
                                        indexMessage = prepareMessage(storeId, catalogProduct, skuMaster, skuids, catalogId, catalogCategoryIdsArray, catalogCategoryNamesArray, catalogCategoryLevel1Array, catalogCategoryLevel2Array, catalogCategoryLevel3Array, catalogCategoryLevel4Array, catalogCategoryLevel5Array);
                                        productMessage = new JMQProductMessage(indexMessage, catalogProduct, null, catalogCategoryNamesArray, catalogCategoryLevel1Array, catalogCategoryLevel2Array, catalogCategoryLevel3Array, catalogCategoryLevel4Array, catalogCategoryLevel5Array, catalogId);
                                    }
                                    else
                                    {
                                        indexMessage = prepareMessage(storeId, catalogProduct, skuMaster, skuids, catalogId, categoryIdsArray, categoryNamesArray, categoryLevel1Array, categoryLevel2Array, categoryLevel3Array, categoryLevel4Array, categoryLevel5Array);
                                        productMessage = new JMQProductMessage(indexMessage, catalogProduct, null, categoryNamesArray, categoryLevel1Array, categoryLevel2Array, categoryLevel3Array, categoryLevel4Array, categoryLevel5Array, catalogId);
                                    }
                                    toRet.add((JMQMessage) productMessage);
                                }
                            }
                        }
                    }
                }
            }

            SkuMaster[] skus = product.getSkus();
            if (skus != null && skus.length > 0)
            {
                ArrayList<String> skuids = new ArrayList<>();
                for (SkuMaster sku : skus)
                {
                    skuids.add(sku.getSkuId());
                }
                for (SkuMaster sku : skus)
                {
                    if (toRet == null)
                    {
                        toRet = new ArrayList<>();
                    }
                    SearchProductIndexMessage indexMessage = prepareMessage(storeId, product, sku, skuids, "0", categoryIdsArray, categoryNamesArray, categoryLevel1Array, categoryLevel2Array, categoryLevel3Array, categoryLevel4Array, categoryLevel5Array);
                    JMQProductMessage productMessage = new JMQProductMessage(indexMessage, product, null, categoryNamesArray, categoryLevel1Array, categoryLevel2Array, categoryLevel3Array, categoryLevel4Array, categoryLevel5Array, "0");
                    toRet.add((JMQMessage) productMessage);
                }
            }
        }
        }
        catch (Exception e)
        {
            logger.error("Exception occured on processing PimSolrDocument", e);
        }
        return toRet;
    }

    void addCategory(Campaign campaign,
                     String categoryId,
                     Set<String> categoryIds,
                     Set<String> categoryNames,
                     Map<String, CategoryMaster> categoryMap,
                     List<String> categoryLevel1,
                     List<String> categoryLevel2,
                     List<String> categoryLevel3,
                     List<String> categoryLevel4,
                     List<String> categoryLevel5) throws ServerException
    {
        Set<String> categoryNameSet = new LinkedHashSet<>();
        List<String> categoryNameList = new ArrayList<>();
        while (categoryId != null && categoryMap.containsKey(categoryId))
        {

            categoryIds.add(categoryId);
            CategoryMaster categoryMaster = categoryMap.get(categoryId);
            if(categoryMaster.isVisible())
            {
                String nameProp = ReadUtil.getString(campaign.getProperty(CampaignProperties.PIM_PIM_CATEGORY_NANE_PROPERTY), "name");
                String name = ReadUtil.getString(ReadUtil.getSingleValue(categoryMaster.getPropertyValue("indexname")), ReadUtil.getSingleValue(categoryMaster.getPropertyValue(nameProp)));
                if (name != null)
                {
                    categoryNameSet.add(name);
                }
                categoryNameList.add(name);
            }
            categoryId = categoryMaster.getParentCategoryId();
        }
        if (!categoryNameSet.isEmpty())
        {
            categoryNames.addAll(categoryNameSet);
            int i = categoryNameList.size();
            while(i < 5)
            {
                categoryNameList.add(0, null);
                i++;
            }
            for (String name : categoryNameList)
            {
                switch (i)
                {
                case 1:
                    categoryLevel1.add(name);
                    break;
                case 2:
                    categoryLevel2.add(name);
                    break;
                case 3:
                    categoryLevel3.add(name);
                    break;
                case 4:
                    categoryLevel4.add(name);
                    break;
                case 5:
                    categoryLevel5.add(name);
                    break;
                default:
                    break;
                }
                i--;
            }
        }
    }

    void addcatalogCategory(Campaign campaign,
                          String categoryId,
                          Map<String, CatalogCategory> catalogCategoryMap,
                          Set<String> categoryIds,
                          Set<String> categoryNames,
                          List<String> categoryLevel1,
                          List<String> categoryLevel2,
                          List<String> categoryLevel3,
                          List<String> categoryLevel4,
                          List<String> categoryLevel5) throws ServerException
    {
        Set<String> categoryNameSet = new LinkedHashSet<>();
        List<String> categoryNameList = new ArrayList<>();
        while (categoryId != null && catalogCategoryMap.containsKey(categoryId))
        {
            String catalogCategoryId = categoryId.split(SolrDocumentInterface.DELIMITER)[1];
            categoryIds.add(catalogCategoryId);
            CatalogCategory catalogCategory = catalogCategoryMap.get(categoryId);
            if(catalogCategory.isVisible())
            {
                String nameProp = ReadUtil.getString(campaign.getProperty(CampaignProperties.PIM_PIM_CATEGORY_NANE_PROPERTY), "name");
                String name = ReadUtil.getString(ReadUtil.getSingleValue(catalogCategory.getPropertyValue("indexname")), ReadUtil.getSingleValue(catalogCategory.getPropertyValue(nameProp)));
                if (name != null)
                {
                    categoryNameSet.add(name);
                }
                categoryNameList.add(name);
            }
            categoryId = null;
            if (catalogCategory.getParentCategoryId() != null)
            {
                categoryId = catalogCategory.getCatalogId() + SolrDocumentInterface.DELIMITER + catalogCategory.getParentCategoryId();
            }
        }
        if (!categoryNameSet.isEmpty())
        {
            categoryNames.addAll(categoryNameSet);
            int i = categoryNameList.size();
            while(i < 5)
            {
                categoryNameList.add(0, null);
                i++;
            }
            for (String name : categoryNameList)
            {
                switch (i)
                {
                case 1:
                    categoryLevel1.add(name);
                    break;
                case 2:
                    categoryLevel2.add(name);
                    break;
                case 3:
                    categoryLevel3.add(name);
                    break;
                case 4:
                    categoryLevel4.add(name);
                    break;
                case 5:
                    categoryLevel5.add(name);
                    break;
                default:
                    break;
                }
                i--;
            }
        }
    }

    private SearchProductIndexMessage prepareMessage(long storeId,
                                                     ProductMaster product,
                                                     SkuMaster sku,
                                                     ArrayList<String> skuids,
                                                     String catalogId,
                                                     String[] categoryids,
                                                     String[] categoryNames,
                                                     String[] categoryLevel1,
                                                     String[] categoryLevel2,
                                                     String[] categoryLevel3,
                                                     String[] categoryLevel4,
                                                     String[] categoryLevel5) throws ServerException
    {
        logger.info("prepareMessage" + product.getProductId());
        HashMap<String, Object> facets = new HashMap<>();
        HashMap<String, Object> sorts = new HashMap<>();
        HashMap<String, Object> search = new HashMap<>();

        ArrayList<String> additionalKeywords = new ArrayList<>();
        additionalKeywords.add(product.getProductId());
        additionalKeywords.add(sku.getSkuId());
        FacetSortModel facetSortModel = (FacetSortModel) CastUtil.fromJSON(ReadUtil.getString(dataMap.get("facetSortModel"), "{}"), FacetSortModel.class);
        String regPriceName = ReadUtil.getString(dataMap.get("regprice"), "price");
        String salePriceName = ReadUtil.getString(dataMap.get("saleprice"), "price1");
        if (product.getProperties() != null && facetMap != null)
        {
            for (PIMItemProperty productMasterProperties : product.getProperties())
            {
                String value = ReadUtil.getString(productMasterProperties.getValue(), null);
                if (value != null)
                {
                    FacetMaster facetMaster = facetMap.get(productMasterProperties.getFacetId());
                    if (facetMaster != null)
                    {
                        String name = ReadUtil.getString(productMasterProperties.getName(), null);
                        if (facetMaster.getProperties() != null)
                        {
                            for (PIMItemProperty fProp : facetMaster.getProperties())
                            {
                                if (fProp.getName().equals(FACETNAME) && fProp.getLocale().equals(locale))
                                {
                                    name = ReadUtil.getString(fProp.getValue(), name);
                                }
                            }
                        }
                        if (name != null && !(name.equalsIgnoreCase(salePriceName) || name.equalsIgnoreCase(regPriceName)))
                        {
                            if (facetMaster.isFilterable())
                            {
                                String facetVal = value.toLowerCase();

                                if (facetMaster.getFieldType() == FieldType.MULTI_LIST.getValue())
                                {
                                    facets.put(name.replaceAll("\\s+", "_"), StringUtil.getStrings(facetVal, ",", true));
                                }
                                else
                                {
                                    facets.put(name.replaceAll("\\s+", "_"), facetVal);
                                }
                                if (!additionalKeywords.contains(facetVal))
                                {
                                    additionalKeywords.add(facetVal);
                                }
                            }

                            if (facetMaster.isSortable())
                            {
                                String sortVal = value.toLowerCase();
                                HashSet<Object> streamSearchResponseSorts = new HashSet<>();
                                Object filterValue = null;
                                if (facetMaster.getValidationType() == DataType.FLOAT.getValue())
                                {
                                    filterValue = Double.parseDouble(sortVal);
                                }
                                else if (facetMaster.getValidationType() == DataType.INT.getValue() || facetMaster.getValidationType() == DataType.TIMESTAMP.getValue())
                                {
                                    filterValue = Long.parseLong(sortVal);
                                }
                                else
                                {
                                    filterValue = sortVal;
                                }
                                streamSearchResponseSorts.add(filterValue);
                                sorts.put(name.replaceAll("\\s+", "_"), sortVal);

                                if (!additionalKeywords.contains(sortVal))
                                {
                                    additionalKeywords.add(sortVal);
                                }
                            }
                            if (facetMaster.isSearchable())
                            {
                                if (!additionalKeywords.contains(value.toLowerCase()))
                                {
                                    additionalKeywords.add(value.toLowerCase());
                                }
                                search.put(name.replaceAll("\\s+", "_"), value.toLowerCase());
                            }
                        }
                    }
                }
            }
        }
        if (sku.getProperties() != null && facetMap != null)
        {
            for (PIMItemProperty skuMasterProperties : sku.getProperties())
            {
                String value = ReadUtil.getString(skuMasterProperties.getValue(), null);
                if (value != null)
                {
                    FacetMaster facetMaster = facetMap.get(skuMasterProperties.getFacetId());
                    if (facetMaster != null)
                    {
                        String name = ReadUtil.getString(skuMasterProperties.getName(), null);
                        if (facetMaster.getProperties() != null)
                        {
                            for (PIMItemProperty fProp : facetMaster.getProperties())
                            {
                                if (fProp.getName().equals(FACETNAME) && fProp.getLocale().equals(locale))
                                {
                                    name = ReadUtil.getString(fProp.getValue(), name);
                                    break;
                                }
                            }
                        }
                        if (name != null && !(name.equalsIgnoreCase(salePriceName) || name.equalsIgnoreCase(regPriceName)))
                        {
                            if (facetMaster.isFilterable())
                            {
                                String facetVal = value.toLowerCase();
                                if (facetMaster.getFieldType() == FieldType.MULTI_LIST.getValue())
                                {
                                    facets.put(name.replaceAll("\\s+", "_"), StringUtil.getStrings(facetVal, ",", true));
                                }
                                else
                                {
                                    facets.put(name.replaceAll("\\s+", "_"), facetVal);
                                }
                                if (!additionalKeywords.contains(facetVal))
                                {
                                    additionalKeywords.add(facetVal);
                                }
                            }

                            if (facetMaster.isSortable())
                            {
                                String sortVal = value.toLowerCase();
                                HashSet<Object> streamSearchResponseSorts = new HashSet<Object>();
                                Object filterValue = null;
                                if (facetMaster.getValidationType() == DataType.FLOAT.getValue())
                                {
                                    filterValue = Double.parseDouble(sortVal);
                                }
                                else if (facetMaster.getValidationType() == DataType.INT.getValue() || facetMaster.getValidationType() == DataType.TIMESTAMP.getValue())
                                {
                                    filterValue = Long.parseLong(sortVal);
                                }
                                else
                                {
                                    filterValue = sortVal;
                                }
                                streamSearchResponseSorts.add(filterValue);
                                sorts.put(name.replaceAll("\\s+", "_"), sortVal);

                                if (!additionalKeywords.contains(sortVal))
                                {
                                    additionalKeywords.add(sortVal);
                                }
                            }
                            if (facetMaster.isSearchable())
                            {
                                if (!additionalKeywords.contains(value.toLowerCase()))
                                {
                                    additionalKeywords.add(value.toLowerCase());
                                }
                                search.put(name.replaceAll("\\s+", "_"), value.toLowerCase());
                            }
                        }
                    }
                }
            }

        }
        HashMap<String, String> prop = new HashMap<>();
        prop.put("productid", product.getProductId());
        prop.put("skuids", skuids.toString());
        String productname = ReadUtil.getString(dataMap.get("name"), "name").toLowerCase();
        String name = ReadUtil.getString(ReadUtil.getSingleValue(sku.getPropertyValue(productname)), ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(productname)), null));

        boolean isVisible = ReadUtil.getBoolean(product.isVisible(), true);
        PIMItemProperty saleProperty = getProperty(Arrays.asList(salePriceName), sku, product, locale, facetMap, true);
        PIMItemProperty regProperty = getProperty(Arrays.asList(regPriceName), sku, product, locale, facetMap, true);
        PIMItemProperty sortProperty = getProperty(Arrays.asList(salePriceName, regPriceName), sku, product, locale, facetMap, false);
        if (sortProperty != null)
        {
            sorts.put(salePriceName.replaceAll("\\s+", "_"), sortProperty.getValue());
        }
        if (saleProperty != null || regProperty != null)
        {
            float price = 0;
            if (saleProperty != null)
            {
                price = ReadUtil.getFloat(saleProperty.getValue(), 0);
            }
            else
            {
                price = ReadUtil.getFloat(regProperty.getValue(), 0);
            }
            if (price > 0)
            {
                if (facetSortModel != null && facetSortModel.getMaxValue() != null && facetSortModel.getMinValue() != null)
                {
                    Long[] minValue = facetSortModel.getMinValue();
                    Long[] maxValue = facetSortModel.getMaxValue();
                    if (minValue.length == maxValue.length)
                    {
                        String facetVal = null;
                        for (int j = 0; j < minValue.length; j++)
                        {
                            if (maxValue[j] == -1 && minValue[j] <= price)
                            {
                                facetVal = (minValue[j] + " & Above");
                                break;
                            }
                            else if (maxValue[j] >= price && minValue[j] < price)
                            {
                                facetVal = (minValue[j] + "-" + maxValue[j]);
                                break;
                            }
                        }
                        facets.put(salePriceName.replaceAll("\\s+", "_"), StringUtil.getStrings(facetVal, ",", true));
                    }
                }

            }
        }
        
        String customFacetsForRange = ReadUtil.getString(dataMap.get("customFacetsForRange"), null);
        ArrayList<String> customFacetList = customFacetsForRange != null ? new ArrayList(Arrays.asList(customFacetsForRange.split(","))) : null;
        if(customFacetList != null)
        {
            addCustomFacets(facets, customFacetList, sku, product);
        }

        float salePrice = saleProperty != null ? ReadUtil.getFloat(saleProperty.getValue(), 0) : 0;
        float regPrice = regProperty != null ? ReadUtil.getFloat(regProperty.getValue(), 0) : 0;
        String colorname = ReadUtil.getString(dataMap.get("color"), "color").toLowerCase();
        String colors = ReadUtil.getString(ReadUtil.getSingleValue(sku.getPropertyValue(colorname)), ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(colorname)), null));

        String brandname = ReadUtil.getString(dataMap.get("brand"), "brand").toLowerCase();
        String brand = ReadUtil.getString(ReadUtil.getSingleValue(sku.getPropertyValue(brandname)), ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(brandname)), null));

        String descriptionname = ReadUtil.getString(dataMap.get("description"), "description").toLowerCase();
        String description = ReadUtil.getString(ReadUtil.getSingleValue(sku.getPropertyValue(descriptionname)), ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(descriptionname)), null));

        String image = ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(ReadUtil.getString(dataMap.get("image"), "image"))), null);
        String ratingString = ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(ReadUtil.getString(dataMap.get("rating"), "rating").toLowerCase())), null);
        float rating = ReadUtil.getFloat(ratingString, 0);

        long sequence = ReadUtil.getLong(ReadUtil.getSingleValue(product.getPropertyValue(ReadUtil.getString(dataMap.get("sequence"), "sequence").toLowerCase())), 0l);

        String varientname = ReadUtil.getString(dataMap.get("varient"), "varient").toLowerCase();
        String varient = ReadUtil.getString(ReadUtil.getSingleValue(sku.getPropertyValue(varientname)), ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(varientname)), null));

        String size1Name = ReadUtil.getString(dataMap.get("size1Name"), "size1").toLowerCase();
        String size1Value = ReadUtil.getString(ReadUtil.getSingleValue(sku.getPropertyValue(size1Name)), ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(size1Name)), null));

        String size2Name = ReadUtil.getString(dataMap.get("size2Name"), "size2").toLowerCase();
        String size2Value = ReadUtil.getString(ReadUtil.getSingleValue(sku.getPropertyValue(size2Name)), ReadUtil.getString(ReadUtil.getSingleValue(product.getPropertyValue(size2Name)), null));

        float relevancyscore = ReadUtil.getFloat(ReadUtil.getSingleValue(product.getPropertyValue(ReadUtil.getString(dataMap.get("relevancyscore"), "relevancyscore").toLowerCase())), 0.0f);

        float sortorderscore = ReadUtil.getFloat(ReadUtil.getSingleValue(product.getPropertyValue(ReadUtil.getString(dataMap.get("sortorderscore"), "sortorderscore").toLowerCase())), 0.0f);

        String[] segments = null;
        if(product.getSegments() != null && !product.getSegments().isEmpty())
        {
            segments = product.getSegments().toArray(new String[product.getSegments().size()]);
        }
        if(segments == null)
        {
            segments = new String[]{ProductCategory.SEGMENTS_ALL};
        }

        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("properties", CastUtil.toJSON(prop));
        String status = (product.getStatus() == ProductMaster.STATUS_ACTIVE) ? "true" : "false";
        SearchProductIndexMessage indexMessage = new SearchProductIndexMessage(null, // String serverName 
        null, //Campaign campaign
        searchCampaign.getId(), //long campaignId
        TenantThreadLocal.get(),
        StreamSearchService.SEARCH_DOMAIN_PRODUCT, //int searchDomainType
        "v5", //String searchDomainVersion
        sku.getSkuId(), //String skuid
        product.getProductId(), //String productid
        name, //String name
        brand, //String brand
        ReadUtil.getSingleValue(product.getPropertyValue("model")), //String model
        varient, //String variant
        colors, //String color
        new StreamV2COMSize(size1Name, size1Value, size2Name, size2Value), //StreamV2COMSize size
        new StreamV2COMPrice(regPrice, 0f, 0f, 0f, 0f, 0f, salePrice, 0f, 0f), //StreamV2COMPrice price
        locale, product.getVersions(), new String[] { "" }, //String[] division TODO
        categoryids, //String[] categoryIds
        categoryNames, //String[] categoryNames
        removeNullValues(categoryLevel1), removeNullValues(categoryLevel2), removeNullValues(categoryLevel3), removeNullValues(categoryLevel4), removeNullValues(categoryLevel5), description, //String description
        additionalKeywords.toArray(new String[additionalKeywords.size()]), //comModel.getAdditionalKeywords(), //String[] additionalkeyword TODO:
        null, //ArrayList<String> searchsuggession
        null, //ArrayList<String> uniquesuggestions
        image, //String productimage
        rating, //float rating
        System.currentTimeMillis(), //long createdTime
        System.currentTimeMillis(), //long updatedTime
        sequence, //long sequence
        status, //String available
        properties, //HashMap<String, Object> properties
        facets, //facets,
        sorts, //HashMap<String, Object> sort
        null, null, //Set<String> tags
        false, //boolean isReindex
        relevancyscore, //float relevancyscore
        sortorderscore,
        String.valueOf(storeId), // storeId
        isVisible,search,
        segments,
        catalogId);
        return indexMessage;
    }
    
    public void addCustomFacets(HashMap<String, Object> facets, ArrayList<String> propNames, SkuMaster sku, ProductMaster product) throws ServerException
    {
        if(facets != null && propNames != null)
        {
            for(String propName : propNames)
            {
                FacetSortModel facetSortModel = null;
                try
                {
                    facetSortModel = (FacetSortModel) CastUtil.fromJSON(ReadUtil.getString(dataMap.get(propName + "RangeModel"), "{}"), FacetSortModel.class);
                }
                catch(Exception e)
                {}
                PIMItemProperty property = getProperty(Arrays.asList(propName), sku, product, locale, facetMap, true);
                if(property != null)
                {
                    float value = ReadUtil.getFloat(property.getValue(), 0);
                    /*if (value > 0)
                    {*/
                    if (facetSortModel != null && facetSortModel.getMaxValue() != null && facetSortModel.getMinValue() != null)
                    {
                        Long[] minValue = facetSortModel.getMinValue();
                        Long[] maxValue = facetSortModel.getMaxValue();
                        if (minValue.length == maxValue.length)
                        {
                            String facetVal = null;
                            for (int j = 0; j < minValue.length; j++)
                            {
                                if (maxValue[j] == -1 && minValue[j] <= value)
                                {
                                    facetVal = (minValue[j] + " & Above");
                                    break;
                                }
                                else if (maxValue[j] >= value && minValue[j] < value)
                                {
                                    facetVal = (minValue[j] + "-" + maxValue[j]);
                                    break;
                                }
                            }
                            facets.put(propName.replaceAll("\\s+", "_"), StringUtil.getStrings(facetVal, ",", true));
                        }
                    }
    
                    /*}*/
                }
            }
        }
    }

    PIMItemProperty getProperty(List<String> properties,
                                SkuMaster sku,
                                ProductMaster product,
                                String locale,
                                HashMap<String, FacetMaster> facetMap,
                                boolean isFiltarable) throws ServerException
    {

        PIMItemProperty toRet = null;
        if (properties != null)
        {
            if (sku != null)
            {
                for (String propertyName : properties)
                {
                    PIMItemProperty pimItemProperty = sku.getProperty(propertyName, null);

                    FacetMaster facetMaster = pimItemProperty != null ? facetMap.get(pimItemProperty.getFacetId()) : null;
                    if (pimItemProperty != null && pimItemProperty.getValue() !=null &&facetMaster != null && (isFiltarable ? facetMaster.isFilterable() : facetMaster.isSortable()))
                    {
                        toRet = pimItemProperty;
                        break;
                    }

                    pimItemProperty = product.getProperty(propertyName, null);
                    facetMaster = pimItemProperty != null ? facetMap.get(pimItemProperty.getFacetId()) : null;
                    if (pimItemProperty != null && pimItemProperty.getValue() !=null && facetMaster != null && (isFiltarable ? facetMaster.isFilterable() : facetMaster.isSortable()))
                    {
                        toRet = pimItemProperty;
                        break;
                    }
                }
            }
        }
        return toRet;
    }

    private String[] removeNullValues(String[] categoryLevel)
    {
        List<String> categoryList = new ArrayList<>();
        if(categoryLevel != null)
        {
        for(int cIdx=0; cIdx<categoryLevel.length; cIdx++)
        {
            if(categoryLevel[cIdx] != null)
            {
                categoryList.add(categoryLevel[cIdx]);
            }
        }
        }
        return categoryList.toArray(new String[categoryList.size()]);
    }
}

class FacetSortModel implements Serializable
{
    private static final long serialVersionUID = -6180253386093921464L;

    private @Getter @Setter Long[] minValue;
    private @Getter @Setter Long[] maxValue;

    public FacetSortModel()
    {}

    public FacetSortModel(Long[] minValue, Long[] maxValue)
    {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
}
