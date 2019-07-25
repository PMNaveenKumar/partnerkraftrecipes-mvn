package com.skava.events.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.skava.events.exception.SkavaEventException;

public class HttpClientService {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientService.class);
    private static RestTemplate restTemplate;
    private static HttpEntity<?> requestEntity;
    private static ResponseEntity<String> respEntity;

    public static String makeHttpPostRequest(String url, String method, Object postObj, HttpHeaders headers,
            Boolean postToString) {

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            restTemplate = new RestTemplate();
            if (postToString)
                requestEntity = new HttpEntity<Object>(postObj.toString(), headers);
            else
                requestEntity = new HttpEntity<Object>(postObj, headers);
            respEntity = restTemplate.exchange(builder.build(true).toUri(), HttpMethod.resolve(method), requestEntity,
                    String.class);
        } catch (RestClientException e) {
            LOG.error("RestClientException  :: {}", e);
            throw e;
        }
        return respEntity.getBody().toString();
    }

    public static String makeHttpGetRequest(String url, String method) {

        if (method.equals("GET") || method.equals("")) {
            return makeHttpPostRequest(url, "GET", null, null, false);
        } else {
            throw new SkavaEventException("Enter valid params");
        }

    }
}
