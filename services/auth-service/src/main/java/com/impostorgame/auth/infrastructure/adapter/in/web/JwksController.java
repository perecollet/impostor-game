package com.impostorgame.auth.infrastructure.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final JwksProvider jwksProvider;

    public JwksController(JwksProvider jwksProvider) {
        this.jwksProvider = jwksProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        return jwksProvider.getJwks();
    }
}
