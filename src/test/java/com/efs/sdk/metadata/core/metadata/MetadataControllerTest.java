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
package com.efs.sdk.metadata.core.metadata;

import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.SchemaService;
import com.efs.sdk.metadata.model.MeasurementDTO;
import com.efs.sdk.metadata.security.oauth.OAuthConfigurationHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.PROCESSING_EXCEPTION;
import static com.efs.sdk.metadata.core.metadata.MetadataController.ENDPOINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetadataController.class)
@ActiveProfiles("test")
class MetadataControllerTest {

    private static final String VALIDATE_JSON_ENDPOINT = ENDPOINT + "/validateJson";
    private static final String INDEX_JSON_ENDPOINT = ENDPOINT + "/index";
    @Autowired
    private MockMvc mvc;

    /* required for security tests to run. Do not remove! */
    @MockBean
    private JwtDecoder decoder;

    @MockBean
    private SchemaService schemaService;

    @MockBean
    private MetadataService metadataService;

    @MockBean
    private OAuthConfigurationHelper authConfigHelper;

    @Test
    void givenAuthentication_whenPutIndex_thenOk() throws Exception {
        given(metadataService.update(any(), any(), anyString(), anyString(), anyString())).willReturn(true);

        mvc.perform(put(INDEX_JSON_ENDPOINT).with(jwt()).param("organization", "sdkcorestorage").param("space", "test-container").param("docid", "qweqweqwe").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    @Test
    void givenAuthentication_whenPutIndex_thenResponseError() throws Exception {
        given(metadataService.update(any(MeasurementDTO.class), anyString(), anyString(), anyString(), anyString())).willThrow(new IOException("test"));

        mvc.perform(put(INDEX_JSON_ENDPOINT).with(jwt()).param("organization", "sdkcorestorage").param("space", "test-container").param("docid", "qweqweqwe").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().is5xxServerError());
    }

    @Test
    void givenNoAuthentication_whenPutIndex_thenError() throws Exception {
        mvc.perform(put(INDEX_JSON_ENDPOINT).param("organization", "sdkcorestorage").param("space", "test-container").param("docid", "qweqweqwe").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void givenNoAuthentication_whenValidateJson_thenError() throws Exception {
        mvc.perform(post(VALIDATE_JSON_ENDPOINT, "")).andExpect(status().isForbidden());
    }

    @Test
    void givenValidateFalse_whenValidateJson_thenError() throws Exception {
        given(schemaService.validateJsonNode(any())).willThrow(new MetadataException(PROCESSING_EXCEPTION, ""));
        mvc.perform(post(VALIDATE_JSON_ENDPOINT).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenValidateTrue_whenValidateJson_thenOk() throws Exception {
        given(schemaService.validateJsonNode(any())).willReturn(true);
        mvc.perform(post(VALIDATE_JSON_ENDPOINT).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }
}
