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
package com.efs.sdk.metadata.core;

import com.efs.sdk.metadata.commons.MetadataException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.util.List;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.*;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = org.opensearch.client.ResponseException.class)
    protected ResponseEntity<Object> handleResponseException(org.opensearch.client.ResponseException e, WebRequest request) {
        if (e.getResponse().getStatusLine().getStatusCode() >= 500) {
            LOG.error(e.getMessage(), e);
            return handleMetadataException(new MetadataException(OPENSEARCH_ERROR), request);
        }
        if (e.getResponse().getStatusLine().getStatusCode() >= 400 && e.getResponse().getStatusLine().getStatusCode() < 500) {
            LOG.error(e.getMessage(), e);
            return handleMetadataException(new MetadataException(OPENSEARCH_BAD_REQUEST, e.getMessage()), request);
        }
        LOG.error(e.getMessage(), e);
        return handleMetadataException(new MetadataException(UNKNOWN_ERROR), request);
    }

    @ExceptionHandler(value = MetadataException.class)
    private ResponseEntity<Object> handleMetadataException(MetadataException e, WebRequest request) {
        LOG.error(e.getMessage(), e);
        return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), e.getHttpStatus(), request);
    }

    @ExceptionHandler(value = IOException.class)
    protected ResponseEntity<Object> handleIOException(java.io.IOException e, WebRequest request) {
        LOG.error(e.getMessage(), e);
        return handleMetadataException(new MetadataException(UNKNOWN_ERROR), request);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(RuntimeException e, WebRequest request) {
        LOG.error(e.getMessage(), e);
        return handleMetadataException(new MetadataException(INSUFFICIENT_RIGHTS), request);
    }

    // handle validation errors
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NotNull MethodArgumentNotValidException ex, @NotNull HttpHeaders headers,
            @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        LOG.error(ex.getMessage(), ex);
        List<String> errors = ex.getBindingResult().getAllErrors().stream().map(error -> {
            if (error instanceof FieldError fieldError) {
                return fieldError.getField() + ": " + error.getDefaultMessage();
            } else {
                return error.getObjectName() + ": " + error.getDefaultMessage();
            }
        }).toList();
        String customErrorMsg = "\n" + String.join("\n", errors);
        return handleMetadataException(new MetadataException(VALIDATION_ERROR, customErrorMsg), request);
    }

    @ExceptionHandler(value = Exception.class)
    protected ResponseEntity<Object> handleException(RuntimeException e, WebRequest request) {
        LOG.error(e.getMessage(), e);
        return handleMetadataException(new MetadataException(UNKNOWN_ERROR), request);
    }

}
