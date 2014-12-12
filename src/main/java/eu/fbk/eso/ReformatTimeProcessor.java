package eu.fbk.eso;

import eu.fbk.eso.reasoner.ESO;
import eu.fbk.rdfpro.*;
import eu.fbk.rdfpro.util.Options;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashSet;

public class ReformatTimeProcessor implements RDFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReformatTimeProcessor.class);
	HashSet<URI> transformIntoInteger = new HashSet<>();
	/**
	 * Internal factory method called by RDFpro to create and initialize the processor starting
	 * from supplied command line arguments.
	 *
	 * @param args command line arguments associated to the processor
	 * @return the created processor
	 */
	static ReformatTimeProcessor doCreate(String name, final String... args) {

		// Parse and validate options. See Options class for spec syntax. Can also do the parsing
		// on your own by directly manipulating the args parameter (as in a regular main()
		// function)
		Options.parse("", args);
		return new ReformatTimeProcessor();
	}

	public ReformatTimeProcessor() {
		transformIntoInteger.add(ESO.day);
		transformIntoInteger.add(ESO.month);
		transformIntoInteger.add(ESO.year);
	}

	@Override
	public RDFHandler wrap(@Nullable final RDFHandler handler) {
		return new HandlerImpl(handler);
	}

	private class HandlerImpl extends AbstractRDFHandlerWrapper {

		public HandlerImpl(RDFHandler handler) {
			super(handler);
		}

		@Override
		public void handleStatement(Statement statement) throws RDFHandlerException {

			/*
			* Transform into integer
			*
			* time:month
			* time:year
			* time:day
			* */

			ValueFactory factory = ValueFactoryImpl.getInstance();

 			if (transformIntoInteger.contains(statement.getPredicate())) {
				String value = statement.getObject().stringValue();
				value = value.replaceAll("[^0-9]", "");
				Integer numericValue = Integer.parseInt(value);
				statement = factory.createStatement(statement.getSubject(), statement.getPredicate(), factory.createLiteral(numericValue));
			}

			super.handleStatement(statement);
		}
	}

}
