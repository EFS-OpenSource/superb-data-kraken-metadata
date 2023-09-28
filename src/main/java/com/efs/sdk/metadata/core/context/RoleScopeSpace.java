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

/**
 * Scopes of roles in elasticsearch
 */
public enum RoleScopeSpace {

    /**
     * Role for reading-purpose with backend-roles &quot;user&quot; and &quot;supplier&quot; and default-action-group
     * &quot;read&quot;
     */
    READ(new String[]{"user", "supplier"}, new String[]{"read", "indices:data/read/scroll", "indices:admin/mappings/get"}, "read", "kibana_all_read"),
    /**
     * Role for crud-purpose (read, create, update and delete) with backend-role &quot;trustee&quot; and
     * default-action-group &quot;crud&quot;
     */
    CRUD(new String[]{"trustee"}, new String[]{"crud", "indices:data/read/scroll", "indices:admin/mappings/get", "indices:admin/mappings/put"}, "crud", "kibana_all_write");

    private final String[] roles;
    private final String[] indexPermissions;
    private final String modelindexPermission;
    private final String tenantPermission;

    RoleScopeSpace(String[] roles, String[] indexPermissions, String modelindexPermission, String tenantPermission) {
        this.roles = roles;
        this.indexPermissions = indexPermissions;
        this.modelindexPermission = modelindexPermission;
        this.tenantPermission = tenantPermission;
    }

    public String[] getRoles() {
        return roles;
    }

    public String[] getIndexPermissions() {
        return indexPermissions;
    }

    public String getModelindexPermission() {
        return this.modelindexPermission;
    }

    public String getTenantPermission() {
        return tenantPermission;
    }
}
