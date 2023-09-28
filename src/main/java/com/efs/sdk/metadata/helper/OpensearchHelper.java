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

import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.metadata.core.context.RoleScopeSpace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Component
public class OpensearchHelper {

    private static final String ROLE_TEMPLATE_PATH = "/role_space_tpl.json";
    private final ObjectMapper objectMapper;

    public OpensearchHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the name of the opensearch-role (&quot;&lt;org.name&gt;_&lt;spc.name&gt;_&lt;role&gt;&quot;)
     *
     * @param role  the role-identifier
     * @param space The Space
     * @return the name of the role
     */
    public String getRoleName(String role, SpaceContextDTO space) {
        return format("%s_%s_%s", space.getOrganization().getName(), space.getName(), role);
    }

    public String getSpaceRoleName(String organizationName, String spaceName, String role) {
        return format("%s_%s_%s", organizationName, spaceName, role);
    }


    /**
     * Builds the name of the tenant-role (&quot;&lt;org.name&gt;__&lt;role&gt;&quot;)
     *
     * @param role             the role-identifier
     * @param organizationName The Name of the organization
     * @return the name of the role
     */
    public String getOrganizationRoleName(String organizationName, String role) {
        return format("%s_%s", organizationName, role);
    }

    /**
     * Creates a rolesmapping json
     * <code>and_backend_roles</code> [ &quot;org_&lt;org.name&gt;_access&quot;, &quot;&lt;organization.name&gt;_&lt;spc.name&gt;_&lt;
     * role&gt;&quot;]
     *
     * @param mappings The role mappings
     * @return the rolesmapping json
     */
    public String getRolesMappingJson(List<String> mappings) {

        List<String> mappingsQuoted = mappings.stream().map(n -> format("\"%s\"", n)).toList();

        String rolesMappingTemplate = getResourceAsString("/rolemapping_tpl.json");

        return format(rolesMappingTemplate, mappingsQuoted);
    }

    /**
     * Gets a resource as String (from a file)
     *
     * @param fileName The name of the file
     * @return the content of the file as String
     */
    public String getResourceAsString(String fileName) {
        return IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream(fileName)), StandardCharsets.UTF_8);
    }

    /**
     * Gets an tenant role request with name &quot;&lt;org.name&gt;_&lt;role&gt;&quot; with permissions <code>scope.getPermission()</code> on tenant
     * with name &quot;&lt;org.name&gt&quot;
     *
     * @param permission The Permission
     * @param tenantName The name of the tenant
     * @return the tenant role request json
     */
    public String getTenantRoleRequest(String permission, String tenantName) {

        String jsonTemplate = getResourceAsString("/role_organization_tpl.json");

        return format(jsonTemplate, tenantName, permission);

    }


    /**
     * Creates a role definition for a specific space and scope.
     *
     * @param space SpaceContextDTO object representing the space context
     * @param scope RoleScopeSpace object representing the role scope
     * @return String containing the role definition
     */
    public String createRoleDefinition(SpaceContextDTO space, RoleScopeSpace scope) {
        String tenantPattern = format("%s_%s", space.getOrganization().getName(), space.getName());
        String tenantPermission = scope.getTenantPermission();
        String indexPattern = format("%s_%s_*", space.getOrganization().getName(), space.getName());
        String[] indexPermissions = scope.getIndexPermissions();
        String modelindexPermission = scope.getModelindexPermission();

        return replaceTemplateParameters(tenantPattern, tenantPermission, indexPattern, indexPermissions, modelindexPermission);
    }

    /**
     * Creates a public role definition for a specific space. This method sets predefined permissions for the role.
     *
     * @param space SpaceContextDTO object representing the space context
     * @return String containing the public role definition
     */
    public String createPublicRoleDefinition(SpaceContextDTO space) {
        String tenantPattern = format("%s_%s", space.getOrganization().getName(), space.getName());
        String tenantPermission = "kibana_all_read";
        String indexPattern = format("%s_%s_*", space.getOrganization().getName(), space.getName());
        String[] indexPermissions = new String[]{"read", "indices:data/read/scroll", "indices:admin/mappings/get"};
        String modelindexPermission = "read";

        return replaceTemplateParameters(tenantPattern, tenantPermission, indexPattern, indexPermissions, modelindexPermission);
    }

    /**
     * Replaces placeholders in the role template with provided parameters and returns the processed template.
     *
     * @param tenantPattern        a String representing the tenant pattern
     * @param tenantPermission     a String representing the tenant permission
     * @param indexPattern         a String representing the index pattern
     * @param indexPermissions     an array of Strings representing index permissions
     * @param modelindexPermission a String representing the model index permission
     * @return a String containing the processed role definition template
     */
    private String replaceTemplateParameters(String tenantPattern, String tenantPermission, String indexPattern, String[] indexPermissions, String modelindexPermission) {
        String roleTemplate = getResourceAsString(ROLE_TEMPLATE_PATH);
        roleTemplate = roleTemplate.replace("<tenant_pattern>", tenantPattern);
        roleTemplate = roleTemplate.replace("<tenant_allowed_action>", tenantPermission);
        roleTemplate = roleTemplate.replace("<index_pattern>", indexPattern);
        String permissionsJson;
        try {
            permissionsJson = objectMapper.writeValueAsString(indexPermissions);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error while converting index permissions to JSON", e);
        }
        roleTemplate = roleTemplate.replace("<index_allowed_actions>", permissionsJson);
        roleTemplate = roleTemplate.replace("<modelindex_allowed_action>", modelindexPermission);

        return roleTemplate;
    }

}
