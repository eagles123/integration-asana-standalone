package com.baker.integration.asana.controller;

import com.baker.integration.asana.model.auth.DamTokenInfo;
import com.baker.integration.asana.model.auth.LoginState;
import com.baker.integration.asana.service.DamAuthService;
import com.baker.integration.asana.service.DamTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth/lytho")
public class LythoAuthController {

    private static final Logger log = LoggerFactory.getLogger(LythoAuthController.class);

    private final DamAuthService damAuthService;
    private final DamTokenStore tokenStore;

    public LythoAuthController(DamAuthService damAuthService, DamTokenStore tokenStore) {
        this.damAuthService = damAuthService;
        this.tokenStore = tokenStore;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam String state) {
        log.info("Login initiated with state: {}", state);

        LoginState loginState = tokenStore.peekLoginState(state)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired login state"));

        String authUrl = damAuthService.buildAuthorizationUrl(state, loginState.getRealm());
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code, @RequestParam String state) {
        log.info("OAuth callback received with state: {}", state);

        LoginState loginState = tokenStore.consumeLoginState(state)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired login state"));

        DamTokenInfo tokenInfo = damAuthService.exchangeCodeForTokens(code, loginState.getRealm());
        tokenStore.storeToken(loginState.getAsanaUserGid(), tokenInfo);

        log.info("Successfully stored DAM token for Asana user: {}", loginState.getAsanaUserGid());

        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>Lytho Login</title></head>
                <body style="font-family: sans-serif; text-align: center; padding: 60px;">
                <h2>Login Successful</h2>
                <p>You are now connected to Lytho DAM.</p>
                <p>You can close this tab and return to Asana.</p>
                </body>
                </html>
                """;

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
