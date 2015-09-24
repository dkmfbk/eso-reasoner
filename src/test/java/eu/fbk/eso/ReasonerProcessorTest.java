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

}
