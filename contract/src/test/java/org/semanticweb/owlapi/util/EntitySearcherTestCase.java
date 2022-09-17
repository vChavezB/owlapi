package org.semanticweb.owlapi.util;

import static org.junit.Assert.assertTrue;
import static org.semanticweb.owlapi.OWLFunctionalSyntaxFactory.ObjectProperty;
import static org.semanticweb.owlapi.OWLFunctionalSyntaxFactory.Ontology;
import static org.semanticweb.owlapi.OWLFunctionalSyntaxFactory.SubObjectPropertyOf;
import static org.semanticweb.owlapi.utilities.OWLAPIStreamUtils.asList;
import static org.semanticweb.owlapi.utilities.OWLAPIStreamUtils.contains;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apitest.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.utilities.PrefixManagerImpl;

public class EntitySearcherTestCase extends TestBase {

    private OWLObjectProperty superProperty;
    private OWLObjectProperty subProperty;
    private Set<OWLOntology> ontologies;

    @Before
    public void setUp() {
        PrefixManager pm =
            new PrefixManagerImpl().withDefaultPrefix("http://www.ontologies.com/ontology");
        subProperty = ObjectProperty("subProperty", pm);
        superProperty = ObjectProperty("superProperty", pm);
        OWLOntology ontology = Ontology(m, SubObjectPropertyOf(subProperty, superProperty));
        ontologies = Collections.singleton(ontology);
    }

    @Test
    public void shouldReturnSuperProperty() {
        List<OWLObjectPropertyExpression> supers =
            asList(Searcher.getSuperProperties(subProperty, ontologies.stream()));
        assertTrue(supers.toString(), supers.contains(superProperty));
    }

    @Test
    public void shouldReturnSubProperty() {
        Stream<OWLObjectPropertyExpression> subs =
            Searcher.getSubProperties(superProperty, ontologies.stream());
        assertTrue(contains(subs, subProperty));
    }
}
