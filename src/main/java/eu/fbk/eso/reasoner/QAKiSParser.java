package eu.fbk.eso.reasoner;

import eu.fbk.eso.reasoner.util.CommandLineWithLogger;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.RDFHandlerBase;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by alessio on 09/12/14.
 */

public class QAKiSParser {

	static Logger logger = Logger.getLogger(QAKiSParser.class.getName());

	class StatementParser extends RDFHandlerBase {

		HashMap<URI, URI> sameAs = new HashMap<>();

		@Override
		public void handleStatement(Statement st) {
			sameAs.put((URI) st.getSubject(), (URI) st.getObject());
		}

		public HashMap<URI, URI> getSameAs() {
			return sameAs;
		}
	}

	class PropertiesParser extends RDFHandlerBase {

		HashSet<URI> haveProperties = new HashSet<>();

		@Override
		public void handleStatement(Statement st) throws RDFHandlerException {
			if (st.getPredicate().equals(new URIImpl("http://xmlns.com/foaf/0.1/name"))) {
				return;
			}
			haveProperties.add((URI) st.getSubject());
		}

		public HashSet<URI> getHaveProperties() {
			return haveProperties;
		}
	}

	public QAKiSParser(String dbpediaFile, String itprop, String enprop) {
		try {

			FileInputStream in;
			RDFParser parser;

			// same As
			in = new FileInputStream(dbpediaFile);
			parser = Rio.createParser(RDFFormat.TURTLE);

			StatementParser statementParser = new StatementParser();
			parser.setRDFHandler(statementParser);
			parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
			parser.getParserConfig().set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false);
			parser.parse(in, "sameAs");
			HashMap<URI, URI> sameAs = statementParser.getSameAs();

			// IT
//			in = new FileInputStream(itprop);
//			parser = Rio.createParser(RDFFormat.TURTLE);
//
//			PropertiesParser it = new PropertiesParser();
//			parser.setRDFHandler(it);
//			parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
//			parser.getParserConfig().set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false);
//			parser.parse(in, "IT");
//			HashSet<URI> itHaveProp = it.getHaveProperties();

			// EN
			in = new FileInputStream(enprop);
			parser = Rio.createParser(RDFFormat.TURTLE);

			PropertiesParser en = new PropertiesParser();
			parser.setRDFHandler(en);
			parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
			parser.getParserConfig().set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false);
			parser.parse(in, "EN");
			HashSet<URI> enHaveProp = en.getHaveProperties();

			System.out.println(sameAs.size());
//			System.out.println(itHaveProp.size());
			System.out.println(enHaveProp.size());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}


	public static void main(String[] args) {
		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();

		commandLineWithLogger.addOption(OptionBuilder.withDescription("DBpedia sameAs file").isRequired().hasArg().withArgName("filename").withLongOpt("sameas").create("s"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("DBpedia IT properties").isRequired().hasArg().withArgName("filename").withLongOpt("itprop").create("i"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("DBpedia EN properties").isRequired().hasArg().withArgName("filename").withLongOpt("enprop").create("e"));

		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		String dbpediaFile = commandLine.getOptionValue("sameas");
		String itprop = commandLine.getOptionValue("itprop");
		String enprop = commandLine.getOptionValue("enprop");
		new QAKiSParser(dbpediaFile, itprop, enprop);
	}
}
