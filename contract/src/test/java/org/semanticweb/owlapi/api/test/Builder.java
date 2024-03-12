package org.semanticweb.owlapi.api.test;

import static org.semanticweb.owlapi.api.test.baseclasses.TestBase.set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

public class Builder {

    private static final String SWRL = "urn:swrl:var#";
    private static final String NS = "urn:test:test#";
    private static OWLDataFactory df = OWLManager.getOWLDataFactory();
    private final OWLAnnotationProperty ap = df.getOWLAnnotationProperty(NS, "ann");
    private final OWLObjectProperty op = df.getOWLObjectProperty(NS, "op");
    private final OWLDataProperty dp = df.getOWLDataProperty(NS, "dp");
    private final OWLLiteral lit = df.getOWLLiteral(false);
    private final OWLLiteral plainlit = df.getOWLLiteral("string", "en");
    private final IRI iri = TestBase.iri(NS, "iri");
    private final List<OWLAnnotation> as = set(df.getOWLAnnotation(ap, df.getOWLLiteral("test")));
    private final OWLClass ce = df.getOWLClass(NS, "c");
    private final OWLNamedIndividual i = df.getOWLNamedIndividual(NS, "i");
    private final OWLNamedIndividual j = df.getOWLNamedIndividual(NS, "j");
    private final OWLDatatype d = df.getOWLDatatype(NS, "datatype");
    private final List<OWLDataProperty> dps = Arrays.asList(df.getOWLDataProperty(iri), dp);
    private final List<OWLObjectProperty> ops = Arrays.asList(df.getOWLObjectProperty(iri), op);
    private final List<OWLClass> classes = Arrays.asList(df.getOWLClass(iri), ce);
    private final List<OWLNamedIndividual> inds = Arrays.asList(i, df.getOWLNamedIndividual(iri));
    private final SWRLAtom v1 = df.getSWRLBuiltInAtom(TestBase.iri(SWRL, "v1"), Arrays.asList((SWRLDArgument) df.getSWRLVariable(SWRL, "var3"), df.getSWRLVariable(SWRL, "var4")));
    private final SWRLAtom v2 = df.getSWRLBuiltInAtom(TestBase.iri(SWRL, "v2"), Arrays.asList((SWRLDArgument) df.getSWRLVariable(SWRL, "var5"), df.getSWRLVariable(SWRL, "var6")));
    private final List<SWRLAtom> body = Arrays.asList(v1);
    private final List<SWRLAtom> head = Arrays.asList(v2);
    private final SWRLDArgument var1 = df.getSWRLVariable(SWRL, "var1");
    private final List<SWRLDArgument> var1list = Arrays.asList(var1);
    private final SWRLIArgument var2 = df.getSWRLVariable(SWRL, "var2");
    private final List<SWRLAtom> body2 = Arrays.asList(v1, df.getSWRLClassAtom(ce, var2),
        df.getSWRLDataRangeAtom(d, var1), df.getSWRLBuiltInAtom(iri, var1list),
        df.getSWRLDifferentIndividualsAtom(var2, df.getSWRLIndividualArgument(i)),
        df.getSWRLSameIndividualAtom(var2,
            df.getSWRLIndividualArgument(df.getOWLNamedIndividual(iri))),
        df.getSWRLBuiltInAtom(iri, var1list));
    private final List<SWRLAtom> head2 =
        Arrays.asList(v2, df.getSWRLDataPropertyAtom(dp, var2, df.getSWRLLiteralArgument(lit)),
            df.getSWRLObjectPropertyAtom(op, var2, var2));
    private final OWLOntologyManager m = getManager();

    // no parsers and storers injected
    private static OWLOntologyManager getManager() {
        OWLOntologyManager instance = OWLManager.createOWLOntologyManager();
        instance.getOntologyParsers().clear();
        instance.getOntologyStorers().clear();
        return instance;
    }

    public SWRLRule bigRule() {
        return df.getSWRLRule(body2, head2, as);
    }

    public OWLHasKeyAxiom hasKey() {
        Set<OWLPropertyExpression> set = new HashSet<>();
        set.add(df.getOWLObjectProperty(iri));
        set.add(op);
        set.add(dp);
        return df.getOWLHasKeyAxiom(ce, set, as);
    }

    public OWLSymmetricObjectPropertyAxiom symm() {
        return df.getOWLSymmetricObjectPropertyAxiom(op, as);
    }

    public OWLTransitiveObjectPropertyAxiom trans() {
        return df.getOWLTransitiveObjectPropertyAxiom(op, as);
    }

    public SWRLRule rule() {
        return df.getSWRLRule(body, head);
    }

    public OWLSubObjectPropertyOfAxiom subObject() {
        return df.getOWLSubObjectPropertyOfAxiom(op, df.getOWLTopObjectProperty(), as);
    }

    public OWLSubDataPropertyOfAxiom subData() {
        return df.getOWLSubDataPropertyOfAxiom(dp, df.getOWLTopDataProperty());
    }

