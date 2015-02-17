package eu.fbk.eso;

import com.google.common.html.HtmlEscapers;
import eu.fbk.eso.reasoner.ESO;
import eu.fbk.eso.reasoner.util.CommandLineWithLogger;
import eu.fbk.rdfpro.util.Namespaces;
import eu.fbk.rdfpro.util.Statements;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.StatementCollector;
import org.w3c.tidy.Tidy;

import java.io.*;
import java.util.HashSet;

/**
 * Created by alessio on 13/02/15.
 */

public class ReasonerAPI {

	static Logger logger = Logger.getLogger(ReasonerAPI.class.getName());
	static String DEFAULT_UL_ID = "navigation";
	static String DEFAULT_DIV_ID = "navigation-content";
	static Integer MAX_EVENTS = 100;

	public static String prettyPrint(Value value) {
		return HtmlEscapers.htmlEscaper().escape(Statements.formatValue(value, Namespaces.DEFAULT));
	}

	public ReasonerAPI(String inputFile, String outputFile, boolean includeNoSituations, boolean showDetails) throws IOException, RDFParseException, RDFHandlerException {

		FileInputStream fileinputstream = new FileInputStream(inputFile);
		InputStreamReader inputstreamreader = new InputStreamReader(fileinputstream);

		RDFParser rdfParser = Rio.createParser(RDFFormat.NQUADS);
		Model myModel = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(myModel));

		ParserConfig config = rdfParser.getParserConfig();
		config.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		rdfParser.setParserConfig(config);

		rdfParser.parse(inputstreamreader, "");

		inputstreamreader.close();
		fileinputstream.close();

		Model eventModel = myModel.filter(null, RDF.TYPE, SEM.EVENT);
		HashSet<Resource> events = new HashSet<>();

		for (Statement statement : eventModel) {
			if (events.contains(statement.getSubject())) {
				continue;
			}
			events.add(statement.getSubject());
		}

		StringBuffer output = new StringBuffer();
		output.append("<div id='");
		output.append(DEFAULT_DIV_ID);
		output.append("'>");

		if (includeNoSituations) {
			output.append("<p>");
			output.append("Number of events: " + events.size() + " - Showing: " + Math.min(events.size(), MAX_EVENTS));
			output.append("</p>");
		}

		output.append("<ul id='");
		output.append(DEFAULT_UL_ID);
		output.append("'>");

		int i = 0;

		for (Resource event : events) {

			StringBuffer situations = new StringBuffer();
			int situationNo = 0;

			Model singleEventModel = myModel.filter(event, null, null);
			if (singleEventModel.size() > 0) {
				situations.append("<ul>");
				for (Statement statement : singleEventModel) {
					situations.append("<li>");
					if (statement.getPredicate().equals(ESO.hasPreSituation) ||
							statement.getPredicate().equals(ESO.hasDuringSituation) ||
							statement.getPredicate().equals(ESO.hasPostSituation)) {
						situationNo++;
						
						String cssClass = "";
						if (statement.getPredicate().equals(ESO.hasPreSituation)) {
							cssClass = "pre-situation";
						}
						if (statement.getPredicate().equals(ESO.hasPostSituation)) {
							cssClass = "post-situation";
						}
						if (statement.getPredicate().equals(ESO.hasDuringSituation)) {
							cssClass = "during-situation";
						}
						situations.append("<span class='");
						situations.append(cssClass);
						situations.append("'>");
						situations.append(prettyPrint(statement.getPredicate()));
						situations.append(" ");
						situations.append(prettyPrint(statement.getObject()));
						situations.append("</span>");
						situations.append("<ul>");

						Model situationModel;

						situationModel = myModel.filter((Resource) statement.getObject(), null, null);
						for (Statement stSit : situationModel) {
							situations.append("<li>");
							situations.append("<span class='predicate'>");
							situations.append(prettyPrint(stSit.getPredicate()));
							situations.append("</span>");
							situations.append(" ");
							situations.append(prettyPrint(stSit.getObject()));
							situations.append("</li>");
						}
						situationModel = myModel.filter(null, null, null, (Resource) statement.getObject());
						for (Statement stSit : situationModel) {
							situations.append("<li>");
							situations.append("<span class='subject'>");
							situations.append(prettyPrint(stSit.getSubject()));
							situations.append("</span>");
							situations.append(" ");
							situations.append("<span class='predicate'>");
							situations.append(prettyPrint(stSit.getPredicate()));
							situations.append("</span>");
							situations.append(" ");
							situations.append("<span class='object'>");
							situations.append(prettyPrint(stSit.getObject()));
							situations.append("</span>");
							situations.append("</li>");
						}

						situations.append("</ul>");
					}
					else {
						String predicate = prettyPrint(statement.getPredicate());
						String object = prettyPrint(statement.getObject());

						if (!showDetails && statement.getPredicate().equals(RDF.TYPE) && !object.startsWith("eso:")) {
							continue;
						}
						if (!showDetails && predicate.startsWith("&lt;")) {
							continue;
						}
						if (!showDetails && predicate.startsWith("sem:") && !predicate.startsWith("sem:hasTime")) {
							continue;
						}

						situations.append("<span>");
						situations.append("<span class='predicate'>");
						situations.append(predicate);
						situations.append("</span>");
						situations.append(" ");
						situations.append("<span class='object'>");
						situations.append(object);
						situations.append("</span>");
						situations.append("</span>");
					}
					situations.append("</li>");
				}
				situations.append("</ul>");
			}

			if (situationNo == 0 && !includeNoSituations) {
				continue;
			}

			i++;
			if (i > MAX_EVENTS) {
				break;
			}

			output.append("<li><span>");
			output.append(prettyPrint(event));
			output.append(" [");
			output.append(situationNo);
			output.append("]");
			output.append("</span>");
			output.append(situations);
			output.append("</li>");
		}

		output.append("</ul>");
		output.append("</div>");

		InputStream is = new ByteArrayInputStream(output.toString().getBytes());
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

		Tidy tidy = new Tidy();
		tidy.setSmartIndent(true);
		tidy.parse(is, writer);

		is.close();
		writer.close();
	}

	public static void main(String[] args) {
		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();

		commandLineWithLogger.addOption(OptionBuilder.withDescription("Input file").isRequired().hasArg().withArgName("file").withLongOpt("input").create("i"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("Output file").isRequired().hasArg().withArgName("file").withLongOpt("output").create("w"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("Include events without situations").withLongOpt("all-events").create("a"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("Show complete details").withLongOpt("details").create("d"));

		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		final String inputFile = commandLine.getOptionValue("input");
		final String outputFile = commandLine.getOptionValue("output");
		boolean includeNoSituations = commandLine.hasOption("all-events");
		boolean details = commandLine.hasOption("details");

		try {
			new ReasonerAPI(inputFile, outputFile, includeNoSituations, details);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}
