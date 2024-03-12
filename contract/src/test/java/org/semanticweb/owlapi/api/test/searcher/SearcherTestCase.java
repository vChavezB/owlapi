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
package org.semanticweb.owlapi.api.test.searcher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Boolean;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataPropertyDomain;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataPropertyRange;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.EquivalentDataProperties;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.EquivalentObjectProperties;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectPropertyDomain;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectPropertyRange;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubClassOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubDataPropertyOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubObjectPropertyOf;
import static org.semanticweb.owlapi.model.parameters.Imports.INCLUDED;
import static org.semanticweb.owlapi.search.Searcher.domain;
import static org.semanticweb.owlapi.search.Searcher.equivalent;
import static org.semanticweb.owlapi.search.Searcher.range;
import static org.semanticweb.owlapi.search.Searcher.sub;
import static org.semanticweb.owlapi.search.Searcher.sup;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asUnorderedSet;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.contains;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.search.Filters;

class SearcherTestCase extends TestBase {

    private static final String URN_TEST = "urn:test#";

    @Test
    void shouldSearch() {
        // given
        OWLOntology o = getOWLOntology();
        OWLAxiom ax = SubClassOf(C, D);
        o.addAxiom(ax);
        assertTrue(contains(o.axioms(AxiomType.SUBCLASS_OF), ax));
        assertTrue(contains(o.axioms(C), ax));
    }

    @Test
    void shouldSearchObjectProperties() {
        // given
        OWLOntology o = getOWLOntology();
        OWLObjectProperty c = ObjectProperty(IRI(URN_TEST, "c"));
        OWLObjectProperty d = ObjectProperty(IRI(URN_TEST, "d"));
        OWLObjectProperty e = ObjectProperty(IRI(URN_TEST, "e"));
        OWLObjectProperty f = ObjectProperty(IRI(URN_TEST, "f"));
        OWLClass x = Class(IRI(URN_TEST, "x"));
        OWLClass y = Class(IRI(URN_TEST, "Y"));
        OWLAxiom ax = SubObjectPropertyOf(c, d);
        OWLAxiom ax2 = ObjectPropertyDomain(c, x);
        OWLAxiom ax3 = ObjectPropertyRange(c, y);
        OWLAxiom ax4 = EquivalentObjectProperties(c, e);
        OWLAxiom ax5 = SubObjectPropertyOf(c, df.getOWLObjectInverseOf(f));
        OWLAxiom ax6 = EquivalentObjectProperties(e, df.getOWLObjectInverseOf(f));
        o.addAxioms(ax, ax2, ax3, ax4, ax5, ax6);
        assertTrue(contains(o.axioms(AxiomType.SUB_OBJECT_PROPERTY), ax));
        Collection<OWLAxiom> axioms1 =
            asUnorderedSet(o.axioms(Filters.subObjectPropertyWithSuper, d, INCLUDED));
        assertTrue(contains(sub(axioms1.stream()), c));
        Collection<OWLAxiom> axioms2 = asUnorderedSet(o.axioms(Filters.subObjectPropertyWithSub, c, INCLUDED));
        assertTrue(contains(sup(axioms2.stream()), d));
        assertTrue(contains(domain(o.objectPropertyDomainAxioms(c)), x));
        assertTrue(contains(equivalent(o.equivalentObjectPropertiesAxioms(c)), e));
        assertTrue(contains(equivalent(o.equivalentObjectPropertiesAxioms(e)),
            df.getOWLObjectInverseOf(f)));
        EntitySearcher.getSuperProperties(c, o).forEach(q -> assertTrue(checkMethod(q)));
    }

    protected boolean checkMethod(OWLObject q) {
        return q instanceof OWLObjectPropertyExpression;
    }

    @Test
    void shouldSearchDataProperties() {
        // given
        OWLOntology o = getOWLOntology();
        OWLDataProperty c = DataProperty(IRI(URN_TEST, "c"));
        OWLDataProperty d = DataProperty(IRI(URN_TEST, "d"));
        OWLDataProperty e = DataProperty(IRI(URN_TEST, "e"));
        OWLAxiom ax = SubDataPropertyOf(c, d);
        OWLClass x = Class(IRI(URN_TEST, "x"));
        OWLAxiom ax2 = DataPropertyDomain(c, x);
        OWLAxiom ax3 = DataPropertyRange(c, Boolean());
        OWLAxiom ax4 = EquivalentDataProperties(c, e);
        o.addAxioms(ax, ax2, ax3, ax4);
        assertTrue(contains(o.axioms(AxiomType.SUB_DATA_PROPERTY), ax));
        assertTrue(contains(sub(o.axioms(Filters.subDataPropertyWithSuper, d, INCLUDED)), c));
        Collection<OWLAxiom> axioms =
            asUnorderedSet(o.axioms(Filters.subDataPropertyWithSub, c, INCLUDED));
        assertTrue(contains(sup(axioms.stream()), d));
        assertTrue(contains(domain(o.dataPropertyDomainAxioms(c)), x));
        assertTrue(contains(range(o.dataPropertyRangeAxioms(c)), Boolean()));
        assertTrue(contains(equivalent(o.equivalentDataPropertiesAxioms(c)), e));
    }
}
