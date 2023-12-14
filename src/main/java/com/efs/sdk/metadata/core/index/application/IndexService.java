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
package com.efs.sdk.metadata.core.index.application;


import com.efs.sdk.metadata.clients.OpenSearchRestClientBuilder;
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.AuthService;
import com.efs.sdk.metadata.model.ApplicationIndexCreateDTO;
import com.efs.sdk.metadata.model.ApplicationIndexType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.Request;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.*;
import static java.lang.String.format;

@Service
public class IndexService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexService.class);
    private static final Pattern INDEX_NAME_PATTERN = Pattern.compile("^[^<>\\\\/*?\"| ]*$");
    private static final Pattern CUSTOM_INDEX_NAME_PATTERN = Pattern.compile("^[^<>\\\\/*?\"| _]*$");
    private static final String APPLICATION_INDEX_PATTERN = "<org>_<space>_analysis_<custom-name>";

    private final OpenSearchRestClientBuilder clientBuilder;
    private final AuthService authService;

    public IndexService(OpenSearchRestClientBuilder clientBuilder, AuthService authService) {
        this.clientBuilder = clientBuilder;
        this.authService = authService;
    }

    public String createApplicationIndex(JwtAuthenticationToken token, ApplicationIndexCreateDTO indexCreateDTO) throws MetadataException, IOException {

        if (!isValidCustomIndexName(indexCreateDTO.getCustomName())) {
            throw new MetadataException(INDEX_NAME_INVALID, format("reason: custom index name %s invalid", indexCreateDTO.getCustomName()));
        }

        String indexName = format("%s_%s_%s_%s", indexCreateDTO.getOrganizationName(), indexCreateDTO.getSpaceName(),
                indexCreateDTO.getIndexType().getIndexNamePrefix(), indexCreateDTO.getCustomName()).toLowerCase(Locale.getDefault());
        if (!isValidIndexName(indexName)) {
            throw new MetadataException(INDEX_NAME_INVALID, format("(%s)", indexName));
        }

        if (!canCreateApplicationIndex(token, indexCreateDTO)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }

        RestClient client = clientBuilder.buildRestClient(authService.getSAAccessToken());
        if (indexExists(client, indexName)) {
            throw new MetadataException(INDEX_ALREADY_EXISTS, format("(%s)", indexName));
        }

        createIndexWithMapping(client, indexName, indexCreateDTO.getMappings());
        //        for future use
        //        createApplicationIndexAlias(client, indexName, indexCreateDTO.getIndexType().getIndexAlias());

        return indexName;
    }

    private boolean isValidCustomIndexName(String customName) {
        return CUSTOM_INDEX_NAME_PATTERN.matcher(customName).find();
    }

    private boolean isValidIndexName(String indexName) {
        return INDEX_NAME_PATTERN.matcher(indexName).find();
    }

    private boolean indexExists(RestClient client, String indexName) throws IOException {
        try {
            Request request = new Request(RequestMethod.GET.name(), format("/_cat/indices/%s", indexName));
            client.performRequest(request);
            return true;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
            throw e;
        }
    }

    protected boolean canCreateApplicationIndex(JwtAuthenticationToken token, ApplicationIndexCreateDTO dto) {
        Set<String> validRoles = Set.of(format("%s_%s_trustee", dto.getOrganizationName(), dto.getSpaceName()));
        return token.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(validRoles::contains);
    }

    private void createIndexWithMapping(RestClient client, String indexName, JsonNode indexMappings) throws IOException {
        LOG.info("creating index '{}'", indexName);
        String endpoint = format("/%s", indexName);
        Request request = new Request(RequestMethod.PUT.name(), endpoint);
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        if (indexMappings != null) {
            mapping.set("mappings", indexMappings);
            request.setJsonEntity(mapping.toString());
        }
        client.performRequest(request);
    }

    public void deleteApplicationIndex(String indexName, JwtAuthenticationToken token) throws MetadataException, IOException {
        if (!canDeleteApplicationIndex(token, indexName)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        if (!isValidApplicationIndexPrefix(indexName)) {
            throw new MetadataException(INDEX_NAME_INVALID, format("- does not contain application index prefix; pattern is '*_PREFIX_*' with possible " +
                    "values: %s", ApplicationIndexType.getIndexNamePrefixes()));
        }
        RestClient client = clientBuilder.buildRestClient(authService.getSAAccessToken());
        deleteApplicationIndex(indexName, client);
    }

    protected boolean canDeleteApplicationIndex(JwtAuthenticationToken token, String indexName) throws MetadataException {
        String[] indexNameParts = indexName.split("_");
        if (indexNameParts.length != APPLICATION_INDEX_PATTERN.split("_").length) {
            throw new MetadataException(INDEX_NAME_INVALID);
        }
        String org = indexNameParts[0];
        String space = indexNameParts[1];

        Set<String> validRoles = Set.of(format("%s_%s_trustee", org, space), format("org_%s_admin", org));
        return token.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(validRoles::contains);
    }

    private boolean isValidApplicationIndexPrefix(String indexName) {
        String applicationPrefix = indexName.split("_")[2];
        return ApplicationIndexType.getIndexNamePrefixes().contains(applicationPrefix);
    }

    private void deleteApplicationIndex(String indexName, RestClient client) throws IOException {
        String endpoint = format("/%s", indexName);
        try {
            client.performRequest(new Request(RequestMethod.DELETE.name(), endpoint));
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() != HttpStatus.NOT_FOUND.value()) {
                throw e;
            }
        }
    }
}

