package com.example.querymicroservice;

import com.example.querymicroservice.Config.AccessTokenUser;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

@Service
public class KeycloakTokenExchangeService {
    private static final String TOKEN_EXCHANGE_URL = "http://localhost:8181/realms/Journal/protocol/openid-connect/token";
    private static final String CLIENT_ID = "read-journal";
    private static final String CLIENT_SECRET = "T8fhv3RsTFsB3IdD4ur6kCvOL3nFTZqS";
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();

    public AccessTokenUser getLimitedScopeToken(AccessTokenUser accessTokenUser, String reducedScopes) throws RestClientException {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        body.add("client_id", CLIENT_ID);
        body.add("client_secret", CLIENT_SECRET);
        body.add("subject_token", accessTokenUser.getToken());
        body.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        body.add("scope", reducedScopes);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(TOKEN_EXCHANGE_URL, HttpMethod.POST, entity, Map.class);
        accessTokenUser.setToken(response.getBody().get("access_token").toString());
        accessTokenUser.setScopes(Arrays.stream(response.getBody().get("scope").toString().split(" ")).toList());
        accessTokenUser.setTokenExpiresAt(Instant.ofEpochSecond(accessTokenUser.getTokenExpiresAt().getEpochSecond() + ((Integer)response.getBody().get("expires_in"))));
        return accessTokenUser;
    }
}

