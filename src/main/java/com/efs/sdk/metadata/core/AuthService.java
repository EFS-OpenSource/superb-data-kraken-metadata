/*
Copyright (C) 2023 e:fs TechHub GmbH (sdk@efs-techhub.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.efs.sdk.metadata.core;

import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.model.TokenModel;
import com.efs.sdk.metadata.security.oauth.OAuthConfiguration;
import com.efs.sdk.metadata.security.oauth.OAuthConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final OAuthConfiguration oauthConfig;

    private final String clientId;
    private final String clientSecret;
    private final RestTemplate restTemplate;

    /**
     * Constructor.
     *
     * @param restTemplate             The rest-template
     * @param clientId                 OIDC-Client-Id (confidential client)
     * @param clientSecret             OIDC-Client-Secret (confidential client)
     * @param oAuthConfigurationHelper OAuthConfigurationHelper
     */
    public AuthService(RestTemplate restTemplate, @Value("${metadata.auth.client-id}") String clientId,
            @Value("${metadata.auth.client-secret}") String clientSecret, OAuthConfigurationHelper oAuthConfigurationHelper) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.oauthConfig = oAuthConfigurationHelper.getOpenidConfigProperty();
    }


    /**
     * Create token for service account
     *
     * @return the created token
     */
    public String getSAAccessToken() throws MetadataException {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.set("grant_type", "client_credentials");
            params.set("client_id", clientId);
            params.set("client_secret", clientSecret);
            params.set("scope", "profile");

            return Objects.requireNonNull(restTemplate.postForEntity(oauthConfig.tokenEndpoint(), params, TokenModel.class).getBody()).getAccessToken();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(MetadataException.METADATA_ERROR.UNABLE_GET_ACCESS_TOKEN);
        }
    }
}
