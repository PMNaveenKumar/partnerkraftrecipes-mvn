package com.skava.transform;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.skava.interfaces.impl.BaseContentGenerator;
import com.skava.model.com.request.SkavaCOMCardModel;
import com.skava.model.com.request.SkavaCOMSmartZoneModel;
import com.skava.model.com.request.SkavaCOMUserAuthenticationModel;
import com.skava.model.com.request.SkavaCOMUserAuthenticationRequest;
import com.skava.model.com.request.SkavaCOMUserModel;
import com.skava.model.com.request.SkavaCOMUserRequest;
import com.skava.model.transform.Type;
import com.skava.util.JSONUtils;
import com.skava.util.ServerException;

public class KraftrecipesContentGenerator extends BaseContentGenerator
{
    @Override
    public byte[] getAdzerkData(Type type,
                                HashMap<String, List<String>> inputParams,
                                SkavaCOMUserRequest request,
                                HashMap<String, List<String>> paginationParams) throws ServerException
    {
        byte[] toRet = null;
        try
        {
            JSONObject postBodyObj = new JSONObject();
            JSONArray placementsArr = new JSONArray();
            if (request.getAddressModel() != null && request.getAddressModel().length > 0)
            {
                SkavaCOMUserModel item = request.getAddressModel()[0];
                if (item.getAdsDetails().length > 0)
                {
                    for (int itr = 0; itr < item.getAdsDetails().length; itr++)
                    {
                        SkavaCOMSmartZoneModel adsDetails = item.getAdsDetails()[itr];
                        JSONObject jObj = new JSONObject();
                        if (adsDetails != null)
                        {
                            if (adsDetails.getName() != null)
                            {
                                jObj.put("divName", adsDetails.getName());
                            }
                            if (adsDetails.getNetworkId() != null)
                            {
                                jObj.put("networkId", adsDetails.getNetworkId());
                            }
                            if (adsDetails.getSiteId() != null)
                            {
                                jObj.put("siteId", adsDetails.getSiteId());
                            }

                            JSONArray adTypes = new JSONArray();
                            if (adsDetails.getTypes() != null)
                            {
                                String types = adsDetails.getTypes();
                                String[] splitTypes = types.split(",");
                                for (int i = 0; i < splitTypes.length; i++)
                                {
                                    adTypes.put(Integer.parseInt(splitTypes[i]));
                                }
                                jObj.put("adTypes", adTypes);
                            }

                            JSONArray zones = new JSONArray();
                            if (adsDetails.getZones() != null)
                            {
                                zones.put(Integer.parseInt(adsDetails.getZones()));
                                jObj.put("zoneIds", zones);
                            }
                            JSONObject properties = new JSONObject();
                            if (adsDetails.getCustomParams() != null)
                            {
                                properties = JSONUtils.getJSONObjectFromMap(adsDetails.getCustomParams(), false);
                            }
                            jObj.put("properties", properties);
                            placementsArr.put(jObj);
                        }
                        
                    }
                    postBodyObj.put("placements", placementsArr);
                }
                if (item.getCustomParams() != null)
                {
                    HashMap<String, List<String>> map = item.getCustomParams();
                    for (Map.Entry<String, List<String>> entry : map.entrySet())
                    {
                        String key = entry.getKey();
                        if (entry.getValue() != null && entry.getValue().size() > 0)
                        {
                            if (key.equals("keywords"))
                            {
                                JSONArray value = new JSONArray();
                                for (String val : entry.getValue())
                                {
                                    value.put(val);
                                }
                                postBodyObj.put(entry.getKey(), value);
                            }
                            else
                            {
                                postBodyObj.put(entry.getKey(), entry.getValue().get(0));
                            }
                        }
                    }
                }
            }
            if (paginationParams != null)
            {
                paginationParams.remove("customparams");
                paginationParams.remove("adsinfo");
            }
            toRet = postBodyObj.toString().getBytes();

        }
        catch (Throwable t)
        {
            if (t instanceof ServerException)
            {
                throw (ServerException) t;
            }
            else
            {
                throw new ServerException("Error While Processing the KraftrecipesContentGenerator.getAdzerkData()", t);
            }
        }
        return toRet;
    }
    
