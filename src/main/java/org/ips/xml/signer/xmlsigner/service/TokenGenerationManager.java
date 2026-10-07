package org.ips.xml.signer.xmlsigner.service;

import org.ips.xml.signer.xmlsigner.configuration.IpsParticipantProperties;
import org.ips.xml.signer.xmlsigner.models.JWTInfo;
import org.ips.xml.signer.xmlsigner.models.Participant;
import org.ips.xml.signer.xmlsigner.models.TokenInfo;
import org.ips.xml.signer.xmlsigner.models.TokenResponse;
import org.ips.xml.signer.xmlsigner.service.apiClient.TokenGenerationClientService;
import org.ips.xml.signer.xmlsigner.service.token.TokenCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class TokenGenerationManager {


    @Value("${ets.ips.token.url}")
    private String tokenUrl;

    private IpsParticipantProperties participantProperties;

    TokenGenerationClientService service;

    JWTManager jwtManager;


    private TokenCacheService cacheService;


    private static final long TOLERANCE_MILLIS = 20; // 1

    @Autowired
    public TokenGenerationManager(TokenGenerationClientService service,
                                  JWTManager jwtManager,
                                  TokenCacheService cacheService,
                                  IpsParticipantProperties participantProperties) {
        this.service = service;
        this.jwtManager = jwtManager;
        this.cacheService = cacheService;
        this.participantProperties = participantProperties;
    }

    public TokenInfo getNewToken(String clientBic) throws Exception {
        Participant credentialInfo = null;
        Optional<Participant> selectedParticipant = participantProperties.getByBic(clientBic);
        TokenInfo tokenInfo = null;
        if (selectedParticipant.isPresent()) {
            JWTInfo jwtInfo = jwtManager.getJWT(clientBic);
            credentialInfo = selectedParticipant.get();
            if (jwtInfo != null) {
                credentialInfo.setTokenGenerationPath(tokenUrl);
                credentialInfo.setJwt(jwtInfo.getJwt());
                tokenInfo = service.generateToken(credentialInfo);
            }
        }
        return tokenInfo;
    }

    public TokenInfo refreshToken(String refreshToken,String clientBic) throws Exception {
        Participant credentialInfo = new Participant();
        TokenInfo tokenInfo = null;
        JWTInfo jwtInfo = jwtManager.getJWT(clientBic);
        if (jwtInfo != null) {
            credentialInfo.setGrantType("refresh_token");
            credentialInfo.setTokenGenerationPath(tokenUrl);
            credentialInfo.setJwt(jwtInfo.getJwt());
            tokenInfo = service.refreshToken(credentialInfo, refreshToken);
        }
        return tokenInfo;
    }


    public TokenResponse getToken(String clientBic ) throws Exception {
        TokenInfo token = cacheService.getToken(clientBic);

        if (token != null && token.getExpires_in() > TOLERANCE_MILLIS) {
            return new TokenResponse(token.getAccess_token());
        }

        boolean canRefresh = token != null && token.getRefresh_expires_in() > TOLERANCE_MILLIS;

        if (canRefresh) {
            token = this.refreshToken(token.getRefresh_token(),clientBic);
        } else {
            token = this.getNewToken(clientBic);
        }

        cacheService.saveToken(clientBic, token);
        return new TokenResponse(token.getAccess_token());
    }
}
