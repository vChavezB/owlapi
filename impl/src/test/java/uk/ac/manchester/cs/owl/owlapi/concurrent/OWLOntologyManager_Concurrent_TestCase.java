package uk.ac.manchester.cs.owl.owlapi.concurrent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.emptyOptional;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.optional;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.ImpendingOWLOntologyChangeListener;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeBroadcastStrategy;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyChangeProgressListener;
import org.semanticweb.owlapi.model.OWLOntologyChangesVetoedListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;
import org.semanticweb.owlapi.model.OWLStorer;
import org.semanticweb.owlapi.model.OWLStorerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLImportsDeclarationImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;

/**
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 13/04/15
 */
class OWLOntologyManager_Concurrent_TestCase {

    private static final String HTTP_OWLAPI = "http://owlapi/";
    private OWLOntologyManager manager;
    private Lock readLock = mock(Lock.class), writeLock = mock(Lock.class);
    private OWLDataFactory dataFactory = mock(OWLDataFactory.class);
    private ReadWriteLock readWriteLock = mock(ReadWriteLock.class);
    private OWLOntology ontology;

    private static OWLOntology notify(int i, InvocationOnMock o, OWLOntology ont) {
        ((OWLOntologyFactory.OWLOntologyCreationHandler) o.getArguments()[i]).ontologyCreated(ont);
        return ont;
    }

    private static IRI mockIRI() {
        return IRI.create("http://owlapi.sourceforge.net/", "stuff");
    }

