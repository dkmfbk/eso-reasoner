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
        final String inputFile = "[path to trig file]";
        final String ontologyFile = "[path to ESO owl file]";
        final String outputFile = "[path to output file]";
        TQL.register();

        final String cmd = String.format("@read %s @reformattime @filtertype @esoreasoner %s @write %s", inputFile, ontologyFile, outputFile);

        RDFProcessors.parse(true, cmd).apply(RDFSources.NIL, RDFHandlers.NIL, 1);
    }

}