    public OWLSubClassOfAxiom subClass() {
        return df.getOWLSubClassOfAxiom(ce, df.getOWLThing(), as);
    }

    public OWLSubAnnotationPropertyOfAxiom subAnn() {
        return df.getOWLSubAnnotationPropertyOfAxiom(ap, df.getRDFSLabel(), as);
    }

    public OWLSameIndividualAxiom same() {
        return df.getOWLSameIndividualAxiom(inds, as);
    }

    public OWLReflexiveObjectPropertyAxiom ref() {
        return df.getOWLReflexiveObjectPropertyAxiom(op, as);
    }

    public OWLSubPropertyChainOfAxiom chain() {
        return df.getOWLSubPropertyChainOfAxiom(new ArrayList<>(ops), op, as);
    }

    public OWLObjectPropertyRangeAxiom oRange() {
        return df.getOWLObjectPropertyRangeAxiom(op, ce, as);
    }

    public OWLObjectPropertyDomainAxiom oDom() {
        return df.getOWLObjectPropertyDomainAxiom(op, ce, as);
    }

    public OWLObjectPropertyAssertionAxiom opaInv() {
        return df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectInverseOf(op), i, i, as);
    }

    public OWLObjectPropertyAssertionAxiom opaInvj() {
        return df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectInverseOf(op), i, j, as);
    }

    public OWLObjectPropertyAssertionAxiom opa() {
        return df.getOWLObjectPropertyAssertionAxiom(op, i, i, as);
    }

    public OWLNegativeObjectPropertyAssertionAxiom nop() {
        return df.getOWLNegativeObjectPropertyAssertionAxiom(op, i, i, as);
    }

    public OWLNegativeDataPropertyAssertionAxiom ndp() {
        return df.getOWLNegativeDataPropertyAssertionAxiom(dp, i, lit, as);
    }

    public OWLIrreflexiveObjectPropertyAxiom irr() {
        return df.getOWLIrreflexiveObjectPropertyAxiom(op, as);
    }

    public OWLInverseObjectPropertiesAxiom iop() {
        return df.getOWLInverseObjectPropertiesAxiom(op, op, as);
    }

    public OWLInverseFunctionalObjectPropertyAxiom ifp() {
        return df.getOWLInverseFunctionalObjectPropertyAxiom(op, as);
    }

    public OWLFunctionalObjectPropertyAxiom fop() {
        return df.getOWLFunctionalObjectPropertyAxiom(op, as);
    }

    public OWLFunctionalDataPropertyAxiom fdp() {
        return df.getOWLFunctionalDataPropertyAxiom(dp, as);
    }

    public OWLEquivalentObjectPropertiesAxiom eOp() {
        return df.getOWLEquivalentObjectPropertiesAxiom(ops, as);
    }

    public OWLEquivalentDataPropertiesAxiom eDp() {
        return df.getOWLEquivalentDataPropertiesAxiom(dps, as);
    }

    public OWLEquivalentClassesAxiom ec() {
        return df.getOWLEquivalentClassesAxiom(classes, as);
    }

    public OWLDisjointUnionAxiom du() {
        return df.getOWLDisjointUnionAxiom(ce, classes, as);
    }

    public OWLDisjointObjectPropertiesAxiom dOp() {
        return df.getOWLDisjointObjectPropertiesAxiom(ops, as);
    }

    public OWLDisjointDataPropertiesAxiom dDp() {
        return df.getOWLDisjointDataPropertiesAxiom(dps, as);
    }

    public OWLDisjointClassesAxiom dc() {
        return df.getOWLDisjointClassesAxiom(ce, df.getOWLClass(iri));
    }

    public OWLDifferentIndividualsAxiom assDi() {
        return df.getOWLDifferentIndividualsAxiom(i, df.getOWLNamedIndividual(iri));
    }

    public OWLDeclarationAxiom decI() {
        return df.getOWLDeclarationAxiom(i, as);
    }

    public OWLDeclarationAxiom decAp() {
        return df.getOWLDeclarationAxiom(ap, as);
    }

    public OWLDeclarationAxiom decDt() {
        return df.getOWLDeclarationAxiom(d, as);
    }

    public OWLDeclarationAxiom decDp() {
        return df.getOWLDeclarationAxiom(dp, as);
    }

    public OWLDeclarationAxiom decOp() {
        return df.getOWLDeclarationAxiom(op, as);
    }

    public OWLDeclarationAxiom decC() {
        return df.getOWLDeclarationAxiom(ce, as);
    }

    public OWLDatatypeDefinitionAxiom dDef() {
        return df.getOWLDatatypeDefinitionAxiom(d, df.getDoubleOWLDatatype(), as);
    }

    public OWLDataPropertyRangeAxiom dRange() {
        return df.getOWLDataPropertyRangeAxiom(dp, d, as);
    }

    public OWLDataPropertyDomainAxiom dDom() {
        return df.getOWLDataPropertyDomainAxiom(dp, ce, as);
    }

    public OWLDataPropertyAssertionAxiom assDPlain() {
        return df.getOWLDataPropertyAssertionAxiom(dp, i, plainlit, as);
    }

    public OWLDataPropertyAssertionAxiom assD() {
        return df.getOWLDataPropertyAssertionAxiom(dp, i, lit, as);
    }

    public OWLDataPropertyRangeAxiom dRangeRestrict() {
        return df.getOWLDataPropertyRangeAxiom(dp,
            df.getOWLDatatypeMinMaxExclusiveRestriction(5.0D, 6.0D), as);
    }

    public OWLDataPropertyRangeAxiom dNot() {
        return df.getOWLDataPropertyRangeAxiom(dp,
            df.getOWLDataComplementOf(df.getOWLDataOneOf(lit)), as);
    }

    public OWLDataPropertyRangeAxiom dOneOf() {
        return df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataOneOf(lit), as);
    }

    public OWLClassAssertionAxiom assDEq() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataExactCardinality(1, dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDMax() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataMaxCardinality(1, dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDMin() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataMinCardinality(1, dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDHas() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataHasValue(dp, lit), i, as);
    }

    public OWLClassAssertionAxiom assDAll() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataAllValuesFrom(dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDSome() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataSomeValuesFrom(dp, d), i, as);
    }

    public OWLClassAssertionAxiom assOneOf() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectOneOf(i), i, as);
    }

    public OWLClassAssertionAxiom assHasSelf() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectHasSelf(op), i, as);
    }

    public OWLClassAssertionAxiom assEq() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectExactCardinality(1, op, ce), i, as);
    }

    public OWLClassAssertionAxiom assMax() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectMaxCardinality(1, op, ce), i, as);
    }

    public OWLClassAssertionAxiom assMin() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectMinCardinality(1, op, ce), i, as);
    }

    public OWLClassAssertionAxiom assMinTop() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectMinCardinality(1, op, df.getOWLThing()),
            i, as);
    }

    public OWLClassAssertionAxiom assHas() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectHasValue(op, i), i, as);
    }

    public OWLClassAssertionAxiom assAll() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectAllValuesFrom(op, ce), i, as);
    }

    public OWLClassAssertionAxiom assSome() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectSomeValuesFrom(op, ce), i, as);
    }

    public OWLClassAssertionAxiom assNotAnon() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectComplementOf(ce),
            df.getOWLAnonymousIndividual("id"), as);
    }

    public OWLClassAssertionAxiom assNot() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectComplementOf(ce), i, as);
    }

    public OWLDataPropertyRangeAxiom dRangeOr() {
        return df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataUnionOf(d, df.getOWLDataOneOf(lit)),
            as);
    }

    public OWLDataPropertyRangeAxiom dRangeAnd() {
        return df.getOWLDataPropertyRangeAxiom(dp,
            df.getOWLDataIntersectionOf(d, df.getOWLDataOneOf(lit)), as);
    }

    public OWLClassAssertionAxiom assOr() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectUnionOf(classes), i, as);
    }

    public OWLClassAssertionAxiom assAnd() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectIntersectionOf(classes), i, as);
    }

    public OWLClassAssertionAxiom ass() {
        return df.getOWLClassAssertionAxiom(ce, i, as);
    }

    public OWLAnnotationPropertyRangeAxiom annRange() {
        return df.getOWLAnnotationPropertyRangeAxiom(ap, iri, as);
    }

    public OWLAnnotationPropertyDomainAxiom annDom() {
        return df.getOWLAnnotationPropertyDomainAxiom(ap, iri, as);
    }

    public OWLAsymmetricObjectPropertyAxiom asymm() {
        return df.getOWLAsymmetricObjectPropertyAxiom(op, as);
    }

    public OWLAnnotationAssertionAxiom ann() {
        return df.getOWLAnnotationAssertionAxiom(ap, iri, lit, as);
    }

    public OWLOntology onto() {
        try {
            return m.createOntology(TestBase.iri(NS, "test"));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public List<OWLAxiom> all() {
        return Arrays.asList(ann(), asymm(), annDom(), annRange(), ass(), assAnd(), assOr(),
            dRangeAnd(), dRangeOr(), assNot(), assNotAnon(), assSome(), assAll(), assHas(),
            assMin(), assMax(), assEq(), assHasSelf(), assOneOf(), assDSome(), assDAll(), assDHas(),
            assDMin(), assDMax(), assDEq(), dOneOf(), dNot(), dRangeRestrict(), assD(), assDPlain(),
            dDom(), dRange(), dDef(), decC(), decOp(), decDp(), decDt(), decAp(), decI(), assDi(),
            dc(), dDp(), dOp(), du(), ec(), eDp(), eOp(), fdp(), fop(), ifp(), iop(), irr(), ndp(),
            nop(), opa(), opaInv(), opaInvj(), oDom(), oRange(), chain(), ref(), same(), subAnn(),
            subClass(), subData(), subObject(), rule(), symm(), trans(), hasKey(), bigRule());
    }
}
