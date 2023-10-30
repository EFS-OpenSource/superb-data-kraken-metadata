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

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;
import java.util.Objects;

public class MassdataFile {

    @Schema(description = "The location of the massdata file")
    private String location;
    @Schema(description = "The name of the massdata file")
    private String name;
    @Schema(description = "The creation-date of the massdata file")
    private ZonedDateTime dateCreated;
    @Schema(description = "The size of the massdata file")
    private long size;

    public MassdataFile(String location, String name, ZonedDateTime dateCreated, long size) {
        this.location = location;
        this.name = name;
        this.dateCreated = dateCreated;
        this.size = size;
    }

    public MassdataFile() {
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(ZonedDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MassdataFile that = (MassdataFile) o;
        return Objects.equals(location, that.location) && Objects.equals(name, that.name) && Objects.equals(dateCreated, that.dateCreated) && Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name, dateCreated, size);
    }
}
