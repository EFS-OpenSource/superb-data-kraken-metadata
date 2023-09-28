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

import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.metadata.commons.MetadataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class OrganizationmanagerService {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationmanagerService.class);

    private final String organizationEndpoint;
    private final String spaceEndpoint;

    private final RestTemplate restTemplate;

    /**
     * Constructor.
     *
     * @param restTemplate         The rest-template
     * @param organizationEndpoint The organization endpoint
     * @param spaceEndpoint        The space endpoint
     */
    public OrganizationmanagerService(RestTemplate restTemplate, @Value("${metadata.organizationmanager-endpoints.organization}") String organizationEndpoint, @Value("${metadata.organizationmanager-endpoints.space}") String spaceEndpoint) {
        this.restTemplate = restTemplate;
        this.organizationEndpoint = organizationEndpoint;
        this.spaceEndpoint = spaceEndpoint;
    }


    /**
     * Get organization
     *
     * @param accessToken The Access Token
     * @param id          The id of the organization
     * @return the description of the organization
     */
    public OrganizationContextDTO getOrganization(String accessToken, Long id) throws MetadataException {
        HttpEntity<String> request = getRequest(accessToken);
        String organizationGetNameEndpoint = organizationEndpoint + "/" + id;
        try {
            return restTemplate.exchange(organizationGetNameEndpoint, HttpMethod.GET, request, OrganizationContextDTO.class).getBody();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(UNABLE_GET_ORGANIZATION);
        }
    }


    /**
     * Get all organizations
     *
     * @param accessToken The Access Token
     * @return List of organizations
     */
    public List<OrganizationContextDTO> getOrganizations(String accessToken) throws MetadataException {
        HttpEntity<String> request = getRequest(accessToken);
        try {
            OrganizationContextDTO[] organizations = restTemplate.exchange(organizationEndpoint, HttpMethod.GET, request, OrganizationContextDTO[].class).getBody();
            if (organizations == null) throw new MetadataException(UNABLE_GET_ORGANIZATIONS);
            return Arrays.asList(organizations);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(UNABLE_GET_ORGANIZATIONS);
        }
    }

    /**
     * Gets all spaces
     *
     * @param accessToken  The Access Token
     * @param organization The Organization
     * @return List of spaces in organization
     */
    public List<SpaceContextDTO> getSpaces(String accessToken, OrganizationContextDTO organization) throws MetadataException {
        HttpEntity<String> request = getRequest(accessToken);
        String spacesGetEndpoint = spaceEndpoint + "/" + organization.getId();

        try {

            SpaceContextDTO[] spaceContextDTOS = restTemplate.exchange(spacesGetEndpoint, HttpMethod.GET, request, SpaceContextDTO[].class).getBody();
            if (spaceContextDTOS == null) throw new MetadataException(UNABLE_GET_SPACES);

            for (SpaceContextDTO space : spaceContextDTOS) space.setOrganization(organization);
            return Arrays.asList(spaceContextDTOS);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(UNABLE_GET_SPACES);
        }
    }

    /**
     * private helper method to prepare request with access token in HttpHeaders and ContentType APPLICATION_JSON
     *
     * @param accessToken The Access Token
     * @return Request in form of HttpEntity<>(headers)
     */
    private HttpEntity<String> getRequest(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(APPLICATION_JSON);

        return new HttpEntity<>(headers);
    }
}
