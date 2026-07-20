package com.sil.asset_tagging_system.mapper;

import com.sil.asset_tagging_system.dto.AuthResponseDTO;
import com.sil.asset_tagging_system.security.CustomUserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseMapper {
    public AuthResponseDTO toAuthResponseDTO(CustomUserDetails customUserDetails)
    {
        AuthResponseDTO authResponseDTO = new AuthResponseDTO(customUserDetails.getUserId()
                , customUserDetails.getFirstName()
                , customUserDetails.getLastName()
                , customUserDetails.getFirstName() +" "+customUserDetails.getLastName()
                , customUserDetails.getUsername()
                , customUserDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
                );
        return authResponseDTO;
    }
}
