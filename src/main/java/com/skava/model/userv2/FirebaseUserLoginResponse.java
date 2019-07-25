package com.skava.model.userv2;

import java.io.Serializable;
import com.skava.model.Response;
import lombok.Getter;

public class FirebaseUserLoginResponse extends Response implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public FirebaseUserLoginResponse(int responseCode, String responseMessage)
    {
        super(responseCode, responseMessage);
    }

    /**
     * Instantiates a new firebase response.
     *
     * @param responseCode the response code
     */
    public FirebaseUserLoginResponse(int responseCode)
    {
        super(responseCode,"");
    }
    
    /**
     * Instantiates a new firebase response.
     */
    public FirebaseUserLoginResponse()
    {
    }
	
	@Getter private String kind;
	@Getter private String localId;
    @Getter private String email;
    @Getter private String displayName;
    @Getter private String idToken;
    @Getter private boolean registered;
    @Getter private String refreshToken;
    @Getter private String expiresIn;

}