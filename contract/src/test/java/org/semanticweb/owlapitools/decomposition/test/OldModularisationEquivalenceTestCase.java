package org.semanticweb.owlapitools.decomposition.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asList;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.apitest.TestFiles;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.atomicdecomposition.AtomicDecomposition;
import uk.ac.manchester.cs.atomicdecomposition.AtomicDecompositionImpl;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

class OldModularisationEquivalenceTestCase extends TestBase {

    static final String DRY_EUCALYPT_FOREST = "DryEucalyptForest";
    static final String QUOKKA = "Quokka";
    static final String STUDENT = "Student";
    static final String KOALA2 = "Koala";
    static final String MALE_STUDENT_WITH3_DAUGHTERS = "MaleStudentWith3Daughters";
    static final String KOALA_WITH_PHD = "KoalaWithPhD";
    static final String TASMANIAN_DEVIL = "TasmanianDevil";
    static final String GRADUATE_STUDENT = "GraduateStudent";
    static final String RAINFOREST = "Rainforest";
    static final String KOALA = "<?xml version=\"1.0\"?>\n"
        + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns=\"http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#\" xml:base=\"http://protege.stanford.edu/plugins/owl/owl-library/koala.owl\">\n"
        + "  <owl:Ontology rdf:about=\"\"/>\n"
        + "  <owl:Class rdf:ID=\"Female\"><owl:equivalentClass><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty><owl:hasValue><Gender rdf:ID=\"female\"/></owl:hasValue></owl:Restriction></owl:equivalentClass></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Marsupials\"><owl:disjointWith><owl:Class rdf:about=\"#Person\"/></owl:disjointWith><rdfs:subClassOf><owl:Class rdf:about=\"#Animal\"/></rdfs:subClassOf></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Student\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Person\"/><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:hasValue></owl:Restriction><owl:Restriction><owl:someValuesFrom><owl:Class rdf:about=\"#University\"/></owl:someValuesFrom><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"KoalaWithPhD\"><owl:versionInfo>1.2</owl:versionInfo><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Restriction><owl:hasValue><Degree rdf:ID=\"PhD\"/></owl:hasValue><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasDegree\"/></owl:onProperty></owl:Restriction><owl:Class rdf:about=\"#Koala\"/></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"University\"><rdfs:subClassOf><owl:Class rdf:ID=\"Habitat\"/></rdfs:subClassOf></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Koala\"><rdfs:subClassOf><owl:Restriction><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">false</owl:hasValue><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf><owl:Restriction><owl:someValuesFrom><owl:Class rdf:about=\"#DryEucalyptForest\"/></owl:someValuesFrom><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Animal\"><rdfs:seeAlso>Male</rdfs:seeAlso><rdfs:subClassOf><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty><owl:minCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:minCardinality></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf><owl:Restriction><owl:cardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:cardinality><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><owl:versionInfo>1.1</owl:versionInfo></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Forest\"><rdfs:subClassOf rdf:resource=\"#Habitat\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Rainforest\"><rdfs:subClassOf rdf:resource=\"#Forest\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"GraduateStudent\"><rdfs:subClassOf><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasDegree\"/></owl:onProperty><owl:someValuesFrom><owl:Class><owl:oneOf rdf:parseType=\"Collection\"><Degree rdf:ID=\"BA\"/><Degree rdf:ID=\"BS\"/></owl:oneOf></owl:Class></owl:someValuesFrom></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Student\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Parent\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Animal\"/><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty><owl:minCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:minCardinality></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass><rdfs:subClassOf rdf:resource=\"#Animal\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"DryEucalyptForest\"><rdfs:subClassOf rdf:resource=\"#Forest\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Quokka\"><rdfs:subClassOf><owl:Restriction><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:hasValue><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"TasmanianDevil\"><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"MaleStudentWith3Daughters\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Student\"/><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty><owl:hasValue><Gender rdf:ID=\"male\"/></owl:hasValue></owl:Restriction><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty><owl:cardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</owl:cardinality></owl:Restriction><owl:Restriction><owl:allValuesFrom rdf:resource=\"#Female\"/><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Degree\"/>\n  <owl:Class rdf:ID=\"Gender\"/>\n"
        + "  <owl:Class rdf:ID=\"Male\"><owl:equivalentClass><owl:Restriction><owl:hasValue rdf:resource=\"#male\"/><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty></owl:Restriction></owl:equivalentClass></owl:Class>\n"
        + "  <owl:Class rdf:ID=\"Person\"><rdfs:subClassOf rdf:resource=\"#Animal\"/><owl:disjointWith rdf:resource=\"#Marsupials\"/></owl:Class>\n"
        + "  <owl:ObjectProperty rdf:ID=\"hasHabitat\"><rdfs:range rdf:resource=\"#Habitat\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:ObjectProperty>\n"
        + "  <owl:ObjectProperty rdf:ID=\"hasDegree\"><rdfs:domain rdf:resource=\"#Person\"/><rdfs:range rdf:resource=\"#Degree\"/></owl:ObjectProperty>\n"
        + "  <owl:ObjectProperty rdf:ID=\"hasChildren\"><rdfs:range rdf:resource=\"#Animal\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:ObjectProperty>\n"
        + "  <owl:FunctionalProperty rdf:ID=\"hasGender\"><rdfs:range rdf:resource=\"#Gender\"/><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#ObjectProperty\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:FunctionalProperty>\n"
        + "  <owl:FunctionalProperty rdf:ID=\"isHardWorking\"><rdfs:range rdf:resource=\"http://www.w3.org/2001/XMLSchema#boolean\"/><rdfs:domain rdf:resource=\"#Person\"/><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#DatatypeProperty\"/></owl:FunctionalProperty>\n"
        + "  <Degree rdf:ID=\"MA\"/>\n</rdf:RDF>";
    static String ns = "http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#";
    static OWLDataFactory f = OWLManager.getOWLDataFactory();

