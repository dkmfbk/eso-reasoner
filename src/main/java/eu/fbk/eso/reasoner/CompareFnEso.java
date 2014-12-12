package eu.fbk.eso.reasoner;

import eu.fbk.eso.reasoner.util.CommandLineWithLogger;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.RDFHandlerBase;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by alessio on 13/11/14.
 */

public class CompareFnEso {

	static Logger logger = Logger.getLogger(CompareFnEso.class.getName());

	public static void main(String[] args) {
		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();

		commandLineWithLogger.addOption(OptionBuilder.withDescription("TriG file").isRequired().hasArg().withArgName("filename").withLongOpt("trig").create("t"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("OWL file").isRequired().hasArg().withArgName("filename").withLongOpt("ontology").create("o"));

		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		String trigFileName = commandLine.getOptionValue("trig");
		String ontologyFileName = commandLine.getOptionValue("ontology");

		logger.info("Loading ontology");
		Model esoModel = EsoOntology.loadModel(ontologyFileName);

//		Model isaModel = esoModel.filter(null, ESO.subclassOf, null);
//		for (Statement s : isaModel) {
//			if (!s.getObject().stringValue().startsWith("http://www.newsreader-project.eu/domain-ontology")) {
//				continue;
//			}
//			System.out.println(s);
//		}
//		System.exit(1);

		HashMap<String, SameAs> clusters = new HashMap<>();
		HashMap<String, SameAs> fnClusters = new HashMap<>();

		Model fnModel = esoModel.filter(null, ESO.correspondToFrameNetFrame, null);
		for (Statement s : fnModel) {
			if (!clusters.containsKey(s.getSubject().stringValue())) {
				clusters.put(s.getSubject().stringValue(), new SameAs());
				clusters.get(s.getSubject().stringValue()).add(SameAs.ESO, s.getSubject().stringValue());
			}
			if (!fnClusters.containsKey(s.getObject().stringValue())) {
				fnClusters.put(s.getObject().stringValue(), new SameAs());
				fnClusters.get(s.getObject().stringValue()).add(SameAs.FRAMENET, s.getObject().stringValue());
			}
			fnClusters.get(s.getObject().stringValue()).add(SameAs.ESO, s.getSubject().stringValue());
			clusters.get(s.getSubject().stringValue()).add(SameAs.FRAMENET, s.getObject().stringValue());
		}

//		for (String c : fnClusters.keySet()) {
//			System.out.println(c);
//		}

		logger.info("Loading TriGs");
		Model trigs = EsoOntology.loadModel(trigFileName);

		Model events = trigs.filter(null, ESO.a, ESO.event);
		for (Statement event : events) {
			Model infoEvent = trigs.filter(event.getSubject(), ESO.a, null);

			HashSet<String> fnValues = new HashSet<>();
			HashSet<String> esoValues = new HashSet<>();
			HashSet<String> shouldHaveEsoValues = new HashSet<>();

			// Looking for FrameNet events
			for (Statement s : infoEvent) {
				if (s.getObject().stringValue().startsWith("http://www.newsreader-project.eu/framenet#")) {
					fnValues.add(s.getObject().stringValue());
				}
				if (s.getObject().stringValue().startsWith("http://www.newsreader-project.eu/domain-ontology")) {
					esoValues.add(s.getObject().stringValue());
				}
			}

			if (fnValues.size() == 0 && esoValues.size() == 0) {
				continue;
			}

			System.out.println();
			System.out.println("===");
			System.out.println("Event: " + event.getSubject());
			System.out.println("FNs: " + fnValues);
			System.out.println("ESOs:" + esoValues);

//			boolean one = false;
			for (String fnValue : fnValues) {
				if (fnClusters.get(fnValue) == null) {
					continue;
				}
//				if (!one) {
//					one = true;
//				}
				System.out.println("FN: " + fnValue);
				System.out.println("ESO: " + fnClusters.get(fnValue).getValues(SameAs.ESO));
				shouldHaveEsoValues.addAll(fnClusters.get(fnValue).getValues(SameAs.ESO));
			}

//			if (one) {
//				System.out.println();
//			}

			HashSet<String> missingEsos = new HashSet<>(shouldHaveEsoValues);
			missingEsos.removeAll(esoValues);
			HashSet<String> overEsos = new HashSet<>(esoValues);
			overEsos.removeAll(shouldHaveEsoValues);

			if (missingEsos.size() > 0) {
				System.out.println("Missing: " + missingEsos);
			}
			if (overEsos.size() > 0) {
				System.out.println("Over: " + overEsos);
			}
		}

//		Model events = trigs.filter(null, URI.create("a"), URI.create("http://semanticweb.cs.vu.nl/2009/11/sem/Event"));
//
//		for (Resource esoClass : clusters.keySet()) {
//			System.out.println("Class: " + esoClass);
//			Model esoFilter = trigs.filter(esoClass, null, null);
//			boolean found = false;
//			for (Statement s : esoFilter) {
//				System.out.println(s);
//			}
//			System.out.println();
//		}

//		System.out.println(clusters);

//		final TriGExtractor extractor = new TriGExtractor();
//		final RDFFormat format = RDFFormat.forFileName(trigFileName);
//		final RDFParser parser = Rio.createParser(format);
//		parser.setRDFHandler(extractor); // parser now configured
//
//		File file = new File(trigFileName);
//
//		try {
//			final InputStream stream = new BufferedInputStream(new FileInputStream(file));
//			try {
//				parser.parse(stream, ""); // base URI = "" - doesn't matter
//			} catch (final RDFParseException ex) {
//				System.err.println("Oh no! There is an error in the ontology!");
//				System.exit(15);
//			} catch (final RDFHandlerException ex) {
//				System.err.println("Oh no! There is a bug in the extractor");
//				System.exit(18);
//			} finally {
//				stream.close();
//			}
//		} catch (final IOException ex) {
//			System.err.println("Oh no! Cannot read from " + file);
//			System.exit(64);
//		}

	}

	private static class TriGExtractor extends RDFHandlerBase {

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
