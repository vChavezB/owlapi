package org.semanticweb.owlapi.io;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.verifyNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.apache.commons.rdf.api.AbstractRDFTest;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.NodeID;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

// NOTE: Always fully qualified IRI  to avoid confusion between
//import org.apache.commons.rdf.api.IRI;
//import org.semanticweb.owlapi.model.IRI;

@SuppressWarnings({"javadoc", "null"})
public class CommonsRDFTermTest extends AbstractRDFTest {

    @Disabled
    @Override
    public void hashCodeBlankNode() {
        // TODO Auto-generated method stub
        // super.hashCodeBlankNode();
    }

    @Disabled
    @Override
    public void hashCodeLiteral() {
        // TODO Auto-generated method stub
        // super.hashCodeLiteral();
    }

    @Disabled
    @Override
    public void testCreateGraph() {
        // TODO Auto-generated method stub
        // super.testCreateGraph();
    }

    @Disabled
    @Override
    public void testCreateBlankNodeIdentifierTwiceDifferentFactories() {
        // TODO Auto-generated method stub
        // super.testCreateBlankNodeIdentifierTwiceDifferentFactories();
    }

    @Disabled
    @Override
    public void hashCodeTriple() {
        // TODO Auto-generated method stub
        // super.hashCodeTriple();
    }

    @Override
    public RDF createFactory() {
        return new OWLAPIRDFTermFactory();
    }

    @Override
    @Disabled
    @Test
    public void testPossiblyInvalidBlankNode() throws Exception {
        // FIXME: Should BlankNode identifiers be validated? At least
        // ntriplesString() output should be checked - but could
        // this also affect load/save issues?
        assumeTrue(false, "BlankNode identifiers are not validated");
        super.testPossiblyInvalidBlankNode();
    }

    @Disabled
    @Override
    public void testInvalidLiteralLang() throws Exception {
        // FIXME: RDFLiteral should not allow spaces in lang
        assumeTrue(false, "RDFLiteral does not validate lang");
        super.testInvalidLiteralLang();
    }

    @Disabled
    @Override
    public void testInvalidIRI() throws Exception {
        assumeTrue(false, "IRI string is not validated");
        super.testInvalidIRI();
    }

    private final class OWLAPIRDFTermFactory implements RDF {

        private final AtomicInteger bnodeCounter = new AtomicInteger();

        public OWLAPIRDFTermFactory() {}

        @Override
        public RDFResourceBlankNode createBlankNode() {
            return new RDFResourceBlankNode(Integer.valueOf(bnodeCounter.incrementAndGet()), false,
                false, false);
        }

        @Override
        public RDFResourceBlankNode createBlankNode(@Nullable String name) {
            org.semanticweb.owlapi.model.IRI iri =
                createIRI(NodeID.getIRIFromNodeID(verifyNotNull(name)));
            return new RDFResourceBlankNode(iri, false, false, false);
        }

        @Override
        public RDFLiteral createLiteral(@Nullable String lexicalForm) {
            return new RDFLiteral(verifyNotNull(lexicalForm), null,
                OWL2Datatype.XSD_STRING.getIRI());
        }

        @Override
        public RDFLiteral createLiteral(@Nullable String lexicalForm, @Nullable IRI dataType) {
            return new RDFLiteral(verifyNotNull(lexicalForm), null,
                createIRI(verifyNotNull(dataType).getIRIString()));
        }

        @Override
        public RDFLiteral createLiteral(@Nullable String lexicalForm,
            @Nullable String languageTag) {
            return new RDFLiteral(verifyNotNull(lexicalForm), languageTag, null);
        }

        @Override
        public org.semanticweb.owlapi.model.IRI createIRI(@Nullable String iriStr) {
            return org.semanticweb.owlapi.model.IRI.create(verifyNotNull(iriStr));
            // TODO: What about RDFResourceIRI?
            // return new RDFResourceIRI(innerIri);
        }

        @Override
        public Triple createTriple(@Nullable BlankNodeOrIRI subject, @Nullable IRI predicate,
            @Nullable RDFTerm object) {
            RDFResource subject2 = (RDFResource) convert(subject);
            RDFResourceIRI predicate2 = (RDFResourceIRI) convert(predicate);
            RDFNode object2 = convert(object);
            return new RDFTriple(subject2, predicate2, object2);
        }

        private RDFNode convert(@Nullable RDFTerm term) {
            verifyNotNull(term);
            if (term instanceof RDFNode) {
                // NOTE: Naively assuming it can then be further casted to
                // RDFResourceIRI etc
                return (RDFNode) term;
            }
            if (term instanceof IRI) {
                IRI iri = (IRI) term;
                return new RDFResourceIRI(createIRI(iri.getIRIString()));
            }
            if (term instanceof BlankNode) {
                BlankNode blankNode = (BlankNode) term;
                String ntriples = blankNode.ntriplesString();
                org.semanticweb.owlapi.model.IRI iriLike;
                if (ntriples.startsWith("<") && ntriples.endsWith(">")) {
                    // strange.. a BlankNode with IRI? Well, we can handle
                    // those.
                    iriLike = createIRI(ntriples.substring(1, ntriples.length() - 1));
                } else if (ntriples.startsWith("_:")) {
                    // org.semanticweb.owlapi.model.IRI supports these directly
                    // even though they are not relly IRIs
                    iriLike = createIRI(ntriples);
                } else {
                    // How can this be valid N-Triples?
                    throw new IllegalArgumentException(
                        "Unsupported ntriplesString on BlankNode: " + ntriples);
                }
                return new RDFResourceBlankNode(iriLike, false, false, false);
            }
            if (term instanceof Literal) {
                Literal literal = (Literal) term;
                org.semanticweb.owlapi.model.IRI dataType = null;
                if (!literal.getLanguageTag().isPresent()) {
                    dataType = createIRI(literal.getDatatype().getIRIString());
                }
                return new RDFLiteral(literal.getLexicalForm(),
                    literal.getLanguageTag().orElse(null), dataType);
            }
            throw new IllegalArgumentException(
                "Unsupported type: " + verifyNotNull(term).getClass());
        }

        @Override
        public Graph createGraph() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Dataset createDataset() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Quad createQuad(BlankNodeOrIRI graphName, BlankNodeOrIRI subject, IRI predicate,
            RDFTerm object) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