    static Set<OWLEntity> l(String... s) {
        return asSet(Stream.of(s).map(st -> f.getOWLClass(ns, st)), OWLEntity.class);
    }

    static List<Set<OWLEntity>> params() {
        List<Set<OWLEntity>> l = new ArrayList<>();
        l.add(l("Person"));
        l.add(l("Habitat"));
        l.add(l("Forest"));
        l.add(l("Degree"));
        l.add(l("Parent"));
        l.add(l(GRADUATE_STUDENT));
        l.add(l(RAINFOREST));
        l.add(l("Marsupials"));
        l.add(l(KOALA_WITH_PHD));
        l.add(l(TASMANIAN_DEVIL));
        l.add(l("University"));
        l.add(l("Animal"));
        l.add(l("Male"));
        l.add(l(MALE_STUDENT_WITH3_DAUGHTERS));
        l.add(l("Female"));
        l.add(l(KOALA2));
        l.add(l(STUDENT));
        l.add(l(QUOKKA));
        l.add(l("Gender"));
        l.add(l(DRY_EUCALYPT_FOREST));
        l.add(l(GRADUATE_STUDENT, KOALA2, KOALA_WITH_PHD, MALE_STUDENT_WITH3_DAUGHTERS, "Person",
            QUOKKA, STUDENT));
        l.add(l(DRY_EUCALYPT_FOREST, "Forest", "Habitat", KOALA2, KOALA_WITH_PHD, QUOKKA,
            RAINFOREST, "University"));
        l.add(l(DRY_EUCALYPT_FOREST, "Forest", KOALA2, KOALA_WITH_PHD, QUOKKA, RAINFOREST));
        l.add(l("Degree", KOALA2, KOALA_WITH_PHD, QUOKKA));
        l.add(l(KOALA2, KOALA_WITH_PHD, MALE_STUDENT_WITH3_DAUGHTERS, "Parent", QUOKKA));
        l.add(l(GRADUATE_STUDENT, KOALA2, KOALA_WITH_PHD, QUOKKA));
        l.add(l(KOALA2, KOALA_WITH_PHD, QUOKKA, RAINFOREST));
        l.add(l(KOALA2, KOALA_WITH_PHD, "Marsupials", QUOKKA, TASMANIAN_DEVIL));
        l.add(l(KOALA2, KOALA_WITH_PHD, QUOKKA));
        l.add(l(KOALA2, KOALA_WITH_PHD, QUOKKA, TASMANIAN_DEVIL));
        l.add(l(KOALA2, KOALA_WITH_PHD, QUOKKA, "University"));
        l.add(l("Animal", "Female", GRADUATE_STUDENT, KOALA2, KOALA_WITH_PHD, "Male",
            MALE_STUDENT_WITH3_DAUGHTERS, "Marsupials", "Parent", "Person", QUOKKA, STUDENT,
            TASMANIAN_DEVIL));
        l.add(l(KOALA2, KOALA_WITH_PHD, "Male", MALE_STUDENT_WITH3_DAUGHTERS, QUOKKA));
        l.add(l(KOALA2, KOALA_WITH_PHD, MALE_STUDENT_WITH3_DAUGHTERS, QUOKKA));
        l.add(l("Female", KOALA2, KOALA_WITH_PHD, QUOKKA));
        l.add(l(KOALA2, KOALA_WITH_PHD, QUOKKA));
        l.add(l(GRADUATE_STUDENT, KOALA2, KOALA_WITH_PHD, MALE_STUDENT_WITH3_DAUGHTERS, QUOKKA,
            STUDENT));
        l.add(l(KOALA2, KOALA_WITH_PHD, QUOKKA));
        l.add(l("Gender", KOALA2, KOALA_WITH_PHD, QUOKKA));
        l.add(l(DRY_EUCALYPT_FOREST, KOALA2, KOALA_WITH_PHD, QUOKKA));
        return l;
    }

