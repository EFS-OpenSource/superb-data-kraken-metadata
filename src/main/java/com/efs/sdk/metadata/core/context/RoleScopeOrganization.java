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
public enum RoleScopeOrganization {

    /**
     * Role for reading-purpose with backend-roles &quot;access&quot; and kibana-permission
     * &quot;read&quot;
     */
    READ(new String[]{"access"}, "kibana_all_read"),
    /**
     * Role for write-purpose with backend-role &quot;trustee&quot; or &quot;admin&quot; and
     * kibana-permission &quot;write&quot;
     */
    WRITE(new String[]{"admin", "trustee"}, "kibana_all_write");

    private final String[] roles;
    private final String permission;

    RoleScopeOrganization(String[] roles, String permission) {
        this.roles = roles;
        this.permission = permission;
    }

    public String[] getRoles() {
        return roles;
    }

    public String getPermission() {
        return permission;
    }
}
