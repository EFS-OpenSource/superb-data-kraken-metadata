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
package com.efs.sdk.metadata.helper;

import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.metadata.core.context.RoleScopeSpace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.List;

import static com.efs.sdk.metadata.utils.TestHelper.RESULT_PATH;
import static com.efs.sdk.metadata.utils.TestHelper.getInputContent;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OpensearchHelperTest {
    private OpensearchHelper opensearchHelper;
    static final String KIBANA_ALL_READ = "kibana_all_read";

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        this.opensearchHelper = new OpensearchHelper(objectMapper);
    }

    @Test
    void givenMappingsPublic_whenGetRolesMappingsJsonPublic_thenOk() throws Exception {
        List<String> mappings = new ArrayList<>();
        mappings.add("spc_all_public");
        mappings.add("org_all_public");
        String rolesMappingPublic = getInputContent(RESULT_PATH, "rolesmapping_public.json");
        JSONAssert.assertEquals(opensearchHelper.getRolesMappingJson(mappings), rolesMappingPublic, false);
    }

    @Test
    void givenReadRole_whenCreateRoleDefinition_thenOk() throws Exception {
        OrganizationContextDTO organization = OrganizationContextDTO.builder().name("organization").build();
        SpaceContextDTO space = SpaceContextDTO.builder().organization(organization).name("space").build();
        String roleRead = getInputContent(RESULT_PATH, "role_read.json");
        String actual = opensearchHelper.createRoleDefinition(space, RoleScopeSpace.READ);
        JSONAssert.assertEquals(actual, roleRead, false);
    }

    @Test
    void givenPublicRole_whenCreatePublicRoleDefinition_thenOk() throws Exception {
        OrganizationContextDTO organization = OrganizationContextDTO.builder().name("organization").build();
        SpaceContextDTO space = SpaceContextDTO.builder().organization(organization).name("space").build();
        String roleRead = getInputContent(RESULT_PATH, "role_read.json");
        String actual = opensearchHelper.createPublicRoleDefinition(space);
        System.out.println(actual);
        JSONAssert.assertEquals(actual, roleRead, false);
    }

    @Test
    void givenRoleReadRequest_whenGetTenantRoleRequest_thenOk() throws Exception {
        String roleRead = getInputContent(RESULT_PATH, "role_tenant_read.json");
        JSONAssert.assertEquals(opensearchHelper.getTenantRoleRequest("read", "organization"), roleRead, false);
    }


    @Test
    void givenRoleNameCorrect_whenGetRoleName_thenOk() {
        OrganizationContextDTO organization = OrganizationContextDTO.builder().name("org").build();
        SpaceContextDTO space = SpaceContextDTO.builder().organization(organization).name("spc").build();
        assertEquals("org_spc_role", opensearchHelper.getRoleName("role", space));
    }


    @Test
    void givenOrgRoleNameCorrect_whenGetOrgRoleName_thenOk() {
        assertEquals("org_role", opensearchHelper.getOrganizationRoleName("org", "role"));
    }
}
