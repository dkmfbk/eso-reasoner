package eu.fbk.eso;

import java.util.Arrays;
import java.util.HashSet;

import javax.annotation.Nullable;

import eu.fbk.eso.reasoner.ESO;
import eu.fbk.eso.reasoner.EsoOntology;
import eu.fbk.rdfpro.*;
import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.rdfpro.util.Options;

public class ReasonerProcessor implements RDFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReasonerProcessor.class);

	private final RDFProcessor wrappedProcessor;

	private final boolean emitInferredOnly;
	private final EsoOntology ontology;

	/**
	 * Internal factory method called by RDFpro to create and initialize the processor starting
	 * from supplied command line arguments.
	 *
	 * @param args command line arguments associated to the processor
	 * @return the created processor
	 */
	static ReasonerProcessor doCreate(String name, final String... args) {

		// Parse and validate options. See Options class for spec syntax. Can also do the parsing
		// on your own by directly manipulating the args parameter (as in a regular main()
		// function)
		final Options options = Options.parse("i|b!|w|+", args);

		final Model ontology = new LinkedHashModel();
		try {
			final String base = options.getOptionArg("b", String.class);
			final boolean rewriteBNodes = options.hasOption("w");
			final String[] fileSpecs = options.getPositionalArgs(String.class).toArray(new String[0]);
			RDFSources.read(true, !rewriteBNodes, base, null, fileSpecs).emit(RDFHandlers.wrap(ontology), 1);
		} catch (final RDFHandlerException ex) {
			throw new IllegalArgumentException("Cannot load ESO ontology: " + ex.getMessage(), ex);
		}

		final boolean emitInferredOnly = options.hasOption("i");

		// TODO: other options to parse here?

		LOGGER.info("Loading ontology");
		EsoOntology o = new EsoOntology(ontology);

		return new ReasonerProcessor(o, emitInferredOnly);
	}

	public ReasonerProcessor(final EsoOntology ontology, final boolean emitInferredOnly) {

		this.ontology = ontology;
		this.emitInferredOnly = emitInferredOnly;
		this.wrappedProcessor = RDFProcessors.mapReduce(Mapper.select("s"), new ReducerImpl(), true);

		// TODO: do something with the ontology, such as initializing some class fields holding
		// the necessary data structures
	}

	@Override
	public RDFHandler wrap(@Nullable final RDFHandler handler) {
		return this.wrappedProcessor.wrap(handler); // implemented as MapReduce job
	}

	private class ReducerImpl implements Reducer {

		@Override
		public void reduce(final Value key, final Statement[] statements, final RDFHandler handler)
				throws RDFHandlerException {

			// Emit the source statements unless we are asked to produce only inferences
			if (!ReasonerProcessor.this.emitInferredOnly) {
				for (final Statement statement : statements) {
					handler.handleStatement(statement);
				}
			}

			// Put all the statements in a model to facilitate further processing
			final Model model = new LinkedHashModel(Arrays.asList(statements));

			// Now check if the subject (common to all statements and also stored in the key) is a
			// sem:event. If it is not the case, abort this reduce job.
			final Resource event = (Resource) key;
			if (!model.contains(event, RDF.TYPE, SEM.EVENT)) {
				return;
			}

			HashSet<Statement> times = new HashSet<>();
			Model timeModel = model.filter(null, ESO.hasTime, null);
			for (Statement statement : timeModel) {
				times.add(statement);
			}
			if (times.size() == 0) {
				//todo: Che fare?
			}

			Model isA = model.filter(null, RDF.TYPE, null);

			for (final Statement statement : isA) {
				String value = statement.getObject().stringValue();
				if (value.startsWith("http://www.newsreader-project.eu/domain-ontology#")) {
					EsoOntology.ESOClass thisClass = ontology.classes.get(statement.getObject());

					// Rules
					for (Value rule : thisClass.getRules().keySet()) {

						URI ruleType = null;
//						System.out.println("Rule: " + rule);

						String situationName = null;
						if (rule.equals(ESO.triggersPreSituationRule)) {
							situationName = "pre";
							ruleType = ESO.hasPreSituation;
						}
						if (rule.equals(ESO.triggersPostSituationRule)) {
							situationName = "post";
							ruleType = ESO.hasPostSituation;
						}
						if (rule.equals(ESO.triggersDuringSituationRule)) {
							situationName = "during";
							ruleType = ESO.hasDuringSituation;
						}

						if (situationName == null) {
							return;
						}

						// http://www.newsreader-project.eu/instances
						
						URI situation = ValueFactoryImpl.getInstance().createURI(event.stringValue() + "_" + situationName);
						URI situationGraph = ValueFactoryImpl.getInstance().createURI("http://www.newsreader-project.eu/situation-graph");
						HashSet<Statement> situations = new HashSet<>();

						// Situation Rules
						for (EsoOntology.SituationRule situationRule : thisClass.getRules().get(rule)) {

//							System.out.println("Situation rule: " + situationRule);

							// Assertions
							for (EsoOntology.SituationRuleAssertion assertion : situationRule.getAssertions()) {

//								System.out.println("Assertion: " + assertion);

								String type = assertion.getType().stringValue();
								if (type.equals(ESO.unaryRuleAssertion.stringValue())) {
									Model filtered;

									HashSet<Value> subjects = new HashSet<>();

									filtered = model.filter(null, ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getSubject()).getLabel()), null);
									for (Statement s : filtered) {
										subjects.add(s.getObject());
									}

									if (subjects.size() > 0) {
//										System.out.println("Subj: " + subjects);
										for (Value s : subjects) {
											Statement x = ValueFactoryImpl.getInstance().createStatement((Resource) s, ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getProperty()).getLabel()), assertion.getObject(), situation);
											situations.add(x);
										}
									}
								}
								else if (type.equals(ESO.binaryRuleAssertion.stringValue())) {
									Model filtered;

									HashSet<Value> subjects = new HashSet<>();
									HashSet<Value> objects = new HashSet<>();

									filtered = model.filter(null, ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getSubject()).getLabel()), null);
									for (Statement s : filtered) {
										subjects.add(s.getObject());
									}

									filtered = model.filter(null, ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getObject()).getLabel()), null);
									for (Statement s : filtered) {
										objects.add(s.getObject());
									}

									if (objects.size() > 0 && subjects.size() > 0) {
//										System.out.println("Subj: " + subjects);
//										System.out.println("Obj: " + objects);
										for (Value s : subjects) {
											for (Value o : objects) {
												if (s.equals(o)) {
													//todo: Che fare?
												}
												Statement x = ValueFactoryImpl.getInstance().createStatement((Resource) s, ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getProperty()).getLabel()), o, situation);
												situations.add(x);
											}
										}
									}
								}
							}
						}

						if (situations.size() > 0) {
//							System.out.println("Situation: " + situation);

							Statement s;

							s = ValueFactoryImpl.getInstance().createStatement(situation, RDF.TYPE, ESO.situation, situationGraph);
							handler.handleStatement(s);
//							System.out.println(s);
							s = ValueFactoryImpl.getInstance().createStatement(event, ruleType, situation, situationGraph);
							handler.handleStatement(s);
//							System.out.println(s);

							for (Statement time : times) {
								s = ValueFactoryImpl.getInstance().createStatement(situation, time.getPredicate(), time.getObject(), situationGraph);
								handler.handleStatement(s);
//								System.out.println(s);
							}

							for (Statement x : situations) {
								handler.handleStatement(x);
//								System.out.println(x);
							}
//							System.out.println();
						}
					}
				}
			}

		}

	}

}
