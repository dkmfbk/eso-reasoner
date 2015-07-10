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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ReasonerProcessor implements RDFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReasonerProcessor.class);

	private final RDFProcessor wrappedProcessor;

	private final boolean emitInferredOnly;
	private final EsoOntology ontology;

	private static final String nwrPrefix = "http://www.newsreader-project.eu/domain-ontology#";

	private static final String inferredInstances = "http://www.newsreader-project.eu/inf-instances";
	private static final Resource inferredInstancesResource = ValueFactoryImpl.getInstance().createURI(inferredInstances);

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

			// Load roles
			HashMap<URI, HashSet<Value>> roles = new HashMap<>();
			for (Statement st : model) {
				String predicate = st.getPredicate().stringValue();
				if (predicate.startsWith(nwrPrefix)) {
					if (roles.get(st.getPredicate()) == null) {
						roles.put(st.getPredicate(), new HashSet<>());
					}
					roles.get(st.getPredicate()).add(st.getObject());
				}
			}

//			System.out.println("Roles: " + roles);

			for (final Statement statement : isA) {
				String value = statement.getObject().stringValue();

				// Only considers ESO type
				if (value.startsWith(nwrPrefix)) {

					HashMap<URI, HashSet<Value>> theseRoles = new HashMap<>(roles);

					EsoOntology.ESOClass thisClass = ontology.classes.get(statement.getObject());
					if (thisClass == null) {
						continue;
					}

					HashSet<Resource> restrictions = ontology.restrictions.get(thisClass.getUri());
					HashSet<URI> usedInferredRoles = new HashSet<>();

					if (restrictions != null) {
						for (Resource restriction : restrictions) {

							String restrictionRole = restriction.stringValue().substring(restriction.stringValue().lastIndexOf("#") + 1);
							restrictionRole = key.stringValue() + "-" + restrictionRole;
							URI uri = ValueFactoryImpl.getInstance().createURI(restrictionRole);

							// Moved in the end of the process
//							Statement s = ValueFactoryImpl.getInstance().createStatement((Resource) key, (URI) restriction, uri, inferredInstancesResource);
//							handler.handleStatement(s);

							if (theseRoles.get(restriction) == null) {
								theseRoles.put((URI) restriction, new HashSet<>());
								theseRoles.get(restriction).add(uri);
							}
						}
					}

					LOGGER.debug("Class: " + thisClass);
					LOGGER.debug("Restrictions: " + restrictions);
					LOGGER.debug("Roles: " + theseRoles);

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

//							LOGGER.debug("Situation rule: " + situationRule);

							// Assertions
							for (EsoOntology.SituationRuleAssertion assertion : situationRule.getAssertions()) {

//								System.out.println("Assertion: " + assertion);

								String type = assertion.getType().stringValue();
								URI subject = ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getSubject()).getLabel());

								if (type.equals(ESO.unaryRuleAssertion.stringValue())) {
									HashSet<Value> subjects = new HashSet<>();
									if (theseRoles.get(subject) != null) {
										subjects = theseRoles.get(subject);
									}

									if (subjects.size() > 0) {
										LOGGER.debug("Subj: " + subjects);
										for (Value s : subjects) {
											Statement x = ValueFactoryImpl.getInstance().createStatement((Resource) s, ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getProperty()).getLabel()), assertion.getObject(), situation);
											situations.add(x);
											usedInferredRoles.add(subject);
										}
									}
								}
								else if (type.equals(ESO.binaryRuleAssertion.stringValue())) {
									URI object = ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getObject()).getLabel());

									HashSet<Value> subjects = new HashSet<>();
									HashSet<Value> objects = new HashSet<>();

									if (theseRoles.get(subject) != null) {
										subjects = theseRoles.get(subject);
									}

									if (theseRoles.get(object) != null) {
										objects = theseRoles.get(object);
									}

									if (objects.size() > 0 && subjects.size() > 0) {
										LOGGER.debug("Subj: " + subjects);
										LOGGER.debug("Obj: " + objects);
										for (Value s : subjects) {
											for (Value o : objects) {
												if (s.equals(o)) {
													//todo: Che fare?
												}
												Statement x = ValueFactoryImpl.getInstance().createStatement((Resource) s, ValueFactoryImpl.getInstance().createURI(((Literal) assertion.getProperty()).getLabel()), o, situation);
												situations.add(x);
												usedInferredRoles.add(subject);
												usedInferredRoles.add(object);
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
								String suffix = time.getObject().stringValue().substring(time.getObject().stringValue().lastIndexOf("#") + 1);
								switch (situationName) {
									case "pre":
										URI situationTimeBefore = ValueFactoryImpl.getInstance().createURI(situation.stringValue() + "_time_" + suffix);
										s = ValueFactoryImpl.getInstance().createStatement(situation, time.getPredicate(), situationTimeBefore, situationGraph);
										handler.handleStatement(s);
										s = ValueFactoryImpl.getInstance().createStatement(situationTimeBefore, ESO.a, ESO.timeInterval, situationGraph);
										handler.handleStatement(s);
										s = ValueFactoryImpl.getInstance().createStatement(situationTimeBefore, ESO.timeBefore, time.getObject(), situationGraph);
										handler.handleStatement(s);
										break;
									case "post":
										URI situationTimeAfter = ValueFactoryImpl.getInstance().createURI(situation.stringValue() + "_time_" + suffix);
										s = ValueFactoryImpl.getInstance().createStatement(situation, time.getPredicate(), situationTimeAfter, situationGraph);
										handler.handleStatement(s);
										s = ValueFactoryImpl.getInstance().createStatement(situationTimeAfter, ESO.a, ESO.timeInterval, situationGraph);
										handler.handleStatement(s);
										s = ValueFactoryImpl.getInstance().createStatement(situationTimeAfter, ESO.timeAfter, time.getObject(), situationGraph);
										handler.handleStatement(s);
										break;
									case "during":
										s = ValueFactoryImpl.getInstance().createStatement(situation, time.getPredicate(), time.getObject(), situationGraph);
										handler.handleStatement(s);
										break;
								}
							}

							for (Statement x : situations) {
								handler.handleStatement(x);
							}
						}
					}

					// Add inferred roles
					for (URI inferredRole : usedInferredRoles) {
						if (roles.containsKey(inferredRole)) {
							continue;
						}
						for (Value uri : theseRoles.get(inferredRole)) {
							Statement s = ValueFactoryImpl.getInstance().createStatement((Resource) key, inferredRole, uri, inferredInstancesResource);
							handler.handleStatement(s);
						}
					}
				}
			}
		}
	}
}
