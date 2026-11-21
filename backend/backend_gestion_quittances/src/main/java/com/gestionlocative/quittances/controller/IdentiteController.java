package com.gestionlocative.quittances.controller;

import com.gestionlocative.quittances.dto.IdentiteResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentiteController {

    @GetMapping("/api/v1/identite")
    public IdentiteResponse consulter(Authentication authentication) {
        return new IdentiteResponse(
                authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(autorite -> autorite.getAuthority())
                        .sorted()
                        .toList()
        );
    }
}