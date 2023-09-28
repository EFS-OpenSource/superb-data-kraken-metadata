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
package com.efs.sdk.metadata.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

public class ApplicationIndexCreateDTO {
    @Schema(description = "The name of the `Organization`")
    private String organizationName;
    @Schema(description = "The name of the `Space`")
    private String spaceName;
    @Schema(description = """
            The custom name - cannot contain one of the following characters:
            "<", ">", "\\", "/", "*", "?", "\"", "|", "_", " "
            """)
    private String customName;
    @Schema(description = "The type of the application-index, can be one of `ANALYSIS`, `CATALOGUE`")
    private ApplicationIndexType indexType;
    @Schema(description = "Initial mapping of the application-index")
    private JsonNode mappings;

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public ApplicationIndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(ApplicationIndexType indexType) {
        this.indexType = indexType;
    }

    public JsonNode getMappings() {
        return mappings;
    }

    public void setMappings(JsonNode mappings) {
        this.mappings = mappings;
    }

}
