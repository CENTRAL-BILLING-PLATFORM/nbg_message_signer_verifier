package org.ips.xml.signer.xmlsigner.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.ips.xml.signer.xmlsigner.models.ServiceRequestHeader;
import org.ips.xml.signer.xmlsigner.service.digestService.DigestService;
import org.ips.xml.signer.xmlsigner.service.digestService.XMLDigestVerifier;
import org.ips.xml.signer.xmlsigner.utils.JwtSigningUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@Slf4j
@RequestMapping("/api")
@RestController()
public class DigestController {


    private DigestService digestService;







    @Autowired
    DigestController(
            DigestService digestService,
            XMLDigestVerifier digestVerifier,
            JwtSigningUtils jwtSigningUtils) {
        this.digestService = digestService;

    }

    @PostMapping(
            value = "/digest",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.ALL_VALUE}
    )

    public String handleXmlRequest(@RequestBody String request, HttpServletRequest servletRequest) {
        // Sanitize XML input
        if (!isValidXml(request)) {
            return HttpStatus.BAD_REQUEST +"Invalid XML input";
        }
        ServiceRequestHeader requestHeader = new ServiceRequestHeader();
        String accessToken = servletRequest.getHeader("access_token");
        String certificateString = servletRequest.getHeader("X-Certificate");
        String bankBic = servletRequest.getHeader("bank_bic");
        if (StringUtils.hasText(accessToken)) {
            requestHeader.setAccess_token(accessToken);
        }
        if (StringUtils.hasText(certificateString)) {
            requestHeader.setCertificateString(certificateString);
        }
        log.info(" the bank bic information is: {}",bankBic);

        // Securely parse XML inside digestService.signDocument()
        String xmlResponse = digestService.signDocument(request,bankBic);

        // Remove unsafe XML characters
        xmlResponse = xmlResponse.replace("&#xD;", "");
        return xmlResponse;
    }

    // Example XML validation function
    private boolean isValidXml(String xml) {
        return xml != null && xml.trim().startsWith("<?xml");
    }


}



