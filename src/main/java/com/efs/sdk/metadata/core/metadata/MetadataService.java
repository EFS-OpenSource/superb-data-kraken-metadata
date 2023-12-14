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

import com.efs.sdk.metadata.clients.MetadataOpensearchClient;
import com.efs.sdk.metadata.clients.MetadataRestClient;
import com.efs.sdk.metadata.clients.OpenSearchRestClientBuilder;
import com.efs.sdk.metadata.clients.OrganizationManagerClient;
import com.efs.sdk.metadata.commons.MetadataException;
import com.efs.sdk.metadata.core.events.EventPublisher;
import com.efs.sdk.metadata.helper.EntityConverter;
import com.efs.sdk.metadata.model.EventPublisherModelDTO;
import com.efs.sdk.metadata.model.MassdataFile;
import com.efs.sdk.metadata.model.MeasurementDTO;
import com.efs.sdk.metadata.model.MetadataDTO;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import org.opensearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.efs.sdk.metadata.clients.OrganizationManagerClient.Permissions.WRITE;
import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.*;
import static java.lang.String.format;

@Service
public class MetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataService.class);


    /**
     * Name of the Kafka-Topic
     */
    @Value("${metadata.topics.indexing-done-topic}")
    private String indexingDoneTopic;

    @Value("${metadata.topics.metadata-update-topic}")
    private String metadataUpdateTopic;

    private final EventPublisher publisher;
    private final EntityConverter converter;
    private final OpenSearchRestClientBuilder esBuilder;
    private final MetadataRestClient client;
    private final MetadataOpensearchClient mOSClient;
    private final OrganizationManagerClient orgaClient;

    public MetadataService(EventPublisher publisher, EntityConverter converter, OpenSearchRestClientBuilder esBuilder, MetadataRestClient client,
            MetadataOpensearchClient mOSClient, OrganizationManagerClient orgaClient) {
        this.publisher = publisher;
        this.converter = converter;
        this.esBuilder = esBuilder;
        this.client = client;
        this.mOSClient = mOSClient;
        this.orgaClient = orgaClient;
    }

    public boolean index(String accessToken, MeasurementDTO measurement) throws MetadataException {

        if (!canWrite(accessToken, measurement.getOrganization(), measurement.getSpace())) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        // validate that required attributes are present
        validate(measurement);

        EventPublisherModelDTO eventPublisherModelDTO = getEventPublisherModelDTO(measurement);
        // create Service Account Token, as supplier does not have write-permission in elasticsearch otherwise
        String saToken = client.getSAToken().getAccessToken();
        return index(measurement, saToken, eventPublisherModelDTO);
    }

    private boolean canWrite(String accessToken, String organization, String space) throws MetadataException {
        Map<String, Object> spaceObj = orgaClient.getSpace(accessToken, organization, space, WRITE);
        return spaceObj != null;
    }

    private void validate(MeasurementDTO measurement) throws MetadataException {
        // validate that required attributes are present
        if (measurement.getOrganization() == null) {
            throw new MetadataException(NO_ORGANIZATION);
        }

        if (measurement.getSpace() == null) {
            throw new MetadataException(NO_SPACE);
        }

        if (measurement.getRootdir() == null) {
            throw new MetadataException(NO_ROOT_DIR);
        }
    }

    private EventPublisherModelDTO getEventPublisherModelDTO(MeasurementDTO measurement) {
        EventPublisherModelDTO eventPublisherModelDTO = new EventPublisherModelDTO();
        eventPublisherModelDTO.setAccountName(measurement.getOrganization());
        eventPublisherModelDTO.setContainerName(measurement.getSpace());
        eventPublisherModelDTO.setRootDir(measurement.getRootdir());
        eventPublisherModelDTO.setUuid(measurement.getDocid());
        return eventPublisherModelDTO;
    }

    /**
     * Updating documents in OpenSearch.
     * <br>
     * <b>CAUTION</b> currently only appending
     *
     * @param input        The input-document
     * @param accessToken  The access-token
     * @param organization The organization
     * @param space        The space
     * @param documentId   The document-id
     * @return whether update was successful
     * @throws IOException       thrown on io-errors
     * @throws MetadataException thrown on errors
     */
    public boolean update(MeasurementDTO input, String accessToken, String organization, String space, String documentId) throws MetadataException,
            IOException {
        // may be canDelete if "update" is defined as "real update" - but now only appending properties is supported!
        if (!canWrite(accessToken, organization, space)) {
            throw new MetadataException(INSUFFICIENT_RIGHTS);
        }
        RestClient restClient = esBuilder.buildRestClient(accessToken);
        String index = getIndex(organization, space);

        Map<String, Object> document = mOSClient.getSourceDocument(restClient, index, documentId);
        MetadataDTO source = MetadataDTO.fromDocument(document);

        if (source.getMetadata() == null) {
            source.setMetadata(Collections.emptyMap());
        }
        if (source.getMassdata() == null) {
            source.setMassdata(Collections.emptyList());
        }

        Map<String, Object> inputMetadataFlatten = JsonFlattener.flattenAsMap(converter.metadataValue(input.getMetadata()));
        Map<String, Object> sourceMetadataFlatten = JsonFlattener.flattenAsMap(converter.metadataValue(source.getMetadata()));

        Map<String, Object> mergedFlatten =
                Stream.of(sourceMetadataFlatten, inputMetadataFlatten).flatMap(map -> map.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (v1, v2) -> v1));

        Map<String, Object> mergedMetadata = converter.metadataValue(JsonUnflattener.unflatten(converter.metadataValue(mergedFlatten)));

        Set<MassdataFile> massdataFiles = new HashSet<>();
        massdataFiles.addAll(source.getMassdata());
        massdataFiles.addAll(input.getMassdata());

        MetadataDTO result = new MetadataDTO();
        result.setUuid(source.getUuid());
        result.setSpace(source.getSpace());
        result.setOrganization(source.getOrganization());
        result.setMetadata(mergedMetadata);
        result.setMassdata(List.copyOf(massdataFiles));

        EventPublisherModelDTO eventPublisherModelDTO = getEventPublisherModelDTO(input);

        LOG.debug("updating");
        int updated = mOSClient.updateMetadata(restClient, index, documentId, converter.metadataValue(result));
        LOG.debug("updating done");
        LOG.debug("publishing event");
        publisher.sendMessage(metadataUpdateTopic, converter.eventPublisherModelAsMessage(eventPublisherModelDTO));
        LOG.debug("publishing event done");
        return updated > 0;
    }

    private boolean index(MeasurementDTO indexDTO, String accessToken, EventPublisherModelDTO eventPublisherModelDTO) throws MetadataException {
        LOG.debug("starting indexing");

        RestClient restClient = esBuilder.buildRestClient(accessToken);
        String index = getIndex(indexDTO);

        eventPublisherModelDTO.setUuid(indexDTO.getDocid());
        MetadataDTO metadata = getMetadataDTO(indexDTO);

        String metadataValue = converter.metadataValue(metadata);
        LOG.debug("indexing");

        int indexed = mOSClient.createMetadata(restClient, index, metadataValue, indexDTO.getDocid());
        LOG.debug("indexing done");
        LOG.debug("publishing event");
        publisher.sendMessage(indexingDoneTopic, converter.eventPublisherModelAsMessage(eventPublisherModelDTO));
        LOG.debug("publishing event done");
        return indexed > 0;
    }

    private MetadataDTO getMetadataDTO(MeasurementDTO measurement) {
        MetadataDTO metadata = new MetadataDTO();
        metadata.setUuid(measurement.getDocid());
        metadata.setMetadata(measurement.getMetadata());
        metadata.setSpace(measurement.getSpace());
        metadata.setOrganization(measurement.getOrganization());
        metadata.setMassdata(measurement.getMassdata());
        return metadata;
    }

    private String getIndex(MeasurementDTO measurement) {
        return getIndex(measurement.getOrganization(), measurement.getSpace());
    }

    private String getIndex(String organization, String space) {
        return format("%s_%s_measurements", organization, space);
    }
}
