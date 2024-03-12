package org.obolibrary.oboformat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.writer.OBOFormatWriter;

/**
 * Tests for {@link OBOFormatWriter}.
 */
class OBOFormatWriterTestCase extends OboFormatTestBasics {

    private static List<Clause> createSynonymClauses(String... labels) {
        List<Clause> clauses = new ArrayList<>(labels.length);
        for (String label : labels) {
            Clause clause = new Clause(OboFormatTag.TAG_SYNONYM, label);
            clauses.add(clause);
        }
        return clauses;
    }

    private static String writeObsolete(Object value) throws Exception {
        Clause cl = new Clause(OboFormatTag.TAG_IS_OBSELETE);
        cl.addValue(value);
        StringWriter out = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(out)) {
            OBOFormatWriter.write(cl, bufferedWriter, null);
        }
        return out.toString().trim();
    }

    /**
     * Test a special case of the specification. For intersections put the genus before the
     * differentia, instead of the default case-insensitive alphabetical ordering.
     */
    @Test
    void testSortTermClausesIntersectionOf() {
        OBODoc oboDoc = parseOBOFile("equivtest.obo");
        Frame frame = oboDoc.getTermFrame("X:1");
        assert frame != null;
        List<Clause> clauses = new ArrayList<>(frame.getClauses(OboFormatTag.TAG_INTERSECTION_OF));
        OBOFormatWriter.sortTermClauses(clauses);
        assertEquals("Y:1", clauses.get(0).getValue());
        assertEquals("R:1", clauses.get(1).getValue());
        assertEquals("Z:1", clauses.get(1).getValue2());
    }

    /**
     * Test for sorting clauses according to alphabetical case-insensitive order. Prefer upper-case
     * over lower case for equal strings. Prefer shorter strings over longer strings.
     */
    @Test
    void testSortTermClausesSynonyms() {
        List<Clause> clauses = createSynonymClauses("cc", "ccc", "AAA", "aaa", "bbbb");
        OBOFormatWriter.sortTermClauses(clauses);
        assertEquals("AAA", clauses.get(0).getValue());
        assertEquals("aaa", clauses.get(1).getValue());
        assertEquals("bbbb", clauses.get(2).getValue());
        assertEquals("cc", clauses.get(3).getValue());
        assertEquals("ccc", clauses.get(4).getValue());
    }

    @Test
    void testWriteObsolete() throws Exception {
        assertEquals("", writeObsolete(Boolean.FALSE));
        assertEquals("", writeObsolete(Boolean.FALSE.toString()));
        assertEquals("is_obsolete: true", writeObsolete(Boolean.TRUE));
        assertEquals("is_obsolete: true", writeObsolete(Boolean.TRUE.toString()));
    }

    /**
     * Test that the OBO format writer only writes one new-line at the end of the file.
     */
    @Test
    void testWriteEndOfFile() throws Exception {
        OBODoc oboDoc = parseOBOFile("caro.obo");
        String oboString = renderOboToString(oboDoc);
        int length = oboString.length();
        assertTrue(length > 0);
        int newLineCount = 0;
        for (int i = length - 1; i >= 0; i--) {
            char c = oboString.charAt(i);
            if (Character.isWhitespace(c)) {
                if (c == '\n') {
                    newLineCount++;
                }
            } else {
                break;
            }
        }
        assertEquals(2, newLineCount, "GO always had an empty newline at the end.");
    }

    @Test
    void testWriteOpaqueIdsAsComments() throws Exception {
        OBODoc oboDoc = parseOBOFile("opaque_ids_test.obo");
        String oboString = renderOboToString(oboDoc);
        assertTrue(Arrays.stream(oboString.split("\n")).anyMatch(
            line -> line.startsWith("relationship:") && line.contains("named relation y1")));
    }

    @Test
    void testPropertyValueOrder() throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(getFile("tag_order_test.obo"));
            InputStreamReader in = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(in);) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        }
        String input = sb.toString();
        OBODoc obodoc = parseOboToString(input);
        String written = renderOboToString(obodoc);
        assertEquals(input, written);
    }
}
