package com.skava.model.search;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.skava.model.searchv2.solr.SolrFacetCountModel;
import com.skava.model.searchv2.solr.SolrResponseError;
import com.skava.model.searchv2.solr.SolrResponseFieldModel;
import com.skava.model.searchv2.solr.SolrResponseHeaderModel;
import com.skava.model.searchv2.solr.SolrSearchResponseGroupModel;
import com.skava.model.searchv2.solr.SolrSearchResponseModel;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SolrSearchKraftResponse
{
    @Getter @Setter private SolrSearchResponseModel response;
    @Getter @Setter private SolrResponseHeaderModel responseHeader;
    @Getter @Setter private SolrFacetCountModel facet_counts;
    @Getter @Setter private HashMap<String, SolrSearchResponseGroupModel> grouped;
    @Getter @Setter private SolrResponseError error;
    @Getter @Setter private HashMap<String, Object> spellcheck;
    @Getter @Setter private HashMap<String, SolrSearchResponseModel> expanded;

    @Getter @Setter private HashMap<String, SolrSearchResponseModel> moreLikeThis;

    @Getter @Setter private ArrayList<String> collections;
    @Getter @Setter private HashMap<String, SolrResponseFieldModel> fields;
}
