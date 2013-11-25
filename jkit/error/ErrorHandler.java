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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.jil.tree.SourceLocation;
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
	{ METHOD_NOT_FOUND, PACKAGE_NOT_FOUND, FIELD_NOT_FOUND }

	//The maximum difference between a target and a candidate to be considered for substitution
	public final static int MAX_DIFFERENCE = 3;

	/**
	 * Main method called by outside classes - given a type of error
	 * and an exception with relevant data, calls the appropriate helper method
	 *
	 * @param type - The type of error encountered
	 * @param ex   - The exception to be improved
	 */
	public static void handleError(ErrorType type, Exception ex, SourceLocation loc) {

		switch(type) {

		case METHOD_NOT_FOUND:
			handleMethodNotFound((MethodNotFoundException)ex, loc);
			break;

		case PACKAGE_NOT_FOUND:
			handlePackageNotFound((PackageNotFoundException)ex);
			break;

		case FIELD_NOT_FOUND:
			handleFieldNotFound((FieldNotFoundException)ex, loc);

		default:
			throw new SyntaxError("Undefined error for error handler: " +ex.getMessage(), -1, -1);
		}
	}

	/**
	 * A simpler version for handling methods not found, must search up
	 * the class hierarchy looking for any possible fields that match.
	 * Also checks for methods of the correct return type that take no parameters,
	 * as it is possible that is what the user intended.
	 *
	 * @param ex - The FieldNotFound exception encountered
	 */
	private static void handleFieldNotFound(FieldNotFoundException ex, SourceLocation loc) {

		Set<Clazz> classes = getClasses(ex.loader(), ex.owner());
		classes = getSuperClasses(ex.loader(), classes);

		//Next, we should look for misspelled field names (ignoring possible methods for now)
		//If we find a candidate, we give it a weighting based on its edit distance
		List<Pair<Clazz.Field, Integer>> fields = new ArrayList<Pair<Clazz.Field, Integer>>();

		for (Clazz c : classes) {
			for (Clazz.Field f : c.fields()) {
				int dist = distance(ex.field(), f.name());
				if (dist <= MAX_DIFFERENCE)
					fields.add(new Pair<Clazz.Field, Integer>(f, dist));
			}
		}

		//Now we check for methods with no parameters
		//If we find a candidate, it is given a weighting based on edit distance, with an extra weighting
		//for being a method (unfortunately, we can't check the type of the method matches)
		List<Pair<Clazz.Method, Integer>> methods = new ArrayList<Pair<Clazz.Method, Integer>>();

		for (Clazz c : classes) {

			for (Clazz.Method m : c.methods()) {
				if (m.type().returnType() instanceof Type.Void || !m.parameters().isEmpty())
					continue;

				int dist = distance(ex.field(), m.name()) + 2;
				if (dist <= MAX_DIFFERENCE)
					methods.add(new Pair<Clazz.Method, Integer>(m, dist));
			}
		}
		//Now sort the two lists to find the one with the least weighting,
		//And throw a Syntax Error with the appropriate error message
		Collections.sort(fields, new WeightComparator<Pair<Clazz.Field, Integer>>());
		Collections.sort(methods, new WeightComparator<Pair<Clazz.Method, Integer>>());
		String suggestion;

		if (fields.isEmpty()) {
			if (methods.isEmpty()) {
				suggestion = "";
			}
			else suggestion = String.format("\nA possible substitute is method %s.%s()", ClassLoader.pathChild(ex.owner().toString()),
					methods.get(0).first().name());
		}
		else if (methods.isEmpty()) {
			suggestion = String.format("\nA possible substitute is field %s.%s", ClassLoader.pathChild(ex.owner().toString()),
					fields.get(0).first().name());
		}
		else {
			suggestion = (fields.get(0).second() < methods.get(0).second()) ?
					String.format("\nA possible substitute is field %s.%s", ClassLoader.pathChild(ex.owner().toString()),
							fields.get(0).first().name()) :
					String.format("\nA possible substitute is method %s.%s()", ClassLoader.pathChild(ex.owner().toString()),
							methods.get(0).first().name());
		}

		ex.loader();
		throw new SyntaxError(String.format("Field %s.%s not found%s", ClassLoader.pathChild(ex.owner().toString()),
				ex.field(), suggestion), loc.line(), loc.column());
	}

	/**
	 * Finds the directory containing the class with the bad package declaration,
	 * to suggest that directory as an alternative package
	 */
	private static void handlePackageNotFound(PackageNotFoundException ex) {
		Clazz jilClass = ex.jilClass();
		String pkg = jilClass.type().pkg().replace('.', File.pathSeparatorChar);
		List<String> sourcepath = ex.sourcepath;
		List<String> classpath = ex.classpath;
		String result = null;

		//A pairing of directory to classpath parent directory (used to find relative path)
		Stack<Pair<String, String>> directories = new Stack<Pair<String, String>>();

		for (String dir : sourcepath) {
			if (!dir.contains("."))
				directories.push(new Pair<String, String>(dir, dir));
		}

		for (String dir : classpath) {
			if (!dir.contains(".") && ! directories.contains(new Pair<String, String>(dir, dir)))
				directories.push(new Pair<String, String>(dir, dir));
		}

		outer:
			while(!directories.isEmpty()) {
				Pair<String,String> dir = directories.pop();
				File f = new File(dir.first());

				if (!f.isDirectory())
					continue;

				for (String file : f.list()) {

					if (file.equals(jilClass.name() + ".java")) {
						result = dir.first().replaceAll(dir.second(), "").substring(1).replace(File.separatorChar, '.');
						break outer;
					}
					else if (!file.contains(".")) {

						//File is a directory, so can push it onto the stack
						directories.push(new Pair<String, String>(dir.first()+File.separator+file, dir.second()));
					}
				}
			}

		//Not sure if possible, but will handle the case anyway
		if (result == null)
			throw new SyntaxError("Unable to find source file directory", -1, -1);

		throw new SyntaxError(String.format("Unable to find package %s\nUse source directory package instead: %s",
				pkg, result), -1, -1);
	}

	/**
	 * This method throws an exception which may contain a suggestion to the user of an alternative
	 * method to use. Alternatives are considered based on the class hierarchy, edit distance between
	 * names, types of parameters and number of parameters. Only the most suitable candidate is considered,
	 * and only those close enough to the target (within three cumulative edits/substitutions) are
	 * actually suggested.
	 *
	 */
	private static void handleMethodNotFound(MethodNotFoundException ex, SourceLocation loc) {

		Set<Clazz> classes = getClasses(ex.loader(), ex.owner());
		classes = getSuperClasses(ex.loader(), classes);

		//First, we should look for misspelled method names (ignoring parameters for now)
		//If we find a candidate, we give it a weighting based on its Levenshtein distance
		Set<Pair<String, Integer>> names = new HashSet<Pair<String, Integer>>();
		for (Clazz c : classes) {
			for (Clazz.Method m : c.methods()) {
				//We want to ignore superclass constructors
				if (m.name().equals(c.name()) && classes.contains(new Pair<Clazz, Integer>(c, 1)))
					continue;

				int dist = distance(ex.method(), m.name());
				if (dist <= MAX_DIFFERENCE)
					names.add(new Pair<String, Integer>(m.name(), dist));
			}
		}

		//Next, check for methods with a different number of parameters
		//Again, the methods are given a weighting based on the difference (and any prior weighting)
		List<Pair<Clazz.Method, Integer>> methods = new ArrayList<Pair<Clazz.Method, Integer>>();

		for (Pair<String, Integer> p : names) {
			for(Clazz c : classes) {
				for (Clazz.Method m : c.methods(p.first())) {
					int diff;

					//The number of different parameters is different for methods with variable arity
					if (!m.isVariableArity())
						diff = Math.abs(m.parameters().size() - ex.parameters().size());

					else
						diff = (m.parameters().size() <= (ex.parameters().size()+1))
								? 0 : m.parameters().size() - (ex.parameters().size()+1);

					if (diff + p.second() <= MAX_DIFFERENCE) methods.add(new Pair<Clazz.Method, Integer>(m, diff + p.second()));
				}
			}
		}

		//Next, check for different types of parameters
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
		ex.loader();
		msg.append(String.format("Method \"%s.%s(", ClassLoader.pathChild(ex.owner().toString()), ex.method()));
		boolean firstTime = true;
		for (Type t : ex.parameters()) {
			if (!firstTime)
				msg.append(", ");
			msg.append(t.toString());
		}
		msg.append(")\" not found.");
		if (suggestion == null)
			throw new SyntaxError(msg.toString(), loc.line(), loc.column());

		ex.loader();
		msg.append(String.format("\nA possible substitute is \"%s.%s(",
				ClassLoader.pathChild(ex.owner().toString()), suggestion.name()));

		firstTime = true;
		for (Type t : suggestion.type().parameterTypes()) {
			if (!firstTime)
				msg.append(", ");
			msg.append(t.toString());
			firstTime = false;
		}
		throw new SyntaxError(msg.toString()+")\"", loc.line(), loc.column());
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

		int[][] matrix = new int[src.length()+2][trg.length()+2];
		int inf = src.length() + trg.length();
		matrix[0][0] = inf;

		//Initialize the first few entries in the matrix

		for (int i = 0; i <= src.length(); i++) {
			matrix[i+1][1] = i;
			matrix[i+1][0] = inf;
		}

		for (int i = 0; i <= trg.length(); i++) {
			matrix[1][i+1] = i;
			matrix[0][i+1] = inf;
		}

		//Need to set up a mapping of characters to integers
		Map<Character, Integer> map = new HashMap<Character, Integer>();

		//Initialize the dictionary with the characters in the source and target words
		for (int i = 0; i < src.length(); i++) {
			char c = src.charAt(i);
			if (!map.containsKey(c))
				map.put(c, 0);
		}
		for (int i = 0; i < trg.length(); i++) {
			char c = trg.charAt(i);
			if (!map.containsKey(c))
				map.put(c, 0);
		}

		//Main loop for calculating the rows of the matrix
		//Once this has terminated, distance will be in matrix[src.length()+1][trg.length()+1]
		for (int i = 1; i <= src.length(); i++) {
			int DB = 0;
			for (int j = 1; j <= trg.length(); j++) {
				int i1 = map.get(trg.charAt(j-1));
				int j1 = DB;

				if (src.charAt(i-1) == trg.charAt(j-1)) {
					matrix[i+1][j+1] = matrix[i][j];
					DB = j;
				}
				else {
					matrix[i+1][j+1] = Math.min(matrix[i][j],
							Math.min(matrix[i+1][j], matrix[i][j+1])) + 1;
				}
				matrix[i+1][j+1] = Math.min(matrix[i+1][j+1], matrix[i1][j1] + (i-i1-1) + 1 + (j - j1 - 1));
			}
			map.put(src.charAt(i-1), i);
		}
		return matrix[src.length()+1][trg.length()+1];
	}

	/**
	 * Utility method that, when given a reference type, returns all the classes
	 * that can be substituted into that type
	 *
	 * @param loader - The ClassLoader required to load the classes
	 * @param owner  - The reference type we are checking
	 * @return
	 */
	private static Set<Clazz> getClasses(ClassLoader loader, Type.Reference owner) {
		Set<Clazz> classes = new HashSet<Clazz>();
		Stack<Type.Reference> stk = new Stack<Type.Reference>();
		stk.push(owner);

		while (!stk.isEmpty()) {
			Type.Reference ref = stk.pop();

			if (ref instanceof Type.Clazz)

				try {
					classes.add(loader.loadClass((Type.Clazz)ref));
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
		return classes;
	}

	/**
	 * Utility method that, when given a set of classes, returns the corresponding
	 * set of all classes and super classes.
	 *
	 * @param loader - A ClassLoader (to read classes)
	 * @param classes - The set of classes we want to expand to include parent classes
	 * @return
	 */
	private static Set<Clazz> getSuperClasses(ClassLoader loader,
			Set<Clazz> classes) {

		Set<Clazz> result = new HashSet<Clazz>();
		for (Clazz c : classes) {
			try {
				Clazz tmp = c;
				while (tmp != null && !result.contains(tmp)) {
					result.add(tmp);
					if (c.superClass() == null)
						break;

					tmp = loader.loadClass(c.superClass());
				}
			} catch (ClassNotFoundException e) {
				//Skip trying to load this superclass
				continue;
			}
		}

		return result;
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

	/**
	 * Exception for the case where a package isn't found during compilation.
	 *
	 * @author Daniel Campbell
	 *
	 */
	public static class PackageNotFoundException extends JKitException {

		private static final long serialVersionUID = 1L;
		private final List<String> classpath;
		private final List<String> sourcepath;
		private final Clazz jilClass;

		public PackageNotFoundException(Clazz jc, List<String> cp, List<String> sp) {
			jilClass = jc;
			classpath = cp;
			sourcepath = sp;
		}

		public Clazz jilClass() {
			return jilClass;
		}
	}
}
