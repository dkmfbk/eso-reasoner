package eu.fbk.eso.reasoner;

//import org.apache.log4j.Logger;

import org.openrdf.model.*;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by alessio on 14/11/14.
 */

public class EsoOntology {

	private class ESONode {
		private Value uri;

		public Value getUri() {
			return uri;
		}

		public ESONode(Value uri) {
			this.uri = uri;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ESONode)) {
				return false;
			}

			ESONode that = (ESONode) o;

			return uri.equals(that.getUri());
		}

		@Override
		public int hashCode() {
			return uri.hashCode();
		}
	}

	private abstract class ESOTaxNode<T extends ESOTaxNode> extends ESONode {
		private T father = null;
		private String label;
		private HashSet<T> children = new HashSet<>();

		public ESOTaxNode(Value uri) {
			super(uri);
		}

		public String getLabel() {
			return label;
		}

		public void addChild(T child) {
			children.add(child);
		}

		public HashSet<T> getChildren() {
			return children;
		}

		public void setLabel(String label) {
			this.label = label;
		}


		public T getFather() {
			return father;
		}

		public void setFather(T father) {
			this.father = father;
		}

		@Override
		public String toString() {
			return this.getClass().getName() + "{" +
					"uri=" + this.getUri() +
					", father=" + (father != null ? father.getUri() : "[root]") +
					", children=" + children +
					'}';
		}

	}

	public class ESOProperty extends ESOTaxNode<ESOProperty> {
		private HashSet<Value> framenetElements = new HashSet<>();

		public ESOProperty(Value uri) {
			super(uri);
		}

		public void addFramenetElement(Value framenetElement) {
			framenetElements.add(framenetElement);
		}

	}

	public class ESOClass extends ESOTaxNode<ESOClass> {
		private HashSet<Value> framenetFrames = new HashSet<>();
		private HashMap<Value, HashSet<SituationRule>> rules = new HashMap<>();

		public ESOClass(Value uri) {
			super(uri);
		}

		public HashMap<Value, HashSet<SituationRule>> getRules() {
			return rules;
		}

		public void addRule(Value type, SituationRule rule) {
			HashSet<SituationRule> typeRules = rules.get(type);
			if (typeRules == null) {
				rules.put(type, new HashSet<>());
			}
			rules.get(type).add(rule);
		}

		public void addFramenetFrame(Value framenetFrame) {
			framenetFrames.add(framenetFrame);
		}

//		@Override
//		public String toString() {
//			return "ESOClass{" +
//					"uri=" + this.getUri() +
//					", rules=" + rules +
//					'}';
//		}
	}

	public class SituationRuleAssertion extends ESONode {
		private Value type;

		private Value subject;
		private Value property;
		private Value object;

		public SituationRuleAssertion(Value uri) {
			super(uri);
		}

		public Value getType() {
			return type;
		}

		public void setType(Value type) {
			this.type = type;
		}

		public Value getSubject() {
			return subject;
		}

		public void setSubject(Value subject) {
			this.subject = subject;
		}

		public Value getProperty() {
			return property;
		}

		public void setProperty(Value property) {
			this.property = property;
		}

		public Value getObject() {
			return object;
		}

		public void setObject(Value object) {
			this.object = object;
		}

		@Override
		public String toString() {
			return "SituationRuleAssertion{" +
					"uri=" + this.getUri() +
					", type=" + type +
					", subject=" + subject +
					", property=" + property +
					", object=" + object +
					'}';
		}
	}

	public class SituationRule extends ESONode {
		private HashSet<SituationRuleAssertion> assertions = new HashSet<>();

		public SituationRule(Value uri) {
			super(uri);
		}

		public void addAssertion(SituationRuleAssertion assertion) {
			assertions.add(assertion);
		}

		public HashSet<SituationRuleAssertion> getAssertions() {
			return assertions;
		}

		@Override
		public String toString() {
			return "SituationRule{" +
					"uri=" + this.getUri() +
//					", assertions=" + assertions +
					'}';
		}
	}

	static Logger logger = LoggerFactory.getLogger(EsoOntology.class.getName());

	private Model model = null;
	public HashMap<Value, ESOClass> classes = new HashMap<>();
	public HashMap<Value, ESOProperty> properties = new HashMap<>();
	public HashMap<Resource, HashSet<Resource>> restrictions = new HashMap<>();

	public EsoOntology(String fileName) {
		model = loadModel(fileName);
		init();
	}

	public EsoOntology(Model m) {
		model = m;
		init();
	}

	private void init() {
		HashMap<Value, URI> objectFromType = new HashMap<>();
		objectFromType.put(ESO.binaryRuleAssertion, ESO.hasSituationAssertionObject);
		objectFromType.put(ESO.unaryRuleAssertion, ESO.hasSituationAssertionObjectValue);

		Model tmpModel;

		// Classes
		tmpModel = model.filter(null, ESO.a, ESO.owlClass);
		for (Statement statement : tmpModel) {
			Resource subject = statement.getSubject();
			if (subject instanceof BNode) {
				logger.debug(statement.toString());
				logger.debug(String.format("%s is a blank node, skipping", subject.toString()));
				continue;
			}

			ESOClass esoClass = new ESOClass(subject);
			classes.put(subject, esoClass);
		}

		// Properties
		tmpModel = model.filter(null, ESO.a, ESO.owlProperty);
		for (Statement statement : tmpModel) {
			Resource subject = statement.getSubject();
			if (subject instanceof BNode) {
				logger.debug(String.format("%s is a blank node, skipping", subject.toString()));
				continue;
			}

			ESOProperty esoProperty = new ESOProperty(subject);
			properties.put(subject, esoProperty);
		}

		// Subproperties
		tmpModel = model.filter(null, ESO.subpropertyOf, null);
		for (Statement statement : tmpModel) {
			Value object = statement.getObject();
			Value subject = statement.getSubject();
			if (properties.containsKey(object) && properties.containsKey(subject)) {
				properties.get(subject).setFather(properties.get(object));
				properties.get(object).addChild(properties.get(subject));
			}
		}

		// Subclasses
		tmpModel = model.filter(null, ESO.subclassOf, null);
		for (Statement statement : tmpModel) {
			Value object = statement.getObject();
			Value subject = statement.getSubject();
			if (classes.containsKey(object) && classes.containsKey(subject)) {
				classes.get(subject).setFather(classes.get(object));
				classes.get(object).addChild(classes.get(subject));
			}
		}

		// Restrictions
		tmpModel = model.filter(null, ESO.someValuesFrom, null);
		for (Statement statement : tmpModel) {
			Resource bn = statement.getSubject();
			Model classModel = model.filter(null, ESO.subclassOf, bn);
			Model onPropertyModel = model.filter(bn, ESO.onProperty, null);

			Resource thisClass = null;
			Resource onProperty = null;
			for (Statement classStatement : classModel) {
				thisClass = classStatement.getSubject();
				break;
			}

			for (Statement propertyStatement : onPropertyModel) {
				onProperty = (Resource) propertyStatement.getObject();
				break;
			}

			if (thisClass == null || onProperty == null) {
				continue;
			}

			for (ESOClass esoClass : getDescendants(classes.get(thisClass))) {
				Resource thisCompleteClass = (Resource) esoClass.getUri();
				if (restrictions.get(thisCompleteClass) == null) {
					restrictions.put(thisCompleteClass, new HashSet<>());
				}
				restrictions.get(thisCompleteClass).add(onProperty);
			}
		}

//		for (Value value : classes.keySet()) {
//			System.out.println(value);
//			System.out.println(classes.get(value));
//		}

//		for (Resource resource : restrictions.keySet()) {
//			System.out.println(resource);
//			System.out.println(restrictions.get(resource));
//			System.out.println();
//		}

		// Rule assertions
		HashMap<Value, SituationRuleAssertion> assertions = new HashMap<>();
		for (ESOClass assertionType : classes.get(ESO.situationRuleAssertion).getChildren()) {
			tmpModel = model.filter(null, ESO.a, assertionType.getUri());

			URI objectType = objectFromType.get(assertionType.getUri());
			if (objectType == null) {
				continue;
			}

			for (Statement statement : tmpModel) {
				Value subject = statement.getSubject();
				SituationRuleAssertion assertion = new SituationRuleAssertion(subject);
				assertion.setType(assertionType.getUri());

				Model ruleModel;

				ruleModel = model.filter((Resource) subject, ESO.hasSituationAssertionSubject, null);
				for (Statement s : ruleModel) {
					assertion.setSubject(s.getObject());
				}
				ruleModel = model.filter((Resource) subject, ESO.hasSituationAssertionProperty, null);
				for (Statement s : ruleModel) {
					assertion.setProperty(s.getObject());
				}
				ruleModel = model.filter((Resource) subject, objectType, null);
				for (Statement s : ruleModel) {
					assertion.setObject(s.getObject());
				}

				assertions.put(subject, assertion);
			}
		}

		// Rules
		HashMap<Value, SituationRule> rules = new HashMap<>();
		tmpModel = model.filter(null, ESO.a, ESO.situationRule);
		for (Statement statement : tmpModel) {
			Value subject = statement.getSubject();
			SituationRule rule = new SituationRule(subject);
			Model ruleModel = model.filter((Resource) subject, ESO.hasSituationRuleAssertion, null);
			for (Statement s : ruleModel) {
				Value obj = s.getObject();
				SituationRuleAssertion assertion = assertions.get(obj);
				rule.addAssertion(assertions.get(obj));
			}
			rules.put(subject, rule);
		}

		// Triggers
		HashSet<ESOProperty> triggerTypes = properties.get(ESO.triggersSituationRule).getChildren();
		for (ESOProperty triggerType : triggerTypes) {
			tmpModel = model.filter(null, ESO.onProperty, triggerType.getUri());
			for (Statement statement : tmpModel) {
				Value subject = statement.getSubject();
				if (subject instanceof BNode) { // useless check
					Model bnModel;
					Value triggeringClass = null;
					Value trigger = null;

					bnModel = model.filter(null, null, subject);
					for (Statement s : bnModel) {
						triggeringClass = s.getSubject();
					}
					if (triggeringClass == null) {
						continue;
					}

					bnModel = model.filter((Resource) subject, ESO.hasValue, null);
					for (Statement s : bnModel) {
						trigger = s.getObject();
					}
					if (trigger == null) {
						continue;
					}

					SituationRule rule = rules.get(trigger);
					if (rule == null) {
						continue;
					}
					for (ESOClass esoClass : getDescendants(classes.get(triggeringClass))) {
//						System.out.println("Adding " + trigger.stringValue() + " to " + esoClass.getUri().stringValue());
						esoClass.addRule(triggerType.getUri(), rules.get(trigger));
					}

//					classes.get(triggeringClass).addRule(triggerType.getUri(), rules.get(trigger));
				}
			}
		}

		// FrameNet frames
//		tmpModel = model.filter(null, ESO.correspondToFrameNetFrame, null);
//		for (Statement statement : tmpModel) {
//			Value object = statement.getObject();
//			Value subject = statement.getSubject();
//			if (classes.containsKey(subject)) {
//				classes.get(subject).addFramenetFrame(object);
//			}
//		}

		//todo: add frame elements

	}

	public ArrayList<ESOClass> getDescendants(ESOClass startingPoint) {
		ArrayList<ESOClass> hierarchy = new ArrayList<>();
		return getDescendants(startingPoint, hierarchy);
	}

	public ArrayList<ESOClass> getDescendants(ESOClass startingPoint, ArrayList<ESOClass> hierarchy) {
		hierarchy.add(startingPoint);
//		System.out.println("Added " + startingPoint);
		for (ESOClass child : startingPoint.getChildren()) {
			getDescendants(child, hierarchy);
		}
		return hierarchy;
	}

