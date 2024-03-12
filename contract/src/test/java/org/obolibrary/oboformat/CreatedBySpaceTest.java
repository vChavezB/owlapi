package org.obolibrary.oboformat;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.model.IRI;

public class CreatedBySpaceTest extends OboFormatTestBasics {

    @Test
    public void testCreatedByWithSpace() {
        String input = "ontology: test\n[Typedef]\nid: R:1\nname: r1\ncreated_by: John Doe";
        loadOntologyFromString(input, IRI.generateDocumentIRI(), new OBODocumentFormat());
    }
}
