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
package com.efs.sdk.metadata.core.context;


import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.common.domain.model.Capability;
import com.efs.sdk.common.domain.model.Confidentiality;
import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.metadata.clients.OpenSearchRestClientBuilder;
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.OrganizationmanagerService;
import com.efs.sdk.metadata.helper.OpensearchHelper;
import com.efs.sdk.metadata.helper.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.net.ConnectException;
import java.util.*;
import java.util.stream.Collectors;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.*;
import static java.lang.String.format;

@Service
public class OpensearchContextService {


    static final String MEASUREMENT_ALIAS = "measurements";
    static final String ENDPOINT_ALIAS = "/_alias";
    static final String KIBANA_ALL_READ = "kibana_all_read";
    static final String PUBLIC = "public";
    static final String ALL_PUBLIC = "all_public";
    static final String ORG_ALL_PUBLIC = "org_all_public";
    static final String SPC_ALL_PUBLIC = "spc_all_public";

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchContextService.class);

    private final OpenSearchRestClientBuilder clientBuilder;

    private final String endpointRoles;
    private final String endpointRolesMapping;
    private final String endpointTenants;
    private final ObjectMapper objectMapper;
    private final OpensearchHelper opensearchHelper;

    private final OrganizationmanagerService organizationmanagerService;

    /**
     * Constructor.
     *
     * @param organizationmanagerService The organizationmanager service
     */
    public OpensearchContextService(OrganizationmanagerService organizationmanagerService, OpenSearchRestClientBuilder clientBuilder,
            OpensearchHelper opensearchHelper, ObjectMapper objectMapper,
            @Value("${metadata.opensearch.security-endpoint}") String opensearchSecurityEndpoint) {
        this.clientBuilder = clientBuilder;
        this.objectMapper = objectMapper;
        this.endpointRoles = opensearchSecurityEndpoint + "/roles";
        this.endpointRolesMapping = opensearchSecurityEndpoint + "/rolesmapping";
        this.endpointTenants = opensearchSecurityEndpoint + "/tenants";
        this.opensearchHelper = opensearchHelper;
        this.organizationmanagerService = organizationmanagerService;
    }

    /**
     * Creates OpenSearch access control objects for a given organization.
     * This includes creating a tenant, organization roles, and organization role mappings.
     * The order of method calls within this method is important for proper setup.
     *
     * @param token        The access token used for authenticating API calls
     * @param organization The organization for which access control objects will be created
     * @throws MetadataException Thrown if any error occurs during the creation of access control objects
     */
    public void createOrganizationContext(String token, OrganizationContextDTO organization) throws MetadataException {
        // Order of method calls is important!
        createTenant(organization, token);
        createOrganizationRoles(organization, token);
        createOrganizationRolesMappings(organization, token);
    }

    /**
     * Creates organization tenant (if not already existing)
     *
     * @param organization The organization
     * @param token        The token
     * @throws MetadataException thrown on io-errors
     */
    public void createTenant(OrganizationContextDTO organization, String token) throws MetadataException {

        String orgName = organization.getName();
        String orgDescription = organization.getDescription() == null ? "" : organization.getDescription();

        createTenant(orgName, orgDescription, token);
    }

    /**
     * Creates space tenant (if not already existing)
     *
     * @param space The space
     * @param token The token
     * @throws MetadataException thrown on io-errors
     */
    public void createTenant(SpaceContextDTO space, String token) throws MetadataException {

        String tenantName = format("%s_%s", space.getOrganization().getName(), space.getName());
        createTenant(tenantName, "", token);
    }

    /**
     * Creates organization tenant (if not already existing)
     *
     * @param tenantName        The name of the tenant
     * @param tenantDescription The description of the tenant
     * @param token             The token
     * @throws MetadataException thrown on io-errors
     */
    public void createTenant(String tenantName, String tenantDescription, String token) throws MetadataException {

        RestClient restClient = clientBuilder.buildRestClient(token);
        String jsonEntity = format("{\"description\":\"%s\"}", tenantDescription);
        Set<String> existingTenants = getTenants(restClient);

        if (existingTenants.contains(tenantName)) {
            LOG.warn("tenant '{}' already exists", tenantName);
            return;
        }

        LOG.debug("creating tenant '{}': {}", tenantName, jsonEntity);

        String endpoint = format("%s/%s", endpointTenants, tenantName);
        putObject(restClient, endpoint, jsonEntity, UNABLE_CREATE_TENANT);
    }

    /**
     * Creates all roles for the given organization.
     * <p>
     * Creates the following roles for the organization:
     * - "<orgname>_admin"
     * - "<orgname>_trustee"
     * - "<orgname>_access"
     * <p>
     * If the organization's confidentiality is set to public, only the role "<orgname>_public" is created.
     * <p>
     * Assumes that the organization name is also the tenant name.
     *
     * @param organization The organization for which to create roles.
     * @param token        The authentication token to use.
     * @throws MetadataException if there is an error creating the roles.
     */
    public void createOrganizationRoles(OrganizationContextDTO organization, String token) throws MetadataException {

        RestClient restClient = clientBuilder.buildRestClient(token);
        String organizationName = organization.getName();

        if (Confidentiality.PUBLIC.equals(organization.getConfidentiality())) {
            createRole(restClient, opensearchHelper.getTenantRoleRequest(KIBANA_ALL_READ, organizationName),
                    opensearchHelper.getOrganizationRoleName(organizationName, PUBLIC));
        } else {
            for (RoleScopeOrganization scope : RoleScopeOrganization.values()) {
                for (String role : scope.getRoles()) {
                    createRole(restClient, opensearchHelper.getTenantRoleRequest(scope.getPermission(), organizationName),
                            opensearchHelper.getOrganizationRoleName(organizationName, role));
                }
            }
        }
    }

    /**
     * Creates role mappings for all roles in the given organization.
     *
     * @param organization The organization for which to create role mappings.
     * @param token        The authentication used.
     * @throws MetadataException if there is an error creating the role mappings.
     */
    public void createOrganizationRolesMappings(OrganizationContextDTO organization, String token) throws MetadataException {

        RestClient restClient = clientBuilder.buildRestClient(token);
        String organizationName = organization.getName();

        if (Confidentiality.PUBLIC.equals(organization.getConfidentiality())) {
            createRolesMapping(restClient, opensearchHelper.getOrganizationRoleName(organizationName, PUBLIC),
                    opensearchHelper.getRolesMappingJson(Collections.singletonList(ORG_ALL_PUBLIC)));
        } else {
            for (RoleScopeOrganization scope : RoleScopeOrganization.values()) {
                for (String role : scope.getRoles()) {
                    createRolesMapping(restClient, opensearchHelper.getOrganizationRoleName(organizationName, role),
                            opensearchHelper.getRolesMappingJson(Collections.singletonList(format("org_%s_%s", organizationName, role))));
                }
            }
        }
    }

    /**
     * Gets the currently present tenants from opensearch
     *
     * @param restClient The RestClient
     * @return the names of all tenants
     * @throws MetadataException thrown on io-errors
     */
    private Set<String> getTenants(RestClient restClient) throws MetadataException {
        return getResourceNames(restClient, endpointTenants, UNABLE_GET_TENANTS);
    }

    /**
     * helper function that puts an object to an opensearch endpoint
     *
     * @param restClient    The RestClient
     * @param endpoint      The endpoint
     * @param requestBody   The request Body
     * @param metadataError The metadata error type
     * @throws MetadataException thrown on io-errors
     */
    private void putObject(RestClient restClient, String endpoint, String requestBody, MetadataException.METADATA_ERROR metadataError) throws MetadataException {
        try {
            StringEntity entity = new StringEntity(requestBody);
            entity.setContentType(ContentType.APPLICATION_JSON.toString());

            Request request = new Request(RequestMethod.PUT.name(), endpoint);
            request.setEntity(entity);
            restClient.performRequest(request);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(metadataError);
        }
    }

    /**
     * Creates a role with name
     *
     * @param restClient  The RestClient
     * @param requestBody The request Body
     * @param roleName    The name of the role
     * @throws MetadataException thrown on io-errors
     */
    private void createRole(RestClient restClient, String requestBody, String roleName) throws MetadataException {
        LOG.debug("creating role {}", roleName);
        String endpoint = format("%s/%s", endpointRoles, roleName);
        putObject(restClient, endpoint, requestBody, UNABLE_CREATE_ESROLE);
    }

    /**
     * @param restClient       The RestClient
     * @param roleName         The full role name in the format <org>_<spc>_("user", "supplier", "trustee")
     * @param rolesMappingJson The mappings as a Json String
     * @throws MetadataException Thrown on input-output errors
     */
    private void createRolesMapping(RestClient restClient, String roleName, String rolesMappingJson) throws MetadataException {
        LOG.debug("creating rolesmapping {}", roleName);
        String endpoint = format("%s/%s", endpointRolesMapping, roleName);
        putObject(restClient, endpoint, rolesMappingJson, UNABLE_CREATE_ROLESMAPPING);
    }

    /**
     * Gets names of resources out of the endpoints return-value
     *
     * @param restClient The RestClient
     * @param endpoint   The endpoint to get the resources out of
     * @return the keys of the endpoints returnvalue
     * @throws MetadataException thrown on io-errors
     */
    private Set<String> getResourceNames(RestClient restClient, String endpoint, MetadataException.METADATA_ERROR metadataError) throws MetadataException {
        try {
            Request request = new Request(RequestMethod.GET.name(), endpoint);
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            Map<String, Object> resourceNames = objectMapper.readValue(responseBody, Map.class);
            return resourceNames.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(metadataError);
        }
    }

    /**
     * Deletes OpenSearch access control objects associated with a given organization.
     * This includes deleting organization role mappings, organization roles, and the tenant.
     *
     * @param token            The access token used for authenticating API calls
     * @param organizationName The organization identifier for which access control objects will be deleted
     * @throws MetadataException Thrown if any error occurs during the deletion of access control objects
     */
    public void deleteOrganizationContext(String token, String organizationName) throws MetadataException {
        LOG.debug("deleting opensearch resources for organization '{}'", organizationName);
        deleteOrganizationRolesMappings(organizationName, token);
        deleteOrganizationRoles(organizationName, token);
        deleteTenant(organizationName, token);
    }

    /**
     * Deletes all organization role mappings
     *
     * @param organizationName The organization
     * @param token            The token
     * @throws MetadataException thrown on io-errors
     */
    public void deleteOrganizationRolesMappings(String organizationName, String token) throws MetadataException {

        RestClient restClient = clientBuilder.buildRestClient(token);

        deleteRolesMapping(restClient, opensearchHelper.getOrganizationRoleName(organizationName, PUBLIC));
        for (RoleScopeOrganization scope : RoleScopeOrganization.values()) {
            for (String role : scope.getRoles()) {
                deleteRolesMapping(restClient, opensearchHelper.getOrganizationRoleName(organizationName, role));
            }
        }
    }

    /**
     * Deletes all organization roles
     *
     * @param organization The organization
     * @param token        The token
     * @throws MetadataException thrown on io-errors
     */
    public void deleteOrganizationRoles(String organization, String token) throws MetadataException {
        RestClient restClient = clientBuilder.buildRestClient(token);

        deleteRole(restClient, opensearchHelper.getOrganizationRoleName(organization, PUBLIC));
        for (RoleScopeOrganization scope : RoleScopeOrganization.values()) {
            for (String role : scope.getRoles()) {
                deleteRole(restClient, opensearchHelper.getOrganizationRoleName(organization, role));
            }
        }
    }

    /**
     * Deletes tenant by name (if existing)
     *
     * @param tenantName The name of the tenant
     * @param token      The token
     * @throws MetadataException thrown on io-errors
     */
    public void deleteTenant(String tenantName, String token) throws MetadataException {

        RestClient restClient = clientBuilder.buildRestClient(token);

        LOG.debug("deleting tenant '{}'", tenantName);
        String endpoint = format("%s/%s", endpointTenants, tenantName);
        deleteObject(restClient, endpoint, UNABLE_DELETE_TENANT);
    }

    /**
     * Deletes the given rolesmapping
     *
     * @param restClient The RestClient
     * @param roleName   The name of the rolesmapping
     * @throws MetadataException thrown on io-errors
     */
    private void deleteRolesMapping(RestClient restClient, String roleName) throws MetadataException {
        LOG.debug("deleting rolesmapping '{}'", roleName);
        String endpoint = format("%s/%s", endpointRolesMapping, roleName);
        deleteObject(restClient, endpoint, UNABLE_DELETE_ROLESMAPPING);
    }

    /**
     * Deletes the given roles
     *
     * @param restClient The RestClient
     * @param roleName   The name of the roles
     * @throws MetadataException thrown on io-errors
     */

    private void deleteRole(RestClient restClient, String roleName) throws MetadataException {

        LOG.debug("deleting role '{}'", roleName);
        String endpoint = format("%s/%s", endpointRoles, roleName);
        deleteObject(restClient, endpoint, UNABLE_DELETE_ROLE);

    }

    /**
     * Deletes an opensearch object on the given endpoint url
     *
     * @param restClient    The RestClient
     * @param endpoint      The endpoint
     * @param metadataError the type of metadata-error
     * @throws MetadataException thrown on io-errors
     */
    private void deleteObject(RestClient restClient, String endpoint, MetadataException.METADATA_ERROR metadataError) throws MetadataException {
        try {
            Request request = new Request(RequestMethod.DELETE.name(), endpoint);
            restClient.performRequest(request);
        } catch (ResponseException e) {
            var status = e.getResponse().getStatusLine().getStatusCode();
            if (HttpStatus.NOT_FOUND.value() == status) {
                LOG.debug("opensearch resource not found, skipping deletion of {}...", endpoint);
            } else {
                LOG.error(e.getMessage(), e);
                throw new MetadataException(metadataError);
            }
        } catch (ConnectException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(OPENSEARCH_CONNECTION_ERROR);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(metadataError);
        }
    }

    /**
     * Deletes OpenSearch access control objects associated with a specific space within an organization (overloaded).
     * This method retrieves the organization and space details and calls deleteSpaceResources for the actual deletion.
     *
     * @param token            The access token used for authenticating API calls
     * @param spaceName        The name of the space for which access control objects will be deleted
     * @param organizationName The name of the organization containing the space
     * @throws MetadataException Thrown if any error occurs during the deletion of access control objects
     */
    public void deleteSpaceContext(String token, String spaceName, String organizationName) throws MetadataException {
        deleteSpaceRoles(organizationName, spaceName, token);
        deleteSpaceRolesMappings(organizationName, spaceName, token);
        deleteTenant(format("%s_%s", organizationName, spaceName), token);
        // TODO: how to handle? see capability ticket
        //        deleteMeasurementIndex(organizationName, spaceName, token);
    }

    /**
     * Deletes all roles for the given space, if they exist.
     * The method does not check beforehand if the resources to be deleted exist. It attempts to delete them and handles not found resources gracefully.
     * It also does not check which resources SHOULD exist for the given space, but attempts to delete all resources that COULD exist.
     *
     * @param organizationName The name of the organization to which the space belongs.
     * @param spaceName        The name of the space for which to delete roles.
     * @param token            The authentication token to use.
     * @throws MetadataException if there is an error deleting the roles.
     */
    public void deleteSpaceRoles(String organizationName, String spaceName, String token) throws MetadataException {
        RestClient restClient = clientBuilder.buildRestClient(token);

        deleteRole(restClient, opensearchHelper.getSpaceRoleName(organizationName, spaceName, ALL_PUBLIC));
        for (RoleScopeSpace scope : RoleScopeSpace.values()) {
            for (String role : scope.getRoles()) {
                deleteRole(restClient, opensearchHelper.getSpaceRoleName(organizationName, spaceName, role));
            }
        }
    }

    /**
     * Deletes all role mappings for the given space, if they exist.
     * <p>
     * Deletes the role mappings that were created for the space, including the public role mapping and any role-specific mappings.
     *
     * @param organizationName The name of the organization to which the space belongs.
     * @param spaceName        The name of the space for which to delete role mappings.
     * @param token            The authentication token to use.
     * @throws MetadataException if there is an error deleting the role mappings.
     */
    public void deleteSpaceRolesMappings(String organizationName, String spaceName, String token) throws MetadataException {
        RestClient restClient = clientBuilder.buildRestClient(token);

        String roleName = opensearchHelper.getSpaceRoleName(organizationName, spaceName, ALL_PUBLIC);
        deleteRolesMapping(restClient, roleName);

        for (RoleScopeSpace scope : RoleScopeSpace.values()) {
            for (String role : scope.getRoles()) {
                roleName = opensearchHelper.getSpaceRoleName(organizationName, spaceName, role);
                deleteRolesMapping(restClient, roleName);
            }
        }
    }

    private void deleteMeasurementIndex(String orgName, String spcName, String token) throws MetadataException {
        RestClient restClient = clientBuilder.buildRestClient(token);

        // delete measurement index
        String indexName = format("%s_%s_%s", orgName, spcName, MEASUREMENT_ALIAS);
        LOG.debug("deleting index '{}'", indexName);
        String endpoint = format("/%s", indexName);
        deleteObject(restClient, endpoint, UNABLE_DELETE_INDEX);

        // delete measurement alias
        LOG.debug("deleting alias '{}' for index '{}'", MEASUREMENT_ALIAS, indexName);
        endpoint = format("/%s/%s/%s", indexName, ENDPOINT_ALIAS, MEASUREMENT_ALIAS);
        deleteObject(restClient, endpoint, UNABLE_DELETE_INDEX);
    }

    /**
     * Recovers roles and role mappings
     *
     * @param token The Token
     * @throws MetadataException thrown on MetadataException Errors
     */
    public void syncOpensearch(String token) throws MetadataException {
        List<OrganizationContextDTO> organizations = organizationmanagerService.getOrganizations(token);

        for (OrganizationContextDTO organization : organizations) {
            AuditLogger.info(LOG, "Updating OpenSearch context for organization {}", Utils.getSubjectAsToken(),
                    organization.getName());
            updateOrganizationContext(organization, token);
            createTenant(organization, token);

            List<SpaceContextDTO> spaceContextDTOList = organizationmanagerService.getSpaces(token, organization);

            for (SpaceContextDTO space : spaceContextDTOList) {
                AuditLogger.info(LOG, "Updating OpenSearch context for space {} in organization {}",
                        Utils.getSubjectAsToken(), space.getName(), organization.getName());
                updateSpaceContext(space, token);
            }
        }
    }

    /**
     * Updates OpenSearch access control objects for a given organization, by recreating the whole context
     *
     * @param organization The organization for which access control objects will be created
     * @param token        The access token used for authenticating API calls
     * @throws MetadataException Thrown if any error occurs during the creation of access control objects
     */
    public void updateOrganizationContext(OrganizationContextDTO organization, String token) throws MetadataException {
        deleteOrganizationRolesMappings(organization.getName(), token);
        deleteOrganizationRoles(organization.getName(), token);

        createOrganizationRoles(organization, token);
        createOrganizationRolesMappings(organization, token);
    }

    /**
     * Updates OpenSearch access control objects for a given space, by recreating the whole context
     * !USE WITH CAUTION!
     * Updating space capabilities is a WIP - it is not clear what should be done with measurement indices if a space looses the METADATA capability
     *
     * @param space The space for which to create access control objects.
     * @param token The authentication token to use.
     * @throws MetadataException if there is an error creating the access control objects.
     */
    public void updateSpaceContext(SpaceContextDTO space, String token) throws MetadataException {
        String organizationName = space.getOrganization().getName();
        String spaceName = space.getName();

        deleteSpaceRoles(organizationName, spaceName, token);
        deleteSpaceRolesMappings(organizationName, spaceName, token);
        // deleteMeasurementIndex(organizationName, spaceName, token); // omit for now

        createSpaceContext(space, token);
    }

    /**
     * Creates OpenSearch access control objects for a given space.
     * This includes creating space roles, and space role mappings.
     *
     * @param space The space for which to create access control objects.
     * @param token The authentication token to use.
     * @throws MetadataException if there is an error creating the access control objects.
     */
    public void createSpaceContext(SpaceContextDTO space, String token) throws MetadataException {
        // Order of method calls is important!
        createTenant(space, token);
        createSpaceRoles(space, token);
        createSpaceRolesMappings(space, token);

        if (hasMetadataCapability(space)) {
            createMeasurementIndex(space, token);
        }
    }

    /**
     * Creates all roles for the given space.
     * <p>
     * If the space's confidentiality is set to public, only one role is created with read permission for all indices. <br>
     * - {orgname}_{spacename}_all_public <br>
     * Otherwise, roles are created for each role scope with the corresponding permission for all indices with the space name as prefix: <br>
     * - {orgname}_{spacename}_user <br>
     * - {orgname}_{spacename}_supplier <br>
     * - {orgname}_{spacename}_trustee <br>
     *
     * @param space The SpaceDTO object containing the space's information.
     * @param token The authentication token for the request.
     * @throws MetadataException If an error occurs while creating the roles.
     */
    public void createSpaceRoles(SpaceContextDTO space, String token) throws MetadataException {
        RestClient restClient = clientBuilder.buildRestClient(token);
        if (Confidentiality.PUBLIC.equals(space.getConfidentiality())) {
            String roleName = opensearchHelper.getRoleName(ALL_PUBLIC, space);
            String roleRequestBody = opensearchHelper.createPublicRoleDefinition(space);
            createRole(restClient, roleRequestBody, roleName);
        } else {
            for (RoleScopeSpace scope : RoleScopeSpace.values()) {
                for (String role : scope.getRoles()) {
                    String roleName = opensearchHelper.getRoleName(role, space);
                    String roleRequestBody = opensearchHelper.createRoleDefinition(space, scope);
                    createRole(restClient, roleRequestBody, roleName);
                }
            }
        }
    }

    /**
     * Creates role mappings for the given space.
     * <p>
     * Creates the role mappings necessary to control access to the space based on the space's confidentiality and the organization's confidentiality.
     *
     * @param space The space for which to create role mappings.
     * @param token The authentication token to use.
     * @throws MetadataException if there is an error creating the role mappings.
     */
    public void createSpaceRolesMappings(SpaceContextDTO space, String token) throws MetadataException {
        RestClient restClient = clientBuilder.buildRestClient(token);

        String orgRole = format("org_%s_access", space.getOrganization().getName());
        if (Confidentiality.PUBLIC.equals(space.getOrganization().getConfidentiality())) {
            orgRole = ORG_ALL_PUBLIC;
        }

        if (Confidentiality.PUBLIC.equals(space.getConfidentiality())) {
            String roleName = opensearchHelper.getRoleName(ALL_PUBLIC, space);
            LOG.debug("creating rolemapping {}", roleName);
            List<String> mappings = new ArrayList<>();
            mappings.add(SPC_ALL_PUBLIC);
            mappings.add(orgRole);

            createRolesMapping(restClient, roleName, opensearchHelper.getRolesMappingJson(mappings));
        } else {
            for (RoleScopeSpace scope : RoleScopeSpace.values()) {
                for (String role : scope.getRoles()) {
                    String roleName = opensearchHelper.getRoleName(role, space);
                    LOG.debug("creating rolemapping '{}'", roleName);

                    List<String> mappings = new ArrayList<>();
                    mappings.add(format("%s_%s_%s", space.getOrganization().getName(), space.getName(), role));
                    mappings.add(orgRole);

                    createRolesMapping(restClient, roleName, opensearchHelper.getRolesMappingJson(mappings));
                }
            }
        }
    }

    private boolean hasMetadataCapability(SpaceContextDTO space) {
        return space.getCapabilities().contains(Capability.METADATA);
    }

    private void createMeasurementIndex(SpaceContextDTO space, String token) throws MetadataException {
        RestClient restClient = clientBuilder.buildRestClient(token);

        // create measurement index
        String indexName = format("%s_%s_%s", space.getOrganization().getName(), space.getName(), MEASUREMENT_ALIAS).toLowerCase(Locale.getDefault());

        createIndex(restClient, indexName);
        createIndexAlias(restClient, indexName);
    }

    private void createIndex(RestClient restClient, String indexName) throws MetadataException {
        LOG.debug("creating index '{}'", indexName);
        String endpoint = format("/%s", indexName);

        putObjectWithoutBody(restClient, endpoint, UNABLE_CREATE_INDEX);
    }

    private void createIndexAlias(RestClient restClient, String indexName) throws MetadataException {
        LOG.debug("creating index alias for '{}'", indexName);
        String endpoint = format("/%s/%s/%s", indexName, ENDPOINT_ALIAS, MEASUREMENT_ALIAS);

        putObjectWithoutBody(restClient, endpoint, UNABLE_CREATE_ALIAS);
    }

    private void putObjectWithoutBody(RestClient restClient, String endpoint, MetadataException.METADATA_ERROR unableCreateIndex) throws MetadataException {
        try {
            Request request = new Request(RequestMethod.PUT.name(), endpoint);
            restClient.performRequest(request);
        } catch (ResponseException e) {
            var status = e.getResponse().getStatusLine().getStatusCode();
            if (HttpStatus.BAD_REQUEST.value() == status) {
                LOG.debug("index already exists, skipping creation of {}...", endpoint);
            } else {
                LOG.error(e.getMessage(), e);
                throw new MetadataException(unableCreateIndex);
            }
        } catch (ConnectException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(OPENSEARCH_CONNECTION_ERROR);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(UNKNOWN_ERROR);
        }
    }

}