    @ParameterizedTest
    @MethodSource("params")
    @Disabled
    void testModularizationWithAtomicDecompositionStar(Set<OWLEntity> signature)
        throws OWLException {
        OWLOntology o = loadOntologyFromString(TestFiles.KOALA, new RDFXMLDocumentFormat());
        List<OWLAxiom> module1 =
            asList(getADModule1(o, signature, ModuleType.STAR).stream().sorted());
        List<OWLAxiom> module2 = asList(getTraditionalModule(m, o, signature, ModuleType.STAR)
            .stream().filter(ax -> ax.isLogicalAxiom()).sorted());
        makeAssertion(module1, module2);
    }

    @ParameterizedTest
    @MethodSource("params")
    void testModularizationWithAtomicDecompositionTop(Set<OWLEntity> signature)
        throws OWLException {
        OWLOntology o = loadOntologyFromString(TestFiles.KOALA, new RDFXMLDocumentFormat());
        List<OWLAxiom> module1 =
            asList(getADModule1(o, signature, ModuleType.TOP).stream().sorted());
        List<OWLAxiom> module2 = asList(getTraditionalModule(m, o, signature, ModuleType.TOP)
            .stream().filter(ax -> ax.isLogicalAxiom()).sorted());
        makeAssertion(module1, module2);
    }

    @ParameterizedTest
    @MethodSource("params")
    void testModularizationWithAtomicDecompositionBottom(Set<OWLEntity> signature)
        throws OWLException {
        OWLOntology o = loadOntologyFromString(TestFiles.KOALA, new RDFXMLDocumentFormat());
        List<OWLAxiom> module1 =
            asList(getADModule1(o, signature, ModuleType.BOT).stream().sorted());
        List<OWLAxiom> module2 = asList(getTraditionalModule(m, o, signature, ModuleType.BOT)
            .stream().filter(ax -> ax.isLogicalAxiom()).sorted());
        makeAssertion(module1, module2);
    }

    protected void makeAssertion(List<OWLAxiom> module1, List<OWLAxiom> module2) {
        List<OWLAxiom> l = new ArrayList<>(module1);
        module1.removeAll(module2);
        module2.removeAll(l);
        String s1 = module1.toString().replace(ns, "");
        String s2 = module2.toString().replace(ns, "");
        if (!s1.equals(s2)) {
            System.out.println(
                "OldModularisationEquivalenceTestCase.testModularizationWithAtomicDecomposition() \n"
                    + s1 + "\n" + s2);
        }
        assertEquals(s1, s2);
    }

    protected Set<OWLAxiom> getTraditionalModule(OWLOntologyManager man, OWLOntology o,
        Set<OWLEntity> seedSig, ModuleType type) {
        SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(man, o, type);
        return sme.extract(seedSig);
    }

    // protected Set<OWLAxiom> getADModule(OWLOntology o, Set<OWLEntity> sig) {
    // OntologyBasedModularizer om = new OntologyBasedModularizer(o,
    // ModuleMethod.SYNTACTIC_STANDARD);
    // return new HashSet<>(om.getModule(sig.stream(), ModuleType.STAR));
    // }
    protected Set<OWLAxiom> getADModule1(OWLOntology o, Set<OWLEntity> sig, ModuleType mt) {
        AtomicDecomposition ad = new AtomicDecompositionImpl(o, mt, false);
        return asSet(ad.getModule(sig.stream(), false, mt));
    }
}
