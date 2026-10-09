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

    public JWTInfo generateJwt(JWTInfo jwtInfo, String bankBic) throws NoSuchAlgorithmException, Exception {
        Optional<PrivateKey> optionalPrivateKey = certeficateAndKeysUtility.getBankPrivatekey(bankBic);
        Optional<X509Certificate> optionalX509Certificate = certeficateAndKeysUtility.getBankCertificate(bankBic);
        PrivateKey privateKey = optionalPrivateKey.isPresent() ? optionalPrivateKey.get() : null;
        Algorithm algorithm = Algorithm.RSA256((RSAKey) privateKey);
        X509Certificate key = optionalX509Certificate.isPresent() ? optionalX509Certificate.get() : null;
        BigInteger serialNumber = key.getSerialNumber();
        String issuer = key.getIssuerX500Principal().getName();
        jwtInfo.setIssuer(issuer);
        jwtInfo.setSerialNumber(serialNumber);


        /**//*String jwtToken = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFTkJHRVRBQSIsImNlcnRfaXNzIjoiQ049RVRTIElQUyBJc3N1aW5nIENBLCBPPUV0aFN3aXRjaCwgQz1FVCIsImNlcnRfc24iOiI2MDIxMjAxMjExNDAwNTMwNTI1MDM3ODM0NjUxMjYzODUxOTkzNjQ0NDAyMTQiLCJqdGkiOiIxMTIyMzMiLCJleHAiOjE2NTQyNTEyNTZ9.KaPaSp8JQrqWvHe1o2xLG_Hn_1G0fEu00Yhzc5xiSTfHZkNR2pbeD0011Z4jlATpItpHZ0J2lJikJrU2VHvVZq0foEaWcaorroQDdQSadTLVuMorfsqQN5F3o9z8ARMBmsoHG8zBdqQc9_t_gticUm_y8VTTNU2re1xR_zzBZ_LArJiKg9ySzClSVTeNRPSLm00TD7Q4UMkKCzR4okNhaz4JT4owUMpzO3igFkK50tabDDJg9r5lAYpUyhqDogTBkGvTqtXt-z2umSONjlsdgI-7BiTGlvIDhJNpWRFZ7Eo0WnEN7uFGvicxeeapjS2bi6frJh6iR1mx6UoL27IaeMOpki3XkdbpK_2V6Qx5txRo9YhdgCLlHuudrSCZXUJG025W1A8LLj-kfsmK8EejRtoPGyC3EAmsnl54BWHo510-O6ylYGoZSFe4ZvrUOVRCtfeuRf_5Kqfq79_TXaqM4yzYwP12Veh1VzqADYu-F2JPAkY4y16EFWMP83mVctWL5g14eKnsEFcjls21s952H9ptu2SRK7Kj-cWxxnepwVnvgH1p7yhz5xEP-UsyGfEqxv0Dju54Zmofj6aWMu9XCMw4mYMrCqUAgZS0A_LploHT_NQfdkJEeW9EJlSbeemr_N09CTbMmolFzlV3rjouSSA1Afy3FSuH-cZGY864KTk";*/

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
