package com.skava.model.userv2;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.skava.model.Response;

@JsonInclude(Include.NON_NULL)
public class KraftUserSearchResponse extends Response implements Serializable 
{

    private static final long serialVersionUID = 7445091952677328864L;

    @Getter @Setter public String userFirstName;
    @Getter @Setter public String userLastName;
    @Getter @Setter public String userEmail;
    @Getter @Setter public String uniqueUserName;
    @Getter @Setter public long skavaUserId;
   
    public KraftUserSearchResponse(int responseCode, String responseMessage,
                            String userStatus)
    {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public KraftUserSearchResponse(int responseCode, String responseMessage)
    {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public KraftUserSearchResponse()
    {
    	
    }
}
