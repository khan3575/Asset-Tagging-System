package com.sil.asset_tagging_system.dto;

import java.util.List;

public record AuthResponseDTO (Long userID, String firstName
        , String lastName,String fullName, String email, List<String> roles){}
