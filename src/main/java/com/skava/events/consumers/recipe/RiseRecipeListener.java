package com.skava.events.consumers.recipe;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.skava.events.constants.ConstantValues;
import com.skava.events.exception.SkavaEventException;
import com.skava.events.http.HttpClientService;
import com.skava.events.util.EventTokenUtil;
import com.skava.events.util.XmlUtil;

//TODO Caching for Tokens
@Component
public class RiseRecipeListener implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(RiseRecipeListener.class);

    @Override
    public void onMessage(Message message) {
        String payload = new String(message.getBody());
        LOG.debug("Message received :: {}", payload);
        String recipeId, response = null, serviceToken, accessToken, welioResp, welioKey = null;
        Document document;
        JSONObject welioObj;

        try {
            document = XmlUtil.parseXml(payload);
            recipeId = XmlUtil.getNodeValue(document, "/RecipeRequest/RecipeID");
            serviceToken = EventTokenUtil.getServiceToken();
            accessToken = EventTokenUtil.getAccessToken(ConstantValues.CORPORATEADMIN_USERNAME,
                    ConstantValues.CORPORATEADMIN_PASSWORD);
            String productExists = isProductExist(recipeId, serviceToken, accessToken);
            String pdtProp = constructPropArr(document);

            if (productExists.equals("true")) {
                response = makeUpdatePdtCall(recipeId, pdtProp, serviceToken, accessToken);
            } else if (productExists.equals("false")) {
                response = makeCreatePdtCall(recipeId, pdtProp, serviceToken, accessToken);
            }

            welioResp = makeWelioPostCall(response, document);
            welioObj = new JSONObject(welioResp);

            if (welioObj.has("header")) {
                JSONObject headerObj = welioObj.getJSONObject("header");
                welioKey = headerObj.has("id") ? headerObj.getString("id") : null;

                pushWelioPayloadToRabbit(welioKey, recipeId);
            } else {
                throw new SkavaEventException("Welio Response doesnt have header field");
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException | JSONException
                | ParseException | TransformerException e) {
            LOG.error("Exception :: {}", e);
            throw new SkavaEventException(e.getMessage());
        } catch (Exception e) {
            throw new SkavaEventException(e.getMessage());
        }
    }

    private void pushWelioPayloadToRabbit(String welioKey, String recipeId)
            throws JSONException, ParseException, UnsupportedEncodingException {
        String url;
        JSONObject payloadObj = new JSONObject();
        HttpHeaders headers = new HttpHeaders();
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        String welioPushResp;

        payloadObj.put("welioKey", welioKey);
        payloadObj.put("recipeId", recipeId);
        headers.add("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((ConstantValues.AUTH_USERNAME + ":" + ConstantValues.AUTH_PWD).getBytes("utf-8")));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LOG.debug("Welio post headers :: {}", headers);
        map.add(ConstantValues.WELIO_POST_OBJ_KEY, payloadObj.toString());
        url = ConstantValues.WELIO_PUSH_URL;
        LOG.debug("Welio push payload :: {}", payloadObj);
        // url = "https://" + "localhost:8443" + "/push/welio.recipe.update";
        LOG.debug("Welio push URL :: {}", url);
        welioPushResp = HttpClientService.makeHttpPostRequest(url, "POST", map, headers, false);
        LOG.debug("Welio Push Response :: {}", welioPushResp);

    }

    // TODO Move this to util
    private String isProductExist(String recipeId, String serviceToken, String accessToken) throws Exception {
        String url, response;
        JSONObject jsonObj;

        // TODO NEED TO change it to grin with country id , languageid , industry sector

        url = "https://" + ConstantValues.DOMAIN + "/pimadmin/v7/product/load?campaignId=" + ConstantValues.CAMPAIGN_ID
                + "&status=1&productId=" + recipeId + "&locale=" + ConstantValues.LOCALE + "&requestor="
                + ConstantValues.REQUESTOR + "&serviceToken=" + serviceToken + "&accessToken=" + accessToken;
        LOG.debug("PIM Load url :: {}", url);

        response = HttpClientService.makeHttpGetRequest(url, "GET");
        LOG.debug("PIM Load response :: {}", response);
        jsonObj = new JSONObject(response);
        if (jsonObj.has("products")) {
            return "true";
        } else if (jsonObj.has("responseMessage") && jsonObj.getString("responseMessage").equals("Success")) {
            return "false";
        }
        LOG.error("Product Load call gave a different response:: {}",response);
        throw new SkavaEventException("Product Load call gave a different response");
    }

    private String makeUpdatePdtCall(String recipeId, String pdtProp, String serviceToken, String accessToken)
            throws XPathExpressionException, JSONException, ParserConfigurationException, IOException, SAXException,
            TransformerException, ParseException {
        String url, pdtPropEncode, updateResp;
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

        pdtProp = pdtProp.substring(1, pdtProp.length() - 1);
        pdtPropEncode = URLEncoder.encode(pdtProp, "UTF-8");
        map.add("properties", pdtPropEncode);
        url = "https://" + ConstantValues.DOMAIN + "/pimadmin/v7/product/update?isBundle=false&defaultParentCategoryId="
                + ConstantValues.PARENT_CAT_ID + "&productId=" + recipeId + "&campaignId=" + ConstantValues.CAMPAIGN_ID
                + "&isCollection=false&locale=en_US&accessToken=" + accessToken + "&requestor="
                + ConstantValues.REQUESTOR + "&isLocked=false&startTime=" + ConstantValues.STARTTIME + "&endTime="
                + ConstantValues.ENDTIME + "&serviceToken=" + serviceToken + "&status=1";
        LOG.debug("Update PIM url :: {}", url);
        updateResp = HttpClientService.makeHttpPostRequest(url, "POST", map, null, false);

        LOG.debug("Update Response : {}", updateResp);
        return updateResp;
    }

    private String makeCreatePdtCall(String recipeId, String pdtProp, String serviceToken, String accessToken)
            throws XPathExpressionException, JSONException, ParserConfigurationException, IOException, SAXException,
            TransformerException, ParseException {
        String url, pdtPropEncode, createResp;
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

        pdtProp = pdtProp.substring(1, pdtProp.length() - 1);
        pdtPropEncode = URLEncoder.encode(pdtProp, "UTF-8");
        map.add("properties", pdtPropEncode);
        url = "https://" + ConstantValues.DOMAIN + "/pimadmin/v7/product/create?isBundle=false&defaultParentCategoryId="
                + ConstantValues.PARENT_CAT_ID + "&productId=" + recipeId + "&campaignId=" + ConstantValues.CAMPAIGN_ID
                + "&isCollection=false&locale=en_US&accessToken=" + accessToken + "&requestor="
                + ConstantValues.REQUESTOR + "&isLocked=false&startTime=" + ConstantValues.STARTTIME + "&endTime="
                + ConstantValues.ENDTIME + "&serviceToken=" + serviceToken + "&status=1";
        LOG.debug("Create PIM url :: {}", url);
        createResp = HttpClientService.makeHttpPostRequest(url, "POST", map, null, false);

        LOG.debug("Create Response : {}", createResp);
        return createResp;
    }

    private Object constructPropObj(String name, JSONArray values) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("name", name);
        item.put("values", values);
        item.put("locale", "en_US"); // TODO Take locale from master list
        item.put("locked", false);

        return item;
    }

    private String constructPropArr(Document document) throws JSONException, XPathExpressionException,
            ParserConfigurationException, IOException, SAXException, TransformerException {
        JSONArray propObj = new JSONArray();
        propObj.put(generatePropObj("name", "/RecipeRequest/RecipeName", document));
        propObj.put(generatePropObj("description", "/RecipeRequest/RecipeDescription", document));
        propObj.put(generatePropObj("grin", "/RecipeRequest/GrinNumber", document));
        propObj.put(generatePropObj("countryid", "/RecipeRequest/CountryId", document));
        propObj.put(generatePropObj("languageid", "/RecipeRequest/LanguageId", document));
        propObj.put(generatePropObj("industrysector", "/RecipeRequest/IndustrySector", document));
        propObj.put(generatePropObj("noofingredients", "/RecipeRequest/NoOfIngredients", document));
        propObj.put(generatePropObj("ingredientsetid", "/RecipeRequest/IngredientSets/IngredientSet/IngredientSetId",
                document));
        propObj.put(generatePropObj("ingredientsetingredientid",
                "/RecipeRequest/IngredientSets/IngredientSet/IngredientSetId", "IngredientSetIngredientId", document));
        propObj.put(generatePropObj("preparationtime", "/RecipeRequest/PrepTime", document));
        propObj.put(generatePropObj("totaltime", "/RecipeRequest/ReadyInTime", document));
        propObj.put(generatePropObj("yield", "/RecipeRequest/Yield", document));
        propObj.put(generatePropObj("numberofservings", "/RecipeRequest/ServesText", document));
        propObj.put(generatePropObj("additionalpreptimetext", "/RecipeRequest/AdditionalPrepTimeText", document));
        propObj.put(generatePropObj("additionalreadytimetext", "/RecipeRequest/AdditionalReadyTimeText", document));
        propObj.put(generatePropObj("preparationdescription", "/RecipeRequest/PreparationDescription", document));
        propObj.put(generatePropObj("ingredientname",
                "/RecipeRequest/IngredientSets/IngredientSet/Ingredients/IngredientSetIngredient", "Caption",
                document));
        /*
         * propObj.put(generatePropObj("nutritionitemid",
         * "/RecipeRequest/IngredientSets/IngredientSet/Nutritions/IngredientSetNutrition",
         * "NutritionalItemId")); propObj.put(generatePropObj("quantity",
         * "/RecipeRequest/IngredientSets/IngredientSet/Nutritions/IngredientSetNutrition",
         * "NutrientQty")); propObj.put(generatePropObj("displayorder",
         * "/RecipeRequest/IngredientSets/IngredientSet/Nutritions/IngredientSetNutrition",
         * "DisplayOrder"));
         */
        /*
         * propObj.put(generatePropObj("classficationid",
         * "/RecipeRequest/NutritionalClaims/NutritionalClaim/ClassificationTypeId"));
         */
        propObj.put(constructIngredientArr("ingredients", document));
        // propObj.put(constructRecipeSpecificArr("tips"));

        return propObj.toString();
    }

    private Object generatePropObj(String name, String Xpath, Document document)
            throws XPathExpressionException, JSONException {
        JSONArray propValArr = new JSONArray();
        if (!Xpath.isEmpty())
            propValArr.put(XmlUtil.getNodeValue(document, Xpath));

        return constructPropObj(name, propValArr);
    }

    private Object generatePropObj(String name, String Xpath, String childNode, Document document)
            throws XPathExpressionException, ParserConfigurationException, IOException, SAXException,
            TransformerException, JSONException {
        JSONArray propValArr = new JSONArray();
        if (!(Xpath.isEmpty() && childNode.isEmpty()))
            propValArr = XmlUtil.getMultipleNodeValues(document, Xpath, childNode);

        return constructPropObj(name, propValArr);
    }

    private String makeWelioPostCall(String PIMResponse, Document document)
            throws JSONException, XPathExpressionException, ParserConfigurationException, IOException, SAXException,
            TransformerException, ParseException {
        JSONObject PIMRespObj = new JSONObject(PIMResponse);
        String response = null;

        if (PIMRespObj.has("responseMessage") && PIMRespObj.getString("responseMessage").equals("Success")) {
            org.json.simple.JSONObject postObj = new org.json.simple.JSONObject();
            JSONParser parser = new JSONParser();
            String url;

            postObj = (org.json.simple.JSONObject) parser.parse(createPostBody(document));

            url = "http://" + ConstantValues.WELIO_DOMAIN + "?key=" + ConstantValues.WELIO_API_KEY;

            response = HttpClientService.makeHttpPostRequest(url, "POST", postObj, null, false);

            LOG.debug("Welio POST response :: {} ", response);
        }
        return response;
    }

    private String createPostBody(Document document) throws XPathExpressionException, ParserConfigurationException,
            IOException, SAXException, TransformerException, JSONException {
        JSONObject prop = new JSONObject();
        JSONObject headerObj = new JSONObject();
        JSONObject recipeTimeObj = new JSONObject();
        JSONArray ingredientsArr = new JSONArray();
        JSONArray preparationsArr = new JSONArray();
        JSONArray ingredientsValueArr;
        int prepTime, cookTime, totalTime;

        prepTime = Integer
                .parseInt(XmlUtil.getNodeValue(document, "/RecipeRequest/PrepTime").replaceAll("\\D+", ""));
        totalTime = Integer
                .parseInt(XmlUtil.getNodeValue(document, "/RecipeRequest/ReadyInTime").replaceAll("\\D+", ""));
        cookTime = totalTime - prepTime;

        recipeTimeObj.put("prep_time", prepTime);
        recipeTimeObj.put("cook_time", cookTime);
        recipeTimeObj.put("total_time", totalTime);

        headerObj.put("description", XmlUtil.getNodeValue(document, "/RecipeRequest/PreparationDescription"));
        headerObj.put("title", XmlUtil.getNodeValue(document, "/RecipeRequest/RecipeName"));
        headerObj.put("servings", Integer
                .parseInt(XmlUtil.getNodeValue(document, "/RecipeRequest/ServesText").replaceAll("\\D+", "")));
        headerObj.put("timing", recipeTimeObj);

        ingredientsValueArr = XmlUtil.getMultipleNodeValues(document,
                "/RecipeRequest/IngredientSets/IngredientSet/Ingredients/IngredientSetIngredient", "Caption");
        for (int i = 0; i < ingredientsValueArr.length(); i++) {
            ingredientsArr.put(new JSONObject().put("ingredients", ingredientsValueArr.get(i)));
        }

        prop.put("header", headerObj);
        prop.put("preparations", preparationsArr);
        prop.put("ingredients", ingredientsArr);

        LOG.debug("Welio POST Body :: {}", prop);
        return prop.toString();
    }

    private Object constructIngredientArr(String name, Document document)
            throws JSONException, XPathExpressionException {
        JSONObject ingreObj = new JSONObject();
        JSONArray valueArr = new JSONArray();
        NodeList ingreNodes = (NodeList) XmlUtil.getNodeSet(document,
                "/RecipeRequest/IngredientSets/IngredientSet/Ingredients/IngredientSetIngredient");
        String baseXpath = "/RecipeRequest/IngredientSets/IngredientSet/Ingredients/IngredientSetIngredient";

        for (int i = 1; i <= ingreNodes.getLength(); i++) {
            JSONObject ingreValueObj = new JSONObject();
            /*
             * ingreValueObj.put("FullMeasure", ProcessXmlData.getNodeValue(document,
             * baseXpath + "[" + i + "]" + "/FullMeasureQty"));
             */
            // ingreValueObj.put("FullWeight", "");
            // ingreValueObj.put("IngredientGridHeaders", "");
            ingreValueObj.put("IngredientID",
                    XmlUtil.getNodeValue(document, baseXpath + "[" + i + "]" + "/IngredientId"));
            ingreValueObj.put("IngredientName",
                    XmlUtil.getNodeValue(document, baseXpath + "[" + i + "]" + "/Caption"));
            ingreValueObj.put("IngredientStep",
                    XmlUtil.getNodeValue(document, baseXpath + "[" + i + "]" + "/DisplayOrder"));
            ingreValueObj.put("LanguageId",
                    XmlUtil.getNodeValue(document, baseXpath + "[" + i + "]" + "/LanguageId"));
            // ingreValueObj.put("PostPreparation", "");
            // ingreValueObj.put("PrePreparation", "");
            ingreValueObj.put("QuantityNum",
                    XmlUtil.getNodeValue(document, baseXpath + "[" + i + "]" + "/Quantity"));
            ingreValueObj.put("QuantityText",
                    XmlUtil.getNodeValue(document, baseXpath + "[" + i + "]" + "/FullMeasureQty"));
            // ingreValueObj.put("QuantityUnit", "");
            /*
             * ingreValueObj.put("IngredientSetIngredientId",
             * ProcessXmlData.getNodeValue(document, baseXpath + "[" + i + "]" +
             * "/IngredientSetIngredientId"));
             */
            // ingreValueObj.put("TrialMeasure", "");
            // ingreValueObj.put("TrialWeight", "");

            valueArr.put(ingreValueObj);
        }
        ingreObj.put(name, valueArr);
        return ingreObj;
    }
}
