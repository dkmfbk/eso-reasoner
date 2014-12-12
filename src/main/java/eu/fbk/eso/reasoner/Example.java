package eu.fbk.eso.reasoner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

public class Example {

    public static void main(final String[] args) {

        if (args.length == 0) {
            System.err.println("Syntax: java Example FILENAME");
            System.exit(42);
        }

        final OntologyExtractor extractor = new OntologyExtractor();

        final File file = new File(args[0]);
        final RDFFormat format = RDFFormat.forFileName(file.getName());
        final RDFParser parser = Rio.createParser(format);
        parser.setRDFHandler(extractor); // parser now configured

        try {
            final InputStream stream = new BufferedInputStream(new FileInputStream(file));
            try {
                parser.parse(stream, ""); // base URI = "" - doesn't matter
            } catch (final RDFParseException ex) {
                System.err.println("Oh no! There is an error in the ontology!");
                System.exit(15);
            } catch (final RDFHandlerException ex) {
                System.err.println("Oh no! There is a bug in the extractor");
                System.exit(18);
            } finally {
                stream.close();
            }
        } catch (final IOException ex) {
            System.err.println("Oh no! Cannot read from " + file);
            System.exit(64);
        }
    }

    private static class OntologyExtractor extends RDFHandlerBase {

        private int counter;

        @Override
        public void handleStatement(final Statement st) throws RDFHandlerException {
            // Do whathever you want with the statement, including building an index of the
            // concepts / relations you need from the ontology

            System.out.println(st.toString());
            System.out.println(st.getSubject());
			System.out.println(st.getPredicate());
			System.out.println(st.getObject());
            System.out.println();

			++this.counter;
        }

        @Override
        public void endRDF() throws RDFHandlerException {
            System.out.println("Hey, there are " + this.counter + " triples in the ontology");
        }

    }

}
