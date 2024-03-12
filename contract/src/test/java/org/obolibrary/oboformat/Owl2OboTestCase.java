package org.obolibrary.oboformat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

class Owl2OboTestCase extends OboFormatTestBasics {

    private static final String TEST_0001 = "TEST:0001";
    private static final String OBO = "http://purl.obolibrary.org/obo/";
    private static final String COMMENT = "Comment";

    private static void addLabelAndId(OWLNamedObject obj, String label, String id, OWLOntology o) {
        OWLDataFactory f = o.getOWLOntologyManager().getOWLDataFactory();
        addAnnotation(obj, f.getRDFSLabel(), f.getOWLLiteral(label), o);
        OWLAnnotationProperty idProp =
            f.getOWLAnnotationProperty(OWLAPIObo2Owl.trTagToIRI(OboFormatTag.TAG_ID.getTag()));
        addAnnotation(obj, idProp, f.getOWLLiteral(id), o);
    }

    private static void setAltId(OWLNamedObject obj, OWLOntology o) {
        OWLDataFactory f = o.getOWLOntologyManager().getOWLDataFactory();
        addAnnotation(obj, f.getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0100001.getIRI()),
            f.getOWLLiteral(TEST_0001), o);
        addAnnotation(obj, f.getOWLAnnotationProperty(Obo2OWLConstants.IRI_IAO_0000231),
            Obo2OWLConstants.IRI_IAO_0000227, o);
        addAnnotation(obj, f.getOWLDeprecated(), f.getOWLLiteral(true), o);
    }

    private static void addAnnotation(OWLNamedObject obj, OWLAnnotationProperty p,
        OWLAnnotationValue v, OWLOntology ont) {
        ont.add(df.getOWLAnnotationAssertionAxiom(obj.getIRI(), df.getOWLAnnotation(p, v)));
    }

    @Test
    void testConversion() throws Exception {
        OWLOntology ontology = convert(parseOBOFile("caro.obo"));
        OBODoc doc = convert(ontology);
        writeOBO(doc);
    }

    @Test
    void testIRTsConversion() throws Exception {
        IRI ontologyIRI = iri(OBO, "test.owl");
        OWLOntology ontology = m.createOntology(ontologyIRI);
        convert(ontology);
        String ontId = OWLAPIOwl2Obo.getOntologyId(ontology);
        assertEquals("test", ontId);
        IRI iri = iri(OBO, "OBI_0000306");
        String id = OWLAPIOwl2Obo.getIdentifier(iri);
        assertTrue("OBI:0000306".endsWith(id));
        iri = iri(OBO, "IAO_0000119");
        id = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("IAO:0000119", id);
        iri = iri(OBO, "caro_part_of");
        id = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("http://purl.obolibrary.org/obo/caro_part_of", id);
        iri = iri("http://purl.obolibrary.org/obo/MyOnt#", "_part_of");
        id = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("MyOnt:part_of", id);
        iri = iri("http://purl.obolibrary.org/obo/MyOnt#", "termid");
        id = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("termid", id);
        // unprefixed IDs from different ontology
        iri = iri("http://purl.obolibrary.org/obo/MyOnt#", "termid");
        id = OWLAPIOwl2Obo.getIdentifier(iri);
        // assertTrue("http://purl.obolibrary.org/obo/MyOnt#termid".equals(id));
        iri = df.getOWLTopObjectProperty().getIRI();
        id = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("owl:topObjectProperty", id);
    }

    @Test
    void testOwl2OboAltIdClass() throws Exception {
        OWLOntology simple = getOWLOntology();
        // add class A
        OWLClass classA = df.getOWLClass(Obo2OWLConstants.DEFAULT_IRI_PREFIX, "TEST_0001");
        simple.add(df.getOWLDeclarationAxiom(classA));
        // add a label and OBO style ID
        addLabelAndId(classA, "test1", TEST_0001, simple);
        // add deprecated class B as an alternate ID for A
        OWLClass classB = df.getOWLClass(Obo2OWLConstants.DEFAULT_IRI_PREFIX, "TEST_0002");
        simple.add(df.getOWLDeclarationAxiom(classB));
        setAltId(classB, simple);
        // add comment to alt_id class, which is not expressible in OBO
        addAnnotation(classB, df.getRDFSComment(), df.getOWLLiteral(COMMENT), simple);
        // translate to OBO
        OWLAPIOwl2Obo owl2obo = new OWLAPIOwl2Obo(simple.getOWLOntologyManager());
        OBODoc oboDoc = owl2obo.convert(simple);
        // check result: expect only one term frame for class TEST:0001 with
        // alt_id Test:0002
        Collection<Frame> termFrames = oboDoc.getTermFrames();
        assertEquals(1, termFrames.size());
        Frame frame = termFrames.iterator().next();
        assertEquals(TEST_0001, frame.getId());
        Collection<Clause> altIdClauses = frame.getClauses(OboFormatTag.TAG_ALT_ID);
        assertEquals(1, altIdClauses.size());
        String altId = altIdClauses.iterator().next().getValue(String.class);
        assertEquals("TEST:0002", altId);
        // roundtrip back to OWL, check that comment is still there
        OWLAPIObo2Owl obo2owl = new OWLAPIObo2Owl(m1);
        OWLOntology roundTripped = obo2owl.convert(oboDoc);
        // three for the alt-id plus one
        assertEquals(4, roundTripped.annotationAssertionAxioms(classB.getIRI()).count());
        // for the comment
        Optional<OWLLiteral> comment = findComment(classB.getIRI(), roundTripped);
        assertTrue(comment.isPresent());
        assertEquals(COMMENT, comment.get().getLiteral());
    }

    protected Optional<OWLLiteral> findComment(IRI i, OWLOntology roundTripped) {
        return roundTripped.annotationAssertionAxioms(i).filter(ax -> ax.getProperty().isComment())
            .map(ax -> ax.getValue().asLiteral()).filter(l -> l.isPresent()).findAny()
            .orElse(Optional.empty());
    }

    @Test
    void testOwl2OboProperty() throws Exception {
        OWLOntology simple = getOWLOntology();
        // add prop1
        OWLObjectProperty p1 =
            df.getOWLObjectProperty(Obo2OWLConstants.DEFAULT_IRI_PREFIX, "TEST_0001");
        simple.add(df.getOWLDeclarationAxiom(p1));
        // add label and OBO style id for
        addLabelAndId(p1, "prop1", TEST_0001, simple);
        // add deprecated prop 2 as an alternate ID for prop 1
        OWLObjectProperty p2 =
            df.getOWLObjectProperty(Obo2OWLConstants.DEFAULT_IRI_PREFIX, "TEST_0002");
        simple.add(df.getOWLDeclarationAxiom(p2));
        setAltId(p2, simple);
        // add comment to alt_id class, which is not expressible in OBO
        addAnnotation(p2, df.getRDFSComment(), df.getOWLLiteral(COMMENT), simple);
        // translate to OBO
        OWLAPIOwl2Obo owl2obo = new OWLAPIOwl2Obo(simple.getOWLOntologyManager());
        OBODoc oboDoc = owl2obo.convert(simple);
        // check result: expect only one typdef frame for prop TEST:0001 with
        // alt_id Test:0002
        Collection<Frame> termFrames = oboDoc.getTypedefFrames();
        assertEquals(1, termFrames.size());
        Frame frame = termFrames.iterator().next();
        assertEquals(TEST_0001, frame.getId());
        Collection<Clause> altIdClauses = frame.getClauses(OboFormatTag.TAG_ALT_ID);
        assertEquals(1, altIdClauses.size());
        String altId = altIdClauses.iterator().next().getValue(String.class);
        assertEquals("TEST:0002", altId);
        // roundtrip back to OWL, check that comment is still there
        OWLAPIObo2Owl obo2owl = new OWLAPIObo2Owl(m1);
        OWLOntology roundTripped = obo2owl.convert(oboDoc);
        // three for the alt-id plus one for the comment
        assertEquals(4, roundTripped.annotationAssertionAxioms(p2.getIRI()).count());
        // for the comment
        Optional<OWLLiteral> comment = findComment(p2.getIRI(), roundTripped);
        assertTrue(comment.isPresent());
        assertEquals(COMMENT, comment.get().getLiteral());
    }
}
