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


import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;

@Component
public class SchemaManagerClient {

    private final RestTemplate restTemplate;
    private final String schemamanagerEndpoint;

    public SchemaManagerClient(RestTemplate restTemplate, @Value("${metadata.schemamanager-endpoints.modelindex}") String schemamanagerEndpoint) {
        this.restTemplate = restTemplate;
        this.schemamanagerEndpoint = schemamanagerEndpoint;
    }

    /**
     * Store index-mapping to modelindex
     *
     * @param token     The access-token
     * @param indexName The name of the index
     * @return size of document
     * @deprecated Model should be indexed once the index is set up
     */
    @Deprecated(forRemoval = true)
    public int indexModel(String token, String indexName) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", format("Bearer %s", token));
        String url = format("%s?index=%s", schemamanagerEndpoint, indexName);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(headers), JsonNode.class);
        JsonNode body = response.getBody();
        return body != null ? body.size() : 0;

    }
}
