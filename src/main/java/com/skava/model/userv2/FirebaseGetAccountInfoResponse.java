package com.skava.model.userv2;

import java.io.Serializable;
import java.util.Arrays;

import com.skava.model.Response;

import lombok.Getter;

public class FirebaseGetAccountInfoResponse extends Response implements Serializable
{

    private static final long serialVersionUID = -3885406987156700499L;
    
    @Getter private String kind;
    @Getter private FirebaseGetAccountInfoUserResponse[] users;
    
    
    public FirebaseGetAccountInfoResponse(int responseCode, String responseMessage)
    {
        super(responseCode, responseMessage);
    }

    /**
     * Instantiates a new firebase response.
     *
     * @param responseCode the response code
     */
    public FirebaseGetAccountInfoResponse(int responseCode)
    {
        super(responseCode,"");
    }
    
    /**
     * Instantiates a new firebase response.
     */
    public FirebaseGetAccountInfoResponse()
    {
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((kind == null) ? 0 : kind.hashCode());
        result = prime * result + Arrays.hashCode(users);
        return result;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FirebaseGetAccountInfoResponse other = (FirebaseGetAccountInfoResponse) obj;
        if (kind == null)
        {
            if (other.kind != null) return false;
        }
        else if (!kind.equals(other.kind)) return false;
        if (!Arrays.equals(users, other.users)) return false;
        return true;
    }

    
    
}
