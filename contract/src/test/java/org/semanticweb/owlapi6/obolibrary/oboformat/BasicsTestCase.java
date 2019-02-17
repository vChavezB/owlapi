package org.semanticweb.owlapi6.obolibrary.oboformat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.semanticweb.owlapi6.model.AxiomType.DISJOINT_CLASSES;
import static org.semanticweb.owlapi6.model.AxiomType.EQUIVALENT_CLASSES;
import static org.semanticweb.owlapi6.model.AxiomType.SUBCLASS_OF;
import static org.semanticweb.owlapi6.utilities.OWLAPIStreamUtils.asList;
import static org.semanticweb.owlapi6.utilities.OWLAPIStreamUtils.asUnorderedSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.Test;
import org.semanticweb.owlapi6.apibinding.OWLManager;
import org.semanticweb.owlapi6.formats.OBODocumentFormat;
import org.semanticweb.owlapi6.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi6.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi6.io.StringDocumentTarget;
import org.semanticweb.owlapi6.model.IRI;
import org.semanticweb.owlapi6.model.OWLAnnotation;
import org.semanticweb.owlapi6.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi6.model.OWLAnnotationProperty;
import org.semanticweb.owlapi6.model.OWLAnnotationSubject;
import org.semanticweb.owlapi6.model.OWLClass;
import org.semanticweb.owlapi6.model.OWLClassExpression;
import org.semanticweb.owlapi6.model.OWLDataFactory;
import org.semanticweb.owlapi6.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi6.model.OWLImportsDeclaration;
import org.semanticweb.owlapi6.model.OWLLiteral;
import org.semanticweb.owlapi6.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi6.model.OWLObjectProperty;
import org.semanticweb.owlapi6.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi6.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi6.model.OWLObjectUnionOf;
import org.semanticweb.owlapi6.model.OWLOntology;
import org.semanticweb.owlapi6.model.OWLOntologyCreationException;
import org.semanticweb.owlapi6.model.OWLOntologyStorageException;
import org.semanticweb.owlapi6.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi6.obolibrary.macro.MacroExpansionGCIVisitor;
import org.semanticweb.owlapi6.obolibrary.macro.MacroExpansionVisitor;
import org.semanticweb.owlapi6.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.owlapi6.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.semanticweb.owlapi6.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.semanticweb.owlapi6.obolibrary.oboformat.diff.Diff;
import org.semanticweb.owlapi6.obolibrary.oboformat.diff.OBODocDiffer;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.Clause;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.Frame;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.Frame.FrameType;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.FrameStructureException;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.OBODoc;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.QualifierValue;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.Xref;
import org.semanticweb.owlapi6.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi6.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi6.obolibrary.oboformat.parser.XrefExpander;
import org.semanticweb.owlapi6.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi6.search.Searcher;
import org.semanticweb.owlapi6.vocab.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi6.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi6.vocab.Obo2OWLConstants.Obo2OWLVocabulary;

/**
 * Tests for the conversion of rdfs:comment in OWL to remark tag in OBO. This is
 * necessary as OBO-Edit won't load any OBO ontology containing a comment-tag in
 * the ontology header. WARNING: This conversion will not conserve the order of
 * remark tags in a round-trip via OWL.
 */
public class BasicsTestCase extends OboFormatTestBasics {

    private static final String OBO = "http://purl.obolibrary.org/obo/";
    private static final IRI SHORTHAND = df.getIRI("http://www.geneontology.org/formats/oboInOwl#", "shorthand");
    private static final IRI ID = df.getIRI("http://www.geneontology.org/formats/oboInOwl#", "id");
    private static final IRI BFO50 = df.getIRI(OBO, "BFO_0000050");
    private static final IRI RO2111 = df.getIRI(OBO, "RO_0002111");
    private static final IRI BAR1 = df.getIRI(OBO, "BAR_0000001");
    private static final IRI BFO51 = df.getIRI(OBO, "BFO_0000051");

    private static void assertAnnotationPropertyCountEquals(OWLOntology owlOnt, IRI subjectIRI,
        OWLAnnotationProperty property, int expected) {
        List<OWLAnnotationAssertionAxiom> matches = asList(
            owlOnt.annotationAssertionAxioms(subjectIRI).filter(ax -> ax.getProperty().equals(property)));
        assertEquals(subjectIRI + " has too many annotations of type " + property + ":\n\t" + matches, expected,
            matches.size());
    }

    private static void checkIdSpace(OBODoc doc) {
        Frame headerFrame = doc.getHeaderFrame();
        assertNotNull(headerFrame);
        Clause clause = headerFrame.getClause(OboFormatTag.TAG_IDSPACE);
        assertNotNull(clause);
        Collection<Object> values = clause.getValues();
        assertNotNull(values);
        assertEquals(3, values.size());
        Iterator<Object> it = values.iterator();
        assertEquals("GO", it.next());
        assertEquals("urn:lsid:bioontology.org:GO:", it.next());
        assertEquals("gene ontology terms", it.next());
    }

