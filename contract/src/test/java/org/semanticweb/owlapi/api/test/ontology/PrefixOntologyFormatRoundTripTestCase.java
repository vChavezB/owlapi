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
package org.semanticweb.owlapi.api.test.ontology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 * @since 3.2.0
 */
class PrefixOntologyFormatRoundTripTestCase extends TestBase {

    protected OWLOntology prefixOntologyFormatRoundTripTestCase() {
        OWLOntology ont = getAnonymousOWLOntology();
        PrefixDocumentFormat format = (PrefixDocumentFormat) ont.getNonnullFormat();
        format.setDefaultPrefix("http://default.com");
        format.setPrefix("a", "http://ontology.com/a#");
        format.setPrefix("b", "http://ontology.com/b#");
        return ont;
    }

    @Override
    public OWLOntology roundTripOntology(OWLOntology ont, OWLDocumentFormat format) {
        OWLOntology ont2 = super.roundTripOntology(ont, format);
        OWLDocumentFormat ont2Format = ont2.getFormat();
        if (format instanceof PrefixDocumentFormat && ont2Format instanceof PrefixDocumentFormat) {
            PrefixDocumentFormat prefixFormat = (PrefixDocumentFormat) format;
            PrefixDocumentFormat prefixFormat2 = (PrefixDocumentFormat) ont2Format;
            prefixFormat.prefixNames().forEach(prefixName -> {
                assertTrue(prefixFormat2.containsPrefixMapping(prefixName));
                assertEquals(prefixFormat.getPrefix(prefixName),
                    prefixFormat2.getPrefix(prefixName));
            });
        }
        return ont2;
    }

    @ParameterizedTest
    @MethodSource("formats")
    void testFormat(OWLDocumentFormat d) throws Exception {
        roundTripOntology(prefixOntologyFormatRoundTripTestCase(), d);
    }

    @Test
    void roundTripRDFXMLAndFunctionalShouldBeSame()
        throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntology o = prefixOntologyFormatRoundTripTestCase();
        OWLOntology o1 = roundTrip(o, new RDFXMLDocumentFormat());
        OWLOntology o2 = roundTrip(o, new FunctionalSyntaxDocumentFormat());
        equal(o, o1);
        equal(o1, o2);
    }
}