    @BeforeEach
    void setUp() throws Exception {
        when(readWriteLock.readLock()).thenReturn(readLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
        manager = new OWLOntologyManagerImpl(dataFactory, readWriteLock);
        mockAndAddOntologyFactory();
        mockAndAddOntologyStorer();
        IRI iri = IRI.create(HTTP_OWLAPI, "ont");
        ontology = manager.createOntology(iri);
        manager.setOntologyDocumentIRI(ontology, iri);
        reset(readLock, writeLock, readWriteLock);
    }

    @SuppressWarnings("boxing")
    private void mockAndAddOntologyFactory() throws OWLOntologyCreationException {
        OWLOntologyFactory ontologyFactory = mock(OWLOntologyFactory.class);
        when(ontologyFactory.canCreateFromDocumentIRI(any(IRI.class))).thenReturn(Boolean.TRUE);
        when(ontologyFactory.canAttemptLoading(any(OWLOntologyDocumentSource.class)))
            .thenReturn(Boolean.TRUE);
        final OWLOntology owlOntology = new OWLOntologyImpl(manager, new OWLOntologyID());
        when(ontologyFactory.createOWLOntology(any(OWLOntologyManager.class),
            any(OWLOntologyID.class), any(IRI.class),
            any(OWLOntologyFactory.OWLOntologyCreationHandler.class)))
                .thenAnswer(i -> notify(3, i, owlOntology));
        when(ontologyFactory.loadOWLOntology(any(OWLOntologyManager.class),
            any(OWLOntologyDocumentSource.class),
            any(OWLOntologyFactory.OWLOntologyCreationHandler.class),
            any(OWLOntologyLoaderConfiguration.class))).thenAnswer(i -> notify(2, i, owlOntology));
        manager.setOntologyFactories(Collections.singleton(ontologyFactory));
    }

    @SuppressWarnings("boxing")
    private void mockAndAddOntologyStorer() {
        OWLStorer storer = mock(OWLStorer.class);
        when(storer.canStoreOntology(any(OWLDocumentFormat.class))).thenReturn(Boolean.TRUE);
        OWLStorerFactory storerFactory = mock(OWLStorerFactory.class);
        when(storerFactory.createStorer()).thenReturn(storer);
        manager.setOntologyStorers(Collections.singleton(storerFactory));
    }

    @Test
    void shouldCall_contains_with_readLock() {
        IRI arg0 = mockIRI();
        manager.contains(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_contains_with_readLock_2() {
        OWLOntologyID arg0 = new OWLOntologyID(IRI.create("urn:test:", "ontology"));
        manager.contains(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_contains_with_no_readLock_onAnonymous() {
        // anonymous ontology ids are never contained, no need to engage locks
        OWLOntologyID arg0 = new OWLOntologyID();
        manager.contains(arg0);
        verify(readLock, never()).lock();
        verify(readLock, never()).unlock();
        verify(writeLock, never()).lock();
        verify(writeLock, never()).unlock();
    }

    @Test
    void shouldCall_contains_with_readLock_3() {
        manager.contains(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologies_with_readLock() {
        OWLAxiom arg0 = mock(OWLAxiom.class);
        manager.getOntologies(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_containsVersion_with_readLock() {
        IRI arg0 = mockIRI();
        manager.containsVersion(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getVersions_with_readLock() {
        IRI arg0 = mockIRI();
        manager.getVersions(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologyIDsByVersion_with_readLock() {
        IRI arg0 = mockIRI();
        manager.getOntologyIDsByVersion(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntology_with_readLock() {
        OWLOntologyID arg0 = new OWLOntologyID();
        manager.getOntology(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntology_with_readLock_2() {
        IRI arg0 = mockIRI();
        manager.getOntology(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getImportedOntology_with_readLock() {
        OWLImportsDeclaration arg0 = new OWLImportsDeclarationImpl(IRI.create(HTTP_OWLAPI, "ont"));
        manager.getImportedOntology(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getDirectImports_with_readLock() {
        manager.getDirectImports(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getImports_with_readLock() {
        manager.getImports(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getImportsClosure_with_readLock() {
        manager.getImportsClosure(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_getSortedImportsClosure_with_readLock() {
        manager.getSortedImportsClosure(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_createOntology_with_writeLock() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        manager.createOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_createOntology_with_writeLock_2() throws OWLOntologyCreationException {
        OWLOntologyID arg0 = new OWLOntologyID();
        manager.createOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_createOntology_with_writeLock_3() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        Set<OWLOntology> arg1 = Collections.emptySet();
        boolean arg2 = true;
        manager.createOntology(arg0, arg1, arg2);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_createOntology_with_writeLock_4() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        Set<OWLOntology> arg1 = Collections.emptySet();
        manager.createOntology(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_createOntology_with_writeLock_5() throws OWLOntologyCreationException {
        manager.createOntology();
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_createOntology_with_writeLock_6() throws OWLOntologyCreationException {
        Set<OWLAxiom> arg0 = Collections.singleton(mock(OWLAxiom.class));
        manager.createOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_createOntology_with_writeLock_7() throws OWLOntologyCreationException {
        Set<OWLAxiom> arg0 = Collections.emptySet();
        IRI arg1 = mockIRI();
        manager.createOntology(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_loadOntology_with_writeLock() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        manager.loadOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_loadOntologyFromOntologyDocument_with_writeLock()
        throws OWLOntologyCreationException {
        OWLOntologyDocumentSource arg0 = mock(OWLOntologyDocumentSource.class);
        when(arg0.getDocumentIRI()).thenReturn(IRI.create(HTTP_OWLAPI, "ontdoc"));
        OWLOntologyLoaderConfiguration arg1 = mock(OWLOntologyLoaderConfiguration.class);
        manager.loadOntologyFromOntologyDocument(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_2()
        throws OWLOntologyCreationException {
        OWLOntologyDocumentSource arg0 = mock(OWLOntologyDocumentSource.class);
        when(arg0.getDocumentIRI()).thenReturn(IRI.create(HTTP_OWLAPI, "ontdoc"));
        manager.loadOntologyFromOntologyDocument(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_3()
        throws OWLOntologyCreationException {
        InputStream arg0 = new StringBufferInputStream("some string");
        manager.loadOntologyFromOntologyDocument(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_4()
        throws OWLOntologyCreationException {
        OWLOntologyDocumentSource source = mock(OWLOntologyDocumentSource.class);
        when(source.getDocumentIRI()).thenReturn(IRI.create(HTTP_OWLAPI, "ontdoc"));
        manager.loadOntologyFromOntologyDocument(source);
        verifyWriteLock_LockUnlock();
    }

    private void verifyWriteLock_LockUnlock() {
        InOrder inOrder = Mockito.inOrder(writeLock, writeLock);
        inOrder.verify(writeLock, atLeastOnce()).lock();
        inOrder.verify(writeLock, atLeastOnce()).unlock();
    }

    @Test
    void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_5()
        throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        manager.loadOntologyFromOntologyDocument(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeOntology_with_writeLock() {
        OWLOntologyID arg0 = mock(OWLOntologyID.class);
        manager.removeOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeOntology_with_writeLock_2() {
        manager.removeOntology(ontology);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologyDocumentIRI_with_readLock() {
        manager.getOntologyDocumentIRI(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_setOntologyDocumentIRI_with_writeLock() {
        IRI arg1 = mockIRI();
        manager.setOntologyDocumentIRI(ontology, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologyFormat_with_readLock() {
        manager.getOntologyFormat(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_setOntologyFormat_with_writeLock() {
        OWLDocumentFormat arg1 = mock(OWLDocumentFormat.class);
        manager.setOntologyFormat(ontology, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_saveOntology_with_writeLock() throws OWLOntologyStorageException {
        OWLDocumentFormat arg1 = mock(OWLDocumentFormat.class);
        IRI arg2 = mockIRI();
        manager.saveOntology(ontology, arg1, arg2);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_saveOntology_with_readLock_2() throws OWLOntologyStorageException {
        OWLDocumentFormat arg1 = mock(OWLDocumentFormat.class);
        OutputStream arg2 = mock(OutputStream.class);
        manager.saveOntology(ontology, arg1, arg2);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_saveOntology_with_readLock_3() throws OWLOntologyStorageException {
        OWLOntologyDocumentTarget arg1 = mock(OWLOntologyDocumentTarget.class);
        manager.setOntologyFormat(ontology, new RDFXMLDocumentFormat());
        verify(writeLock, atLeastOnce()).lock();
        verify(writeLock, atLeastOnce()).unlock();
        manager.saveOntology(ontology, arg1);
        InOrder inOrder = Mockito.inOrder(readLock, readLock);
        inOrder.verify(readLock, atLeastOnce()).lock();
        inOrder.verify(readLock, atLeastOnce()).unlock();
    }

    @Test
    void shouldCall_saveOntology_with_readLock_4() throws OWLOntologyStorageException {
        OWLDocumentFormat arg1 = mock(OWLDocumentFormat.class);
        OWLOntologyDocumentTarget arg2 = mock(OWLOntologyDocumentTarget.class);
        manager.saveOntology(ontology, arg1, arg2);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_saveOntology_with_readLock_5() throws OWLOntologyStorageException {
        manager.setOntologyFormat(ontology, new RDFXMLDocumentFormat());
        verify(writeLock, atLeastOnce()).lock();
        verify(writeLock, atLeastOnce()).unlock();
        manager.saveOntology(ontology);
        InOrder inOrder = Mockito.inOrder(readLock, readLock);
        inOrder.verify(readLock, atLeastOnce()).lock();
        inOrder.verify(readLock, atLeastOnce()).unlock();
    }

    @Test
    void shouldCall_saveOntology_with_readLock_6() throws OWLOntologyStorageException {
        IRI arg1 = mockIRI();
        manager.setOntologyFormat(ontology, new RDFXMLDocumentFormat());
        verify(writeLock, atLeastOnce()).lock();
        verify(writeLock, atLeastOnce()).unlock();
        manager.saveOntology(ontology, arg1);
        InOrder inOrder = Mockito.inOrder(readLock, readLock);
        inOrder.verify(readLock, atLeastOnce()).lock();
        inOrder.verify(readLock, atLeastOnce()).unlock();
    }

    @Test
    void shouldCall_saveOntology_with_readLock_7() throws OWLOntologyStorageException {
        OutputStream arg1 = mock(OutputStream.class);
        manager.setOntologyFormat(ontology, new RDFXMLDocumentFormat());
        verify(writeLock, atLeastOnce()).lock();
        verify(writeLock, atLeastOnce()).unlock();
        manager.saveOntology(ontology, arg1);
        InOrder inOrder = Mockito.inOrder(readLock, readLock);
        inOrder.verify(readLock, atLeastOnce()).lock();
        inOrder.verify(readLock, atLeastOnce()).unlock();
    }

    @Test
    void shouldCall_saveOntology_with_readLock_8() throws OWLOntologyStorageException {
        OWLDocumentFormat arg1 = mock(OWLDocumentFormat.class);
        manager.saveOntology(ontology, arg1);
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_addIRIMapper_with_writeLock() {
        OWLOntologyIRIMapper arg0 = mock(OWLOntologyIRIMapper.class);
        manager.addIRIMapper(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeIRIMapper_with_writeLock() {
        OWLOntologyIRIMapper arg0 = mock(OWLOntologyIRIMapper.class);
        manager.removeIRIMapper(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_clearIRIMappers_with_writeLock() {
        manager.clearIRIMappers();
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addOntologyStorer_with_writeLock() {
        OWLStorerFactory arg0 = mock(OWLStorerFactory.class);
        manager.addOntologyStorer(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeOntologyStorer_with_writeLock() {
        OWLStorerFactory arg0 = mock(OWLStorerFactory.class);
        manager.removeOntologyStorer(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_clearOntologyStorers_with_writeLock() {
        manager.clearOntologyStorers();
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_setIRIMappers_with_writeLock() {
        Set<OWLOntologyIRIMapper> arg0 = Collections.emptySet();
        manager.setIRIMappers(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getIRIMappers_with_readLock() {
        manager.getIRIMappers().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldAddIRIMapper_with_writeLock() {
        manager.getIRIMappers().add(mock(OWLOntologyIRIMapper.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldRemoveIRIMapper_with_writeLock() {
        manager.getIRIMappers().remove(mock(OWLOntologyIRIMapper.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_setOntologyParsers_with_writeLock() {
        Set<OWLParserFactory> arg0 = Collections.emptySet();
        manager.setOntologyParsers(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologyParsers_with_readLock() {
        manager.getOntologyParsers().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldAddOntologyParser_with_writeLock() {
        manager.getOntologyParsers().add(mock(OWLParserFactory.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldRemoveOntologyParser_with_writeLock() {
        manager.getOntologyParsers().remove(mock(OWLParserFactory.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_setOntologyFactories_with_writeLock() {
        Set<OWLOntologyFactory> arg0 = Collections.emptySet();
        manager.setOntologyFactories(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologyFactories_with_readLock() {
        manager.getOntologyFactories().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_setOntologyStorers_with_writeLock() {
        Set<OWLStorerFactory> arg0 = Collections.emptySet();
        manager.setOntologyStorers(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologyStorers_with_readLock() {
        manager.getOntologyStorers().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_addOntologyChangeListener_with_writeLock() {
        OWLOntologyChangeListener arg0 = mock(OWLOntologyChangeListener.class);
        OWLOntologyChangeBroadcastStrategy arg1 = mock(OWLOntologyChangeBroadcastStrategy.class);
        manager.addOntologyChangeListener(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addImpendingOntologyChangeListener_with_writeLock() {
        ImpendingOWLOntologyChangeListener arg0 = mock(ImpendingOWLOntologyChangeListener.class);
        manager.addImpendingOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeImpendingOntologyChangeListener_with_writeLock() {
        ImpendingOWLOntologyChangeListener arg0 = mock(ImpendingOWLOntologyChangeListener.class);
        manager.removeImpendingOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addOntologyChangesVetoedListener_with_writeLock() {
        OWLOntologyChangesVetoedListener arg0 = mock(OWLOntologyChangesVetoedListener.class);
        manager.addOntologyChangesVetoedListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeOntologyChangesVetoedListener_with_writeLock() {
        OWLOntologyChangesVetoedListener arg0 = mock(OWLOntologyChangesVetoedListener.class);
        manager.removeOntologyChangesVetoedListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_setDefaultChangeBroadcastStrategy_with_writeLock() {
        OWLOntologyChangeBroadcastStrategy arg0 = mock(OWLOntologyChangeBroadcastStrategy.class);
        manager.setDefaultChangeBroadcastStrategy(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_makeLoadImportRequest_with_writeLock() {
        OWLImportsDeclaration arg0 = mock(OWLImportsDeclaration.class);
        when(arg0.getIRI()).thenReturn(IRI.create(HTTP_OWLAPI, "other"));
        OWLOntologyLoaderConfiguration arg1 = mock(OWLOntologyLoaderConfiguration.class);
        manager.makeLoadImportRequest(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_makeLoadImportRequest_with_writeLock_2() {
        OWLImportsDeclaration arg0 =
            new OWLImportsDeclarationImpl(IRI.create(HTTP_OWLAPI, "otheront"));
        manager.makeLoadImportRequest(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addMissingImportListener_with_writeLock() {
        MissingImportListener arg0 = mock(MissingImportListener.class);
        manager.addMissingImportListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeMissingImportListener_with_writeLock() {
        MissingImportListener arg0 = mock(MissingImportListener.class);
        manager.removeMissingImportListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addOntologyLoaderListener_with_writeLock() {
        OWLOntologyLoaderListener arg0 = mock(OWLOntologyLoaderListener.class);
        manager.addOntologyLoaderListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeOntologyLoaderListener_with_writeLock() {
        OWLOntologyLoaderListener arg0 = mock(OWLOntologyLoaderListener.class);
        manager.removeOntologyLoaderListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addOntologyChangeProgessListener_with_writeLock() {
        OWLOntologyChangeProgressListener arg0 = mock(OWLOntologyChangeProgressListener.class);
        manager.addOntologyChangeProgessListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeOntologyChangeProgessListener_with_writeLock() {
        OWLOntologyChangeProgressListener arg0 = mock(OWLOntologyChangeProgressListener.class);
        manager.removeOntologyChangeProgessListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologies_with_readLock_2() {
        manager.getOntologies();
        verifyReadLock_LockUnlock();
    }

    @Test
    void shouldCall_applyChanges_with_writeLock() {
        List<OWLOntologyChange> arg0 = Collections.emptyList();
        manager.applyChanges(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_applyChange_with_writeLock() {
        OWLAxiom ax = mock(OWLAxiom.class);
        OWLOntologyChange arg0 = new AddAxiom(ontology, ax);
        manager.applyChange(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addAxioms_with_writeLock() {
        OWLOntology arg0 = mockOntology();
        Set<OWLAxiom> axioms = Collections.singleton(mock(OWLAxiom.class));
        manager.addAxioms(arg0, axioms);
        verifyWriteLock_LockUnlock();
    }

    protected OWLMutableOntology mockOntology() {
        OWLMutableOntology mock = mock(OWLMutableOntology.class);
        when(mock.getOntologyID()).thenReturn(
            new OWLOntologyID(optional(IRI.create("urn:mock:", "ontology")), emptyOptional()));
        return mock;
    }

    @Test
    void shouldCall_addAxiom_with_writeLock() {
        OWLOntology arg0 = mockOntology();
        OWLAxiom arg1 = mock(OWLAxiom.class);
        manager.addAxiom(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeAxioms_with_writeLock() {
        Set<OWLAxiom> arg1 = Collections.emptySet();
        manager.removeAxioms(ontology, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeAxiom_with_writeLock() {
        OWLOntology arg0 = mockOntology();
        OWLAxiom arg1 = mock(OWLAxiom.class);
        manager.removeAxiom(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_addOntologyChangeListener_with_writeLock_2() {
        OWLOntologyChangeListener arg0 = mock(OWLOntologyChangeListener.class);
        manager.addOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_removeOntologyChangeListener_with_writeLock() {
        OWLOntologyChangeListener arg0 = mock(OWLOntologyChangeListener.class);
        manager.removeOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_setOntologyWriterConfiguration_with_writeLock() {
        OWLOntologyWriterConfiguration arg0 = mock(OWLOntologyWriterConfiguration.class);
        manager.setOntologyWriterConfiguration(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_setOntologyLoaderConfiguration_with_writeLock() {
        OWLOntologyLoaderConfiguration arg0 = mock(OWLOntologyLoaderConfiguration.class);
        manager.setOntologyLoaderConfiguration(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    void shouldCall_getOntologyLoaderConfiguration_with_readLock() {
        manager.getOntologyLoaderConfiguration();
        verifyReadLock_LockUnlock();
    }

    private void verifyReadLock_LockUnlock() {
        InOrder inOrder = Mockito.inOrder(readLock, readLock);
        inOrder.verify(readLock, atLeastOnce()).lock();
        inOrder.verify(readLock, atLeastOnce()).unlock();
        verify(writeLock, never()).lock();
        verify(writeLock, never()).unlock();
    }
}
