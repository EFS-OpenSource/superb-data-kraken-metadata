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
import com.efs.sdk.metadata.clients.OpenSearchRestClientBuilder;
import com.efs.sdk.metadata.clients.OpenSearchRestClientBuilderTest;
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.OrganizationmanagerService;
import com.efs.sdk.metadata.helper.OpensearchHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static com.efs.sdk.metadata.utils.TestHelper.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.TOKEN;

class OpensearchContextServiceTest {
    private static final String ENDPOINT_ROLES = "/_plugins/_security/api/roles";
    private static final String ENDPOINT_ROLESMAPPING = "/_plugins/_security/api/rolesmapping";
    private static final String ENDPOINT_TENANTS = "/_plugins/_security/api/tenants";

    @MockBean
    private OrganizationmanagerService organizationmanagerService;
    private OpensearchContextService opensearchContextServiceSpy;

    private OrganizationContextDTO testOrganization;
    private SpaceContextDTO testSpace;

    @MockBean
    private OpenSearchRestClientBuilder clientBuilder;
    @MockBean
    private OpensearchHelper opensearchHelper;
    @MockBean
    private ObjectMapper objectMapper;
    private ClientAndServer mockServer;

    @AfterEach
    void destroy() {
        mockServer.stop();
    }

    @Test
    void givenDeletedAll_whenDeleteOrga_thenTrue() {
        // roles can be deleted
        String roleName = format("%s_%s", testOrganization.getName(), "public");
        String endpoint = format("%s/%s", ENDPOINT_ROLESMAPPING, roleName);
        HttpRequest roleMappingRequest = HttpRequest.request().withMethod(HttpMethod.DELETE.name()).withPath(endpoint);
        mockServer.when(roleMappingRequest).respond(response().withStatusCode(OK.value()));

        for (RoleScopeOrganization scope : RoleScopeOrganization.values()) {
            for (String role : scope.getRoles()) {
                roleName = format("%s_%s", testOrganization.getName(), role);
                endpoint = format("%s/%s", ENDPOINT_ROLESMAPPING, roleName);
                roleMappingRequest = HttpRequest.request().withMethod(HttpMethod.DELETE.name()).withPath(endpoint);
                mockServer.when(roleMappingRequest).respond(response().withStatusCode(OK.value()));
            }
        }

        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteOrganizationRolesMappings(testOrganization.getName(), TOKEN));
    }

    @BeforeEach
    void setup() throws IOException {
        Integer port = findRandomPort();
        ConfigurationProperties.logLevel("INFO");
        this.clientBuilder = new OpenSearchRestClientBuilderTest("http://127.0.0.1:" + port);
        this.mockServer = ClientAndServer.startClientAndServer(port);
        this.objectMapper = Mockito.spy(ObjectMapper.class);
        this.opensearchHelper = Mockito.spy(new OpensearchHelper(this.objectMapper));
        this.organizationmanagerService = Mockito.mock(OrganizationmanagerService.class);
        String opensearch_security_endpoint = "/_plugins/_security/api";
        this.opensearchContextServiceSpy = Mockito.spy(new OpensearchContextService(organizationmanagerService, clientBuilder, opensearchHelper, objectMapper, opensearch_security_endpoint));
        OpensearchContextService opensearchContextServiceMock = Mockito.mock(OpensearchContextService.class);
        this.testOrganization = OrganizationContextDTO.builder().id(1L).name("test").description("description").build();
        this.testSpace = SpaceContextDTO.builder().name("test").organization(testOrganization).build();
    }

    private void mockOrganizationRoleEndpoints(OrganizationContextDTO organization, HttpMethod method, HttpStatus retStatus) {
        String roleName = format("%s_%s", organization.getName(), "public");
        String endpoint = format("%s/%s", ENDPOINT_ROLES, roleName);
        HttpRequest roleMappingRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
        mockServer.when(roleMappingRequest).respond(response().withStatusCode(retStatus.value()));

        for (RoleScopeOrganization scope : RoleScopeOrganization.values()) {
            for (String role : scope.getRoles()) {
                roleName = format("%s_%s", organization.getName(), role);
                endpoint = format("%s/%s", ENDPOINT_ROLES, roleName);
                roleMappingRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
                mockServer.when(roleMappingRequest).respond(response().withStatusCode(retStatus.value()));
            }
        }
    }

    private void mockOrganizationRolesmappingEndpoints(OrganizationContextDTO organization, HttpMethod method, HttpStatus retStatus) {
        String roleName = format("%s_%s", organization.getName(), "public");
        String endpoint = format("%s/%s", ENDPOINT_ROLESMAPPING, roleName);
        HttpRequest roleMappingRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
        mockServer.when(roleMappingRequest).respond(response().withStatusCode(retStatus.value()));

        for (RoleScopeOrganization scope : RoleScopeOrganization.values()) {
            for (String role : scope.getRoles()) {
                roleName = format("%s_%s", organization.getName(), role);
                endpoint = format("%s/%s", ENDPOINT_ROLESMAPPING, roleName);
                roleMappingRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
                mockServer.when(roleMappingRequest).respond(response().withStatusCode(retStatus.value()));
            }
        }
    }

    private void mockSpaceRoleEndpoints(SpaceContextDTO space, HttpMethod method, HttpStatus returnStatus) {
        String rolesResult = null;
        try {
            rolesResult = getInputContent(RESULT_PATH, "roles.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String roleName = format("%s_%s", space.getOrganization().getName(), "public");
        String endpoint = format("%s/%s", ENDPOINT_ROLES, roleName);
        HttpRequest roleRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
        mockServer.when(roleRequest).respond(response().withBody(rolesResult).withStatusCode(OK.value()));

        for (RoleScopeSpace scope : RoleScopeSpace.values()) {
            for (String role : scope.getRoles()) {
                roleName = format("%s_%s_%s", space.getOrganization().getName(), space.getName(), role);
                endpoint = format("%s/%s", ENDPOINT_ROLES, roleName);
                roleRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
                mockServer.when(roleRequest).respond(response().withBody(rolesResult).withStatusCode(returnStatus.value()));
            }
        }
    }

    private void mockSpaceRolesmappingEndpoints(SpaceContextDTO space, HttpMethod method, HttpStatus retStatus) {
        String rolesResult = null;
        try {
            rolesResult = getInputContent(RESULT_PATH, "roles.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String roleName = format("%s_%s", space.getOrganization().getName(), "public");
        String endpoint = format("%s/%s", ENDPOINT_ROLESMAPPING, roleName);
        HttpRequest rolesMappingRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
        mockServer.when(rolesMappingRequest).respond(response().withBody(rolesResult).withStatusCode(retStatus.value()));

        for (RoleScopeSpace scope : RoleScopeSpace.values()) {
            for (String role : scope.getRoles()) {
                roleName = format("%s_%s_%s", space.getOrganization().getName(), space.getName(), role);
                endpoint = format("%s/%s", ENDPOINT_ROLESMAPPING, roleName);
                rolesMappingRequest = HttpRequest.request().withMethod(method.name()).withPath(endpoint);
                mockServer.when(rolesMappingRequest).respond(response().withBody(rolesResult).withStatusCode(retStatus.value()));
            }
        }
    }

    @Test
    void givenErrorCreatingTenant_whenCreateTenant_thenError() throws Exception {

        // tenant does not exist
        String tenantsResult = getInputContent(RESULT_PATH, "tenants.json");
        HttpRequest tenantsRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(ENDPOINT_TENANTS);
        mockServer.when(tenantsRequest).respond(response().withBody(tenantsResult).withStatusCode(OK.value()));

        // create tenant
        String endpoint = format("%s/%s", ENDPOINT_TENANTS, testOrganization.getName());
        HttpRequest createTenantRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(endpoint);
        mockServer.when(createTenantRequest).respond(response().withStatusCode(CONFLICT.value()));
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.createTenant(testOrganization.getName(), testOrganization.getDescription() == null ? "" : testOrganization.getDescription(), TOKEN));
    }

    @Test
    void givenOkCreatingTenant_whenCreateTenant_thenTrue() throws Exception {

        // tenant does not exist
        String tenantsResult = getInputContent(RESULT_PATH, "tenants.json");
        HttpRequest tenantsRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(ENDPOINT_TENANTS);
        mockServer.when(tenantsRequest).respond(response().withBody(tenantsResult).withStatusCode(OK.value()));

        // create tenant
        String endpoint = format("%s/%s", ENDPOINT_TENANTS, testOrganization.getName());
        HttpRequest createTenantRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(endpoint);
        mockServer.when(createTenantRequest).respond(response().withStatusCode(OK.value()));
        assertDoesNotThrow(() -> opensearchContextServiceSpy.createTenant(testOrganization.getName(), testOrganization.getDescription() == null ? "" : testOrganization.getDescription(), TOKEN));
    }

    @Test
    void givenTenantExists_whenCreatingTenant_thenFalse() throws Exception {

        // tenant exists
        testOrganization.setName("testorganization");
        String tenantsResult = getInputContent(RESULT_PATH, "tenants.json");
        HttpRequest tenantsRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(ENDPOINT_TENANTS);
        mockServer.when(tenantsRequest).respond(response().withBody(tenantsResult).withStatusCode(OK.value()));

        // create tenant
        String endpoint = format("%s/%s", ENDPOINT_TENANTS, testOrganization.getName());
        HttpRequest createTenantRequest = HttpRequest.request().withMethod(HttpMethod.PUT.name()).withPath(endpoint);
        mockServer.when(createTenantRequest).respond(response().withStatusCode(OK.value()));
        assertDoesNotThrow(() -> opensearchContextServiceSpy.createTenant(testOrganization.getName(), testOrganization.getDescription() == null ? "" : testOrganization.getDescription(), TOKEN));
    }

    @Test
    void givenAllOk_whenCreateOrganizationRoles_thenOk() throws IOException {
        mockOrganizationRoleEndpoints(testOrganization, HttpMethod.PUT, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.createOrganizationRoles(testOrganization, TOKEN));
    }

    @Test
    void givenAllOk_whenCreateSpaceRoles_thenOk() {
        mockSpaceRoleEndpoints(testSpace, HttpMethod.PUT, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.createSpaceRoles(testSpace, TOKEN));
    }

    @Test
    void givenOpensearchError_whenCreateOrganizationRoles_thenError() throws IOException {
        mockOrganizationRoleEndpoints(testOrganization, HttpMethod.PUT, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.createOrganizationRoles(testOrganization, TOKEN));
    }

    @Test
    void givenOpensearchError_whenCreateSpaceRoles_thenError() {
        mockSpaceRoleEndpoints(testSpace, HttpMethod.PUT, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.createSpaceRoles(testSpace, TOKEN));
    }

    @Test
    void givenAllOk_whenCreateOrgRoleMappings_thenOk() {
        mockOrganizationRolesmappingEndpoints(testOrganization, HttpMethod.PUT, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.createOrganizationRolesMappings(testOrganization, TOKEN));
    }

    @Test
    void givenAllOk_whenCreateSpaceRoleMappings_thenOk() {
        mockSpaceRolesmappingEndpoints(testSpace, HttpMethod.PUT, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.createSpaceRolesMappings(testSpace, TOKEN));
    }

    @Test
    void givenOpensearchError_whenCreateOrgRoleMappings_thenError() {
        mockOrganizationRolesmappingEndpoints(testOrganization, HttpMethod.PUT, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.createOrganizationRolesMappings(testOrganization, TOKEN));
    }

    @Test
    void givenOpensearchError_whenCreateSpaceRolesMappings_thenError() {
        mockSpaceRolesmappingEndpoints(testSpace, HttpMethod.PUT, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.createSpaceRolesMappings(testSpace, TOKEN));
    }

    @Test
    void givenAllOk_whenDeleteOrganizationRoleMappings_thenOk() {
        mockOrganizationRolesmappingEndpoints(testOrganization, HttpMethod.DELETE, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteOrganizationRolesMappings(testOrganization.getName(), TOKEN));
    }

    @Test
    void givenAllOk_whenDeleteSpaceRoleMappings_thenOk() {
        mockSpaceRolesmappingEndpoints(testSpace, HttpMethod.DELETE, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteSpaceRolesMappings(testSpace.getOrganization().getName(), testSpace.getName(), TOKEN));
    }

    @Test
    void givenOpensearchError_whenDeleteOrganizationRoleMappings_thenError() {
        mockOrganizationRolesmappingEndpoints(testOrganization, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.deleteOrganizationRolesMappings(testOrganization.getName(), TOKEN));
    }

    @Test
    void givenOpensearchError_whenDeleteSpaceRoleMappings_thenError() {
        mockSpaceRolesmappingEndpoints(testSpace, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.deleteSpaceRolesMappings(testSpace.getOrganization().getName(), testSpace.getName(), TOKEN));
    }

    @Test
    void givenAllOk_whenDeleteOrganizationRoles_thenOk() {
        mockOrganizationRoleEndpoints(testOrganization, HttpMethod.DELETE, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteOrganizationRoles(testOrganization.getName(), TOKEN));
    }

    @Test
    void givenAllOk_whenDeleteSpaceRoles_thenOk() {
        mockSpaceRoleEndpoints(testSpace, HttpMethod.DELETE, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteSpaceRoles(testSpace.getOrganization().getName(), testSpace.getName(), TOKEN));
    }

    @Test
    void givenOpensearchError_whenDeleteOrganizationRoles_thenError() {
        mockOrganizationRoleEndpoints(testOrganization, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.deleteOrganizationRoles(testOrganization.getName(), TOKEN));
    }

    @Test
    void givenOpensearchError_whenDeleteSpaceRoles_thenError() {
        mockSpaceRoleEndpoints(testSpace, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.deleteSpaceRoles(testSpace.getOrganization().getName(), testSpace.getName(), TOKEN));
    }

    @Test
    void givenAllOk_whenDeleteSpaceAccessControlObjects_thenOk() {
        mockSpaceRoleEndpoints(testSpace, HttpMethod.DELETE, OK);
        mockSpaceRolesmappingEndpoints(testSpace, HttpMethod.DELETE, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteSpaceContext(TOKEN, testSpace.getOrganization().getName(), testSpace.getName()));
    }

    @Test
    void givenContextResourcesNotFound_whenDeleteSpaceAccessControlObjects_thenOk() {
        mockSpaceRoleEndpoints(testSpace, HttpMethod.DELETE, NOT_FOUND);
        mockSpaceRolesmappingEndpoints(testSpace, HttpMethod.DELETE, NOT_FOUND);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteSpaceContext(TOKEN, testSpace.getOrganization().getName(), testSpace.getName()));
    }

    @Test
    void givenOpensearchError_whenDeleteSpaceAccessControlObjects_thenError() {
        mockSpaceRoleEndpoints(testSpace, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        mockSpaceRolesmappingEndpoints(testSpace, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.deleteSpaceContext(TOKEN, testSpace.getOrganization().getName(), testSpace.getName()));
    }

    @Test
    void givenAllOk_whenDeleteOrganizationAccessControlObjects_thenOk() {
        mockOrganizationRoleEndpoints(testOrganization, HttpMethod.DELETE, OK);
        mockOrganizationRolesmappingEndpoints(testOrganization, HttpMethod.DELETE, OK);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteOrganizationContext(TOKEN, testOrganization.getName()));
    }

    @Test
    void givenContextResourcesNotFound_whenDeleteOrganizationAccessControlObjects_thenOk() {
        mockOrganizationRoleEndpoints(testOrganization, HttpMethod.DELETE, NOT_FOUND);
        mockOrganizationRolesmappingEndpoints(testOrganization, HttpMethod.DELETE, NOT_FOUND);
        assertDoesNotThrow(() -> opensearchContextServiceSpy.deleteOrganizationContext(TOKEN, testOrganization.getName()));
    }

    @Test
    void givenOpensearchError_whenDeleteOrganizationAccessControlObjects_thenError() {
        mockOrganizationRoleEndpoints(testOrganization, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        mockOrganizationRolesmappingEndpoints(testOrganization, HttpMethod.DELETE, INTERNAL_SERVER_ERROR);
        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.deleteOrganizationContext(TOKEN, testOrganization.getName()));
    }

    @Test
    void givenErrorOnRolesmappingDelete_whenDeleteSpaceAccessControlObjects_thenError() throws Exception {
        OrganizationContextDTO organization = OrganizationContextDTO.builder().name("test").build();
        SpaceContextDTO space = SpaceContextDTO.builder().organization(organization).name("test").build();

        // mock roles
        String rolesResult = getInputContent(RESULT_PATH, "rolesmapping.json");
        HttpRequest rolesRequest = HttpRequest.request().withMethod(HttpMethod.GET.name()).withPath(ENDPOINT_ROLESMAPPING);
        mockServer.when(rolesRequest).respond(response().withBody(rolesResult).withStatusCode(OK.value()));

        // mock delete roles calls
        for (RoleScopeSpace scope : RoleScopeSpace.values()) {
            for (String role : scope.getRoles()) {
                String roleName = format("%s_%s_%s", space.getOrganization().getName(), space.getName(), role);
                String endpoint = format("%s/%s", ENDPOINT_ROLESMAPPING, roleName);
                HttpRequest roleRequest = HttpRequest.request().withMethod(HttpMethod.DELETE.name()).withPath(endpoint);
                mockServer.when(roleRequest).respond(response().withStatusCode(INTERNAL_SERVER_ERROR.value()));
            }
        }

        assertThrows(MetadataException.class, () -> opensearchContextServiceSpy.deleteSpaceContext(TOKEN, space.getOrganization().getName(), space.getName()));
    }


}