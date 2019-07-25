package com.skava.model.userv2;

import java.io.Serializable;
import java.util.Arrays;

import lombok.Getter;

public class FirebaseGetAccountInfoUserResponse implements Serializable
{

    private static final long serialVersionUID = 8705091133502127785L;
    
    @Getter private String localId;
    @Getter private String email;
    @Getter private String displayName;
    @Getter private String language;
    @Getter private String photoUrl;
    @Getter private FirebaseUserProviderInfo[] providerUserInfo;
    @Getter private String validSince;
    @Getter private String createdAt;
    @Getter private String screenName;
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((language == null) ? 0 : language.hashCode());
        result = prime * result + ((localId == null) ? 0 : localId.hashCode());
        result = prime * result + ((photoUrl == null) ? 0 : photoUrl.hashCode());
        result = prime * result + Arrays.hashCode(providerUserInfo);
        result = prime * result + ((screenName == null) ? 0 : screenName.hashCode());
        result = prime * result + ((validSince == null) ? 0 : validSince.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FirebaseGetAccountInfoUserResponse other = (FirebaseGetAccountInfoUserResponse) obj;
        if (createdAt == null)
        {
            if (other.createdAt != null) return false;
        }
        else if (!createdAt.equals(other.createdAt)) return false;
        if (displayName == null)
        {
            if (other.displayName != null) return false;
        }
        else if (!displayName.equals(other.displayName)) return false;
        if (language == null)
        {
            if (other.language != null) return false;
        }
        else if (!language.equals(other.language)) return false;
        if (localId == null)
        {
            if (other.localId != null) return false;
        }
        else if (!localId.equals(other.localId)) return false;
        if (photoUrl == null)
        {
            if (other.photoUrl != null) return false;
        }
        else if (!photoUrl.equals(other.photoUrl)) return false;
        if (!Arrays.equals(providerUserInfo, other.providerUserInfo)) return false;
        if (screenName == null)
        {
            if (other.screenName != null) return false;
        }
        else if (!screenName.equals(other.screenName)) return false;
        if (validSince == null)
        {
            if (other.validSince != null) return false;
        }
        else if (!validSince.equals(other.validSince)) return false;
        return true;
    }
    
    
    
}
