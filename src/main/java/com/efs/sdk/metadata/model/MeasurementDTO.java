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

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;

public class MeasurementDTO extends HashMap<String, Object> {

    @Schema(description = "The id of the document")
    @JsonAlias({"accountName", "organization"})
    private static final String PROP_DOCID = "docid";
    @Schema(description = "The massdata-block")
    private static final String PROP_MASSDATA = "massdata";
    @Schema(description = "The metadata")
    private static final String PROP_METADATA = "metadata";
    @Schema(description = "The name of the `Organization`")
    private static final String PROP_ORGANIZATION = "organization";
    @Schema(description = "The name of the `Space`")
    private static final String PROP_SPACE = "space";
    @Schema(description = "The root-directory")
    private static final String PROP_ROOTDIR = "rootdir";

    public String getDocid() {
        return (String) this.get(PROP_DOCID);
    }

    public void setDocid(String uuid) {
        this.put(PROP_DOCID, uuid);
    }

    public String getOrganization() {
        return (String) this.get(PROP_ORGANIZATION);
    }

    public void setOrganization(String organization) {
        this.put(PROP_ORGANIZATION, organization);
    }

    public String getSpace() {
        return (String) this.get(PROP_SPACE);
    }

    public void setSpace(String space) {
        this.put(PROP_SPACE, space);
    }

    public String getRootdir() {
        return (String) this.get(PROP_ROOTDIR);
    }

    public void setRootdir(String rootdir) {
        this.put(PROP_ROOTDIR, rootdir);
    }

    public List<MassdataFile> getMassdata() {
        if (containsKey(PROP_MASSDATA)) {
            return (List<MassdataFile>) this.get(PROP_MASSDATA);
        }
        return Collections.emptyList();
    }

    public void setMassdata(List<MassdataFile> massdata) {
        this.put(PROP_MASSDATA, massdata);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.put(PROP_METADATA, metadata);
    }

    public Map<String, Object> getMetadata() {
        Object metadata = this.get(PROP_METADATA);

        if (metadata instanceof Map) {
            return (Map<String, Object>) metadata;
        }
        return Map.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeasurementDTO that = (MeasurementDTO) o;
        return Objects.equals(getDocid(), that.getDocid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDocid());
    }
}