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
package com.efs.sdk.metadata.clients;

import com.efs.sdk.metadata.model.TokenModel;
import com.efs.sdk.metadata.security.oauth.OAuthConfiguration;
import com.efs.sdk.metadata.security.oauth.OAuthConfigurationHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * RestClient for metadata-backend
 */
@Component
public class MetadataRestClient {

    private final RestTemplate restTemplate;

    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;

    /**
     * Constructor.
     *
     * @param restTemplate The rest-template
     */
    public MetadataRestClient(RestTemplate restTemplate, @Value("${metadata.auth.client-id}") String clientId, @Value("${metadata.auth.client-secret}") String clientSecret, OAuthConfigurationHelper configHelper) {
        this.restTemplate = restTemplate;
        OAuthConfiguration oauthConfig = configHelper.getOpenidConfigProperty();
        this.tokenEndpoint = oauthConfig.tokenEndpoint();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Create token for service account
     *
     * @return the created token
     */
    public TokenModel getSAToken() {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("grant_type", "client_credentials");
        params.set("client_id", clientId);
        params.set("client_secret", clientSecret);
        params.set("scope", "profile");

        return restTemplate.postForEntity(tokenEndpoint, params, TokenModel.class).getBody();
    }
}