    private static void checkIntersection(OWLClassExpression expression, String genus, String relId,
        String differentia) {
        OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf) expression;
        List<? extends OWLClassExpression> list = intersection.getOperandsAsList();
        OWLClass cls = (OWLClass) list.get(0);
        assertEquals(genus, OWLAPIOwl2Obo.getIdentifier(cls.getIRI()));
        OWLClassExpression rhs = list.get(1);
        OWLClass cls2 = rhs.classesInSignature().iterator().next();
        assertEquals(differentia, OWLAPIOwl2Obo.getIdentifier(cls2.getIRI()));
        OWLObjectProperty property = rhs.objectPropertiesInSignature().iterator().next();
        assertEquals(relId, OWLAPIOwl2Obo.getIdentifier(property.getIRI()));
    }

    private static OBODoc createPVDoc() {
        OBODoc oboDoc = new OBODoc();
        Frame headerFrame = new Frame(FrameType.HEADER);
        headerFrame.addClause(new Clause(OboFormatTag.TAG_FORMAT_VERSION, "1.2"));
        headerFrame.addClause(new Clause(OboFormatTag.TAG_ONTOLOGY, "test"));
        addPropertyValue(headerFrame, "http://purl.org/dc/elements/1.1/title", "Ontology for Biomedical Investigation",
            "xsd:string");
        addPropertyValue(headerFrame, "defaultLanguage", "en", "xsd:string");
        oboDoc.setHeaderFrame(headerFrame);
        return oboDoc;
    }

    private static void addPropertyValue(Frame frame, String v1, String v2, @Nullable String v3) {
        Clause cl = new Clause(OboFormatTag.TAG_PROPERTY_VALUE);
        cl.addValue(v1);
        cl.addValue(v2);
        if (v3 != null) {
            cl.addValue(v3);
        }
        frame.addClause(cl);
    }

    private static void checkFrame(OBODoc doc, String id, String name, String namespace) {
        Frame frame = doc.getTermFrame(id);
        if (frame == null) {
            frame = doc.getTypedefFrame(id);
        }
        assertNotNull(frame);
        assertEquals(name, frame.getTagValue(OboFormatTag.TAG_NAME));
        assertEquals(namespace, frame.getTagValue(OboFormatTag.TAG_NAMESPACE));
    }

    private static void checkOBODoc2(OBODoc obodoc) {
        // OBODoc tests
        Frame tf = obodoc.getTermFrame("x1"); // TODO - may change
        assert tf != null;
        Collection<Clause> cs = tf.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
        assertTrue(cs.size() != 1);
        // there should NEVER be a situation with single intersection tags
        // TODO - add validation step prior to saving
    }

    @Test
    public void testCommentRemarkConversion() throws Exception {
        OBODoc obo = parseOBOFile("comment_remark_conversion.obo", true, Collections.emptyMap());
        Frame headerFrame = obo.getHeaderFrame();
        assertNotNull(headerFrame);
        Collection<String> remarks = headerFrame.getTagValues(OboFormatTag.TAG_REMARK, String.class);
        OWLAPIObo2Owl obo2Owl = new OWLAPIObo2Owl(m1);
        OWLOntology owlOntology = obo2Owl.convert(obo);
        Set<String> comments = asUnorderedSet(owlOntology.annotations(df.getRDFSComment()).map(OWLAnnotation::getValue)
            .filter(a -> a instanceof OWLLiteral).map(a -> ((OWLLiteral) a).getLiteral()));
        // check that all remarks have been translated to rdfs:comment
        assertEquals(remarks.size(), comments.size());
        assertTrue(comments.containsAll(remarks));
        assertTrue(remarks.containsAll(comments));
        OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(m1);
        OBODoc oboRoundTrip = owl2Obo.convert(owlOntology);
        Frame headerFrameRoundTrip = oboRoundTrip.getHeaderFrame();
        assertNotNull(headerFrameRoundTrip);
        Collection<String> remarksRoundTrip = headerFrameRoundTrip.getTagValues(OboFormatTag.TAG_REMARK, String.class);
        assertEquals(remarks.size(), remarksRoundTrip.size());
        assertTrue(remarksRoundTrip.containsAll(remarks));
        assertTrue(remarks.containsAll(remarksRoundTrip));
    }

    @Test
    public void testBFOROXrefCorrectIdAnnotationCount() {
        OWLOntology owlOnt = convertOBOFile("rel_xref_test.obo");
        assertEquals(4, owlOnt.objectPropertiesInSignature().count());
        OWLAnnotationProperty OBO_ID = df.getOWLAnnotationProperty(ID);
        // Check ID Property Count Exactly 1
        assertAnnotationPropertyCountEquals(owlOnt, BAR1, OBO_ID, 1);
        assertAnnotationPropertyCountEquals(owlOnt, RO2111, OBO_ID, 1);
        assertAnnotationPropertyCountEquals(owlOnt, BFO50, OBO_ID, 1);
        assertAnnotationPropertyCountEquals(owlOnt, BFO51, OBO_ID, 2);
    }

    @Test
    public void testBFOROXrefRelationXrefConversion() {
        OWLOntology owlOnt = convertOBOFile("rel_xref_test.obo");
        // test initial conversion
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(SHORTHAND);
        assertEquals(4, owlOnt.objectPropertiesInSignature().count());
        Stream<OWLAnnotationAssertionAxiom> aaas = owlOnt.annotationAssertionAxioms(BFO51);
        boolean ok = aaas.filter(ax -> ax.getProperty().equals(ap)).map(a -> (OWLLiteral) a.getValue())
            .anyMatch(v -> v.getLiteral().equals("has_part"));
        assertTrue(ok);
        aaas = owlOnt.annotationAssertionAxioms(BFO50);
        assertTrue(aaas.count() > 0);
        aaas = owlOnt.annotationAssertionAxioms(RO2111);
        assertTrue(aaas.count() > 0);
        aaas = owlOnt.annotationAssertionAxioms(BAR1);
        assertTrue(aaas.count() > 0);
        OWLAPIOwl2Obo revbridge = new OWLAPIOwl2Obo(m1);
        OBODoc d2 = revbridge.convert(owlOnt);
        Frame partOf = d2.getTypedefFrame("part_of");
        assert partOf != null;
        Collection<Clause> xrcs = partOf.getClauses(OboFormatTag.TAG_XREF);
        boolean okBfo = false;
        boolean okOboRel = false;
        for (Clause c : xrcs) {
            Xref value = c.getValue(Xref.class);
            if (value.getIdref().equals("BFO:0000050")) {
                okBfo = true;
            }
            if (value.getIdref().equals("OBO_REL:part_of")) {
                okOboRel = true;
            }
        }
        assertTrue(okBfo);
        assertTrue(okOboRel);
        Frame a = d2.getTermFrame("TEST:a");
        assert a != null;
        Clause rc = a.getClause(OboFormatTag.TAG_RELATIONSHIP);
        assert rc != null;
        assertEquals("part_of", rc.getValue());
        assertEquals("TEST:b", rc.getValue2());
    }

    @Test
    public void testParseCARO() {
        OBODoc obodoc = parseOBOFile("caro.obo");
        assertTrue(obodoc.getTermFrames().size() > 2);
        Frame cc = obodoc.getTermFrame("CARO:0000014");
        assertNotNull(cc);
        assertEquals("cell component", cc.getTagValue(OboFormatTag.TAG_NAME));
        assertEquals("Anatomical structure that is a direct part of the cell.", cc.getTagValue(OboFormatTag.TAG_DEF));
        Clause dc = cc.getClause(OboFormatTag.TAG_DEF);
        assertNotNull(dc);
        Collection<Xref> dcxs = dc.getXrefs();
        assertEquals("CARO:MAH", dcxs.iterator().next().getIdref());
        /*
         * Collection<Xref> defxrefs = cc.getTagXrefs("def");
         * System.out.println("def xrefs = "+defxrefs);
         * assertTrue(defxrefs.iterator().next().getIdref().equals("CARO:MAH"));
         */
        // assertTrue(frame.getClause(OboFormatTag.TAG_NAME.getTag()).getValue().equals("x1"));
    }

    @Test
    public void testDanglingOwl2OboConversion() throws Exception {
        OBODoc doc = convert(parseOWLFile("dangling_owl2_obo_test.owl"));
        Frame f = doc.getTermFrame("UBERON:0000020");
        assert f != null;
        Clause rc = f.getClause(OboFormatTag.TAG_NAME);
        assert rc != null;
        assertEquals("sense organ", rc.getValue());
        Collection<Clause> ics = f.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
        assertEquals(2, ics.size());
        writeOBO(doc);
    }

    @Test
    public void testExpandChebiXRef() {
        OBODoc obodoc = parseOBOFile("chebi_problematic_xref.obo");
        assertNotNull(obodoc);
    }

    @Test(expected = OBOFormatParserException.class)
    public void testCurlyBracesInComments() {
        /*
         * Expect an parser exception, as the comment line contains '{' and '}'. This will lead the
         * parser to try and parse it as a trailing qualifier, which fails in this case.
         */
        parseOBOFile("fbbt_comment_test.obo");
    }

    @Test
    public void testWriteCurlyBracesInComments() throws Exception {
        OBODoc doc = new OBODoc();
        Frame h = new Frame(FrameType.HEADER);
        h.addClause(new Clause(OboFormatTag.TAG_ONTOLOGY, "test"));
        doc.setHeaderFrame(h);
        Frame t = new Frame(FrameType.TERM);
        String id = "TEST:0001";
        t.setId(id);
        t.addClause(new Clause(OboFormatTag.TAG_ID, id));
        String comment = "Comment with a '{' curly braces '}'";
        t.addClause(new Clause(OboFormatTag.TAG_COMMENT, comment));
        doc.addFrame(t);
        String oboString = renderOboToString(doc);
        assertTrue(oboString.contains("comment: Comment with a '\\{' curly braces '}'"));
        OBODoc doc2 = parseOboToString(oboString);
        assertNotNull(doc2);
        Frame termFrame = doc2.getTermFrame(id);
        assertNotNull(termFrame);
        assertEquals(comment, termFrame.getTagValue(OboFormatTag.TAG_COMMENT));
    }

    @Test
    public void testDanglingRestrictionOwl2OboConversion() throws Exception {
        // this is a test ontology that has had its imports axioms removed
        OBODoc doc = convert(parseOWLFile("dangling_restriction_test.owl"));
        Frame f = doc.getTermFrame("FUNCARO:0000014");
        assert f != null;
        Clause rc = f.getClause(OboFormatTag.TAG_NAME);
        assert rc != null;
        assertEquals("digestive system", rc.getValue());
        Collection<Clause> isas = f.getClauses(OboFormatTag.TAG_IS_A);
        assertEquals(1, isas.size());
        Collection<Clause> rs = f.getClauses(OboFormatTag.TAG_RELATIONSHIP);
        assertEquals(1, rs.size());
        writeOBO(doc);
    }

    @Test
    public void testDanglingRoundTripConvertXPs() throws Exception {
        OWLOntology owlOnt = convertOBOFile("dangling_roundtrip_test.obo");
        OWLAPIOwl2Obo revbridge = new OWLAPIOwl2Obo(m1);
        OBODoc d2 = revbridge.convert(owlOnt);
        Frame f = d2.getTermFrame("UBERON:0000020");
        assert f != null;
        Clause rc = f.getClause(OboFormatTag.TAG_NAME);
        assert rc != null;
        assertEquals("sense organ", rc.getValue());
        OBOFormatWriter w = new OBOFormatWriter();
        w.write(d2, File.createTempFile("zzz", ".obo"));
    }

    @Test
    public void testDbXrefCommentsRoundtrip() throws Exception {
        OBODoc obodoc = parseOBOFile("db_xref_comments.obo");
        Frame frame = obodoc.getTermFrame("MOD:00516");
        assertNotNull(frame);
        Clause defClause = frame.getClause(OboFormatTag.TAG_DEF);
        assertNotNull(defClause);
        Collection<Xref> xrefs = defClause.getXrefs();
        assertEquals(2, xrefs.size());
        Iterator<Xref> iterator = xrefs.iterator();
        Xref xref1 = iterator.next();
        assertEquals("RESID:AA0151", xref1.getIdref());
        String annotation = xref1.getAnnotation();
        assertEquals("variant", annotation);
        Xref xref2 = iterator.next();
        assertEquals("UniMod:148", xref2.getIdref());
        String original = readResource("db_xref_comments.obo");
        String renderedOboString = renderOboToString(obodoc);
        assertEquals(original, renderedOboString);
    }

    @Test
    public void testDuplicateTags() throws Exception {
        OWLOntology owl = parseOWLFile("duplicate-def.ofn");
        final List<Clause> duplicates = new ArrayList<>();
        OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(m1) {

            @Override
            protected boolean handleDuplicateClause(Frame frame, Clause clause) {
                duplicates.add(clause);
                return super.handleDuplicateClause(frame, clause);
            }
        };
        OBODoc convert = owl2Obo.convert(owl);
        assertEquals(1, duplicates.size());
        // test that no exception is thrown during write.
        renderOboToString(convert);
    }

    @Test
    public void testEmptyFirstLine() {
        OBODoc obodoc = parseOBOFile("empty_lines.obo");
        Collection<Frame> frames = obodoc.getTermFrames();
        assertEquals(1, frames.size());
        assertEquals("GO:0009555", frames.iterator().next().getId());
    }

    @Test
    public void testConvertEquivalentTo() throws Exception {
        // PARSE TEST FILE
        OWLOntology ontology = convert(parseOBOFile("equivtest.obo"));
        // TEST CONTENTS OF OWL ONTOLOGY
        assertEquals(2, ontology.axioms(EQUIVALENT_CLASSES).count());
        // CONVERT BACK TO OBO
        OWLAPIOwl2Obo owl2obo = new OWLAPIOwl2Obo(m1);
        OBODoc obodoc = owl2obo.convert(ontology);
        checkOBODoc(obodoc);
        // ROUNDTRIP AND TEST AGAIN
        OBOFormatWriter w = new OBOFormatWriter();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter out = new BufferedWriter(osw);
            PrintWriter bufferedWriter = new PrintWriter(out);) {
            w.write(obodoc, bufferedWriter);
        }
        OBOFormatParser p = new OBOFormatParser();
        obodoc = p.parse(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(os.toByteArray()))));
        checkOBODoc(obodoc);
    }

    public void checkOBODoc(OBODoc obodoc) {
        // OBODoc tests
        // test ECA between named classes is persisted using correct tag
        Frame tf = obodoc.getTermFrame("X:1");
        assert tf != null;
        Collection<Clause> cs = tf.getClauses(OboFormatTag.TAG_EQUIVALENT_TO);
        assertEquals(1, cs.size());
        Object v = cs.iterator().next().getValue();
        assertEquals("X:2", v);
        // test ECA between named class and anon class is persisted as
        // genus-differentia intersection_of tags
        tf = obodoc.getTermFrame("X:1");
        assert tf != null;
        cs = tf.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
        assertEquals(2, cs.size());
        boolean okGenus = false;
        boolean okDifferentia = false;
        for (Clause c : cs) {
            Collection<Object> vs = c.getValues();
            if (vs.size() == 2) {
                if (c.getValue().equals("R:1") && c.getValue2().equals("Z:1")) {
                    okDifferentia = true;
                }
            } else if (vs.size() == 1) {
                if (c.getValue().equals("Y:1")) {
                    okGenus = true;
                }
            } else {
                fail();
            }
        }
        assertTrue(okGenus);
        assertTrue(okDifferentia);
        // check reciprocal direction
        Frame tf2 = obodoc.getTermFrame("X:2");
        assert tf2 != null;
        Collection<Clause> cs2 = tf2.getClauses(OboFormatTag.TAG_EQUIVALENT_TO);
        Frame tf1 = obodoc.getTermFrame("X:1");
        assert tf1 != null;
        Collection<Clause> cs1 = tf1.getClauses(OboFormatTag.TAG_EQUIVALENT_TO);
        boolean ok = false;
        if (cs2.size() == 1) {
            if (cs2.iterator().next().getValue(String.class).equals("X:1")) {
                ok = true;
            }
        }
        if (cs1.size() == 1) {
            if (cs1.iterator().next().getValue(String.class).equals("X:2")) {
                ok = true;
            }
        }
        assertTrue(ok);
    }

    @Test
    public void testExpandExpressionGCI() {
        OWLOntology ontology = convert(parseOBOFile("no_overlap.obo"));
        MacroExpansionGCIVisitor mev = new MacroExpansionGCIVisitor(m1, ontology, false);
        OWLOntology gciOntology = mev.createGCIOntology();
        int axiomCount = gciOntology.getAxiomCount();
        assertTrue(axiomCount > 0);
        OWLClass cls = df.getOWLClass(OBO, "TEST_2");
        assertEquals(1, gciOntology.disjointClassesAxioms(cls).count());
        gciOntology.axioms(EQUIVALENT_CLASSES).forEach(eca -> {
            Set<OWLClassExpression> ces = asUnorderedSet(eca.classExpressions());
            OWLClass clst4 = df.getOWLClass(OBO, "TEST_4");
            OWLObjectPropertyExpression p = df.getOWLObjectProperty(OBO, "RO_0002104");
            OWLClassExpression cet4 = df.getOWLObjectSomeValuesFrom(p, clst4);
            OWLClass clst5 = df.getOWLClass(OBO, "TEST_5");
            OWLClassExpression cet5 = df.getOWLObjectSomeValuesFrom(p, clst5);
            if (ces.contains(cet4)) {
                ces.remove(cet4);
                OWLClassExpression clst4ex = ces.iterator().next();
                assertEquals("ObjectSomeValuesFrom(" + BFO51.toQuotedString()
                    + " ObjectIntersectionOf(<http://purl.obolibrary.org/obo/GO_0005886> ObjectSomeValuesFrom("
                    + BFO51.toQuotedString() + " " + clst4.getIRI().toQuotedString() + ")))", clst4ex.toString());
            } else if (ces.contains(cet5)) {
                ces.remove(cet5);
                OWLClassExpression clst5ex = ces.iterator().next();
                assertEquals("ObjectSomeValuesFrom(" + BFO51.toQuotedString()
                    + " ObjectIntersectionOf(<http://purl.obolibrary.org/obo/GO_0005886> ObjectSomeValuesFrom("
                    + BFO51.toQuotedString() + " <http://purl.obolibrary.org/obo/TEST_5>)))", clst5ex.toString());
            } else {
                fail("Unknown OWLEquivalentClassesAxiom: " + eca);
            }
        });
    }

    @Test
    public void testExpandExpression() {
        OWLOntology ontology = convert(parseOBOFile("no_overlap.obo"));
        MacroExpansionVisitor mev = new MacroExpansionVisitor(ontology);
        OWLOntology outputOntology = mev.expandAll();
        OWLClass cls = df.getOWLClass(OBO, "TEST_2");
        assertEquals(1, outputOntology.disjointClassesAxioms(cls).count());
        cls = df.getOWLClass(OBO, "TEST_3");
        assertEquals(1, outputOntology.subClassAxiomsForSubClass(cls).count());
        assertEquals(
            "SubClassOf(<http://purl.obolibrary.org/obo/TEST_3> ObjectSomeValuesFrom(" + BFO51.toQuotedString()
                + " ObjectIntersectionOf(<http://purl.obolibrary.org/obo/GO_0005886> ObjectSomeValuesFrom("
                + BFO51.toQuotedString() + " <http://purl.obolibrary.org/obo/TEST_4>))))",
            outputOntology.subClassAxiomsForSubClass(cls).iterator().next().toString());
        cls = df.getOWLClass(OBO, "TEST_4");
        AtomicBoolean ok = new AtomicBoolean(false);
        outputOntology.equivalentClassesAxioms(cls).flatMap(OWLEquivalentClassesAxiom::classExpressions)
            .filter(ce -> ce instanceof OWLObjectIntersectionOf).flatMap(x -> ((OWLObjectIntersectionOf) x).operands())
            .filter(y -> y instanceof OWLObjectSomeValuesFrom)
            .map(y -> ((OWLObjectSomeValuesFrom) y).getProperty().toString()).forEach(pStr -> {
                assertEquals(BFO51.toQuotedString(), pStr);
                ok.set(true);
            });
        assertTrue(ok.get());
        writeOWL(ontology);
    }

    @Test
    public void testExpandSynapsedTo() {
        OWLOntology ontology = convert(parseOBOFile("synapsed_to.obo"));
        MacroExpansionGCIVisitor mev = new MacroExpansionGCIVisitor(m1, ontology, false);
        OWLOntology gciOntology = mev.createGCIOntology();
        int axiomCount = gciOntology.getAxiomCount();
        assertTrue(axiomCount > 0);
        assertEquals(4, gciOntology.axioms().count());
    }

    @Test
    public void testExpandWithAnnotations() {
        OWLOntology ontology = convert(parseOBOFile("annotated_no_overlap.obo"));
        MacroExpansionVisitor mev = new MacroExpansionVisitor(ontology, true, true);
        OWLOntology gciOntology = mev.expandAll();
        gciOntology.axioms(DISJOINT_CLASSES)
            .forEach(ax -> assertEquals("annotation count", 2, ax.annotations().count()));
    }

    @Test
    public void testExpandNothing() {
        OWLOntology ontology = convert(parseOBOFile("nothing_expansion_test.obo"));
        MacroExpansionGCIVisitor mev = new MacroExpansionGCIVisitor(m1, ontology, false);
        OWLOntology gciOntology = mev.createGCIOntology();
        int axiomCount = gciOntology.getAxiomCount();
        assertEquals(2, axiomCount);
        assertEquals(2, gciOntology.axioms().count());
    }

    @Test
    public void testConvertGCIQualifier() {
        // PARSE TEST FILE, CONVERT TO OWL, AND WRITE TO OWL FILE
        OWLOntology ontology = convert(parseOBOFile("gci_qualifier_test.obo"));
        long scas = ontology.axioms(SUBCLASS_OF).count();
        boolean ok = scas > 0;
        assertTrue(ok);
        // CONVERT BACK TO OBO
        OBODoc obodoc = convert(ontology);
        // test that relation IDs are converted back to symbolic form
        Frame tf = obodoc.getTermFrame("X:1");
        assert tf != null;
        Collection<Clause> clauses = tf.getClauses(OboFormatTag.TAG_RELATIONSHIP);
        assertEquals(2, clauses.size());
    }

    /**
     * During the conversion of the rdfxml formatfile the ontology header tags
     * are lost. The possible reason is that the RDFXMLOntologyFormat format
     * writes the annotation assertion axioms as annotations.
     */
    @Test
    public void testHeaderLostBug() throws OWLOntologyStorageException {
        OWLOntology ontology = roundTrip(convert(parseOBOFile("header_lost_bug.obo")), new RDFXMLDocumentFormat());
        IRI ontologyIRI = df.getIRI(OBO, "test.owl");
        // two tags in the header of the obo file are translated as annotation
        // assertions, so the axioms
        // should have two axioms in count.
        assertEquals(2, ontology.annotationsAsList().size());
        assertEquals(0, ontology.annotationAssertionAxioms(ontologyIRI).count());
    }

    @Test
    public void testExpandHomeomorphicRelation() {
        OWLOntology owlOnt = convertOBOFile("homrel.obo");
        assertNotNull(owlOnt);
    }

    @Test
    public void testIdSpace() throws Exception {
        OBODoc doc1 = parseOBOFile("idspace_test.obo");
        checkIdSpace(doc1);
        String oboString = renderOboToString(doc1);
        assertTrue(oboString.contains("idspace: GO urn:lsid:bioontology.org:GO: \"gene ontology terms\""));
        OBODoc doc2 = parseOboToString(oboString);
        checkIdSpace(doc2);
    }

    @Test
    public void testIgnoreImportAnnotations() {
        OBODoc oboDoc = parseOBOFile("annotated_import.obo");
        Frame headerFrame = oboDoc.getHeaderFrame();
        assertNotNull(headerFrame);
        Collection<Clause> imports = headerFrame.getClauses(OboFormatTag.TAG_IMPORT);
        assertEquals(1, imports.size());
        Clause clause = imports.iterator().next();
        Collection<QualifierValue> qualifierValues = clause.getQualifierValues();
        assertTrue(qualifierValues.isEmpty());
    }

    @Test
    public void testImportsConverted() throws OWLOntologyCreationException {
        Map<String, OBODoc> cache = new HashMap<>();
        IRI iri = df.getIRI("http://purl.obolibrary.org/obo/tests/test.obo");
        cache.put(iri.toString(), new OBODoc());
        m.createOntology(iri);
        OBODoc oboDoc = parseOBOFile("annotated_import.obo", false, cache);
        OWLAPIObo2Owl toOWL = new OWLAPIObo2Owl(m);
        Stream<OWLImportsDeclaration> imports = toOWL.convert(oboDoc).importsDeclarations();
        assertTrue(imports.allMatch(i -> i.getIRI().equals(iri)));
    }

    @Test
    public void testConvertLogicalDefinitionPropertyView() {
        // PARSE TEST FILE
        OWLOntology owlOntology = convert(parseOBOFile("logical-definition-view-relation-test.obo"));
        OWLObjectProperty op = df.getOWLObjectProperty(OBO, "BFO_0000050");
        boolean ok = owlOntology.axioms(EQUIVALENT_CLASSES).anyMatch(eca -> eca.classExpressions()
            .anyMatch(x -> x instanceof OWLObjectSomeValuesFrom && x.containsEntityInSignature(op)));
        assertTrue(ok);
        // reverse translation
        OBODoc obodoc = convert(owlOntology);
        Frame fr = obodoc.getTermFrame("X:1");
        assert fr != null;
        Collection<Clause> clauses = fr.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
        assertEquals(2, clauses.size());
    }

    /*
     *
     * Note there is currently a bug whereby blocks of constraints are not translated. E.g
     *
     * [Term] id: GO:0009657 name: plastid organization relationship: never_in_taxon NCBITaxon:33208
     * {id="GOTAX:0000492", source="PMID:21311032"} ! Metazoa relationship: never_in_taxon
     * NCBITaxon:4751 {id="GOTAX:0000502", source="PMID:21311032"} ! Fungi relationship:
     * never_in_taxon NCBITaxon:28009 {id="GOTAX:0000503", source="PMID:21311032"} !
     * Choanoflagellida relationship: never_in_taxon NCBITaxon:554915 {id="GOTAX:0000504",
     * source="PMID:21311032"} ! Amoebozoa
     *
     *
     */
    @Test
    public void testExpandTaxonConstraints() {
        OWLOntology ontology = convert(parseOBOFile("taxon_constraints.obo"));
        MacroExpansionVisitor mev = new MacroExpansionVisitor(ontology);
        OWLOntology outputOntology = mev.expandAll();
        assertTrue(outputOntology.axioms(DISJOINT_CLASSES).iterator().hasNext());
    }

    @Test
    public void testParseManchesterSyntaxToolIds() {
        OWLOntology owlOntology = convert(parseOBOFile("simplego.obo"));
        ManchesterSyntaxTool parser = new ManchesterSyntaxTool(owlOntology);
        OWLClassExpression expression = parser.parseManchesterExpression("GO_0018901 AND BFO:0000050 some GO_0055124");
        checkIntersection(expression, "GO:0018901", "BFO:0000050", "GO:0055124");
    }

    @Test
    public void testParseManchesterSyntaxToolNames() {
        OWLOntology owlOntology = convert(parseOBOFile("simplego.obo"));
        ManchesterSyntaxTool parser = new ManchesterSyntaxTool(owlOntology);
        OWLClassExpression expression = parser.parseManchesterExpression(
            "'2,4-dichlorophenoxyacetic acid metabolic process' AND 'part_of' some 'premature neural plate formation'");
        checkIntersection(expression, "GO:0018901", "BFO:0000050", "GO:0055124");
    }

    @Test(expected = FrameStructureException.class)
    public void testCheckForMultipleCommentsinFrame() throws Exception {
        OBODoc obodoc = parseOBOFile("multiple_comments_test.obo");
        assertEquals(1, obodoc.getTermFrames().size());
        Frame frame = obodoc.getTermFrames().iterator().next();
        assertNotNull(frame);
        renderOboToString(obodoc); // throws FrameStructureException
    }

    @Test
    public void testConvertCAROObo2Owl() {
        OWLOntology owlOnt = convertOBOFile("caro.obo");
        assertNotNull(owlOnt);
    }

    @Test
    public void testConvertXPWithQVObo2Owl() {
        OWLOntology owlOnt = convertOBOFile("testqvs.obo");
        assertNotNull(owlOnt);
    }

    @Test
    public void testIdenticalOBODocDiffer() {
        OBODoc obodoc1 = parseOBOFile("caro.obo");
        OBODoc obodoc2 = parseOBOFile("caro.obo");
        List<Diff> diffs = OBODocDiffer.getDiffs(obodoc1, obodoc2);
        assertEquals(0, diffs.size());
    }

    @Test
    public void testDiffOBODocDiffer() {
        OBODoc obodoc1 = parseOBOFile("caro.obo");
        OBODoc obodoc2 = parseOBOFile("caro_modified.obo");
        List<Diff> diffs = OBODocDiffer.getDiffs(obodoc1, obodoc2);
        assertEquals(19, diffs.size());
    }

    @Test
    public void writeTypeDefComments() throws Exception {
        OBODoc doc = parseOBOFile("typedef_comments.obo", true, Collections.emptyMap());
        String original = readResource("typedef_comments.obo");
        String written = renderOboToString(doc);
        assertEquals(original, written);
    }

    @Test
    public void testOBOEscapeChars() {
        OBODoc obodoc = parseOBOFile("escape_chars_test.obo");
        assertEquals(3, obodoc.getTermFrames().size());
        Frame f1 = obodoc.getTermFrame("GO:0033942");
        assertNotNull(f1);
        assertEquals("GO:0033942", f1.getId());
        Clause nameClause = f1.getClause(OboFormatTag.TAG_NAME);
        assertNotNull(nameClause);
        assertEquals("4-alpha-D-{(1->4)-alpha-D-glucano}trehalose trehalohydrolase activity", nameClause.getValue());
        Frame f2 = obodoc.getTermFrame("CL:0000096");
        assertNotNull(f2);
        assertEquals("CL:0000096", f2.getId());
        Clause defClause = f2.getClause(OboFormatTag.TAG_DEF);
        assertNotNull(defClause);
        assertEquals("bla bla .\"", defClause.getValue());
        Clause commentClause = f2.getClause(OboFormatTag.TAG_COMMENT);
        assertNotNull(commentClause);
        assertEquals("bla bla bla.\nbla bla (bla).", commentClause.getValue());
    }

    @Test
    public void testOBORoundTripEscapeChars() throws Exception {
        OBODoc oboDoc = parseOBOFile("escape_chars_test.obo");
        String oboToString = renderOboToString(oboDoc);
        OBODoc oboDoc2 = parseOboToString(oboToString);
        assertNotNull("There was an error during parsing of the obodoc", oboDoc2);
        List<Diff> diffs = OBODocDiffer.getDiffs(oboDoc, oboDoc2);
        assertEquals("Expected no diffs.", 0, diffs.size());
        String original = readResource("escape_chars_test.obo");
        assertEquals(original, oboToString);
    }

    @Test
    public void testExpandPropertyValue() {
        OBODoc obodoc = parseOBOFile("property_value_test.obo");
        Frame termFrame = obodoc.getTermFrame("UBERON:0004657");
        assertNotNull(termFrame);
        Clause propertyValue = termFrame.getClause(OboFormatTag.TAG_PROPERTY_VALUE);
        assertNotNull(propertyValue);
        assertEquals("IAO:0000412", propertyValue.getValue());
        assertEquals("http://purl.obolibrary.org/obo/uberon.owl", propertyValue.getValue2());
    }

    @Test
    public void testWriteReadValuesPropertyValue() throws Exception {
        OBODoc doc = createPVDoc();
        String oboString = renderOboToString(doc);
        OBODoc doc2 = parseOboToString(oboString);
        List<Diff> diffs = OBODocDiffer.getDiffs(doc, doc2);
        assertEquals("Expected no diffs", 0, diffs.size());
    }

    @Test
    public void testParseOBOFileSimpleGO() {
        OBODoc obodoc = parseOBOFile("simplego.obo");
        assertEquals(3, obodoc.getTermFrames().size());
        assertEquals(5, obodoc.getTypedefFrames().size());
        checkFrame(obodoc, "GO:0018901", "2,4-dichlorophenoxyacetic acid metabolic process", "biological_process");
        checkFrame(obodoc, "GO:0055124", "premature neural plate formation", "biological_process");
        checkFrame(obodoc, "GO:0055125", "Nic96 complex", "cellular_component");
        checkFrame(obodoc, "has_part", "has_part", "gene_ontology");
        checkFrame(obodoc, "negatively_regulates", "negatively_regulates", "gene_ontology");
        checkFrame(obodoc, "part_of", "part_of", "gene_ontology");
        checkFrame(obodoc, "positively_regulates", "positively_regulates", "gene_ontology");
        checkFrame(obodoc, "regulates", "regulates", "gene_ontology");
    }

    @Test(expected = FrameStructureException.class)
    public void testParseOBOFileSingleIntersectionOfTag() throws Exception {
        OBODoc obodoc = parseOBOFile("single_intersection_of_tag_test.obo");
        assertEquals(2, obodoc.getTermFrames().size());
        Frame frame = obodoc.getTermFrames().iterator().next();
        assertNotNull(frame);
        renderOboToString(obodoc); // throws FrameStructureException
    }

    @Test
    public void testIDs() throws OWLOntologyCreationException {
        OBODoc doc = new OBODoc();
        Frame header = new Frame(FrameType.HEADER);
        Clause c = new Clause(OboFormatTag.TAG_ONTOLOGY.getTag());
        c.setValue("test");
        header.addClause(c);
        doc.setHeaderFrame(header);
        OWLAPIObo2Owl obo2owl = new OWLAPIObo2Owl(m1);
        OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(m1);
        OWLOntology ontology = obo2owl.convert(doc);
        owl2Obo.convert(ontology);
        // Obo 2 OWL
        IRI iri = obo2owl.oboIdToIRI("GO:001");
        assertEquals("http://purl.obolibrary.org/obo/GO_001", iri.toString());
        // OWL 2 obo
        String oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("GO:001", oboId);
        iri = obo2owl.oboIdToIRI("My_Ont:FOO_002");
        assertEquals("http://purl.obolibrary.org/obo/My_Ont#_FOO_002", iri.toString());
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("My_Ont:FOO_002", oboId);
        iri = obo2owl.oboIdToIRI("My_Ont:002");
        assertEquals("http://purl.obolibrary.org/obo/My_Ont_002", iri.toString());
        // OWL 2 obo
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("My_Ont:002", oboId);
        // unprefixed IDs are prefixed with the current ontology ID
        iri = obo2owl.oboIdToIRI("003");
        assertEquals("http://purl.obolibrary.org/obo/test#003", iri.toString());
        // OWL 2 obo
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("003", oboId);
        // arbitrary URL to obo ID
        oboId = OWLAPIOwl2Obo.getIdentifier(df.getIRI("http://purl.obolibrary.org/obo/alternate#", "abcdef"));
        // todo - test this
        // System.out.println("== "+oboId);
        iri = obo2owl.oboIdToIRI("part_of");
        assertEquals("http://purl.obolibrary.org/obo/test#part_of", iri.toString());
        // OWL 2 obo
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("part_of", oboId);
        iri = obo2owl.oboIdToIRI("OBO_REL:part_of");
        assertEquals("http://purl.obolibrary.org/obo/OBO_REL#_part_of", iri.toString());
        // OWL 2 obo
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("OBO_REL:part_of", oboId);
        iri = obo2owl.oboIdToIRI("http://purl.obolibrary.org/testont");
        assertEquals("http://purl.obolibrary.org/testont", iri.toString());
        // OWL 2 obo
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("http://purl.obolibrary.org/testont", oboId);
        iri = obo2owl.oboIdToIRI("http://purl.obolibrary.org/obo/BFO_0000050");
        assertEquals("http://purl.obolibrary.org/obo/BFO_0000050", iri.toString());
        // OWL 2 obo
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("BFO:0000050", oboId);
        // MGI IDs are perverse - they have a double-separator
        iri = obo2owl.oboIdToIRI("MGI:MGI:1");
        assertEquals("http://purl.obolibrary.org/obo/MGI_MGI%3A1", iri.toString());
        // OWL 2 obo
        oboId = OWLAPIOwl2Obo.getIdentifier(iri);
        assertEquals("MGI:MGI:1", oboId);
    }

    @Test
    public void testConvertObsoleteTerm() {
        // PARSE TEST FILE
        OWLOntology ontology = convert(parseOBOFile("obsolete_term_test.obo"));
        // TEST CONTENTS OF OWL ONTOLOGY
        OWLAnnotationSubject subj = df.getIRI(OBO, "XX_0000034");
        boolean okDeprecated = Searcher
            .annotationObjects(ontology.annotationAssertionAxioms(subj),
                df.getOWLAnnotationProperty(OWLRDFVocabulary.OWL_DEPRECATED))
            .map(OWLAnnotation::getValue).map(x -> (OWLLiteral) x).filter(OWLLiteral::isBoolean)
            .filter(OWLLiteral::parseBoolean).findAny().isPresent();
        assertTrue(okDeprecated);
        // CONVERT TO OWL FILE
        writeOWL(ontology, new RDFXMLDocumentFormat());
        // CONVERT BACK TO OBO
        OBODoc obodoc = convert(ontology);
        Frame tf = obodoc.getTermFrame("XX:0000034");
        assert tf != null;
        Clause c = tf.getClause(OboFormatTag.TAG_IS_OBSOLETE);
        assert c != null;
        Object v = c.getValue();
        assertEquals(Boolean.TRUE, v);
    }

    @Test
    public void testConvertXPsPropertyChain() {
        assertNotNull(parseOBOFile("chaintest.obo", true, Collections.emptyMap()));
    }

    @Test
    public void testConvertRelationShorthand() {
        // PARSE TEST FILE, CONVERT TO OWL, AND WRITE TO OWL FILE
        OWLOntology ontology = convert(parseOBOFile("relation_shorthand_test.obo"));
        // TEST CONTENTS OF OWL ONTOLOGY
        List<OWLSubClassOfAxiom> scas = asList(ontology.axioms(SUBCLASS_OF));
        boolean ok = false;
        for (OWLSubClassOfAxiom sca : scas) {
            OWLClassExpression sup = sca.getSuperClass();
            if (sup instanceof OWLObjectSomeValuesFrom) {
                OWLObjectProperty p = (OWLObjectProperty) ((OWLObjectSomeValuesFrom) sup).getProperty();
                OWLClass v = (OWLClass) ((OWLObjectSomeValuesFrom) sup).getFiller();
                if (p.getIRI().equals(BFO51)
                    && v.getIRI().toString().equals("http://purl.obolibrary.org/obo/GO_0004055")) {
                    ok = true;
                }
            }
        }
        assertTrue(ok);
        scas = asList(ontology.axioms(SUBCLASS_OF));
        ok = false;
        for (OWLSubClassOfAxiom sca : scas) {
            OWLClassExpression sup = sca.getSuperClass();
            if (sup instanceof OWLObjectSomeValuesFrom) {
                OWLObjectProperty p = (OWLObjectProperty) ((OWLObjectSomeValuesFrom) sup).getProperty();
                OWLClass v = (OWLClass) ((OWLObjectSomeValuesFrom) sup).getFiller();
                if (p.getIRI().equals(BFO50)
                    && v.getIRI().toString().equals("http://purl.obolibrary.org/obo/XX_0000001")) {
                    ok = true;
                }
            }
        }
        assertTrue(ok);
        // CONVERT BACK TO OBO
        OBODoc obodoc = convert(ontology);
        // test that relation IDs are converted back to symbolic form
        Frame tf = obodoc.getTermFrame("GO:0000050");
        assert tf != null;
        Clause c = tf.getClause(OboFormatTag.TAG_RELATIONSHIP);
        assert c != null;
        Object v = c.getValue();
        // should be converted back to symbolic form
        assertEquals("has_part", v);
        tf = obodoc.getTermFrame("GO:0004055");
        assert tf != null;
        c = tf.getClause(OboFormatTag.TAG_RELATIONSHIP);
        assert c != null;
        v = c.getValue();
        // should be converted back to symbolic form
        assertEquals("part_of", v);
        tf = obodoc.getTypedefFrame("has_part");
        assert tf != null;
        Collection<Clause> cs = tf.getClauses(OboFormatTag.TAG_XREF);
        assertEquals(1, cs.size());
        v = cs.iterator().next().getValue(Xref.class).getIdref();
        // should be converted back to symbolic form
        assertEquals("BFO:0000051", v);
    }

    @Test
    public void testRoundTripMultiLineDefinitions() throws Exception {
        // create minimal ontology
        OBODoc oboDocSource = new OBODoc();
        oboDocSource.setHeaderFrame(new Frame(FrameType.HEADER));
        oboDocSource.addDefaultOntologyHeader("caro");
        // add source frame that contains at least one new line
        Frame sourceFrame = new Frame(FrameType.TERM);
        sourceFrame.setId("CARO:0000049");
        sourceFrame.addClause(new Clause(OboFormatTag.TAG_DEF,
            "Sequential hermaphroditic organism that produces\ngametes first of the male sex, and then later of the\nfemale sex."));
        oboDocSource.addTermFrame(sourceFrame);
        // convert to OWL and retrieve def
        OWLAPIObo2Owl bridge = new OWLAPIObo2Owl(m1);
        OWLOntology owlOntology = bridge.convert(oboDocSource);
        OWLDataFactory factory = owlOntology.getOWLOntologyManager().getOWLDataFactory();
        // IRI
        IRI iri = bridge.oboIdToIRI("CARO:0000049");
        OWLClass c = factory.getOWLClass(iri);
        // Def
        OWLAnnotationProperty defProperty = factory.getOWLAnnotationProperty(
            Obo2OWLVocabulary.IRI_IAO_0000115.getNamespace(), Obo2OWLVocabulary.IRI_IAO_0000115.getShortName());
        int counter = 0;
        for (OWLAnnotationAssertionAxiom ax : asList(owlOntology.annotationAssertionAxioms(c.getIRI()))) {
            if (ax.getProperty().equals(defProperty)) {
                counter++;
                assertTrue(ax.getValue() instanceof OWLLiteral);
                String owlDef = ((OWLLiteral) ax.getValue()).getLiteral();
                // check that owl def also contains at least one new line
                assertTrue(owlDef.indexOf('\n') > 0);
            }
        }
        assertEquals(1, counter);
        // convert back to OBO
        OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(m1);
        OBODoc convertedOboDoc = owl2Obo.convert(owlOntology);
        Frame convertedFrame = convertedOboDoc.getTermFrame("CARO:0000049");
        assert convertedFrame != null;
        String convertedDef = convertedFrame.getTagValue(OboFormatTag.TAG_DEF, String.class);
        assert convertedDef != null;
        // check that round trip still contains newlines
        assertTrue(convertedDef.indexOf('\n') > 0);
    }

    @Test
    public void testConvertSubset() {
        // PARSE TEST FILE
        OWLOntology ontology = convert(parseOBOFile("subset_test.obo"));
        OWLAnnotationSubject subj = df.getIRI(OBO, "GO_0000003");
        OWLAnnotationProperty p = df.getOWLAnnotationProperty("http://www.geneontology.org/formats/oboInOwl#",
            "inSubset");
        boolean ok = ontology.annotationAssertionAxioms(subj).anyMatch(a -> a.getProperty().equals(p));
        assertTrue(ok);
    }

    @Test
    public void testConvertSynonym() {
        // PARSE TEST FILE
        assertNotNull(convert(parseOBOFile("synonym_test.obo")));
    }

    @Test
    public void testUnionOf() {
        OWLOntology owlOnt = convertOBOFile("taxon_union_terms.obo");
        assertNotNull(owlOnt);
        OWLClass cls = df.getOWLClass(OBO, "NCBITaxon_Union_0000000");
        boolean ok = owlOnt.equivalentClassesAxioms(cls).flatMap(ax -> ax.classExpressions())
            .anyMatch(ce -> ce instanceof OWLObjectUnionOf);
        assertTrue(ok);
    }

    @Test
    public void testConvertUnmappableExpressions() throws Exception {
        OWLAPIOwl2Obo bridge = new OWLAPIOwl2Obo(m1);
        bridge.setMuteUntranslatableAxioms(true);
        OBODoc doc = bridge.convert(parseOWLFile("nesting.owl"));
        assertEquals("untranslatable axiom count", 1, bridge.getUntranslatableAxioms().size());
        OBODoc obodoc = doc;
        // checkOBODoc(obodoc);
        // ROUNDTRIP AND TEST AGAIN
        String file = writeOBO(obodoc);
        obodoc = parseOBOFile(new StringReader(file), false, Collections.emptyMap());
        checkOBODoc2(obodoc);
    }

    @Test
    public void testConvertXPBridgeFile() {
        OWLOntology owlOnt = convertOBOFile("xptest.obo");
        assertNotNull(owlOnt);
    }

    @Test
    public void testXRefExpander() {
        OBODoc obodoc = parseOBOFile("treat_xrefs_test.obo");
        XrefExpander x = new XrefExpander(obodoc);
        x.expandXrefs();
        OBODoc tdoc = obodoc.getImportedOBODocs().iterator().next();
        assertTrue(!tdoc.getTermFrames().isEmpty());
        Frame termFrame = tdoc.getTermFrame("ZFA:0001689");
        assertNotNull(termFrame);
        assertEquals(2, termFrame.getClauses(OboFormatTag.TAG_INTERSECTION_OF).size());
        termFrame = tdoc.getTermFrame("EHDAA:571");
        assertNotNull(termFrame);
        Clause clause = termFrame.getClause(OboFormatTag.TAG_IS_A);
        assertNotNull(clause);
        assertEquals("UBERON:0002539", clause.getValue());
        termFrame = tdoc.getTermFrame("UBERON:0006800");
        assertNotNull(termFrame);
        clause = termFrame.getClause(OboFormatTag.TAG_IS_A);
        assertNotNull(clause);
        assertEquals("CARO:0000008", clause.getValue());
    }

    @Test
    public void testXRefExpanderIntoSeparateBridges() {
        OBODoc obodoc = parseOBOFile("treat_xrefs_test.obo");
        XrefExpander x = new XrefExpander(obodoc, "bridge");
        x.expandXrefs();
        int n = 0;
        for (OBODoc tdoc : obodoc.getImportedOBODocs()) {
            Frame hf = tdoc.getHeaderFrame();
            if (hf == null) {
                continue;
            }
            Clause impClause = hf.getClause(OboFormatTag.TAG_ONTOLOGY);
            assertNotNull(impClause);
            String tid = impClause.getValue(String.class).replace("bridge-", "");
            if (tid.equals("zfa")) {
                Frame termFrame = tdoc.getTermFrame("ZFA:0001689");
                assertNotNull(termFrame);
                assertEquals(2, termFrame.getClauses(OboFormatTag.TAG_INTERSECTION_OF).size());
                Frame pf = tdoc.getTypedefFrame("part_of");
                assert pf != null;
                Clause clause = pf.getClause(OboFormatTag.TAG_XREF);
                assertNotNull(clause);
                assertEquals("BFO:0000050", clause.getValue().toString());
                n++;
            }
            if (tid.equals("ehdaa")) {
                Frame termFrame = tdoc.getTermFrame("EHDAA:571");
                assertNotNull(termFrame);
                Clause clause = termFrame.getClause(OboFormatTag.TAG_IS_A);
                assertNotNull(clause);
                assertEquals("UBERON:0002539", clause.getValue());
                n++;
            }
            if (tid.equals("caro")) {
                Frame termFrame = tdoc.getTermFrame("UBERON:0006800");
                assertNotNull(termFrame);
                Clause clause = termFrame.getClause(OboFormatTag.TAG_IS_A);
                assertNotNull(clause);
                assertEquals("CARO:0000008", clause.getValue());
                n++;
            }
        }
        assertEquals(3, n);
        // assertTrue(frame.getClause("name").getValue().equals("x1"));
    }
    /*
     * @Test public void testUberonHeader() throws Exception { OBODoc obodoc =
     * parseOBOFile("uberon_header_test.obo"); XrefExpander x = new XrefExpander(obodoc, "bridge");
     * x.expandXrefs(); }
     */

    @Test
    public void testConversionXRefExpanderIRI() throws Exception {
        OWLOntology ontology = parseOWLFile("xrefIRItest.owl");
        OBODoc doc = convert(ontology);
        doc.getTermFrame("FOO:1");
        writeOBO(doc);
    }

    @Test
    public void testUntranslatableAxiomsInHeader() throws Exception {
        untranslatableAxiomsInHeader(parseOWLFile("untranslatable_axioms.owl"));
    }

    @Test
    public void testUntranslatableAxiomsInHeader2() throws Exception {
        untranslatableAxiomsInHeader(parseOWLFile("untranslatable_axioms2.owl"));
    }

    @Test
    public void testNoDeadlock() throws OWLOntologyStorageException, OWLOntologyCreationException {
        OWLOntology o = OWLManager.createConcurrentOWLOntologyManager().createOntology(df.getIRI("urn:test:ontology"));
        o.add(df.getOWLSubClassOfAxiom(df.getOWLNothing(), df.getOWLThing()));
        OWLOntologyDocumentTarget target = new StringDocumentTarget();
        o.saveOntology(new OBODocumentFormat(), target);
    }

    public void untranslatableAxiomsInHeader(OWLOntology original) throws IOException, OWLOntologyCreationException {
        OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(m1);
        OBODoc obo = owl2Obo.convert(original);
        renderOboToString(obo);
        Frame headerFrame = obo.getHeaderFrame();
        assertNotNull(headerFrame);
        String owlAxiomString = headerFrame.getTagValue(OboFormatTag.TAG_OWL_AXIOMS, String.class);
        assertNotNull(owlAxiomString);
        OWLAPIObo2Owl obo2Owl = new OWLAPIObo2Owl(m1);
        OWLOntology converted = obo2Owl.convert(obo);
        Set<OWLEquivalentClassesAxiom> originalEqAxioms = asUnorderedSet(original.axioms(EQUIVALENT_CLASSES));
        Set<OWLEquivalentClassesAxiom> convertedEqAxioms = asUnorderedSet(converted.axioms(EQUIVALENT_CLASSES));
        assertEquals(originalEqAxioms, convertedEqAxioms);
    }
}