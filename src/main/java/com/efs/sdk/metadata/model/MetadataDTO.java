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

import java.util.*;

public class MetadataDTO extends HashMap<String, Object> {

    private static final String DEFAULT_NONE = "none";

    private static final String PROP_UUID = "uuid";
    private static final String PROP_MASSDATA = "massdata";
    private static final String PROP_METADATA = "metadata";
    private static final String PROP_ORGANIZATION = "organization";
    private static final String PROP_SPACE = "space";

    public static MetadataDTO fromDocument(Map<String, Object> document) {
        MetadataDTO result = new MetadataDTO();

        result.setUuid((String) document.getOrDefault(PROP_UUID, DEFAULT_NONE));
        result.setOrganization((String) document.getOrDefault(PROP_ORGANIZATION, DEFAULT_NONE));
        result.setSpace((String) document.getOrDefault(PROP_SPACE, DEFAULT_NONE));
        result.setMetadata((Map<String, Object>) document.getOrDefault(PROP_METADATA, Map.of()));
        result.setMassdata((List<MassdataFile>) document.getOrDefault(PROP_MASSDATA, List.of()));

        return result;
    }

    public String getUuid() {
        return (String) this.getOrDefault(PROP_UUID, DEFAULT_NONE);
    }

    public void setUuid(String uuid) {
        this.put(PROP_UUID, uuid);
    }

    public String getOrganization() {
        return (String) this.getOrDefault(PROP_ORGANIZATION, DEFAULT_NONE);
    }

    public void setOrganization(String organization) {
        this.put(PROP_ORGANIZATION, organization);
    }

    public String getSpace() {
        return (String) this.getOrDefault(PROP_SPACE, DEFAULT_NONE);
    }

    public void setSpace(String space) {
        this.put(PROP_SPACE, space);
    }

    public List<MassdataFile> getMassdata() {
        return (List<MassdataFile>) this.getOrDefault(PROP_MASSDATA, Collections.emptyList());
    }

    public void setMassdata(List<MassdataFile> massdata) {
        this.put(PROP_MASSDATA, massdata);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.put(PROP_METADATA, metadata);
    }

    public Map<String, Object> getMetadata() {
        Object metadata = this.getOrDefault(PROP_METADATA, DEFAULT_NONE);

        if (metadata instanceof Map) {
            return (Map<String, Object>) metadata;
        }
        return Map.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetadataDTO that = (MetadataDTO) o;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}
