// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can
// redistribute it and/or modify it under the terms of the
// GNU General Public License as published by the Free Software
// Foundation; either version 2 of the License, or (at your
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without
// even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE.  See the GNU General Public License
// for more details.
//
// You should have received a copy of the GNU General Public
// License along with the Java Compiler Kit; if not,
// write to the Free Software Foundation, Inc., 59 Temple Place,
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009.

package jkit.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import jkit.compiler.SyntaxError;
import jkit.compiler.MethodNotFoundException;
import jkit.jil.tree.Type;
import jkit.util.Pair;
import jkit.compiler.Clazz;

/**
 * This class contains a collection of helper methods to increase
 * the usefulness of JKit's error messages by offering suggestions for
 * substitute code. Currently a WIP
 *
 * @author Daniel Campbell
 *
 */
public class ErrorHandler {

	public enum ErrorType
	{ METHOD_NOT_FOUND, FIELD_NOT_FOUND, VARIABLE_NOT_FOUND, BAD_TYPE_CONVERSION }

	//The maximum difference between a target and a candidate to be considered for substitution
	public final static int MAX_DIFFERENCE = 3;

	/**
	 * Main method called by outside classes - given a type of error
	 * and an exception with relevant data, calls the appropriate helper method
	 *
	 * @param type - The type of error encountered
	 * @param ex   - The exception to be improved
	 */
	public static void handleError(ErrorType type, Exception ex) {

		switch(type) {

		case METHOD_NOT_FOUND:
			handleMethodNotFound((MethodNotFoundException)ex);
			break;

		case FIELD_NOT_FOUND:

			break;

		case VARIABLE_NOT_FOUND:

			break;

		case BAD_TYPE_CONVERSION:

			break;

		default:
			throw new SyntaxError("Undefined error for error handler: " +ex.getMessage(), -1, -1);
		}
	}

	private static void handleMethodNotFound(MethodNotFoundException ex) {

		//First, need to turn the owner into an explicit class or set of classes
		//We store the distance of the found class from the original class as an int
		//At this point, all distances are 0
		Set<Pair<Clazz, Integer>> classes = new HashSet<Pair<Clazz, Integer>>();
		Stack<Type.Reference> stk = new Stack<Type.Reference>();
		stk.push(ex.owner());

		while (!stk.isEmpty()) {
			Type.Reference ref = stk.pop();

			if (ref instanceof Type.Clazz)

				try {
					classes.add(new Pair<Clazz, Integer>(ex.loader().loadClass((Type.Clazz)ref), 0));
				} catch (ClassNotFoundException e) {
					//Skip trying to load this class
					continue;
				}

			else if (ref instanceof Type.Intersection) {
				Type.Intersection it = (Type.Intersection) ref;
				for (Type.Reference b : it.bounds())
					stk.push(b);
			}

			else if (ref instanceof Type.Wildcard) {
				//Not sure what to do here, if anything
			}
		}

		//Now need to make sure all super classes are added as well
		//Super classes have a distance of 1 from their subclass
		//Regardless of how far up the chain they are

		Set<Pair<Clazz, Integer>> tmpSet = new HashSet<Pair<Clazz, Integer>>();
		for (Pair<Clazz, Integer> p : classes) {
			Clazz c = p.first();
			try {
				Clazz tmp = c;
				while (tmp != null && !tmpSet.contains(new Pair<Clazz, Integer>(tmp, tmp.equals(c) ? 0 : 1))) {
					tmpSet.add(new Pair<Clazz, Integer>(tmp, tmp.equals(c) ? 0 : 1));
					tmp = ex.loader().loadClass(c.superClass());
				}
			} catch (ClassNotFoundException e) {
				//Skip trying to load this superclass
				continue;
			}
		}

		classes = tmpSet;

		//Next, we should look for misspelled method names (ignoring parameters for now)
		//If we find a candidate, we give it a weighting based on its Levenshtein distance
		Set<Pair<String, Integer>> names = new HashSet<Pair<String, Integer>>();
		for (Pair<Clazz, Integer> p : classes) {
			Clazz c = p.first();
			for (Clazz.Method m : c.methods()) {
				//We want to ignore superclass constructors
				if (m.name().equals(c.name()) && classes.contains(new Pair<Clazz, Integer>(c, 1)))
					continue;

				int dist = distance(ex.method(), m.name()) + p.second();
				if (dist < MAX_DIFFERENCE)
					names.add(new Pair<String, Integer>(m.name(), dist));
			}
		}

		//Next, check for methods with a different number of parameters
		//Again, the methods are given a weighting based on the difference (and any prior weighting)
		List<Pair<Clazz.Method, Integer>> methods = new ArrayList<Pair<Clazz.Method, Integer>>();

		for (Pair<String, Integer> p : names) {
			for(Pair<Clazz, Integer> p2 : classes) {
				Clazz c = p2.first();
				for (Clazz.Method m : c.methods(p.first())) {
					int diff = Math.abs(m.parameters().size() - ex.parameters().size());
					if (diff + p.second() <= MAX_DIFFERENCE) methods.add(new Pair<Clazz.Method, Integer>(m, diff + p.second()));
				}
			}
		}

		//Finally, check for different types of parameters
		//Again, a weighting is given based on the number of parameters changed
		List<Pair<Clazz.Method, Integer>> suggestions = new ArrayList<Pair<Clazz.Method, Integer>>();

		for (Pair<Clazz.Method, Integer> p : methods) {

			List<Type> subParams = p.first().type().parameterTypes();
			List<Type> targetParams = ex.parameters();

			int min = Math.min(targetParams.size(), subParams.size());
			int weight = p.second();


			for (int i = 0; i < min; i++) {
				if (!subParams.get(i).equals(targetParams.get(i)))
					weight++;
			}

			if (weight <= MAX_DIFFERENCE)
				suggestions.add(new Pair<Clazz.Method, Integer>(p.first(), weight));
		}

		//Now sort the suggestions by weight
		Collections.sort(suggestions, new WeightComparator<Pair<Clazz.Method, Integer>>());

		//For now, we will only throw the top level suggestion - that could change
		Clazz.Method suggestion = (suggestions.isEmpty()) ? null : suggestions.get(0).first();

		StringBuilder msg = new StringBuilder(100);
		msg.append(String.format("Method \"%s.%s(", ex.owner().toString(), ex.method()));
		boolean firstTime = true;
		for (Type t : ex.parameters()) {
			if (!firstTime)
				msg.append(", ");
			msg.append(t.toString());
		}
		msg.append(")\" not found.");
		if (suggestion == null)
			throw new SyntaxError(msg.toString(), -1, -1);

		msg.append(String.format("\nA possible substitute is \"%s.%s(",
				ex.owner().toString(), suggestion.name()));

		firstTime = true;
		for (Type t : suggestion.type().parameterTypes()) {
			if (!firstTime)
				msg.append(", ");
			msg.append(t.toString());
			firstTime = false;
		}
		throw new SyntaxError(msg.toString()+")\"", -1, -1);
	}

