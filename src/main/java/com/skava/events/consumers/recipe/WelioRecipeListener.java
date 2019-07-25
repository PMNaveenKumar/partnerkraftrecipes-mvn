package com.skava.events.consumers.recipe;

import java.io.IOException;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.skava.events.constants.ConstantValues;
import com.skava.events.exception.SkavaEventException;
import com.skava.events.http.HttpClientService;
import com.skava.events.util.EventTokenUtil;
import com.skava.events.util.XmlUtil;

@Component
public class WelioRecipeListener implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(WelioRecipeListener.class);

    @Override
    public void onMessage(Message message) {
        String payload = new String(message.getBody());
        LOG.debug("Message received :: {}", payload);
        String welioKey = null, getRecipes = null, recipeId, pimResponse = null;

        try {
            welioKey = new JSONObject(payload).getString("welioKey");
            recipeId = new JSONObject(payload).getString("recipeId");
            LOG.debug("welio Key :: {} recipeId :: {}", welioKey, recipeId);

            getRecipes = getUpdatedRecipeInfo(welioKey);
            if (getRecipes != null) {
                LOG.debug("Updated Recipe Response :: {}", getRecipes);
                pimResponse = makeUpdateProductCall(getRecipes, recipeId);
                LOG.debug("PIM Update Response : {}", pimResponse);
            }
        } catch (XPathExpressionException | JSONException | ParseException | ParserConfigurationException | SAXException
                | IOException | TransformerException e) {
            LOG.error("Exception :: {}", e);
            throw new SkavaEventException(e.getMessage());
        } catch (Exception e) {
            LOG.error("Exception :: {}", e);
            throw new SkavaEventException(e.getMessage());
        }
    }

    private void getAnnotationStatus(String welioKey) throws JSONException {
        String statusResponse;
        JSONArray arr;
        String url = "http://" + ConstantValues.WELIO_DOMAIN + "annotations/" + welioKey + "?key="
                + ConstantValues.WELIO_API_KEY;

        statusResponse = HttpClientService.makeHttpGetRequest(url, "GET");
        LOG.debug("Welio Recipe Status :: {}", statusResponse);

        arr = new JSONArray(statusResponse);
        for (int i = 0; i < arr.length(); i++) {
            String key = (String) arr.getJSONObject(i).names().get(0);
            JSONObject obj = (JSONObject) arr.getJSONObject(i).get(key);
            if (!((boolean) obj.get("needs_annotation")) == false) {
                throw new AmqpRejectAndDontRequeueException("need annotation flag failed");
            }
        }
    }

    private String getUpdatedRecipeInfo(String welioKey) throws JSONException {
        String resipeRes = null;
        getAnnotationStatus(welioKey);
        String url = "http://" + ConstantValues.WELIO_DOMAIN + "/" + welioKey + "?key=" + ConstantValues.WELIO_API_KEY;
        LOG.debug("getUpdated call URL :: {}", resipeRes);
        resipeRes = HttpClientService.makeHttpGetRequest(url, "GET");
        LOG.debug("UpDated Recipe Responce :: {}", resipeRes);
        return resipeRes;
    }

    private String makeUpdateProductCall(String updateRes, String recipeId) throws JSONException, ParseException,
            ParserConfigurationException, SAXException, IOException, XPathExpressionException, TransformerException {
        String pdtProp, pimUpdateUrl, pdtPropEncode, response;
        Document document;
        String serviceToken = EventTokenUtil.getServiceToken();
        String accessToken = EventTokenUtil.getAccessToken(ConstantValues.CORPORATEADMIN_USERNAME,
                ConstantValues.CORPORATEADMIN_PASSWORD);

        document = XmlUtil.jsonToXml(updateRes);
        pdtProp = constructUpdateProperty(document);

        LOG.debug("WelioToPIM Mapping :: {}", pdtProp);

        pdtProp = pdtProp.substring(1, pdtProp.length() - 1);
        pdtPropEncode = URLEncoder.encode(pdtProp, "UTF-8");

        pimUpdateUrl = "https://" + ConstantValues.DOMAIN
                + "/pimadmin/v7/product/update?isBundle=false&defaultParentCategoryId=" + ConstantValues.PARENT_CAT_ID
                + "&productId=" + recipeId + "&campaignId=" + ConstantValues.CAMPAIGN_ID
                + "&isCollection=false&locale=en_US&accessToken=" + accessToken + "&requestor="
                + ConstantValues.REQUESTOR + "&isLocked=false&startTime=" + ConstantValues.STARTTIME + "&endTime="
                + ConstantValues.ENDTIME + "&serviceToken=" + serviceToken + "&properties=" + pdtPropEncode
                + "&status=1";

        LOG.debug("Update PIM  URL :: {}", pimUpdateUrl);

        response = HttpClientService.makeHttpPostRequest(pimUpdateUrl, "POST", null, null, false);

        return response;
    }

    public String constructUpdateProperty(Document document) throws JSONException, XPathExpressionException,
            ParserConfigurationException, IOException, SAXException, TransformerException {
        JSONArray propObj = new JSONArray();
        propObj.put(generateProp(document, "dishdetails_text", "/root/header/derived_labels", "."));
        propObj.put(generateProp(document, "Diet", "/root/header/diet_tags", "."));
        propObj.put(
                generateProp(document, "nutritioncalsperserving", "/root/header/nutrition_summary/cals_per_serving"));
        propObj.put(
                generateProp(document, "nutritioncarbsperserving", "/root/header/nutrition_summary/carbs_per_serving"));
        propObj.put(generateProp(document, "nutritionconfidence", "/root/header/nutrition_summary/confidence"));
        propObj.put(generateProp(document, "nutritionfatperserving", "/root/header/nutrition_summary/fat_per_serving"));
        propObj.put(generateProp(document, "nutritionnrfindex", "/root/header/nutrition_summary/nrf_index"));
        propObj.put(generateProp(document, "nutritionproteinperserving",
                "/root/header/nutrition_summary/protein_per_serving"));
        propObj.put(generateProp(document, "priceperserving", "/root/header/price_per_serving"));
        propObj.put(generateProp(document, "tags", "/root/ingredients/alternatives", "entity"));
        propObj.put(
                generateProp(document, "nutritionaggvalue", "/root/nutrition/derived_nutrition", "details/agg_value"));
        propObj.put(generateProp(document, "nutritionitemname", "/root/nutrition/derived_nutrition", "details/name"));
        propObj.put(generateProp(document, "nutritionNutritionid", "/root/nutrition/derived_nutrition",
                "details/nutrient_id"));
        propObj.put(generateProp(document, "nutritionunit", "/root/nutrition/derived_nutrition", "details/unit"));
        propObj.put(generateProp(document, "nutritiondisplay", "/root/nutrition/derived_nutrition", "display"));
        return propObj.toString();
    }

    private Object generateProp(Document document, String name, String xmlPath) throws JSONException,
            XPathExpressionException, ParserConfigurationException, IOException, SAXException, TransformerException {
        JSONArray propArr = new JSONArray();
        if (!xmlPath.isEmpty()) {
            propArr.put(XmlUtil.getNodeValue(document, xmlPath));
        }
        return constructPropObj(name, propArr);
    }

    private Object generateProp(Document document, String name, String xmlPath, String childId) throws JSONException,
            XPathExpressionException, ParserConfigurationException, IOException, SAXException, TransformerException {
        JSONArray propArr = new JSONArray();
        if (!xmlPath.isEmpty()) {
            propArr = XmlUtil.getMultipleNodeValues(document, xmlPath, childId);
        }
        return constructPropObj(name, propArr);
    }

    private Object constructPropObj(String name, JSONArray propArr) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("name", name);
        item.put("values", propArr);
        return item;
    }

}
