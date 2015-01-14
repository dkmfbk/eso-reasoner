package eu.fbk.eso;

import eu.fbk.rdfpro.AbstractRDFHandlerWrapper;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.util.Options;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class RemoveObjEqSubjProcessor implements RDFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemoveObjEqSubjProcessor.class);
	/**
	 * Internal factory method called by RDFpro to create and initialize the processor starting
	 * from supplied command line arguments.
	 *
	 * @param args command line arguments associated to the processor
	 * @return the created processor
	 */
	static RemoveObjEqSubjProcessor doCreate(String name, final String... args) {

		// Parse and validate options. See Options class for spec syntax. Can also do the parsing
		// on your own by directly manipulating the args parameter (as in a regular main()
		// function)
		Options.parse("", args);
		return new RemoveObjEqSubjProcessor();
	}

	public RemoveObjEqSubjProcessor() {
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
			
			if (statement.getObject().stringValue().equals(statement.getSubject().stringValue())) {
				return;
			}

			super.handleStatement(statement);
		}
	}

}
