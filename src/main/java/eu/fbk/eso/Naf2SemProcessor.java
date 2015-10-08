package eu.fbk.eso;

import com.google.common.collect.Iterables;
import eu.fbk.knowledgestore.KnowledgeStore;
import eu.fbk.knowledgestore.OperationException;
import eu.fbk.knowledgestore.Session;
import eu.fbk.knowledgestore.client.Client;
import eu.fbk.knowledgestore.data.Stream;
import eu.fbk.rdfpro.*;
import eu.fbk.rdfpro.util.Options;
import org.openrdf.model.*;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.BindingSet;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/*
Queries:

SELECT ?s ?p ?o ?g WHERE {
  BIND($$ AS ?e)
  ?e a sem:Event .
  {
	GRAPH ?g { ?e ?p ?o }
	BIND(?e AS ?s)
  }
  UNION
  {
	GRAPH ?g { ?s ?p ?e }
	BIND(?e AS ?o)
  }
  FILTER (?g != <http://www.newsreader-project.eu/situation-graph>)
}

SELECT ?s ?p ?o ?g WHERE {
  BIND($$ AS ?e)
  ?e a sem:Event
  {
	VALUES ?p { eso:hasPreSituation eso:hasPostSituation eso:hasDuringSituation }
	GRAPH ?g { ?e ?p ?o }
	BIND(?e AS ?s)
  }
  UNION
  {
	?e eso:hasPreSituation|eso:hasPostSituation|eso:hasDuringSituation ?x .
	{
	  GRAPH ?g { ?x ?p ?o }
	  BIND(?x AS ?s)
	}
	UNION
	{
	  GRAPH ?x { ?s ?p ?o }
	  BIND(?x AS ?g)
	}
  }
}

*/
public class Naf2SemProcessor implements RDFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(Naf2SemProcessor.class);
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");

	@Nullable
	private File savePath;
	@Nullable
	private KnowledgeStore ksClient;
	@Nullable
	private String defByGroovy;
	@Nullable
	private String timeLabelGroovy;
	@Nullable
	private File ontology;

	final private String queryDesc = "SELECT ?s ?p ?o ?g WHERE {\n" +
			"  BIND($$ AS ?e)\n" +
			"  ?e a sem:Event .\n" +
			"  {\n" +
			"\tGRAPH ?g { ?e ?p ?o }\n" +
			"\tBIND(?e AS ?s)\n" +
			"  }\n" +
			"  UNION\n" +
			"  {\n" +
			"\tGRAPH ?g { ?s ?p ?e }\n" +
			"\tBIND(?e AS ?o)\n" +
			"  }\n" +
			"  FILTER (?g != <http://www.newsreader-project.eu/situation-graph>)\n" +
			"}";
	final private String querySituation = "SELECT ?s ?p ?o ?g WHERE {\n" +
			"  BIND($$ AS ?e)\n" +
			"  ?e a sem:Event\n" +
			"  {\n" +
			"\tVALUES ?p { eso:hasPreSituation eso:hasPostSituation eso:hasDuringSituation }\n" +
			"\tGRAPH ?g { ?e ?p ?o }\n" +
			"\tBIND(?e AS ?s)\n" +
			"  }\n" +
			"  UNION\n" +
			"  {\n" +
			"\t?e eso:hasPreSituation|eso:hasPostSituation|eso:hasDuringSituation ?x .\n" +
			"\t{\n" +
			"\t  GRAPH ?g { ?x ?p ?o }\n" +
			"\t  BIND(?x AS ?s)\n" +
			"\t}\n" +
			"\tUNION\n" +
			"\t{\n" +
			"\t  GRAPH ?x { ?s ?p ?o }\n" +
			"\t  BIND(?x AS ?g)\n" +
			"\t}\n" +
			"  }\n" +
			"}";

	static Naf2SemProcessor doCreate(String name, final String... args) {

		final Options options = Options.parse("b!|k!|t!|d!|o!", args);

		final String savePath = options.getOptionArg("b", String.class);
		final String ksAddress = options.getOptionArg("k", String.class);
		String defByGroovy = options.getOptionArg("d", String.class);
		String timeLabelGroovy = options.getOptionArg("t", String.class);
		String ontology = options.getOptionArg("o", String.class);

		if (ksAddress == null) {
			throw new IllegalArgumentException("The KnowledgeStore address (-k) is mandatory");
		}

		File trigFolder = null;
		if (savePath != null) {
			trigFolder = new File(savePath);

			if (trigFolder.exists() && trigFolder.isFile()) {
				throw new IllegalArgumentException(String.format("Folder %s is an existing file", savePath));
			}

			if (!trigFolder.exists()) {
				boolean result = trigFolder.mkdirs();
				if (!result) {
					throw new IllegalArgumentException(String.format("Unable to create folder %s", savePath));
				}
			}
		}

		File defByGroovyFile = null;
		File timeLabelGroovyFile = null;
		File ontologyFile = null;

		if (defByGroovy != null) {
			defByGroovyFile = new File(defByGroovy);
			if (!defByGroovyFile.exists()) {
				throw new IllegalArgumentException(String.format("File %s does not exist", defByGroovy));
			}
			defByGroovy = defByGroovyFile.getAbsolutePath();
		}
		else {
			defByGroovy = ClassLoader.getSystemClassLoader().getResource("gen_definedBy.groovy").toExternalForm();
		}

		if (timeLabelGroovy != null) {
			timeLabelGroovyFile = new File(timeLabelGroovy);
			if (!timeLabelGroovyFile.exists()) {
				throw new IllegalArgumentException(String.format("File %s does not exist", timeLabelGroovy));
			}
			timeLabelGroovy = timeLabelGroovyFile.getAbsolutePath();
		}
		else {
			timeLabelGroovy = ClassLoader.getSystemClassLoader().getResource("gen_time_label.groovy").toExternalForm();
		}

		if (ontology != null) {
			ontologyFile = new File(ontology);
			if (!ontologyFile.exists()) {
				throw new IllegalArgumentException(String.format("File %s does not exist", ontology));
			}
		}

		KnowledgeStore ks = null;
		if (ksAddress != null) {
			ks = Client.builder(ksAddress).compressionEnabled(true).maxConnections(2).validateServer(false).build();
		}

		return new Naf2SemProcessor(trigFolder, ks, defByGroovy, timeLabelGroovy, ontologyFile);
	}

	public Naf2SemProcessor(File savePath, KnowledgeStore ksClient, String defByGroovy, String timeLabelGroovy, File ontology) {
		this.savePath = savePath;
		this.ksClient = ksClient;
		this.defByGroovy = defByGroovy;
		this.timeLabelGroovy = timeLabelGroovy;
		this.ontology = ontology;
	}

	@Override
	public RDFHandler wrap(@Nullable final RDFHandler handler) {
		return new ReformatTimeProcessor().wrap(new HandlerImpl(handler));
	}

	private class HandlerImpl extends AbstractRDFHandlerWrapper {

		List<Statement> statements;

		public HandlerImpl(RDFHandler handler) {
			super(handler);
		}

		@Override
		public void startRDF() throws RDFHandlerException {
			statements = new ArrayList<>();
			super.startRDF();
		}

		@Override
		synchronized public void handleStatement(Statement statement) throws RDFHandlerException {
			if (statement != null) {
				statements.add(statement);
			}
		}

		@Override
		public void endRDF() throws RDFHandlerException {

			Session session;
			String thisDate = dateFormat.format(new Date());

			if (savePath != null) {
				RDFSource source = RDFSources.wrap(statements);

				StringBuffer fileName = new StringBuffer();
				fileName.append(savePath.getAbsolutePath());
				fileName.append(File.separator);
				fileName.append("nwr-").append(thisDate).append(".trig.gz");

				try {
					RDFHandler rdfHandler = RDFHandlers.write(null, 1000, fileName.toString());
					RDFProcessors.prefix(null).wrap(source).emit(rdfHandler, 1);
				} catch (Exception e) {
					LOGGER.error("Input/output error, the file {} has not been saved ({})", fileName, e.getMessage());
					throw new RDFHandlerException(e);
				}
			}

			List<Statement> descStatements = new ArrayList<>();
			List<Statement> situationStatements = new ArrayList<>();

			// Looking for sameAs events
			Model sameAs = new LinkedHashModel(statements);
			sameAs = sameAs.filter(null, OWL.SAMEAS, null);
			Set<URI> sameAsList = new HashSet<>();

			for (Statement statement : sameAs) {
				sameAsList.add((URI) statement.getSubject());
				if (statement.getObject() instanceof URI) {
					sameAsList.add((URI) statement.getObject());
				}
			}
			LOGGER.info("sameAs list contains {} URIs", sameAsList.size());

			List<RDFProcessor> processors = new ArrayList<>();

			session = ksClient.newSession();
			try {
				for (URI uri : sameAsList) {
					Stream<BindingSet> stream;

					stream = session.sparql(queryDesc, uri).execTuples();
					for (BindingSet bindingset : stream) {
						addBindingSetToStatements(bindingset, descStatements);
					}
					stream.close();

					stream = session.sparql(querySituation, uri).execTuples();
					for (BindingSet bindingset : stream) {
						addBindingSetToStatements(bindingset, situationStatements);
					}
					stream.close();
				}
			} catch (OperationException e) {
				throw new RDFHandlerException(e);
			} finally {
				session.close();
			}

			LOGGER.info("Statements: {}", statements.size());
			LOGGER.info("Situation statements: {}", situationStatements.size());
			LOGGER.info("Desc statements: {}", descStatements.size());

			// gen_definedBy.groovy
			if (defByGroovy != null) {
				processors.add(RDFProcessors.parse(true, "@groovy -p " + defByGroovy));
			}

			// gen_time_label.groovy
			if (timeLabelGroovy != null) {
				processors.add(RDFProcessors.parse(true, String.format("@groovy -p %s", timeLabelGroovy)));
			}

			processors.add(RDFProcessors.IDENTITY);

			RDFProcessor allProcessors = RDFProcessors.parallel(SetOperator.UNION_MULTISET, processors.toArray(new RDFProcessor[processors.size()]));

			RDFProcessor esoProcessor = RDFProcessors.IDENTITY;
			if (ontology != null) {
				esoProcessor = RDFProcessors.parse(false, "@esoreasoner", ontology.getAbsolutePath());
			}

			RDFProcessor finalProcessor = RDFProcessors.sequence(
					RDFProcessors.smush(),
					RDFProcessors.transform(Transformer.filter((Statement statement) -> !statement.getPredicate().equals(OWL.SAMEAS))),
					allProcessors,
					esoProcessor,
					RDFProcessors.unique(false)
			);

			List<Statement> statementsToAdd = Collections.synchronizedList(new ArrayList<>());
			List<Statement> statementsToDelete = new ArrayList<>();

			finalProcessor.apply(RDFSources.wrap(Iterables.concat(statements, descStatements)), RDFHandlers.wrap(statementsToAdd), 1);
			statementsToDelete.addAll(descStatements);
			statementsToDelete.addAll(situationStatements);

			LOGGER.info("Deleting {} statements", statementsToDelete.size());
			LOGGER.info("Adding {} statements", statementsToAdd.size());

			HashSet<String> tempFileNames = new HashSet<>();

			if (savePath != null) {
				RDFSource source;
				StringBuilder fileName;
				String tempFileName;

				// update

				source = RDFSources.wrap(statementsToAdd);

				fileName = new StringBuilder();
				fileName.append(savePath.getAbsolutePath());
				fileName.append(File.separator);
				fileName.append("nwr-add-").append(thisDate).append(".trig.gz");

				tempFileName = fileName.toString();
				tempFileNames.add(tempFileName);

				try {
					RDFHandler rdfHandler = RDFHandlers.write(null, 1000, tempFileName);
					RDFProcessors.prefix(null).wrap(source).emit(rdfHandler, 1);
				} catch (Exception e) {
					LOGGER.error("Input/output error, the file {} has not been saved ({})", tempFileName, e.getMessage());
					throw new RDFHandlerException(e);
				}

				// delete

				source = RDFSources.wrap(statementsToDelete);

				fileName = new StringBuilder();
				fileName.append(savePath.getAbsolutePath());
				fileName.append(File.separator);
				fileName.append("nwr-delete-").append(thisDate).append(".trig.gz");

				tempFileName = fileName.toString();
				tempFileNames.add(tempFileName);

				try {
					RDFHandler rdfHandler = RDFHandlers.write(null, 1000, tempFileName);
					RDFProcessors.prefix(null).wrap(source).emit(rdfHandler, 1);
				} catch (Exception e) {
					LOGGER.error("Input/output error, the file {} has not been saved ({})", tempFileName, e.getMessage());
					throw new RDFHandlerException(e);
				}
			}

			session = ksClient.newSession();
			try {
				session.sparqldelete().statements(statementsToDelete).exec();
				session.sparqlupdate().statements(statementsToAdd).exec();

//				if (savePath != null) {
//					for (String tempFileName : tempFileNames) {
//						File f = new File(tempFileName);
//						if (f.exists()) {
//							f.delete();
//						}
//					}
//				}
			} catch (OperationException e) {
				throw new RDFHandlerException(e);
			} finally {
				session.close();
			}

			super.endRDF();
		}

		private void addBindingSetToStatements(BindingSet bindingset, Collection<Statement> statements) {
			Value s = bindingset.getValue("s");
			Value p = bindingset.getValue("p");
			Value o = bindingset.getValue("o");
			Value g = bindingset.getValue("g");

			if (s instanceof Resource && p instanceof URI && o instanceof Value && g instanceof Resource) {
				ContextStatementImpl statement = new ContextStatementImpl((Resource) s, (URI) p, (Value) o, (Resource) g);
				statements.add(statement);
			}
		}
	}

}
