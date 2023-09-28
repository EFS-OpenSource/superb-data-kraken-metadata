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

import com.efs.sdk.metadata.model.MassdataFile;
import com.efs.sdk.metadata.model.MetadataDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class EntityConverterTest {

    @MockBean
    private ObjectMapper mockMapper;

    private EntityConverter converter;

    @BeforeEach
    void setup() {
        this.mockMapper = Mockito.spy(new ObjectMapper());
        this.converter = new EntityConverter(mockMapper);
    }

    @Test
    void givenMetadataDTO_whenMetadataValue_thenString() {
        MetadataDTO dto = new MetadataDTO();
        dto.setSpace("my-space");
        dto.setOrganization("my-organization");
        dto.setUuid(UUID.randomUUID().toString());
        dto.setMetadata(Map.of("property1", "value1", "property2", "value2"));
        dto.setMassdataFiles(List.of(new MassdataFile("location", "name", ZonedDateTime.now(), 123456)));

        assertNotNull(converter.metadataValue(dto));
    }

    @Test
    void givenInputStream_whenMetadataValue_thenMap() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/metadataPutQuery.json")) {
            Map<String, Object> metadataValue = converter.metadataValue(inputStream);
            assertNotNull(metadataValue);
        }
    }

    @Test
    void thrownJsonProcessingException_whenMetadataValue_thenThrowIllegalArgumentException() throws Exception {
        MetadataDTO obj = new MetadataDTO();
        Map<String, Object> map = new HashMap<>();

        given(mockMapper.writeValueAsString(any(MetadataDTO.class))).willThrow(new JsonProcessingException("") {
        });
        given(mockMapper.writeValueAsString(any(Map.class))).willThrow(new JsonProcessingException("") {
        });

        assertThrows(IllegalArgumentException.class, () -> {
            converter.metadataValue(obj);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            converter.metadataValue(map);
        });
    }
}
