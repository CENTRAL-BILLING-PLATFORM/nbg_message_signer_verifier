package org.ips.xml.signer.xmlsigner.service.apiClient;

import org.ips.xml.signer.xmlsigner.models.Participant;
import org.ips.xml.signer.xmlsigner.models.TokenInfo;
import org.ips.xml.signer.xmlsigner.service.RetryExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TokenGenerationClientService {

    private static final Logger logger = LoggerFactory.getLogger(TokenGenerationClientService.class);

    private final RestTemplate restTemplate;

    private RetryExecutorService retryExecutorService;

    private HttpHeaders headers;

    private URI AUTH_SERVER_TOKEN_URI;

    @Autowired
    public TokenGenerationClientService(RestTemplate restTemplate, RetryExecutorService retryExecutorService) {
        this.restTemplate = restTemplate;
        this.retryExecutorService = retryExecutorService;
    }

    public void create(String tokenUrl) {
        AUTH_SERVER_TOKEN_URI = URI.create(tokenUrl);
        headers = new HttpHeaders();
    }


    public TokenInfo generateToken(Participant credentialInfo) throws Exception {
        logger.info("Attempting to generate token for participant: {}", credentialInfo.getBic());

        // Ensure base URI or connection setup
        create(credentialInfo.getTokenGenerationPath());

        // Build request headers
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        requestHeaders.add("jwt-assertion", credentialInfo.getJwt());

        // Build request body
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("username", credentialInfo.getUserName());
        requestBody.add("password", credentialInfo.getPassword());
        requestBody.add("grant_type", credentialInfo.getGrantType());

        logger.info(" trying to generate token with jwt-assertion of {}", credentialInfo.getJwt());

        logger.info(" trying to generate token with inof {}, {}, {}", credentialInfo.getUserName(),

                credentialInfo.getPassword(), credentialInfo.getGrantType());
        logger.info(" trying to generate token with url for token {}", credentialInfo.getTokenGenerationPath());
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(requestBody, requestHeaders);

        // Retryable block for token generation
        return retryExecutorService.executeWithRetry(context -> {
            ResponseEntity<TokenInfo> response = restTemplate.postForEntity(AUTH_SERVER_TOKEN_URI, httpEntity, TokenInfo.class);
            logger.info("trying to create new token");
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Token generation failed with status: " + response.getStatusCode());
            }

            logger.info("Token generated successfully for BIC: {}", credentialInfo.getBic());
            return response.getBody();
        });
    }


    public TokenInfo refreshToken(Participant credentialInfo, String refreshToken) throws Exception {
        logger.info("Attempting to refresh token for participant: {}", credentialInfo.getBic());
        create(credentialInfo.getTokenGenerationPath());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("jwt-assertion", credentialInfo.getJwt());

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("refresh_token", refreshToken);
        requestBody.add("grant_type", credentialInfo.getGrantType());

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(requestBody, headers);
// Retryable block for token generation
        return retryExecutorService.executeWithRetry(context -> {
            logger.info("trying to refresh token");
            ResponseEntity<TokenInfo> response = restTemplate.postForEntity(AUTH_SERVER_TOKEN_URI, httpEntity, TokenInfo.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Token generation failed with status: " + response.getStatusCode());
            }

            logger.info("Token generated successfully for BIC: {}", credentialInfo.getBic());
            return response.getBody();
        });

    }

    @Recover
    public TokenInfo recover(Exception e, Participant credentialInfo) {
        logger.error("Token generation failed after retries for participant: {}. Error: {}", credentialInfo.getBic(), e.getMessage());
        return null; // Or return a dummy token object or throw a custom exception
    }
}
