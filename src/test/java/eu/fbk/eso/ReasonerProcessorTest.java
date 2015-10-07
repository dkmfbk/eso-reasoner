package eu.fbk.eso;

import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessors;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.tql.TQL;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.rio.RDFHandlerException;

public class ReasonerProcessorTest {

    @Test
    @Ignore
    public void test() throws RDFHandlerException {
        final String inputFile = "/Users/alessio/Desktop/eso/events/all.ok.trig";
        final String ontologyFile = "/Users/alessio/Documents/scripts/eso/ESO_V2_Final.owl";
        final String outputFile = "/tmp/out.tql";
        TQL.register();

        final String cmd = String.format("@read %s @reformattime @filtertype @esoreasoner -i %s @write %s", inputFile, ontologyFile, outputFile);

        RDFProcessors.parse(true, cmd).apply(RDFSources.NIL, RDFHandlers.NIL, 1);
    }

    @Test
    @Ignore
    public void testNaf2Sem() throws RDFHandlerException {
//        final String inputFile = "/Users/alessio/Desktop/trig/events/dest.trig";
//        final String inputFile = "/Users/alessio/Desktop/trig/events/contextualEvent/e-1995-08/sem.trig";
        final String inputFile = "/tmp/prova.trig";
        final String ontologyFile = "/Users/alessio/Documents/scripts/eso/ESO_V2_Final.owl";
        final String tempFolder = "/tmp/";
        final String ksAddress = "http://localhost:9058/";
        TQL.register();

        final String cmd = String.format("@read %s @naf2sem -b %s -k %s -o %s", inputFile, tempFolder, ksAddress, ontologyFile);
//        final String cmd = String.format("@read %s @esoreasoner %s", inputFile, ontologyFile);
//        final String cmd = String.format("@read %s", inputFile);

        RDFProcessors.parse(true, cmd).apply(RDFSources.NIL, RDFHandlers.NIL, 1);
    }

}
