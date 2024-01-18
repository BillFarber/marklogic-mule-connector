/**
 * MarkLogic Mule Connector
 *
 * Copyright © 2024 MarkLogic Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.mule.extension;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchDocumentsStructuredQueriesTest extends AbstractFlowTester {

    @Override
    protected String getFlowTestFile() {
        return "search-documents-structured-queries.xml";
    }

    @Test
    public void noQuery() {
        assertTrue(1 < runFlowForDocumentCount("search-documents-no-query"));
    }

    @Test
    public void structuredXmlQuery() {
        assertEquals("3 docs are expected to have 'world' in them",
            3, runFlowForDocumentCount("search-documents-xml-structuredQuery"));
    }

    @Test
    public void structuredJsonQuery() {
        assertEquals("3 docs are expected to have 'world' in them",
            3, runFlowForDocumentCount("search-documents-json-structuredQuery"));
    }

    @Test
    public void structuredJsonQueryWithNoMatches() {
        assertEquals("A search term with no matches should return no documents",
            0, runFlowForDocumentCount("search-documents-structuredQuery-noMatches"));
    }

    @Test(expected = RuntimeException.class)
    public void structuredJsonQueryWithBadJson() {
        runFlowGetMessage("search-documents-structuredQuery-badJson");
    }

    @Test(expected = RuntimeException.class)
    public void structuredJsonQueryWithBadXml() {
        runFlowGetMessage("search-documents-structuredQuery-badXml");
    }
}
