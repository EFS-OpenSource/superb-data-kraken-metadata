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
package com.efs.sdk.metadata.commons;

import org.springframework.http.HttpStatus;

public class MetadataException extends Exception {

    private final HttpStatus httpStatus;

    public MetadataException(METADATA_ERROR error) {
        super(error.code + ": " + error.msg);
        httpStatus = error.status;
    }

    public MetadataException(METADATA_ERROR error, String additionalMessage) {
        super(error.code + ": " + error.msg + " " + additionalMessage);
        httpStatus = error.status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }


    /**
     * Provides the errors to the application.
     *
     * @author e:fs TechHub GmbH
     */
    public enum METADATA_ERROR {
        // @formatter:off
        UNABLE_CREATE_INDEX(10001, HttpStatus.BAD_REQUEST, "unable to create index"),
        UNABLE_CREATE_ALIAS(10002, HttpStatus.BAD_REQUEST, "unable to create alias"),
        NO_META_JSON(10004, HttpStatus.NOT_FOUND, "No file 'meta.json' found within collection"),
        UNABLE_INDEX(10005, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to index meta.json"),
        UNABLE_DOWNLOAD_META(10006, HttpStatus.INSUFFICIENT_STORAGE, "Unable to provide meta.json from storage"),
        UNABLE_SEND_OS_REQUEST(10007, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send request to opensearch"),
        UNABLE_GET_ACCESS_TOKEN(10008, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to get token from keycloak"),
        OPENSEARCH_UNABLE_QUERY_UUID(10010, HttpStatus.INSUFFICIENT_STORAGE, "Unable to query for existing uuid"),
        NO_SPACE(10011, HttpStatus.CONFLICT, "Space missing"),
        NO_ORGANIZATION(10012, HttpStatus.CONFLICT, "Organization missing"),
        NOT_AUTHORIZED(10013, HttpStatus.UNAUTHORIZED, "no authorization-token provided"),
        PROCESSING_EXCEPTION(10015, HttpStatus.BAD_REQUEST, "Meta json does not match meta json schema."),
        NO_ROOT_DIR(10016, HttpStatus.CONFLICT, "Root-directory missing"),
        UNEXPECTED_ORGA_FORMAT(10017, HttpStatus.CONFLICT, "organization is in unexpected format!"),
        UNEXPECTED_SPACE_FORMAT(10018, HttpStatus.CONFLICT, "space is in unexpected format!"),
        INSUFFICIENT_RIGHTS(10019, HttpStatus.FORBIDDEN, "you do not have permission for this action!"),
        CONFLICTING_AUTH_CONFIGURATION(10020, HttpStatus.CONFLICT, "conflicting auth-configuration found!"),
        UNABLE_UPDATE(10022, HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update document"),
        INDEX_ALREADY_EXISTS(10030, HttpStatus.CONFLICT, "index already exists"),
        INDEX_NAME_INVALID(10031, HttpStatus.BAD_REQUEST, "index name invalid"),
        OPENSEARCH_BAD_REQUEST(10032, HttpStatus.BAD_REQUEST, "problems with open search request"),
        OPENSEARCH_ERROR(10050, HttpStatus.INTERNAL_SERVER_ERROR, "problems with open search service"),
        UNKNOWN_ERROR(10100, HttpStatus.INTERNAL_SERVER_ERROR, "something unexpected happened"),

        // unable creating resources
        UNABLE_CREATE_ESROLE(10003, HttpStatus.BAD_REQUEST, "unable to create role"), UNABLE_CREATE_ROLESMAPPING(10004, HttpStatus.INTERNAL_SERVER_ERROR, "unable to create rolesmapping"), UNABLE_CREATE_TENANT(10005, HttpStatus.INTERNAL_SERVER_ERROR, "unable to create tenant"), // unable deleting resources
        UNABLE_DELETE_INDEX(10011, HttpStatus.INTERNAL_SERVER_ERROR, "unable to delete index"), UNABLE_DELETE_ROLE(10012, HttpStatus.INTERNAL_SERVER_ERROR, "unable to delete role"), UNABLE_DELETE_ROLESMAPPING(10013, HttpStatus.INTERNAL_SERVER_ERROR, "unable to delete rolesmapping"), UNABLE_DELETE_TENANT(10014, HttpStatus.INTERNAL_SERVER_ERROR, "unable to delete tenant"), // unable getting resources
        UNABLE_GET_TENANTS(10024, HttpStatus.BAD_REQUEST, "unable to get tenants"), UNABLE_GET_ORGANIZATION(20023, HttpStatus.BAD_REQUEST, "unable to retrieve organization"), UNABLE_GET_ORGANIZATIONS(20025, HttpStatus.BAD_REQUEST, "unable to retrieve organizations"), UNABLE_GET_SPACES(20026, HttpStatus.BAD_REQUEST, "unable to retrieve spaces"), VALIDATION_ERROR(30000, HttpStatus.BAD_REQUEST, "Validation error"),


        OPENSEARCH_CONNECTION_ERROR(50000, HttpStatus.BAD_GATEWAY, "opensearch connection error");
        // @formatter:on

        private final int code;
        private final HttpStatus status;
        private final String msg;

        METADATA_ERROR(int code, HttpStatus status, String msg) {
            this.code = code;
            this.status = status;
            this.msg = msg;
        }

    }
}