//	public ArrayList<ESOClass> getClassHierarchy(@Nullable ESOClass startingPoint, @Nullable ArrayList<ESOClass> hierarchy) {
//		if (hierarchy == null) {
//			hierarchy = new ArrayList<>();
//		}
//		if (startingPoint != null) {
//			hierarchy.add(startingPoint);
//			hierarchy.addAll(getClassHierarchy(startingPoint.getFather(), hierarchy));
//		}
//		return hierarchy;
//	}

//	public HashSet<ESOClass> getRootNodes() {
//		HashSet<ESOClass> ret = new HashSet<>();
//
//		for (ESOClass esoClass : classes.values()) {
//			if (esoClass.getFather() == null) {
//				ret.add(esoClass);
//			}
//		}
//		return ret;
//	}

//	public HashSet<ESOProperty> getRootProperties() {
//		HashSet<ESOProperty> ret = new HashSet<>();
//
//		for (ESOProperty esoProperty : properties.values()) {
//			if (esoProperty.getFather() == null) {
//				ret.add(esoProperty);
//			}
//		}
//		return ret;
//	}

	public static Model loadModel(String fileName) {
		Model model = null;

		final File file = new File(fileName);
		final RDFFormat format = RDFFormat.forFileName(file.getName());

		try {
			try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
				model = Rio.parse(stream, "", format);
			} catch (final RDFParseException ex) {
				System.err.println("Oh no! There is an error in the ontology!");
				System.exit(15);
			}
		} catch (final IOException ex) {
			System.err.println("Oh no! Cannot read from " + file);
			System.exit(64);
		}

		return model;
	}

//	public static void main(String[] args) {
//		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();
//
//		commandLineWithLogger.addOption(OptionBuilder.withDescription("OWL file").isRequired().hasArg().withArgName("filename").withLongOpt("ontology").create("o"));
//
//		CommandLine commandLine = null;
//		try {
//			commandLine = commandLineWithLogger.getCommandLine(args);
//			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
//		} catch (Exception e) {
//			System.exit(1);
//		}
//
//		String ontologyFile = commandLine.getOptionValue("ontology");
//		EsoOntology ontology = new EsoOntology(ontologyFile);
//	}
}
