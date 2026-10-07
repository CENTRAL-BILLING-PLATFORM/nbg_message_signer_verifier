package org.ips.xml.signer.xmlsigner.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.NoArgsConstructor;
import org.ips.xml.signer.xmlsigner.models.JWTInfo;
import org.ips.xml.signer.xmlsigner.repository.CertificateCacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.util.Date;
import java.util.Optional;

@Service
@NoArgsConstructor
public class JwtSigningUtils {
    CertificateCacheRepository certeficateAndKeysUtility;
    int accessExpirationMs = 9600000;

    @Autowired
    public JwtSigningUtils(CertificateCacheRepository certeficateAndKeysUtility) {
        this.certeficateAndKeysUtility = certeficateAndKeysUtility;
    }

    public JWTInfo generateJwt(JWTInfo jwtInfo,String bankBic) throws NoSuchAlgorithmException, Exception {
        Optional<PrivateKey> optionalPrivateKey=certeficateAndKeysUtility.getBankPrivatekey(bankBic);
        Optional<X509Certificate> optionalX509Certificate=certeficateAndKeysUtility.getBankCertificate(bankBic);
        PrivateKey privateKey =optionalPrivateKey.isPresent()?optionalPrivateKey.get():null;
        Algorithm algorithm = Algorithm.RSA256((RSAKey) privateKey);
        X509Certificate key = optionalX509Certificate.isPresent()? optionalX509Certificate.get() : null;
        BigInteger serialNumber = key.getSerialNumber();
        String issuer = key.getIssuerX500Principal().getName();
        jwtInfo.setIssuer(issuer);
        jwtInfo.setSerialNumber(serialNumber);
        String jwtToken = JWT.create()
                .withIssuer(jwtInfo.getParticipantBic())
                .withClaim("cert_iss", issuer)
                .withClaim("cert_sn", String.valueOf(serialNumber))
                .withExpiresAt(new Date(System.currentTimeMillis() + 5000L))
                .withJWTId("11223312412321090909090")
                .sign(algorithm);
        jwtInfo.setJwt(jwtToken);
        return jwtInfo;
    }

}