    @Override
    public byte[] getLoginData(Type type,
                               HashMap<String, List<String>> inputParams,
                               SkavaCOMUserAuthenticationRequest request,
                               HashMap<String, List<String>> paginationParams) throws ServerException
    {
        try
        {
            String toRet = "";
            if (request != null)
            {
                JSONObject userIdentitiesObj = new JSONObject();
                JSONArray userIdentitiesArr = new JSONArray();
                JSONObject userObj = new JSONObject();
                JSONObject userPropertiesObj = new JSONObject();
                if (request.getAuthenticationModel() != null)
                {
                    SkavaCOMUserAuthenticationModel userDetails = request.getAuthenticationModel();

                    if (userDetails != null)
                    {
                        if (userDetails.getUserName() != null)
                        {
                            userPropertiesObj.put("userEmail", userDetails.getUserName());
                        }
                        if (userDetails.getPassword() != null)
                        {
                            userPropertiesObj.put("userPassword", userDetails.getPassword());
                        }
                    }
                }
                if (request.getCustomParams() != null)
                {
                    HashMap<String, List<String>> map = request.getCustomParams();
                    for (Map.Entry<String, List<String>> entry : map.entrySet())
                    {
                        String key = entry.getKey();
                        if (entry.getKey().equals("type") || entry.getKey().equals("value"))
                        {
                            for (String val : entry.getValue())
                            {
                                userIdentitiesObj.put(key, val);
                            }
                        }
                        if (entry.getKey().equals("authToken") || entry.getKey().equals("userPhoneNumber") || entry.getKey().equals("uniqueUserName") || entry.getKey().equals("captchaText"))
                        {
                            for (String val : entry.getValue())
                            {
                                userPropertiesObj.put(key, val);
                            }
                        }
                        if (request.getCustomParams().containsKey("customProperties") && request.getCustomParams().get("customProperties").size() > 0)
                        {
                            userPropertiesObj.put("customProperties", JSONUtils.getJSONObjectFromString(URLDecoder.decode(request.getCustomParams().get("customProperties").get(0)), null));
                        }
                        if (!entry.getKey().equals("type") && !entry.getKey().equals("value") && !entry.getKey().equals("authToken") && !entry.getKey().equals("userPhoneNumber") && !request.getCustomParams().containsKey("customProperties") && !entry.getKey().equals("captchaText"))
                        {
                            for (String val : entry.getValue())
                            {
                                toRet += key + "=" + val + "&";
                            }
                        }
                    }
                }

                userObj.put("userProperties", userPropertiesObj);
                if (userIdentitiesObj != null && userIdentitiesObj.length() > 0)
                {
                    userIdentitiesArr.put(userIdentitiesObj);
                }
                userObj.put("userIdentities", userIdentitiesArr);

                if (toRet != null)
                {
                    toRet += "user=" + userObj.toString();
                }

                if (paginationParams != null)
                {
                    paginationParams.remove("userinfo");
                    paginationParams.remove("customparams");
                }
            }
            return toRet.toString().getBytes();
        }
        catch (Exception t)
        {
            if (t instanceof ServerException)
            {
                throw (ServerException) t;
            }
            else
            {
                throw new ServerException("Error While Processing the KraftrecipesContentGenerator.getLoginData()" + t);
            }
        }
    }

