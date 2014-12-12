package eu.fbk.eso.reasoner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by alessio on 14/11/14.
 */

public class SameAs {

	static Logger logger = Logger.getLogger(SameAs.class.getName());
	public final static Integer ESO = 1;
	public final static Integer FRAMENET = 2;
	public final static Integer PROPBANK = 3;

	private HashMap<Integer, HashSet<String>> equivalents = new HashMap<>();

	public SameAs() {
		equivalents.put(ESO, new HashSet<String>());
		equivalents.put(FRAMENET, new HashSet<String>());
		equivalents.put(PROPBANK, new HashSet<String>());
	}

	public void add(Integer type, String value) {
		equivalents.get(type).add(value);
	}

	public HashSet<String> getValues(Integer type) {
		return equivalents.get(type);
	}

	@Override
	public String toString() {
		return "SameAs{" +
				"equivalents=" + equivalents +
				'}';
	}
}
