package jkit.java.stages;

/**
 * The aim of this class is purely to eliminate ambiguities over the scope of a
 * variable. There are several different situations that can arise for a given
 * variable:
 * 
 * <ol>
 * <li>It is declared locally (this is the easiest)</li>
 * <li>It is declared as a field of the current class</li>
 * <li>It is declared as a field of a superclass for the current class</li>
 * <li>It is declared as a field of an enclosing class (i.e. the current class
 * is an inner class)</li>
 * <li>It is declared as a field of a superclass of the enclosing class</li>
 * <li>It is declared as a local variable for an enclosing method (i.e. the
 * variable is used in an anonymous inner class, and the enclosing method has
 * final variable of the appropriate name).</li>
 * </ol>
 * 
 * As an example, consider the following case:
 * 
 * <pre>
 * public class Test {
 * 	public int f = 0;
 * 
 * 	public class Inner1 {
 * 		public int f = 1;
 * 	}
 * 
 * 	public class Inner2 extends Inner1 {
 * 		public void print() {
 * 			System.out.println(f);
 * 		}
 * 	}
 * 
 * 	public static void main(final String[] args) {
 * 		Test x = new Test();
 * 		Inner2 i = x.new Inner2();
 * 		i.print();
 * 	}
 * }
 * </pre>
 * 
 * Here, the question is: <i>what gets printed?</i> The answer is "1". The
 * reason is that scope resolution priorities superclasses over enclosing
 * classes.
 * 
 * The purpose of this class is to apply the rules from the Java Language Spec
 * to determine where a variable is defined, since this is not trivial. In
 * addition to determining the scope of variables, this class also must
 * determine the scope of method calls in a similar fashion. For example,
 * consider the following variant of the above:
 * 
 * <pre>
 * public class Test {
 * 	public int f() {
 * 		return 0;
 * 	}
 * 
 * 	public class Inner1 {
 * 		public int f() {
 * 			return 1;
 * 		}
 * 	}
 * 
 * 	public class Inner2 extends Inner1 {
 * 		public void print() {
 * 			System.out.println(f());
 * 		}
 * 	}
 * 
 * 	public static void main(final String[] args) {
 * 		Test x = new Test();
 * 		Inner2 i = x.new Inner2();
 * 		i.print();
 * 	}
 * }
 * </pre>
 * 
 * In order to resolve these situations, this class introduces "this" variables
 * appropriately. Thus, it modifies the source code slightly to do this. Let us
 * consider a final example to illustrate:
 * 
 * <pre>
 * public class Test {
 *   public int g() { return 0; }
 *
 *   public class Inner {
 *	   public int f;
 *
 *	   public void print() {
 *	     System.out.println(f);
 *	     System.out.println(g());
 * } } }
 * </pre>
 * 
 * This code would be transformed into the following, which remains valid Java:
 * 
 * <pre>
 * public class Test {
 *   public int g() { return 0; }
 *
 *   public class Inner {
 *	   public int f;
 *
 *	   public void print() {
 *	     System.out.println(this.f);
 *	     System.out.println(Test.this.g());
 * } } }
 * </pre>
 *
 * @author djp
 * 
 */
public class ScopePropagation {

}
