package jkit.error;

import jkit.compiler.ClassLoader;
import jkit.java.stages.TypeSystem;
import jkit.java.tree.Expr;
import jkit.jil.tree.Type;
import jkit.jil.util.Types;

/**
 * Exception for the more specific case where an expression does not have the required type for
 * an operator. Carries additional context information in the form of the operator that threw the
 * exception, and what type(s) the operator will accept.
 *
 * @author Daniel Campbell
 */
public class OperatorTypeMismatchException extends TypeMismatchException {

	private static final long serialVersionUID = 1L;

	/**
	 * Enum used to define what types are allowed for a given operator
	 */
	public static enum AllowedType {
		BOOL, PRIMITIVE, NUMBER, INTEGER, INT, ANY

	}

	private final String operator; //Additional context information
	private final String allowed;

	public OperatorTypeMismatchException(Expr f, Type e, ClassLoader l,
			TypeSystem t, String o, AllowedType a) {
		super(f, e, l, t);
		operator = o;
		allowed = getAllowedString(a);
	}

	public String allowed() {
		return allowed;
	}

	public String operator() {
		return operator;
	}

	/**
	 * Utility method for printing out error messages tailored to an operator's allowed types
	 */
	private String getAllowedString(AllowedType a) {

		switch(a) {

		case BOOL:
			return Types.T_BOOL.toString();

		case PRIMITIVE:
			return "primitive (non object) value";

		case NUMBER:
			return "primitive number";

		case INTEGER:
			return "byte, short, int or long";

		case INT:
			return "int";

		case ANY:  //This type won't come up in errors
			return "any type";

		}

		return null; //Dead code;
	}

	/**
	 * Utility method, checks if a given type is allowed by an operator
	 */
	public static boolean checkTypeAllowed(Type type, AllowedType allowed) {
		switch (allowed) {

		case BOOL:
			return type instanceof Type.Bool;

		case PRIMITIVE:
			return type instanceof Type.Primitive;

		case NUMBER:
			return (!(type instanceof Type.Bool) && type instanceof Type.Primitive);

		case INT:
			return type instanceof Type.Int;

		case ANY:
			return true;

		case INTEGER:
			return  type instanceof Type.Byte	||
					type instanceof Type.Short	||
					type instanceof Type.Int	||
					type instanceof Type.Long;
		}

		return false; //Dead code
	}
}
