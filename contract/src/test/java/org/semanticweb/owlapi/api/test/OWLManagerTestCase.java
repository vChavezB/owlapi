package org.semanticweb.owlapi.api.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl;

/**
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 10/04/15
 */
class OWLManagerTestCase {

    private OWLOntologyManager manager;
    private OWLOntology ontology;

    @BeforeEach
    void setUp() throws Exception {
        manager = OWLManager.createOWLOntologyManager();
        ontology = manager.createOntology();
    }

    @Test
    void shouldCreateOntologyWithCorrectManager() {
        assertThat(ontology.getOWLOntologyManager(), is(manager));
    }

    @Test
    void shouldCreateConcurrentOntologyByDefault() {
        assertThat(ontology, is(instanceOf(ConcurrentOWLOntologyImpl.class)));
    }

    @Test
    void shouldShareReadWriteLockOnConcurrentManager() throws Exception {
        // Nasty, but not sure of another way to do this without exposing it in
        // the interface
        manager = OWLManager.createConcurrentOWLOntologyManager();
        ontology = manager.createOntology();
        Field ontologyLockField = ConcurrentOWLOntologyImpl.class.getDeclaredField("lock");
        ontologyLockField.setAccessible(true);
        Field ontologyManagerField = OWLOntologyManagerImpl.class.getDeclaredField("lock");
        ontologyManagerField.setAccessible(true);
        assertThat(ontologyLockField.get(ontology), is(ontologyManagerField.get(manager)));
    }
}
