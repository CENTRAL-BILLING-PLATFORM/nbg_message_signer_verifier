package org.ips.xml.signer.xmlsigner.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class InsecureRestTemplateConfig {

    @Bean("insecureRestTemplate")
    public RestTemplate insecureRestTemplate() throws Exception {

        TrustManager[] trustAllManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                        // Trust all client certificates
                    }

                    @Override
                    public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                        // Trust all server certificates
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Empty key managers avoid loading a default client keystore.
        sslContext.init(
                new javax.net.ssl.KeyManager[0],
                trustAllManagers,
                new SecureRandom()
        );

        HostnameVerifier trustAllHostnames = (hostname, session) -> true;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory() {

                    @Override
                    protected void prepareConnection(
                            HttpURLConnection connection, String httpMethod)
                            throws java.io.IOException {

                        if (connection instanceof HttpsURLConnection https) {
                            https.setSSLSocketFactory(
                                    sslContext.getSocketFactory()
                            );
                            https.setHostnameVerifier(trustAllHostnames);
                        }

                        super.prepareConnection(connection, httpMethod);
                    }

                    @Override
                    protected HttpURLConnection openConnection(
                            URL url, Proxy proxy) throws java.io.IOException {
                        return (HttpURLConnection) (
                                proxy != null
                                        ? url.openConnection(proxy)
                                        : url.openConnection()
                        );
                    }
                };

        return new RestTemplate(factory);
    }
}