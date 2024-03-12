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
package org.semanticweb.owlapi.api.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.AsymmetricObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.FunctionalDataProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.FunctionalObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.InverseFunctionalObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IrreflexiveObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ReflexiveObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SymmetricObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.TransitiveObjectProperty;
import static org.semanticweb.owlapi.search.EntitySearcher.isAsymmetric;
import static org.semanticweb.owlapi.search.EntitySearcher.isFunctional;
import static org.semanticweb.owlapi.search.EntitySearcher.isInverseFunctional;
import static org.semanticweb.owlapi.search.EntitySearcher.isIrreflexive;
import static org.semanticweb.owlapi.search.EntitySearcher.isReflexive;
import static org.semanticweb.owlapi.search.EntitySearcher.isSymmetric;
import static org.semanticweb.owlapi.search.EntitySearcher.isTransitive;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 * @since 3.1.0
 */
class PropertyCharacteristicsAccessorsTestCase extends TestBase {

    @Test
    void testTransitive() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isTransitive(P, ont));
        ont.add(TransitiveObjectProperty(P));
        assertTrue(isTransitive(P, ont));
    }

    @Test
    void testSymmetric() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isSymmetric(P, ont));
        ont.add(SymmetricObjectProperty(P));
        assertTrue(isSymmetric(P, ont));
    }

    @Test
    void testAsymmetric() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isAsymmetric(P, ont));
        ont.add(AsymmetricObjectProperty(P));
        assertTrue(isAsymmetric(P, ont));
    }

    @Test
    void testReflexive() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isReflexive(P, ont));
        ont.add(ReflexiveObjectProperty(P));
        assertTrue(isReflexive(P, ont));
    }

    @Test
    void testIrreflexive() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isIrreflexive(P, ont));
        ont.add(IrreflexiveObjectProperty(P));
        assertTrue(isIrreflexive(P, ont));
    }

    @Test
    void testFunctional() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isFunctional(P, ont));
        ont.add(FunctionalObjectProperty(P));
        assertTrue(isFunctional(P, ont));
    }

    @Test
    void testInverseFunctional() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isInverseFunctional(P, ont));
        ont.add(InverseFunctionalObjectProperty(P));
        assertTrue(isInverseFunctional(P, ont));
    }

    @Test
    void testFunctionalDataProperty() {
        OWLOntology ont = getOWLOntology();
        assertFalse(isFunctional(DP, ont));
        ont.add(FunctionalDataProperty(DP));
        assertTrue(isFunctional(DP, ont));
    }
}
