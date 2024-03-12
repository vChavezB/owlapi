/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.api.test.syntax.rdfxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.emptyOptional;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.optional;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;

/**
 * Tests the loading of a single ontology multiple times, using a different ontologyIRI in the
 * OWLOntologyID as that used in the actual ontology that is being imported.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
class MultipleDistinctOntologyLoadsTestCase extends TestBase {

    IRI jb = IRI("http://example.purl.org.au/domainontology/", "JB_000007");
    IRI v1 = IRI("http://test.example.org/ontology/0139/", "version:1");
    IRI v2 = IRI("http://test.example.org/ontology/0139/", "version:2");

    @Test
    void testMultipleVersionLoadChangeIRI() throws Exception {
        OWLOntologyDocumentSource initialDocumentSource = getDocument();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v2));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser initialParser = new RDFXMLParser();
        initialParser.parse(initialDocumentSource, initialOntology, config);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v2));
        assertThrowsWithPredicate(OWLOntologyAlreadyExistsException.class,
            e -> assertEquals(new OWLOntologyID(optional(jb), optional(v2)),
                ((OWLOntologyAlreadyExistsException) e).getOntologyID()),
            () -> m.createOntology(secondUniqueOWLOntologyID));
    }

    private OWLOntologyDocumentSource getDocument() {
        try {
            return new FileDocumentSource(
                new File(getClass().getResource("/owlapi/multipleOntologyLoadsTest.rdf").toURI()));
        } catch (URISyntaxException e) {
            throw new OWLRuntimeException(e);
        }
    }

    @Test
    void testMultipleVersionLoadNoChange() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntologyID expected = new OWLOntologyID(optional(jb), optional(v1));
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v1));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v1));
        assertThrowsWithPredicate(OWLOntologyAlreadyExistsException.class,
            e -> assertEquals(expected, ((OWLOntologyAlreadyExistsException) e).getOntologyID()),
            () -> m.createOntology(secondUniqueOWLOntologyID));
    }

    @Test
    void testMultipleVersionLoadsExplicitOntologyIDs() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v1));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        assertEquals(jb, initialOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v1, initialOntology.getOntologyID().getVersionIRI().get());
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(secondDocumentSource, secondOntology, config);
        assertEquals(jb, secondOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v2, secondOntology.getOntologyID().getVersionIRI().get());
    }

    @Test
    void testMultipleVersionLoadsNoOntologyIDFirstTime() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntology initialOntology = m.createOntology();
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        assertEquals(IRI("http://test.example.org/ontology/0139", ""),
            initialOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v1, initialOntology.getOntologyID().getVersionIRI().get());
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(secondDocumentSource, secondOntology, config);
        assertEquals(jb, secondOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v2, secondOntology.getOntologyID().getVersionIRI().get());
    }

    @Test
    void testMultipleVersionLoadsNoOntologyVersionIRIFirstTime() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        IRI iri = IRI.create("http://test.example.org/ontology/0139");
        OWLOntologyID initialUniqueOWLOntologyID =
            new OWLOntologyID(optional(iri), emptyOptional(IRI.class));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        assertEquals(iri, initialOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v1, initialOntology.getOntologyID().getVersionIRI().get());
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(secondDocumentSource, secondOntology, config);
        assertEquals(jb, secondOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v2, secondOntology.getOntologyID().getVersionIRI().get());
    }

    @Test
    void testSingleVersionLoadChangeIRI() throws Exception {
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        // the following throws the exception
        secondParser.parse(secondDocumentSource, secondOntology, config);
        assertEquals(jb, secondOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v2, secondOntology.getOntologyID().getVersionIRI().get());
    }

    @Test
    void testSingleVersionLoadNoChange() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(optional(jb), optional(v1));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        assertEquals(jb, initialOntology.getOntologyID().getOntologyIRI().get());
        assertEquals(v1, initialOntology.getOntologyID().getVersionIRI().get());
    }
}
