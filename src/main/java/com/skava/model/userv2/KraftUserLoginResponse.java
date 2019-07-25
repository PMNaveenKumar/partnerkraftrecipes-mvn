package com.skava.model.userv2;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.skava.model.Response;

@JsonInclude(Include.NON_NULL)
public class KraftUserLoginResponse extends Response implements Serializable
{
    private static final long serialVersionUID = 7445091952677328864L;

    @Getter @Setter public String userStatus;
    @Getter @Setter public boolean loginmaxcountreached;
    @Getter @Setter public ComUserResponse user;

    public KraftUserLoginResponse(int responseCode, String responseMessage, String userStatus)
    {
        this.responseCode = responseCode;
        this.userStatus = userStatus;
        this.responseMessage = responseMessage;
    }

    public KraftUserLoginResponse(int responseCode, String responseMessage)
    {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public KraftUserLoginResponse()
    {
        this.userStatus = null;
    }
}