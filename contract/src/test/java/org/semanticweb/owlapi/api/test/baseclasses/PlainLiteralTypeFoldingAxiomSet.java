package org.semanticweb.owlapi.api.test.baseclasses;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * Created by ses on 9/30/14.
 */
public class PlainLiteralTypeFoldingAxiomSet implements Set<OWLAxiom> {

    private final Set<OWLAxiom> delegate = createPlainLiteralTypeFoldingSet();

    /**
     * @param axioms axioms to be used
     */
    public PlainLiteralTypeFoldingAxiomSet(Collection<OWLAxiom> axioms) {
        delegate.addAll(axioms);
    }

    static Set<OWLAxiom> createPlainLiteralTypeFoldingSet() {
        return new HashSet<>();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<OWLAxiom> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(@Nullable T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(@Nullable OWLAxiom owlAxiom) {
        return delegate.add(owlAxiom);
    }

    @Override
    public boolean remove(@Nullable Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(@Nullable Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(@Nullable Collection<? extends OWLAxiom> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean retainAll(@Nullable Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public boolean removeAll(@Nullable Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
