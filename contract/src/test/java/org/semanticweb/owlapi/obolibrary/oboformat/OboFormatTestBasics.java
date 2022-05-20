package org.semanticweb.owlapi.obolibrary.oboformat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.documents.StringDocumentTarget;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.io.OWLStorerParameters;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.semanticweb.owlapi.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.semanticweb.owlapi.obolibrary.oboformat.model.OBODoc;
import org.semanticweb.owlapi.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.obolibrary.oboformat.writer.OBOFormatWriter;

public class OboFormatTestBasics extends TestBase {

    protected OWLStorerParameters storerParameters = new OWLStorerParameters();

    protected static String renderOboToString(OBODoc oboDoc) throws IOException {
        OBOFormatWriter writer = new OBOFormatWriter();
        writer.setCheckStructure(true);
        StringWriter out = new StringWriter();
        writer.write(oboDoc, new PrintWriter(out));
        return out.getBuffer().toString();
    }

    protected static OBODoc parseOboToString(String oboString) {
        return new OBOFormatParser().parse(new StringReader(oboString));
    }

    protected OBODoc parseOBOFile(String fn) {
        return parseOBOFile(fn, false, Collections.emptyMap());
    }

    protected OBODoc parseOBOFile(String fn, boolean allowEmptyFrames, Map<String, OBODoc> cache) {
        try (InputStream inputStream = getInputStream(fn)) {
            OBOFormatParser p = new OBOFormatParser(cache);
            OBODoc obodoc = p.parse(new BufferedReader(new InputStreamReader(inputStream)));
            assertNotNull(obodoc);
            if (obodoc.getTermFrames().isEmpty() && !allowEmptyFrames) {
                fail("Term frames should not be empty.");
            }
            return obodoc;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected OBODoc parseOBOFile(Reader fn, boolean allowEmptyFrames, Map<String, OBODoc> cache) {
        OBOFormatParser p = new OBOFormatParser(cache);
        OBODoc obodoc = p.parse(new BufferedReader(fn));
        assertNotNull(obodoc);
        if (obodoc.getTermFrames().isEmpty() && !allowEmptyFrames) {
            fail("Term frames should not be empty.");
        }
        return obodoc;
    }

    @SuppressWarnings("resource")
    protected InputStream getInputStream(String fn) {
        InputStream inputStream = OboFormatTestBasics.class.getResourceAsStream(fn);
        if (inputStream == null) {
            inputStream = getClass().getResourceAsStream("obo/" + fn);
        }
        if (inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(fn);
        }
        if (inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream("obo/" + fn);
        }
        if (inputStream == null) {
            try {
                inputStream = new FileInputStream(new File("obo/" + fn));
            } catch (FileNotFoundException e) {
                throw new OWLRuntimeException(e);
            }
        }
        return inputStream;
    }

    protected OWLOntology parseOWLFile(String fn) throws OWLOntologyCreationException {
        OWLOntologyManager manager = setupManager();
        // TODO replace
        return manager.loadOntologyFromOntologyDocument(getInputStream(fn));
    }

    protected OWLOntology convert(OBODoc obodoc) {
        OWLAPIObo2Owl bridge = new OWLAPIObo2Owl(setupManager());
        OWLOntology ontology;
        try {
            ontology = bridge.convert(obodoc);
            return ontology;
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    protected OWLOntology convertOBOFile(String fn) {
        OWLOntology convert = convert(parseOBOFile(fn));
        writeOWL(convert);
        return convert;
    }

    protected OBODoc convert(OWLOntology ontology) {
        return convert(ontology, false);
    }

    protected OBODoc convert(OWLOntology ontology, boolean strictness) {
        OWLAPIOwl2Obo bridge = new OWLAPIOwl2Obo(setupManager());
        bridge.setStrictConversion(strictness);
        return bridge.convert(ontology);
    }

    protected String writeOBO(OBODoc obodoc) throws IOException {
        StringWriter target = new StringWriter();
        OBOFormatWriter oboWriter = new OBOFormatWriter();
        BufferedWriter bw = new BufferedWriter(target);
        oboWriter.write(obodoc, new PrintWriter(bw));
        bw.flush();
        return target.toString();
    }

    protected StringDocumentTarget writeOWL(OWLOntology ontology) {
        return writeOWL(ontology, new OWLXMLDocumentFormat());
    }

    protected StringDocumentTarget writeOWL(OWLOntology ontology, OWLDocumentFormat format) {
        StringDocumentTarget target = new StringDocumentTarget();
        try {
            ontology.saveOntology(format, target);
        } catch (OWLOntologyStorageException e) {
            throw new OWLRuntimeException(e);
        }
        return target;
    }

    protected @Nullable IRI getIriByLabel(OWLOntology ontology, String label) {
        Optional<OWLAnnotationAssertionAxiom> anyMatch = ontology.axioms(AxiomType.ANNOTATION_ASSERTION)
            .filter(aa -> aa.getProperty().isLabel() && aa.getValue() instanceof OWLLiteral
                && label.equals(((OWLLiteral) aa.getValue()).getLiteral()))
            .filter(aa -> aa.getSubject().isIRI()).findAny();
        if (anyMatch.isPresent()) {
            return (IRI) anyMatch.get().getSubject();
        }
        return null;
    }

    protected String readResource(String resource) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = getInputStream(resource);
            Reader r = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(r);) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}