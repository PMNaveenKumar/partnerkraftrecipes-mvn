package com.skava.model.userv2;

import java.io.Serializable;

import lombok.Getter;

public class FirebaseUserProviderInfo implements Serializable
{

    private static final long serialVersionUID = 1978516644156183910L;
    
    @Getter private String providerId;
    @Getter private String displayName;
    @Getter private String photoUrl;
    @Getter private String federatedId;
    @Getter private String rawId;
    @Getter private String screenName;
    
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((federatedId == null) ? 0 : federatedId.hashCode());
        result = prime * result + ((photoUrl == null) ? 0 : photoUrl.hashCode());
        result = prime * result + ((providerId == null) ? 0 : providerId.hashCode());
        result = prime * result + ((rawId == null) ? 0 : rawId.hashCode());
        result = prime * result + ((screenName == null) ? 0 : screenName.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FirebaseUserProviderInfo other = (FirebaseUserProviderInfo) obj;
        if (displayName == null)
        {
            if (other.displayName != null) return false;
        }
        else if (!displayName.equals(other.displayName)) return false;
        if (federatedId == null)
        {
            if (other.federatedId != null) return false;
        }
        else if (!federatedId.equals(other.federatedId)) return false;
        if (photoUrl == null)
        {
            if (other.photoUrl != null) return false;
        }
        else if (!photoUrl.equals(other.photoUrl)) return false;
        if (providerId == null)
        {
            if (other.providerId != null) return false;
        }
        else if (!providerId.equals(other.providerId)) return false;
        if (rawId == null)
        {
            if (other.rawId != null) return false;
        }
        else if (!rawId.equals(other.rawId)) return false;
        if (screenName == null)
        {
            if (other.screenName != null) return false;
        }
        else if (!screenName.equals(other.screenName)) return false;
        return true;
    }

}