    @Override
    public byte[] getRegisterData(Type type,
                                  HashMap<String, List<String>> inputParams,
                                  SkavaCOMUserRequest request,
                                  HashMap<String, List<String>> paginationParams) throws ServerException
    {
        try
        {
            String toRet = null;
            if (request != null && request.getAddressModel() != null && request.getAddressModel()[0] != null)
            {
                SkavaCOMUserModel userModel = request.getAddressModel()[0];
                JSONObject toRetUserObj = new JSONObject();

                JSONObject userPropertiesObj = new JSONObject();
                JSONObject userPreferencesObj = new JSONObject();
                JSONArray shippingArray = new JSONArray();
                JSONArray userIdentitiesArray = new JSONArray();
                JSONArray billingArray = new JSONArray();
                JSONArray secqaArray = new JSONArray();
                JSONArray cardsArray = new JSONArray();

                if (userModel != null)
                {
                    if (userModel.getId() != null)
                    {
                        toRetUserObj.put("id", userModel.getId());
                    }

                    if (userModel.getEmail() != null)
                    {
                        userPropertiesObj.put("userEmail", userModel.getEmail());
                    }

                    if (userModel.getPhone() != null)
                    {
                        userPropertiesObj.put("userPhoneNumber", userModel.getPhone());
                    }

                    if (userModel.getDob() != null)
                    {
                        userPropertiesObj.put("userDOB", userModel.getDob());
                    }

                    if (userModel.getGender() != null)
                    {
                        userPropertiesObj.put("userGender", userModel.getGender());
                    }

                    if (userModel.getFirstName() != null)
                    {
                        userPropertiesObj.put("userFirstName", userModel.getFirstName());
                    }

                    if (userModel.getLastName() != null)
                    {
                        userPropertiesObj.put("userLastName", userModel.getLastName());
                    }

                    if (userModel.getCity() != null)
                    {
                        userPropertiesObj.put("userCity", userModel.getCity());
                    }

                    if (userModel.getState() != null)
                    {
                        userPropertiesObj.put("userState", userModel.getState());
                    }

                    if (userModel.getCountry() != null)
                    {
                        userPropertiesObj.put("userCountry", userModel.getCountry());
                    }

                    if (userModel.getPostalCode() != null)
                    {
                        userPropertiesObj.put("userZipCode", userModel.getPostalCode());
                    }

                    if (userModel.getVerificationDetails() != null)
                    {
                        SkavaCOMUserAuthenticationModel verificationModel = userModel.getVerificationDetails();
                        if (verificationModel.getUserName() != null)
                        {
                            userPropertiesObj.put("userName", verificationModel.getUserName());
                        }
                        if (verificationModel.getPassword() != null)
                        {
                            userPropertiesObj.put("userPassword", verificationModel.getPassword());
                        }
                    }

                    if (userModel.getCustomParams() != null)
                    {
                        if (userModel.getCustomParams().containsKey("userPhoto"))
                        {
                            userPropertiesObj.put("userPhoto", userModel.getCustomParams().get("userPhoto").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("userTimeZone"))
                        {
                            userPropertiesObj.put("userTimeZone", userModel.getCustomParams().get("userTimeZone").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("contactPreferences"))
                        {
                            userPropertiesObj.put("contactPreferences", userModel.getCustomParams().get("contactPreferences").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("additionalPreferences"))
                        {
                            userPropertiesObj.put("additionalPreferences", userModel.getCustomParams().get("additionalPreferences").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("promotions"))
                        {
                            userPropertiesObj.put("promotions", userModel.getCustomParams().get("promotions").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("preferences"))
                        {
                            userPropertiesObj.put("preferences", userModel.getCustomParams().get("preferences").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("notificationCount"))
                        {
                            userPropertiesObj.put("notificationCount", userModel.getCustomParams().get("notificationCount").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("unlockCount"))
                        {
                            userPropertiesObj.put("unlockCount", userModel.getCustomParams().get("unlockCount").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("userResetLastToken"))
                        {
                            userPropertiesObj.put("userResetLastToken", userModel.getCustomParams().get("userResetLastToken").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("userActivationLastToken"))
                        {
                            userPropertiesObj.put("userActivationLastToken", userModel.getCustomParams().get("userActivationLastToken").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("userActivationLastOtp"))
                        {
                            userPropertiesObj.put("userActivationLastOtp", userModel.getCustomParams().get("userActivationLastOtp").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("userResetLinkValid"))
                        {
                            userPropertiesObj.put("userResetLinkValid", userModel.getCustomParams().get("userResetLinkValid").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("userResetLastOtp"))
                        {
                            userPropertiesObj.put("userResetLastOtp", userModel.getCustomParams().get("userResetLastOtp").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("resetMethod"))
                        {
                            userPreferencesObj.put("resetMethod", userModel.getCustomParams().get("resetMethod").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("notificationMethod"))
                        {
                            userPreferencesObj.put("notificationMethod", userModel.getCustomParams().get("notificationMethod").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("emailNotificationMethod"))
                        {
                            userPreferencesObj.put("emailNotificationMethod", userModel.getCustomParams().get("emailNotificationMethod").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("smsNotificationMethod"))
                        {
                            userPreferencesObj.put("smsNotificationMethod", userModel.getCustomParams().get("smsNotificationMethod").get(0).toString());
                        }
                        if (userPreferencesObj != null && userPreferencesObj.length() > 0)
                        {
                            userPropertiesObj.put("userPreferences", userPreferencesObj);
                        }
                        if (userModel.getCustomParams().containsKey("status"))
                        {
                            toRetUserObj.put("status", userModel.getCustomParams().get("status").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("createdTime"))
                        {
                            toRetUserObj.put("createdTime", userModel.getCustomParams().get("createdTime").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("authToken"))
                        {
                            userPropertiesObj.put("authToken", userModel.getCustomParams().get("authToken").get(0).toString());
                        }
                        if (userModel.getCustomParams().containsKey("uniqueUserName"))
                        {
                            userPropertiesObj.put("uniqueUserName", userModel.getCustomParams().get("uniqueUserName").get(0).toString());
                        }
                    }

                    if (userModel.getCardDetails() != null && userModel.getCardDetails().length > 0)
                    {
                        SkavaCOMCardModel[] cards = userModel.getCardDetails();

                        for (int i = 0; i < cards.length; i++)
                        {
                            JSONObject cardobj = new JSONObject();
                            if (cards[i].getId() != null)
                            {
                                cardobj.put("id", cards[i].getId());
                            }
                            if (cards[i].getName() != null)
                            {
                                cardobj.put("cardHolderName", cards[i].getName());
                            }
                            if (cards[i].getType() != null)
                            {
                                cardobj.put("cardType", cards[i].getType());
                            }
                            if (cards[i].getNumber() != null)
                            {
                                cardobj.put("cardNumber", cards[i].getNumber());
                            }
                            if (cards[i].getExpirationMonth() != null)
                            {
                                cardobj.put("cardExpirationMonth", cards[i].getExpirationMonth());
                            }
                            if (cards[i].getExpirationYear() != null)
                            {
                                cardobj.put("cardExpirationYear", cards[i].getExpirationYear());
                            }
                            if (cards[i].getCvv() != null)
                            {
                                cardobj.put("cardCVVNumber", cards[i].getCvv());
                            }
                            if (cards[i].getCustomParams() != null)
                            {
                                HashMap<String, String> map = cards[i].getCustomParams();

                                for (Map.Entry<String, String> entry : map.entrySet())
                                {
                                    String key = entry.getKey();
                                    String val = entry.getValue();
                                    cardobj.put(key, val);
                                }
                            }
                            cardsArray.put(cardobj);
                        }
                        userPropertiesObj.put("paymentCards", cardsArray);
                    }

                    if (userModel.getAddresses() != null)
                    {
                        SkavaCOMUserModel[] addressesModel = userModel.getAddresses();

                        for (int i = 0; i < addressesModel.length; i++)
                        {
                            JSONObject billingobj = new JSONObject();
                            JSONObject shippingobj = new JSONObject();
                            JSONObject secqaobj = new JSONObject();
                            JSONObject useridentitiesobj = new JSONObject();
                            if (addressesModel[i].getType().equals("billingAddress"))
                            {
                                if (addressesModel[i].getId() != null)
                                {
                                    billingobj.put("id", addressesModel[i].getId());
                                }
                                if (addressesModel[i].getFirstName() != null)
                                {
                                    billingobj.put("firstName", addressesModel[i].getFirstName());
                                }
                                if (addressesModel[i].getLastName() != null)
                                {
                                    billingobj.put("lastName", addressesModel[i].getLastName());
                                }
                                if (addressesModel[i].getPhone() != null)
                                {
                                    billingobj.put("phone", addressesModel[i].getPhone());
                                }
                                if (addressesModel[i].getEmail() != null)
                                {
                                    billingobj.put("email", addressesModel[i].getEmail());
                                }
                                if (addressesModel[i].getAddressLine1() != null)
                                {
                                    billingobj.put("street1", addressesModel[i].getAddressLine1());
                                }
                                if (addressesModel[i].getAddressLine2() != null)
                                {
                                    billingobj.put("street2", addressesModel[i].getAddressLine2());
                                }
                                if (addressesModel[i].getCity() != null)
                                {
                                    billingobj.put("city", addressesModel[i].getCity());
                                }
                                if (addressesModel[i].getState() != null)
                                {
                                    billingobj.put("state", addressesModel[i].getState());
                                }
                                if (addressesModel[i].getCountry() != null)
                                {
                                    billingobj.put("country", addressesModel[i].getCountry());
                                }
                                if (addressesModel[i].getCounty() != null)
                                {
                                    billingobj.put("county", addressesModel[i].getCounty());
                                }
                                if (addressesModel[i].getPostalCode() != null)
                                {
                                    billingobj.put("zipCode", addressesModel[i].getPostalCode());
                                }
                                if (addressesModel[i].getCustomParams() != null)
                                {
                                    if (addressesModel[i].getCustomParams().containsKey("validated"))
                                    {
                                        billingobj.put("validated", addressesModel[i].getCustomParams().get("validated").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("isDefault"))
                                    {
                                        billingobj.put("isDefault", addressesModel[i].getCustomParams().get("isDefault").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("override"))
                                    {
                                        shippingobj.put("override", addressesModel[i].getCustomParams().get("override").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("default"))
                                    {
                                        shippingobj.put("default", addressesModel[i].getCustomParams().get("default").get(0).toString());
                                    }
                                }
                                if (billingobj != null && billingobj.length() > 0)
                                {
                                    billingArray.put(billingobj);
                                }
                                userPropertiesObj.put("billingAddress", billingArray);
                            }
                            if (addressesModel[i].getType().equals("shippingAddress"))
                            {
                                if (addressesModel[i].getId() != null)
                                {
                                    shippingobj.put("id", addressesModel[i].getId());
                                }
                                if (addressesModel[i].getFirstName() != null)
                                {
                                    shippingobj.put("firstName", addressesModel[i].getFirstName());
                                }
                                if (addressesModel[i].getLastName() != null)
                                {
                                    shippingobj.put("lastName", addressesModel[i].getLastName());
                                }
                                if (addressesModel[i].getPhone() != null)
                                {
                                    shippingobj.put("phone", addressesModel[i].getPhone());
                                }
                                if (addressesModel[i].getEmail() != null)
                                {
                                    shippingobj.put("email", addressesModel[i].getEmail());
                                }
                                if (addressesModel[i].getAddressLine1() != null)
                                {
                                    shippingobj.put("street1", addressesModel[i].getAddressLine1());
                                }
                                if (addressesModel[i].getAddressLine2() != null)
                                {
                                    shippingobj.put("street2", addressesModel[i].getAddressLine2());
                                }
                                if (addressesModel[i].getCity() != null)
                                {
                                    shippingobj.put("city", addressesModel[i].getCity());
                                }
                                if (addressesModel[i].getState() != null)
                                {
                                    shippingobj.put("state", addressesModel[i].getState());
                                }
                                if (addressesModel[i].getCountry() != null)
                                {
                                    shippingobj.put("country", addressesModel[i].getCountry());
                                }
                                if (addressesModel[i].getCounty() != null)
                                {
                                    shippingobj.put("county", addressesModel[i].getCounty());
                                }
                                if (addressesModel[i].getPostalCode() != null)
                                {
                                    shippingobj.put("zipCode", addressesModel[i].getPostalCode());
                                }
                                if (addressesModel[i].getCustomParams() != null)
                                {
                                    if (addressesModel[i].getCustomParams().containsKey("validated"))
                                    {
                                        shippingobj.put("validated", addressesModel[i].getCustomParams().get("validated").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("isDefault"))
                                    {
                                        shippingobj.put("isDefault", addressesModel[i].getCustomParams().get("isDefault").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("override"))
                                    {
                                        shippingobj.put("override", addressesModel[i].getCustomParams().get("override").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("default"))
                                    {
                                        shippingobj.put("default", addressesModel[i].getCustomParams().get("default").get(0).toString());
                                    }
                                }
                                if (shippingobj != null && shippingobj.length() > 0)
                                {
                                    shippingArray.put(shippingobj);
                                }
                                userPropertiesObj.put("shippingAddress", shippingArray);
                            }
                            if (addressesModel[i].getType().equals("securityQuestions"))
                            {
                                if (addressesModel[i].getVerificationDetails() != null)
                                {
                                    if (addressesModel[i].getVerificationDetails().getChallengeQuestion() != null)
                                    {
                                        secqaobj.put("question", addressesModel[i].getVerificationDetails().getChallengeQuestion());
                                    }

                                    if (addressesModel[i].getVerificationDetails().getChallengeAnswer() != null)
                                    {
                                        secqaobj.put("answer", addressesModel[i].getVerificationDetails().getChallengeAnswer());
                                    }
                                }
                                if (secqaobj != null && secqaobj.length() > 0)
                                {
                                    secqaArray.put(secqaobj);
                                }
                                userPropertiesObj.put("securityQuestions", secqaArray);
                            }
                            if (addressesModel[i].getType().equals("userIdentities"))
                            {
                                if (addressesModel[i].getValue() != null)
                                {
                                    useridentitiesobj.put("value", addressesModel[i].getValue());
                                }
                                if (addressesModel[i].getValue() != null)
                                {
                                    useridentitiesobj.put("id", addressesModel[i].getId());
                                }
                                if (addressesModel[i].getCustomParams() != null)
                                {
                                    if (addressesModel[i].getCustomParams().containsKey("type"))
                                    {
                                        useridentitiesobj.put("type", addressesModel[i].getCustomParams().get("type").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("channel"))
                                    {
                                        useridentitiesobj.put("channel", addressesModel[i].getCustomParams().get("channel").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("userId"))
                                    {
                                        useridentitiesobj.put("userId", addressesModel[i].getCustomParams().get("userId").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("skavaUserId"))
                                    {
                                        useridentitiesobj.put("skavaUserId", addressesModel[i].getCustomParams().get("skavaUserId").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("partnerId"))
                                    {
                                        useridentitiesobj.put("partnerId", addressesModel[i].getCustomParams().get("partnerId").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("lastLoggedinTime"))
                                    {
                                        useridentitiesobj.put("lastLoggedinTime", addressesModel[i].getCustomParams().get("lastLoggedinTime").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("createdTime"))
                                    {
                                        useridentitiesobj.put("createdTime", addressesModel[i].getCustomParams().get("createdTime").get(0).toString());
                                    }
                                }
                                if (useridentitiesobj != null && useridentitiesobj.length() > 0)
                                {
                                    userIdentitiesArray.put(useridentitiesobj);
                                }
                            }
                        }
                    }
                    if (userModel.getCustomDetails() != null && userModel.getCustomDetails().length == 1)
                    {
                        SkavaCOMUserModel customModel = userModel.getCustomDetails()[0];
                        JSONObject customObj = new JSONObject();
                        if (customModel.getType() != null && customModel.getType().equals("customProperties"))
                        {
                            if (customModel.getCustomParams() != null)
                            {
                                HashMap<String, List<String>> map = customModel.getCustomParams();
                                for (Map.Entry<String, List<String>> entry : map.entrySet())
                                {
                                    String key = entry.getKey();
                                    for (String val : entry.getValue())
                                    {
                                        customObj.put(key, val);
                                    }
                                }
                            }
                        }
                        if (customObj != null)
                        {
                            userPropertiesObj.put("customProperties", customObj);
                        }
                    }
                    toRetUserObj.put("userIdentities", userIdentitiesArray);
                    if (userPropertiesObj != null)
                    {
                        toRetUserObj.put("userProperties", userPropertiesObj);
                    }
                    if (toRetUserObj != null)
                    {
                        toRet = "user=" + toRetUserObj.toString();
                    }
                    if (request.getCustomParams() != null)
                    {
                        HashMap<String, List<String>> map = request.getCustomParams();
                        for (Map.Entry<String, List<String>> entry : map.entrySet())
                        {
                            String key = entry.getKey();
                            for (String val : entry.getValue())
                            {
                                toRet += "&" + key + "=" + val;
                            }
                        }
                    }
                }
            }
            if (paginationParams != null)
            {
                paginationParams.remove("userinfo");
                paginationParams.remove("customparams");
            }
            return (toRet != null ? toRet.getBytes() : null);
        }
        catch (Exception t)
        {
            if (t instanceof ServerException)
            {
                throw (ServerException) t;
            }
            else
            {
                throw new ServerException("Error While Processing the KraftrecipesContentGenerator.getRegisterData()" + t);
            }
        }
    }

    @Override
    public byte[] getProfileData(Type type,
                                 HashMap<String, List<String>> inputParams,
                                 SkavaCOMUserRequest request,
                                 HashMap<String, List<String>> paginationParams) throws ServerException
    {
        String toRet = null;
        try
        {
            JSONArray userArr = new JSONArray();
            if (type.getIdPattern() != null && (type.getIdPattern().pattern().equalsIgnoreCase("getusersbyname")))
            {
                if (request != null && request.getAddressModel() != null && request.getAddressModel().length > 0)
                {
                    SkavaCOMUserModel userModel = request.getAddressModel()[0];
                    if ((userModel.getCustomDetails() != null) && userModel.getCustomDetails().length > 0)
                    {
                        for (int itr = 0; itr < userModel.getCustomDetails().length; itr++)
                        {
                            JSONObject jObj = new JSONObject();
                            SkavaCOMUserModel customDetails = userModel.getCustomDetails()[itr];
                            if ((customDetails != null) && (customDetails.getId() != null))
                            {
                                String id = customDetails.getId();
                                jObj.put("id", id);
                                userArr.put(jObj);
                            }
                        }
                    }
                    toRet = "user=" + userArr.toString();
                }
            }
            if (type.getIdPattern() != null && (type.getIdPattern().pattern().equalsIgnoreCase("forgetusername")))
            {
                if (request != null && request.getAddressModel() != null && request.getAddressModel()[0] != null)
                {
                    SkavaCOMUserModel userModel = request.getAddressModel()[0];
                    JSONObject toRetUserObj = new JSONObject();

                    JSONObject userPropertiesObj = new JSONObject();
                    JSONArray userIdentitiesArray = new JSONArray();

                    if (userModel != null)
                    {
                        if (userModel.getAddresses() != null)
                        {
                            if (userModel.getEmail() != null)
                            {
                                userPropertiesObj.put("userEmail", userModel.getEmail());
                            }
                            
                            SkavaCOMUserModel[] addressesModel = userModel.getAddresses();
                            for (int i = 0; i < addressesModel.length; i++)
                            {
                                JSONObject useridentitiesobj = new JSONObject();
                                if ((addressesModel[i].getType() != null) && addressesModel[i].getType().equals("userIdentities"))
                                {
                                    if (addressesModel[i].getValue() != null)
                                    {
                                        useridentitiesobj.put("value", addressesModel[i].getValue());
                                    }
                                    if (addressesModel[i].getCustomParams() != null)
                                    {
                                        if (addressesModel[i].getCustomParams().containsKey("type") && (addressesModel[i].getCustomParams().get("type").size() > 0))
                                        {
                                            useridentitiesobj.put("type", addressesModel[i].getCustomParams().get("type").get(0).toString());
                                        }
                                    }
                                    if (useridentitiesobj != null && useridentitiesobj.length() > 0)
                                    {
                                        userIdentitiesArray.put(useridentitiesobj);
                                    }
                                }
                            }
                        }
                        toRetUserObj.put("userIdentities", userIdentitiesArray);
                        if (userPropertiesObj != null)
                        {
                            toRetUserObj.put("userProperties", userPropertiesObj);
                        }
                        if (toRetUserObj != null)
                        {
                            toRet = "user=" + toRetUserObj.toString();
                        }
                    }
                }
            }
            if (type.getIdPattern() != null && type.getIdPattern().pattern().equals("checkuserexists"))
            {
                toRet = getCheckUserExists(request);
            }
            if (type.getIdPattern() != null && (type.getIdPattern().pattern().equalsIgnoreCase("captcha")))
            {
                if (request.getAddressModel() != null && request.getAddressModel().length > 0)
                {
                    SkavaCOMUserModel captchaUser = request.getAddressModel()[0];
                    if (captchaUser.getType() != null)
                    {
                        toRet = "type=" + captchaUser.getType();
                    }
                }
            }
            if (type.getIdPattern() != null && (type.getIdPattern().pattern().equalsIgnoreCase("updatemail")))
            {
                if (request != null && request.getAddressModel() != null && request.getAddressModel()[0] != null)
                {
                    SkavaCOMUserModel userModel = request.getAddressModel()[0];
                    if (userModel != null)
                    {
                        if (userModel.getEmail() != null)
                        {
                            List<String> emailList = new ArrayList<String>();
                            emailList.add(userModel.getEmail());
                            inputParams.put("userEmail", emailList);
                        }
                    }
                }
            }
            if (paginationParams != null)
            {
                paginationParams.remove("userinfo");
                paginationParams.remove("customparams");
                paginationParams.remove("appid");
            }
        }
        catch (Exception t)
        {
            if (t instanceof ServerException)
            {
                throw (ServerException) t;
            }
            else
            {
                throw new ServerException("Error While Processing the KraftrecipesContentGenerator.getProfileData()" + t);
            }
        }
        return (toRet != null) ? toRet.getBytes() : null;

    }

    private String getCheckUserExists(SkavaCOMUserRequest request) throws ServerException
    {
        try
        {
            String toRet = "";
            if (request != null && request.getAddressModel() != null && request.getAddressModel()[0] != null)
            {
                SkavaCOMUserModel userModel = request.getAddressModel()[0];
                JSONObject toRetUserObj = new JSONObject();
                JSONObject userPropertiesObj = new JSONObject();
                JSONArray userIdentitiesArray = new JSONArray();

                if (userModel != null)
                {
                    if (userModel.getPhone() != null)
                    {
                        userPropertiesObj.put("userPhoneNumber", userModel.getPhone());
                        toRetUserObj.put("userProperties", userPropertiesObj);
                    }

                    if (userModel.getEmail() != null)
                    {
                        userPropertiesObj.put("userEmail", userModel.getEmail());
                        toRetUserObj.put("userProperties", userPropertiesObj);
                    }

                    if (userModel.getVerificationDetails() != null)
                    {
                        if (userModel.getVerificationDetails().getPassword() != null)
                        {
                            userPropertiesObj.put("newPassword", userModel.getVerificationDetails().getPassword());
                        }
                        if (userModel.getVerificationDetails().getOldPassword() != null)
                        {
                            toRet = "password=" + userModel.getVerificationDetails().getOldPassword() + "&";
                        }
                    }

                    if (userModel.getAddresses() != null)
                    {
                        SkavaCOMUserModel[] addressesModel = userModel.getAddresses();

                        for (int i = 0; i < addressesModel.length; i++)
                        {
                            JSONObject useridentitiesobj = new JSONObject();
                            JSONObject secqaobj = new JSONObject();
                            if (addressesModel[i].getType().equals("userIdentities"))
                            {
                                if (addressesModel[i].getValue() != null)
                                {
                                    useridentitiesobj.put("value", addressesModel[i].getValue());
                                }
                                if (addressesModel[i].getId() != null)
                                {
                                    useridentitiesobj.put("userId", addressesModel[i].getId());
                                }
                                if (addressesModel[i].getCustomParams() != null)
                                {
                                    if (addressesModel[i].getCustomParams().containsKey("type"))
                                    {
                                        useridentitiesobj.put("type", addressesModel[i].getCustomParams().get("type").get(0).toString());
                                    }
                                    if (addressesModel[i].getCustomParams().containsKey("channel"))
                                    {
                                        useridentitiesobj.put("channel", addressesModel[i].getCustomParams().get("channel").get(0).toString());
                                    }
                                    if (userModel.getCustomParams().containsKey("uniqueUserName"))
                                    {
                                        userPropertiesObj.put("uniqueUserName", userModel.getCustomParams().get("uniqueUserName").get(0).toString());
                                    }
                                }
                                if (useridentitiesobj != null && useridentitiesobj.length() > 0)
                                {
                                    userIdentitiesArray.put(useridentitiesobj);
                                }
                            }
                        }
                    }

                    toRetUserObj.put("userProperties", userPropertiesObj);

                    toRetUserObj.put("userIdentities", userIdentitiesArray);

                    if (toRetUserObj != null)
                    {
                        toRet += "user=" + toRetUserObj.toString();
                    }
                    if (request.getCustomParams() != null)
                    {
                        HashMap<String, List<String>> map = request.getCustomParams();
                        for (Map.Entry<String, List<String>> entry : map.entrySet())
                        {
                            String key = entry.getKey();
                            for (String val : entry.getValue())
                            {
                                toRet += "&" + key + "=" + val;
                            }
                        }
                    }
                }
            }
            return toRet;
        }
        catch (Exception t)
        {
            if (t instanceof ServerException)
            {
                throw (ServerException) t;
            }
            else
            {
                throw new ServerException("Error While Processing the KraftrecipesContentGenerator.getCheckUserExists()" + t);
            }
        }
    }

}
