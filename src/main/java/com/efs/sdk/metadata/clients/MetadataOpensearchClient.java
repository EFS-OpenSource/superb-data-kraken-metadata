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
package com.efs.sdk.metadata.clients;

import com.efs.sdk.metadata.commons.MetadataException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.efs.sdk.metadata.commons.MetadataException.METADATA_ERROR.*;
import static java.lang.String.format;

@Component
public class MetadataOpensearchClient {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataOpensearchClient.class);

    public int createMetadata(RestClient restClient, String index, String metadataValue, String docid) throws MetadataException {
        try {
            LOG.debug("put index to '{}'", index);
            Request metadataRequest = new Request("PUT", format("/%s/_doc/%s", index, docid));
            metadataRequest.setJsonEntity(metadataValue);

            JSONObject searchResponseJson = handleRequest(restClient, metadataRequest);
            Map<String, Object> resultMap = searchResponseJson.getJSONObject("_shards").toMap();

            LOG.debug("request performed");
            return (int) resultMap.get("successful");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(UNABLE_INDEX);
        }
    }

    public int updateMetadata(RestClient restClient, String index, String docid, String json) throws MetadataException {
        try {
            Request metadataRequest = new Request("PUT", format("/%s/_doc/%s", index, docid));
            metadataRequest.setJsonEntity(json);

            JSONObject response = handleRequest(restClient, metadataRequest);
            Map<String, Object> result = response.getJSONObject("_shards").toMap();

            return (int) result.get("successful");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(UNABLE_UPDATE);
        }
    }

    public boolean documentExists(RestClient restClient, String index, String docid) throws MetadataException {
        try {
            Request docidRequest = new Request("GET", format("/%s/_search?q=_id:%s", index, docid));
            JSONObject searchResponseJson = handleRequest(restClient, docidRequest);
            JSONArray hits = searchResponseJson.getJSONObject("hits").getJSONArray("hits");
            List<Map<String, Object>> list = hits.toList().stream().map(hit -> (Map<String, Object>) hit).toList();

            return !list.isEmpty();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(OPENSEARCH_UNABLE_QUERY_UUID);
        }
    }

    public Map<String, Object> getSourceDocument(RestClient restClient, String index, String docid) throws MetadataException {
        try {
            return handleRequest(restClient, new Request("GET", format("/%s/_source/%s", index, docid))).toMap();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new MetadataException(OPENSEARCH_UNABLE_QUERY_UUID);
        }
    }

    private JSONObject handleRequest(RestClient restClient, Request metadataRequest) throws IOException, MetadataException {
        Response response = restClient.performRequest(metadataRequest);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode > 299) {
            throw new MetadataException(UNABLE_SEND_OS_REQUEST);
        }

        String searchResponse = EntityUtils.toString(response.getEntity());
        return new JSONObject(searchResponse);
    }
}
