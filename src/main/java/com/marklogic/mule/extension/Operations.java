/*
 * Copyright (c) 2023 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.mule.extension;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.*;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.marker.JSONReadHandle;
import com.marklogic.client.io.marker.JSONWriteHandle;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.mule.extension.api.DocumentAttributes;
import com.marklogic.mule.extension.api.QueryFormat;
import com.marklogic.mule.extension.api.QueryType;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class Operations {

    @MediaType(value = ANY, strict = false)
    @DisplayName("Read document")
    public Result<InputStream, DocumentAttributes> readDocument(
        @Connection DatabaseClient databaseClient,
        @DisplayName("Document URI") @Example("/data/customer.json") String uri,
        @DisplayName("Metadata Category List") @Optional(defaultValue = "ALL") @Example("COLLECTIONS,PERMISSIONS") String categories
    ) {
        DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
        GenericDocumentManager documentManager = databaseClient.newDocumentManager();

        if (Utilities.hasText(categories)) {
            documentManager.setMetadataCategories(buildMetadataCategories(categories));
        }

        InputStreamHandle handle = documentManager.read(uri, metadataHandle, new InputStreamHandle());
        return Result.<InputStream, DocumentAttributes>builder()
            .output(handle.get())
            .attributes(new DocumentAttributes(uri, metadataHandle))
            .mediaType(makeMediaType(handle.getFormat()))
            .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
            .build();
    }

    /**
     * Write document(s) with/without metadata.
     *
     * @param databaseClient
     * @param myContent
     * @param format
     * @param permissions
     * @param quality
     * @param collections
     * @param uriPrefix
     * @param uriSuffix
     * @param generateUUID
     * @param temporalCollection
     * @DisplayName ("REST Transform") @Optional String restTransform,
     * @DisplayName ("REST Transform Parameters") @Optional String restTransformParameters,
     * @DisplayName ("REST Transform Parameters Delimiter") @Optional @Example(",") String restTransformParametersDelimiter
     */
    public void writeDocuments(
        @Connection DatabaseClient databaseClient, @Content InputStream myContent,
        @Optional Format format,
        @Optional @Example("Role,permission") String permissions,
        @Optional(defaultValue = "0") int quality,
        @Optional @Example("Comma separated collection strings") String collections,
        @Optional @Example("/test/") String uriPrefix,
        @Optional @Example(".json") String uriSuffix,
        @Optional(defaultValue = "True") boolean generateUUID,
        @Optional @Example("temporal-collection string") String temporalCollection,
        @DisplayName("REST Transform") @Optional String restTransform,
        @DisplayName("REST Transform Parameters") @Optional String restTransformParameters,
        @DisplayName("REST Transform Parameters Delimiter") @Optional @Example(",") String restTransformParametersDelimiter) {

        new WriteOperations().writeDocuments(databaseClient, myContent, format, permissions, quality, collections,
            uriPrefix, uriSuffix, generateUUID, temporalCollection, restTransform, restTransformParameters, restTransformParametersDelimiter);
    }

    /**
     * Search for documents, returning the documents but not yet any metadata for them.
     * Will eventually support many parameters here for searching.
     */
    @MediaType(value = ANY, strict = false)
    @DisplayName("Search Documents")
    public List<Result<InputStream, DocumentAttributes>> oldSearchDocuments(
        @Connection DatabaseClient databaseClient,
        @DisplayName("Collection") @Optional(defaultValue = "") @Example("myCollection") String collection,
        @DisplayName("Query") @Text @Optional @Example("searchTerm") String query,
        @DisplayName("Query Type") @Optional QueryType queryType,
        @DisplayName("Query Format") @Optional QueryFormat queryFormat,
        @DisplayName("Metadata Category List") @Optional(defaultValue = "all") @Example("COLLECTIONS,PERMISSIONS") String categories,
        @DisplayName("Max Results") @Optional @Example("10") Integer maxResults,
        @DisplayName("Search Options") @Optional @Example("appSearchOptions") String searchOptions,
        @DisplayName("Directory") @Optional @Example("/customerData") String directory,
        @DisplayName("REST Transform") @Optional String restTransform,
        @DisplayName("REST Transform Parameters") @Optional String restTransformParameters,
        @DisplayName("REST Transform Parameters Delimiter") @Optional @Example(",") String restTransformParametersDelimiter
    ) {
        DocumentManager<JSONReadHandle, JSONWriteHandle> documentManager = databaseClient.newJSONDocumentManager();
        if (Utilities.hasText(categories)) {
            documentManager.setMetadataCategories(buildMetadataCategories(categories));
        }
        if (maxResults != null) {
            documentManager.setPageLength(maxResults);
        }

        QueryDefinition queryDefinition = ReadUtil.buildQueryDefinitionFromParams(databaseClient, query, queryType, queryFormat);
        if (Utilities.hasText(collection)) {
            queryDefinition.setCollections(collection);
        }
        if (Utilities.hasText(searchOptions)) {
            queryDefinition.setOptionsName(searchOptions);
        }
        if (Utilities.hasText(directory)) {
            queryDefinition.setDirectory(directory);
        }
        if (Utilities.hasText(restTransform)) {
            ServerTransform serverTransform = new ServerTransform(restTransform);
            if (Utilities.hasText(restTransformParameters)) {
                String[] parametersArray = restTransformParameters.split(restTransformParametersDelimiter);
                for (int i = 0; i < parametersArray.length; i = i + 2) {
                    serverTransform.addParameter(parametersArray[i], parametersArray[i + 1]);
                }
            }
            queryDefinition.setResponseTransform(serverTransform);
        }

        DocumentPage page = documentManager.search(queryDefinition, 1);
        List<Result<InputStream, DocumentAttributes>> results = new ArrayList<>();
        while (page.hasNext()) {
            InputStreamHandle handle = new InputStreamHandle();
            DocumentRecord documentRecord = page.next();
            InputStream content = documentRecord.getContent(handle).get();
            DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
            documentRecord.getMetadata(metadataHandle);
            Result<InputStream, DocumentAttributes> resultDoc = Result.<InputStream, DocumentAttributes>builder()
                .output(content)
                .attributes(new DocumentAttributes(documentRecord.getUri(), metadataHandle))
                .mediaType(makeMediaType(handle.getFormat()))
                .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
                .build();
            results.add(resultDoc);
        }
        return results;
    }

    @MediaType(value = ANY, strict = false)
    @DisplayName("List Integers")
    public PagingProvider<DatabaseClient, Integer> listIntegers() {
        final Stack<Integer> results = new Stack<>();
        for (int i = 0; i < 20; i++) { results.push(i); }

        return new PagingProvider<DatabaseClient, Integer>() {

            @Override
            public List<Integer> getPage(DatabaseClient databaseClient) {
                List<Integer> page = new ArrayList<>();
                int pageSize = 3;
                for (int i = 0; i< pageSize; i++) {
                    if (!results.isEmpty()) {
                        page.add(results.pop());
                    }
                }
                System.out.println("getPage: " + page);
                return page;
            }

            @Override
            public java.util.Optional<Integer> getTotalResults(DatabaseClient databaseClient) {
                return java.util.Optional.empty();
            }

            @Override
            public void close(DatabaseClient databaseClient) { }
        };
    }

    @MediaType(value = ANY, strict = false)
    @DisplayName("Search Documents")
    public PagingProvider<DatabaseClient, Result<InputStream, DocumentAttributes>> searchDocuments(
        @DisplayName("Collection") @Optional(defaultValue = "") @Example("myCollection") String collection,
        @DisplayName("Query") @Text @Optional @Example("searchTerm") String query,
        @DisplayName("Query Type") @Optional QueryType queryType,
        @DisplayName("Query Format") @Optional QueryFormat queryFormat,
        @DisplayName("Metadata Category List") @Optional(defaultValue = "all") @Example("COLLECTIONS,PERMISSIONS") String categories,
        @DisplayName("Max Results") @Optional @Example("10") Integer maxResults,
        @DisplayName("Search Options") @Optional @Example("appSearchOptions") String searchOptions,
        @DisplayName("Directory") @Optional @Example("/customerData") String directory,
        @DisplayName("REST Transform") @Optional String restTransform,
        @DisplayName("REST Transform Parameters") @Optional String restTransformParameters,
        @DisplayName("REST Transform Parameters Delimiter") @Optional @Example(",") String restTransformParametersDelimiter
    ) {
        int pageSize = 3;
        if (maxResults != null) {
            pageSize = maxResults;
        }
        final int finalPageSize = pageSize;
        return new PagingProvider<DatabaseClient, Result<InputStream, DocumentAttributes>>() {
            Integer curPage = -1;
            @Override
            public List<Result<InputStream, DocumentAttributes>> getPage(DatabaseClient databaseClient) {
                List<Result<InputStream, DocumentAttributes>> resultSet = new ArrayList<>();
                curPage++;

                DocumentManager<JSONReadHandle, JSONWriteHandle> documentManager = databaseClient.newJSONDocumentManager();
                documentManager.setPageLength(finalPageSize);
                if (Utilities.hasText(categories)) {
                    documentManager.setMetadataCategories(buildMetadataCategories(categories));
                }

                QueryDefinition queryDefinition = ReadUtil.buildQueryDefinitionFromParams(databaseClient, query, queryType, queryFormat);
                if (Utilities.hasText(collection)) {
                    queryDefinition.setCollections(collection);
                }
                if (Utilities.hasText(searchOptions)) {
                    queryDefinition.setOptionsName(searchOptions);
                }
                if (Utilities.hasText(directory)) {
                    queryDefinition.setDirectory(directory);
                }
                if (Utilities.hasText(restTransform)) {
                    ServerTransform serverTransform = new ServerTransform(restTransform);
                    if (Utilities.hasText(restTransformParameters)) {
                        String[] parametersArray = restTransformParameters.split(restTransformParametersDelimiter);
                        for (int i = 0; i < parametersArray.length; i = i + 2) {
                            serverTransform.addParameter(parametersArray[i], parametersArray[i + 1]);
                        }
                    }
                    queryDefinition.setResponseTransform(serverTransform);
                }

                int startingDocument = (curPage * finalPageSize) + 1;
                DocumentPage documentPage = documentManager.search(queryDefinition, startingDocument);
                while (documentPage.hasNext()) {
                    InputStreamHandle contentHandle = new InputStreamHandle();
                    DocumentRecord documentRecord = documentPage.next();
                    InputStream contentStream = documentRecord.getContent(contentHandle).get();
                    DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
                    documentRecord.getMetadata(metadataHandle);
                    Result<InputStream, DocumentAttributes> resultDoc = Result.<InputStream, DocumentAttributes>builder()
                        .output(contentStream)
                        .attributes(new DocumentAttributes(documentRecord.getUri(), metadataHandle))
                        .mediaType(makeMediaType(contentHandle.getFormat()))
                        .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
                        .build();
                    resultSet.add(resultDoc);
                }
                return resultSet;
            }

            @Override
            public java.util.Optional<Integer> getTotalResults(DatabaseClient databaseClient) {
                return java.util.Optional.empty();
            }

            @Override
            public void close(DatabaseClient databaseClient) { }
        };
    }

    /**
     * Write a batch of documents.
     * <p>
     * TODO This will eventually have parameters for controlling how each document
     * is written.
     *
     * @param databaseClient
     * @param myContents
     */
    public void writeBatch(
        @Connection DatabaseClient databaseClient,
        @Content InputStream[] myContents) {
        System.out.println("CONTENT COUNT: " + myContents.length);
        DocumentManager mgr = databaseClient.newDocumentManager();
        DocumentWriteSet writeSet = mgr.newWriteSet();
        DocumentMetadataHandle metadata = new DocumentMetadataHandle()
            .withPermission("rest-reader", DocumentMetadataHandle.Capability.READ,
                DocumentMetadataHandle.Capability.UPDATE)
            .withCollections("batch-output");
        for (InputStream content : myContents) {
            String uri = "/batch-output/" + UUID.randomUUID().toString() + ".json";
            writeSet.add(uri, metadata, new InputStreamHandle(content));
        }
        mgr.write(writeSet);
    }

    /**
     * Evaluate custom JavaScript code on the MarkLogic server.
     */
    @MediaType(value = ANY, strict = false)
    @DisplayName("Eval JavaScript")
    public String evalJavascript(
        @Connection DatabaseClient databaseClient,
        @DisplayName("Script") @Text @Example("xdmp.log('Hello, World!');") String script
    ) {
        if (Utilities.hasText(script)) {
            return databaseClient.newServerEval().javascript(script).evalAs(String.class);
        } else {
            throw new RuntimeException("A valid script must be provided.");
        }
    }

    private org.mule.runtime.api.metadata.MediaType makeMediaType(Format format) {
        if (Format.JSON.equals(format)) {
            return org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
        } else if (Format.XML.equals(format)) {
            return org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
        } else if (Format.TEXT.equals(format)) {
            return org.mule.runtime.api.metadata.MediaType.TEXT;
        }
        return org.mule.runtime.api.metadata.MediaType.BINARY;
    }

    private DocumentManager.Metadata[] buildMetadataCategories(String categories) {
        String[] categoriesArray = categories.split(",");
        DocumentManager.Metadata[] transformedCategories = new DocumentManager.Metadata[categoriesArray.length];
        int index = 0;
        for (String category : categoriesArray) {
            transformedCategories[index++] = DocumentManager.Metadata.valueOf(category.toUpperCase());
        }
        return transformedCategories;
    }
}