	/**
	 * Computes the Levenshtein distance between two strings
	 * (Edit distance where allowed edits are deletion, insertion, and modification)
	 *
	 * @param src	- source string
	 * @param trg	- target string
	 * @return		- The Levenshtein distance between the two
	 */
	private static int distance(String src, String trg) {

		if (src.equals(trg)) return 0;
		if (src.length() == 0) return trg.length();
		if (trg.length() == 0) return src.length();

		//Distance is always at least the size difference between the strings
		//If this is greater than max allowed distance we can exit immediately
		int lowerBound = Math.abs(src.length() - trg.length());
		if (lowerBound > MAX_DIFFERENCE)
			return lowerBound;

		//Need two arrays, one for the current (working) row,
		//and one for the previous (worked) row
		int[] curr = new int[Math.max(trg.length()+1, src.length()+1)];
		int[] prev = new int[curr.length];

		//Initialise prev to be the edit distance if src was empty
		for (int i = 0; i < prev.length; i++) {
			prev[i] = i;
		}

		for (int i = 0; i < src.length(); i++) {

			//First value of current row is always i+1
			curr[i] = i+1;
			int min = i+1;

			//The following formula calculates the rest of the current row
			//If at the end the calculated min distance is greater than a threshold
			//We return immediately, as we only care about 'close' strings

			for (int j = 0; j < trg.length(); j++) {
				int cost = (src.charAt(i) == trg.charAt(j)) ? 0 : 1;
				curr[j+1] = Math.min(curr[j]+1, Math.min(prev[j+1]+1, prev[j]+cost));
				min = (curr[j+1] < min) ? curr[j+1] : min;
			}

			if (min > MAX_DIFFERENCE) return min;

			//Copy current row over to old row and start over
			for (int j = 0; j < prev.length; j++)
				prev[j] = curr[j];
		}

		return curr[trg.length()];
	}

	/**
	 * Private class used to sort substitutes by a given weighting
	 *
	 * @author Daniel Campbell
	 *
	 * @param <E> - A pair where the (Integer) weighting is stored in the second value of the pair
	 */
	private static class WeightComparator<E extends Pair<?, Integer>> implements Comparator<E> {

		public int compare(E o1, E o2) {
			return o1.second().compareTo(o2.second());
		}
	}
}
