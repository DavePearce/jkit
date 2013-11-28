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
import jkit.java.stages.TypeSystem;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.Type;
import jkit.jil.util.Types;
import jkit.util.Pair;
import jkit.compiler.Clazz;

/**
 * This class contains a collection of helper methods to increase
 * the usefulness of JKit's error messages by offering suggestions for
 * substitute code.
 *
 * @author Daniel Campbell
 *
 */
public class ErrorHandler {

	//The maximum difference between a target and a candidate to be considered for substitution
	public final static int MAX_DIFFERENCE = 3;

	/**
	 * Handles errors where an operator was used in an expression where the lhs or rhs
	 * were not valid types for the operator. Uses the same code as for the more general type
	 * mismatch exceptions to generate suggestions, but the error message contains more context information.
	 *
	 * @param ex	- The exception being handled
	 * @param loc	- The location of the exception in the source code
	 * @throws ClassNotFoundException
	 */
	public static void handleOperatorTypeMismatch(OperatorTypeMismatchException e,
			SourceLocation loc) {

		Expr found = e.found();
		Type foundType = found.attribute(Type.class);
		Type expected = e.expected();
		ClassLoader loader = e.loader();
		TypeSystem types = e.types();

		List<Pair<String, Integer>> suggestions = findTypeSuggestions(found, expected, loader, types);

		String suggestion = (suggestions.isEmpty()) ? "" : suggestions.get(0).first();

		String msg = String.format("Syntax Error: Type %s invalid for operator %s, %s required.%s",
				foundType, e.operator(), e.allowed(), suggestion);

		SyntaxError.syntax_error(msg, loc);
	}

	/**
	 * This method handles the broad case where JKit was expecting one type and found another.
	 * It analyzes the types found and expected, and then employs a mix of heuristics (such
	 * as the fact that if there's an assignment where a boolean is expected == could be used)
	 * and suggestions to improve the error message given.
	 *
	 * @param e - The TypeMismatchException containing the necessary data
	 * @param loc - The location of the error
	 * @throws ClassNotFoundException
	 */
	public static void handleTypeMismatch(TypeMismatchException e, SourceLocation loc) {
		Expr found = e.found();
		Type foundType = found.attribute(Type.class);
		Type expected = e.expected();
		ClassLoader loader = e.loader();
		TypeSystem types = e.types();

		List<Pair<String, Integer>> suggestions = findTypeSuggestions(found, expected, loader, types);

		String msg = String.format("Syntax Error: Expected %s, found %s%s", expected, foundType,
				(suggestions.isEmpty()) ? "" : suggestions.get(0).first());

		SyntaxError.syntax_error(msg, loc);
	}

	/**
	 * This work does the heavy lifting for the handleTypeMismatch and handleOperatorTypeMismatch
	 * methods - it generates a list of possible suggestions for the given type mismatch, with
	 * a weighting attached to each suggestion.
	 *
	 * @param found
	 * @param expected
	 * @param loader
	 * @param types
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static List<Pair<String, Integer>> findTypeSuggestions(
			Expr found, Type expected, ClassLoader loader,
			TypeSystem types) {


		Type foundType = found.attribute(Type.class);
		List<Pair<String, Integer>> suggestions = new ArrayList<Pair<String, Integer>>();

		if (expected instanceof Type.Bool) {
			//Special case for = instead of ==
			if (found instanceof Stmt.Assignment) {
				Stmt.Assignment assign = (Stmt.Assignment) found;
				suggestions.add(new Pair<String, Integer>
					(String.format("\nA possible substitute is %s == %s", assign.lhs(), assign.rhs()), 0));
			}
		}

		/* Failed type conversion from a primitive to a primitive.
		 * We can suggest an explicit cast if neither of the types was boolean
		 * (up to the user to decide if that's safe)
		 */
		if (expected instanceof Type.Primitive &&
				foundType instanceof Type.Primitive) {
			String suggestion = "";
			if (!(expected instanceof Type.Bool) && ! (found instanceof Type.Bool)) {
				suggestion = String.format("\nExplicit cast possible: (%s) (%s)", expected, found);
				suggestions.add(new Pair<String, Integer>(suggestion, 0));
			}
		}

