package com.sil.asset_tagging_system.controller;

import com.sil.asset_tagging_system.dto.AuthResponseDTO;
import com.sil.asset_tagging_system.mapper.AuthResponseMapper;
import com.sil.asset_tagging_system.dto.response.ApiResponse;
import com.sil.asset_tagging_system.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthResponseMapper authResponseMapper;

    @Autowired
    AuthController(AuthResponseMapper authResponseMapper) {
        this.authResponseMapper = authResponseMapper;
    }


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> getMe(@AuthenticationPrincipal CustomUserDetails userDetails)
    {

        if (userDetails == null) {
            /*
                ApiResponse<AuthResponseDTO> api = ApiResponse.failure("Failed Authentication");
                return api.toResponseEntity(HttpStatus.UNAUTHORIZED);
            */
            return ApiResponse.<AuthResponseDTO>failure("Failed Authentication")
                    .toResponseEntity(HttpStatus.UNAUTHORIZED);
        }
        /*
            ApiResponse<AuthResponseDTO> api = ApiResponse.success("Successful Authentication", authResponseMapper.toAuthResponseDTO(userDetails));
                return api.toResponseEntity(HttpStatus.OK);
         */
        return ApiResponse.success("Successful Authentication", authResponseMapper.toAuthResponseDTO(userDetails))
                .toResponseEntity(HttpStatus.OK);
    }
}
