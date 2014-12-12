package eu.fbk.eso;

import eu.fbk.eso.reasoner.ESO;
import eu.fbk.eso.reasoner.EsoOntology;
import eu.fbk.rdfpro.*;
import eu.fbk.rdfpro.util.Options;
import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class FilterTypeProcessor implements RDFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(FilterTypeProcessor.class);

	private final RDFProcessor wrappedProcessor;
	private ArrayList<String> sendToReducer = new ArrayList<>();

	/**
	 * Internal factory method called by RDFpro to create and initialize the processor starting
	 * from supplied command line arguments.
	 *
	 * @param args command line arguments associated to the processor
	 * @return the created processor
	 */
	static FilterTypeProcessor doCreate(String name, final String... args) {

		// Parse and validate options. See Options class for spec syntax. Can also do the parsing
		// on your own by directly manipulating the args parameter (as in a regular main()
		// function)
		Options.parse("", args);
		return new FilterTypeProcessor();
	}

	public FilterTypeProcessor() {
		sendToReducer.add("http://www.newsreader-project.eu/domain-ontology#");
		sendToReducer.add("http://www.newsreader-project.eu/ontologies/framenet/");
		sendToReducer.add("http://www.newsreader-project.eu/ontologies/propbank/");
		this.wrappedProcessor = RDFProcessors.mapReduce(new MapperImpl(), new ReducerImpl(), true);
	}

	@Override
	public RDFHandler wrap(@Nullable final RDFHandler handler) {
		return this.wrappedProcessor.wrap(handler); // implemented as MapReduce job
	}

	private class MapperImpl implements Mapper {
		@Override
		public Value[] map(Statement statement) throws RDFHandlerException {

			// Lo riduco
			if (sendToReducer.contains(statement.getPredicate().getNamespace())) {
				return new Value[]{statement.getSubject()};
			}
			if (statement.getPredicate().equals(RDF.TYPE)) {
				URI resource = (URI) statement.getObject();
				if (sendToReducer.contains(resource.getNamespace())) {
					return new Value[]{statement.getSubject()};
				}
			}

			// Lo passo
			return new Value[]{Mapper.BYPASS_KEY};

			// Lo droppo
//			return new Value[0];
		}
	}

	private class ReducerImpl implements Reducer {

		@Override
		synchronized public void reduce(final Value key, final Statement[] statements, final RDFHandler handler) throws RDFHandlerException {

//			System.out.println("Key: " + key);
//			for (Statement statement : statements) {
//				System.out.println(statement);
//			}
//			System.out.println();

			final Model model = new LinkedHashModel(Arrays.asList(statements));
			Model isA = model.filter(null, RDF.TYPE, null);

			String pass = null;

			typeLoop:
			for (String type : sendToReducer) {
				for (Statement statement : isA) {
					URI resource = (URI) statement.getObject();
					if (resource.getNamespace().equals(type)) {
						pass = type;
						break typeLoop;
					}
				}
			}

			if (pass != null) {
				for (Statement statement : model) {
					if (statement.getPredicate().equals(RDF.TYPE)) {
						URI resource = (URI) statement.getObject();
						if (resource.getNamespace().equals(pass)) {
//							System.out.println(statement);
							handler.handleStatement(statement);
						}
					}
					else {
						if (statement.getPredicate().getNamespace().equals(pass)) {
//							System.out.println(statement);
							handler.handleStatement(statement);
						}
					}
				}
			}

		}

	}

}