		//Check if the boxed type has a method that fits
		//Boxed methods are weighted on number of parameters (since name edit distance doesn't apply)
		else if (foundType instanceof Type.Primitive) {
			try {
				Clazz boxed = loader.loadClass(Types.boxedType((Type.Primitive)foundType));
				for (Clazz.Method m : boxed.methods()) {
					if (m.type().returnType() instanceof Type.Void || !m.type().returnType().equals(expected))
						continue;

					int weight = (m.isVariableArity()) ? m.parameters().size()-1 : m.parameters().size();
					String suggestion = String.format("\nA possible substitute is: ((%s)(%s)).%s(",
							Types.boxedType((Type.Primitive) foundType), found, m.name());

					boolean first = true;
					for (Type t : m.type().parameterTypes()) {
						if (!first)
							suggestion += (", ");
						suggestion += t.toString();
						first = false;
					}
					if (m.isVariableArity())
						suggestion += "...";
					suggestion += ")";
					suggestions.add(new Pair<String, Integer>(suggestion, weight));
				}
			}
			catch (ClassNotFoundException e) {
				//Should never happen. We just skip this step if it does
			}
		}

		/* We check if found was a method call or field,
		 * and if it was, we check for alternative methods/fields.
		 */
		if (found instanceof Expr.Deref) {

			//Look for alternative fields
			Expr.Deref deref = (Expr.Deref)found;
			String field = deref.name();

			//If owner is null, something is very wrong
			Type.Reference owner = deref.target().attribute(Type.Reference.class);
			Set<Clazz> classes = getClasses(loader, owner);
			classes = getSuperClasses(loader, classes);

			Pair<List<Pair<Clazz.Field, Integer>>,
			     List<Pair<Clazz.Method, Integer>>> subs = findFieldSuggestions(classes, field, expected, loader, types);

			List<Pair<Clazz.Field, Integer>> fields = subs.first();
			List<Pair<Clazz.Method, Integer>> methods = subs.second();

			String suggestion;

			if (fields.isEmpty()) {
				if (methods.isEmpty()) {
					suggestion = "";
				}
				else suggestion = String.format("\nA possible substitute is method %s.%s()", ClassLoader.pathChild(owner.toString()),
						methods.get(0).first().name());
			}
			else if (methods.isEmpty()) {
				suggestion = String.format("\nA possible substitute is field %s.%s", ClassLoader.pathChild(owner.toString()),
						fields.get(0).first().name());
			}
			else {
				suggestion = (fields.get(0).second() > methods.get(0).second()) ?
						String.format("\nA possible substitute is method %s.%s()", ClassLoader.pathChild(owner.toString()),
								methods.get(0).first().name()) :
						String.format("\nA possible substitute is field %s.%s", ClassLoader.pathChild(owner.toString()),
										fields.get(0).first().name());
			}
			int weight = (fields.get(0).second() > methods.get(0).second()) ?
					methods.get(0).second() : fields.get(0).second();

			suggestions.add(new Pair<String, Integer>(suggestion, weight));
		}

		if (found instanceof Expr.Invoke) {

			//Look for alternative methods
			Expr.Invoke inv = (Expr.Invoke)found;

			Type.Reference owner = inv.target().attribute(Type.Reference.class);

			Set<Clazz> classes = getClasses(loader, owner);
			classes = getSuperClasses(loader, classes);

			//Need to get the concrete parameters
			List<Type> params = classes.iterator().next().methods(inv.name()).get(0).type().parameterTypes();
			List<Pair<Clazz.Method, Integer>> subs =
					findMethodSuggestions(classes, inv.name(), params, expected, loader, types);

			if (subs.isEmpty()) {}

			else {
				Clazz.Method suggestion = subs.get(0).first();
				StringBuilder msg = new StringBuilder();
				msg.append(String.format("\nA possible substitute is %s.%s(",
					ClassLoader.pathChild(owner.toString()), suggestion.name()));

				boolean firstTime = true;
				for (Type t : suggestion.type().parameterTypes()) {
					if (!firstTime)
						msg.append(", ");
					msg.append(t.toString());
					firstTime = false;
				}
				if (suggestion.isVariableArity())
					msg.append("...");

				msg.append(")");

				suggestions.add(new Pair<String, Integer>(msg.toString(),suggestions.get(0).second()));
			}

		}

