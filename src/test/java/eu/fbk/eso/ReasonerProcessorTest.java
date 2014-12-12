package eu.fbk.eso;

import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessors;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.tql.TQL;
import org.junit.Test;
import org.openrdf.rio.RDFHandlerException;

public class ReasonerProcessorTest {

    @Test
    public void test() throws RDFHandlerException {
        final String inputFile = "/Users/alessio/Documents/newsreader/sem.trig";
        final String ontologyFile = "/Users/alessio/Documents/newsreader/ESO.owl";
        final String outputFile = "/tmp/output.tql";
        TQL.register();

        final String cmd = String.format("@read %s @reformattime @filtertype @esoreasoner %s @write %s", inputFile, ontologyFile, outputFile);
//        final String cmd = String.format("@read %s @filtertype @write %s", inputFile, outputFile);

        RDFProcessors.parse(true, cmd).apply(RDFSources.NIL, RDFHandlers.NIL, 1);
    }

}