		//A general catch all - if the expression is tied to some class, we will hunt through
		//that class's methods looking for substitutions
		if (found.attribute(Type.Clazz.class) != null) {
			try {
				Clazz foundClass = loader.loadClass(found.attribute(Type.Clazz.class));

				for (Clazz.Method m : foundClass.methods()) {
					if (m.type().returnType() instanceof Type.Void || !m.type().returnType().equals(expected))
						continue;

					int weight = (m.isVariableArity()) ? m.parameters().size()-1 : m.parameters().size();
					String suggestion = String.format("\nA possible substitute is: (%s).%s(",
							found, m.name());

					boolean first = true;
					for (Type t : m.type().parameterTypes()) {
						if (!first)
							suggestion += (", ");
						suggestion += t.toString();
						first = false;
					}
					if (m.isVariableArity())
						suggestion += "...";
					suggestion += ")";
					suggestions.add(new Pair<String, Integer>(suggestion, weight));
				}
			}
			catch(ClassNotFoundException e) {
				//Should never happen, if it does we just skip this step
			}
		}

		Collections.sort(suggestions, new WeightComparator<Pair<String, Integer>>());
		return suggestions;

	}

	/**
	 * A simpler version of the method for handling methods not found, must search up
	 * the class hierarchy looking for any possible fields that match.
	 * Also checks for similarly named methods that take no parameters,
	 * as it is possible that is what the user intended.
	 *
	 * @param ex - The FieldNotFound exception encountered
	 */
	public static void handleFieldNotFound(FieldNotFoundException ex, SourceLocation loc) {

		Set<Clazz> classes = getClasses(ex.loader(), ex.owner());
		classes = getSuperClasses(ex.loader(), classes);

		Pair<List<Pair<Clazz.Field, Integer>>,
		 List<Pair<Clazz.Method, Integer>>> suggestions = findFieldSuggestions(classes, ex.field(), null, ex.loader(), new TypeSystem());

		List<Pair<Clazz.Field, Integer>> fields = suggestions.first();
		List<Pair<Clazz.Method, Integer>> methods = suggestions.second();

		//Throw a Syntax Error with the appropriate error message

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

		throw new SyntaxError(String.format("Field %s.%s not found%s", ClassLoader.pathChild(ex.owner().toString()),
				ex.field(), suggestion), loc.line(), loc.column());
	}

	/**
	 * Helper method that returns a set of suggested fields and methods to replace a given field.
	 *
	 * @param classes	- The list of all classes to check
	 * @param field 	- The field we are substituting for
	 * @param expected	- the expected return type of the field (or null if unknown)
	 * @return
	 */
	private static Pair<List<Pair<Clazz.Field, Integer>>,
				 List<Pair<Clazz.Method, Integer>>> findFieldSuggestions
				 (Set<Clazz> classes, String field, Type expected,
						 ClassLoader loader, TypeSystem types) {

		List<Pair<Clazz.Field, Integer>> fields = new ArrayList<Pair<Clazz.Field, Integer>>();
		List<Pair<Clazz.Method, Integer>> methods = new ArrayList<Pair<Clazz.Method, Integer>>();

		//First, we look for misspelled field names (ignoring possible methods for now)
		//If we find a candidate, we give it a weighting based on its edit distance
		for (Clazz c : classes) {
			for (Clazz.Field f : c.fields()) {
				try {
					if (expected != null && !types.boxSubtype(expected, f.type(), loader))
						continue;
					int dist = distance(field, f.name());
					if (dist <= MAX_DIFFERENCE)
						fields.add(new Pair<Clazz.Field, Integer>(f, dist));
				}
				catch (ClassNotFoundException e) {
					//Shouldn't happen. We continue without adding to suggestions
				}
			}
		}

		//Now we check for methods with no parameters
		//If we find a candidate, it is given a weighting based on edit distance, with an extra weighting
		//for being a method
		for (Clazz c : classes) {

			for (Clazz.Method m : c.methods()) {
				try {
					if (m.type().returnType() instanceof Type.Void || !m.parameters().isEmpty())
						continue;
					if (expected != null && !types.boxSubtype(expected, m.type().returnType(), loader))
						continue;

					int dist = distance(field, m.name()) + 2;
					if (dist <= MAX_DIFFERENCE)
						methods.add(new Pair<Clazz.Method, Integer>(m, dist));
				}
				catch (ClassNotFoundException e) {
					//Shouldn't happen. We continue without adding to suggestions
				}
			}
		}

		//Finally, we sort the two lists and return them
		Collections.sort(fields, new WeightComparator<Pair<Clazz.Field, Integer>>());
		Collections.sort(methods, new WeightComparator<Pair<Clazz.Method, Integer>>());

		return new Pair<List<Pair<Clazz.Field, Integer>>,
				 List<Pair<Clazz.Method, Integer>>>(fields, methods);
	}

	/**
	 * Finds the directory containing the class with the bad package declaration,
	 * to suggest that directory as an alternative package
	 */
	public static void handlePackageNotFound(PackageNotFoundException ex) {
		Clazz jilClass = ex.jilClass();
		String pkg = jilClass.type().pkg().replace('.', File.pathSeparatorChar);
		List<String> sourcepath = ex.sourcepath();
		List<String> classpath = ex.classpath();
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
	 * @throws ClassNotFoundException
	 *
	 */
	public static void handleMethodNotFound(MethodNotFoundException ex, SourceLocation loc) {

		Set<Clazz> classes = getClasses(ex.loader(), ex.owner());
		classes = getSuperClasses(ex.loader(), classes);

		List<Pair<Clazz.Method, Integer>> suggestions = findMethodSuggestions(classes, ex.method(),
				ex.parameters(), null, ex.loader(), new TypeSystem());

		//For now, we will only suggest the top level suggestion - that could change
		Clazz.Method suggestion = (suggestions.isEmpty()) ? null : suggestions.get(0).first();

		StringBuilder msg = new StringBuilder(100);
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

		msg.append(String.format("\nA possible substitute is \"%s.%s(",
				ClassLoader.pathChild(ex.owner().toString()), suggestion.name()));

		firstTime = true;
		for (Type t : suggestion.type().parameterTypes()) {
			if (!firstTime)
				msg.append(", ");
			msg.append(t.toString());
			firstTime = false;
		}
		if (suggestion.isVariableArity())
			msg.append("...");
		throw new SyntaxError(msg.toString()+")\"", loc.line(), loc.column());
	}

	/**
	 * Helper method that returns a list of the possible substitutes/suggestions for a method
	 *
	 * @param classes		- The set of all classes to search
	 * @param method		- The name of the method you are looking for
	 * @param parameters	- The list of parameters of the method you are looking for
	 * @param expected		- The expected return type of the method (or null if unknown)
	 * @param loader		- The classloader (needed to check subtyping)
	 * @param types			- The TypeSystem (needed to check subtyping)
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static List<Pair<Clazz.Method, Integer>> findMethodSuggestions
		(Set<Clazz> classes, String method, List<Type> parameters, Type expected,
				ClassLoader loader, TypeSystem types) {

		List<Pair<Clazz.Method, Integer>> suggestions = new ArrayList<Pair<Clazz.Method, Integer>>();

		//First, we should look for misspelled method names (ignoring parameters for now)
		//Note if a method has an invalid return type, we ignore it
		//If we find a candidate, we give it a weighting based on its Levenshtein distance
		Set<Pair<String, Integer>> names = new HashSet<Pair<String, Integer>>();
		for (Clazz c : classes) {
			for (Clazz.Method m : c.methods()) {
				try {
					//We want to ignore superclass constructors
					if (m.name().equals(c.name()) && classes.contains(new Pair<Clazz, Integer>(c, 1)))
						continue;
					if (expected != null && !types.boxSubtype(expected, m.type().returnType(), loader))
						continue;

					int dist = distance(method, m.name());
					if (dist <= MAX_DIFFERENCE)
						names.add(new Pair<String, Integer>(m.name(), dist));
				}
				catch (ClassNotFoundException e) {
					//Shouldn't happen. Continue without considering this method
				}
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
						diff = Math.abs(m.parameters().size() - parameters.size());

					else
						diff = (m.parameters().size() <= (parameters.size()+1))
								? 0 : m.parameters().size() - (parameters.size()+1);

					if (diff + p.second() <= MAX_DIFFERENCE) methods.add(new Pair<Clazz.Method, Integer>(m, diff + p.second()));
				}
			}
		}

		//Next, check for different types of parameters
		//Again, a weighting is given based on the number of parameters changed
		for (Pair<Clazz.Method, Integer> p : methods) {
			try {
				List<Type> subParams = p.first().type().parameterTypes();
				List<Type> targetParams = parameters;

				int min = Math.min(targetParams.size(), subParams.size());
				int weight = p.second();


				for (int i = 0; i < min; i++) {
					if (!types.boxSubtype(targetParams.get(i), subParams.get(i), loader))
					weight++;
				}

				if (weight <= MAX_DIFFERENCE)
					suggestions.add(new Pair<Clazz.Method, Integer>(p.first(), weight));
			}
			catch (ClassNotFoundException e) {
				//Shouldn't happen, but if it does we simply skip over the method
			}
		}

		Collections.sort(suggestions, new WeightComparator<Pair<Clazz.Method,Integer>>());
		return suggestions;
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
}
