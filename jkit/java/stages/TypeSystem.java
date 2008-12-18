package jkit.java.stages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jkit.compiler.ClassLoader;
import jkit.jil.Type;
import jkit.jil.Clazz;
import jkit.util.Pair;

/**
 * This method contains a variety of useful algorithms for deal with Java's type
 * system.
 * 
 * @author djp
 */
public class TypeSystem {

	
	/**
     * This method determines whether t1 :> t2; that is, whether t2 is a subtype
     * of t1 or not, following the class heirarchy. Observe that this relation
     * is reflexive, transitive and anti-symmetric:
     * 
     * 1) t1 :> t1 always holds
     * 2) if t1 :> t2 and t2 :> t3, then t1 :> t3
     * 3) if t1 :> t2 then not t2 :> t1 (unless t1 == t2)
     * 
     * @param t1
     * @param t2
     * @return
     * @throws ClassNotFoundException
     */
	public boolean subtype(Type.Clazz t1, Type.Clazz t2, ClassLoader loader)
			throws ClassNotFoundException {
		
		ArrayList<Type.Clazz> worklist = new ArrayList<Type.Clazz>();
		HashSet<Type.Clazz> closure = new HashSet<Type.Clazz>();
		
		worklist.add(t2);
		
		// Ok, so the idea behind the worklist is to start from type t2, and
        // proceed up the class heirarchy visiting all supertypes (i.e. classes
        // + interfaces) of t2 until either we reach t1, or java.lang.Object.
		while(!worklist.isEmpty()) {
			Type.Clazz type = worklist.remove(worklist.size() - 1);
			if(type.equals(t1)) {
				return true;
			} else if(haveSameBaseComponents(type, t1)) {
				// at this point, we have reached a type with the same base
                // components as t1, but they are not identical. We now have to
                // check wether or not any types in the generic parameter
                // position are compatible or not.
			}
			
			Clazz c = loader.loadClass(type);
			
			// The current type we're visiting is not a match. Therefore, we
            // need to explore its supertypes as well. A key issue
            // in doing this, is that we must preserve the appropriate types
            // according to the class declaration in question. For example,
            // suppose we're checking:
			// 
			// subtype(List<String>,ArrayList<String>)
			
			test
			
			
			if (c.superClass() != null) {
				worklist.add((Type.Clazz) substituteTypes(type, c.type(), c
						.superClass()));
			}
			for (Type.Clazz t : c.interfaces()) {
				worklist
						.add((Type.Clazz) substituteTypes(type, c.type(), t));
			}
		}
		
		return false;
	}
	
	
		// this performs quite a complicated search,
		// which could be made more efficient
		// through caching and by recording which have
		// already been visited.

		ArrayList<Type.Clazz> worklist = new ArrayList<Type.Clazz>();
		HashSet<Type.Clazz> closure = new HashSet<Type.Clazz>();
		worklist.add(refT1);

		while (!worklist.isEmpty()) {
			// first, remove and check
			Type.Clazz type = worklist.remove(worklist.size() - 1);
			
			if (type.equals(refT2)) {
				return type;
			} else if (haveSameBaseComponents(type, refT2)) {
				// There are several cases here:
				//
				// 1) computing common supertype of erased and non-erased types.
                //    e.g. ArrayList<String> and ArrayList. In this case, we employ
                //    the unchecked assumption that they go together (as Java does)
				//
				// 2) computing common supertype of two non-erased types.
                //    e.g. ArrayList<String> and ArrayList<Integer>. In this case,
                //    we can generate ArrayList<?>
				List<Pair<String,List<Type>>> tClasses = type.components();
				List<Pair<String,List<Type>>> rClasses = refT2.components();
				List<Pair<String,List<Type>>> nClasses = new ArrayList();
				boolean failed = false;
				for(int i=0;i!=tClasses.size();++i) {
					List<Type> ts = tClasses.get(i).second();
					List<Type> rs = rClasses.get(i).second();					
					if(ts.size() != rs.size()) {
						if(ts.size() == 0) {
							nClasses.add(rClasses.get(i));							
						} else if(rs.size() == 0) {
							nClasses.add(tClasses.get(i));
						} else {
							// fall through, since this one doesn't work
							failed = true;
							break;
						}
					} else {
						ArrayList<Type> ns = new ArrayList<Type>();
						for (int j = 0; j != ts.size(); ++j) {
							Type ts_t = ts.get(j);
							Type rs_t = rs.get(j);
							if(!ts_t.supsetEqOfElem(rs_t)) && !rs_t.supsetEqOfElem(ts_t)) {
								// FIXME: need to deal with bounds here
								ns.add(new Type.Wildcard(null,null));
							} else if(rs_t.supsetEqOfElem(ts_t)){
								ns.add(rs_t);
							} else {
								ns.add(ts_t);								
							}							
						}
						nClasses.add(new Pair<String, List<Type>>(tClasses.get(i).first(),ns));
					}		
				}
				if(!failed) {
					return new Type.Clazz(type.pkg(),nClasses);
				}				
			}
			closure.add(type);
			Clazz c = loader.loadClass(type);

			// no match yet, so traverse super class and interfaces
			if (c.superClass() != null) {
				worklist.add((Type.Clazz) substituteTypes(type, c.type(), c
						.superClass()));
			}
			for (Type.Clazz t : c.interfaces()) {
				worklist
						.add((Type.Clazz) substituteTypes(type, c.type(), t));
			}
		}
		// Now, do the second half of the search
		worklist.add(refT2);

		while (!worklist.isEmpty()) {
			// first, remove and check
			Type.Clazz type = worklist.remove(worklist.size() - 1);
			for(Type.Clazz t : closure) {
				if(t.equals(type)) {
					return t;
				}
			}
			
			if (closure.contains(type)) {
				return type;
			}
			Clazz c = findClass(type);
			// no match yet, so traverse super class and interfaces
			if (c.superClass() != null) {
				worklist.add((Type.Clazz) substituteTypes(type, c.type(), c
						.superClass()));
			}
			for (Type.Clazz t : c.interfaces()) {
				worklist
						.add((Type.Clazz) substituteTypes(type, c.type(), t));
			}
		}

		// this is always true!
		return new Type.Clazz("java.lang","Object");
	}
	
	/**
     * This method checks whether the two types in question have the same base
     * components. So, for example, ArrayList<String> and ArrayList<Integer>
     * have the same base component --- namely, ArrayList.
     * 
     * @param t
     * @return
     */
	protected boolean haveSameBaseComponents(Type.Clazz t1, Type.Clazz t2) {
		List<Pair<String, List<Type>>> t1components = t1.components();
		List<Pair<String, List<Type>>> t2components = t2.components();

		// first, check they have the same number of components.
		if(t1components.size() != t2components.size()) {
			return false;
		}
		
		// second, check each component in turn
		for(int i=0;i!=t1components.size();++i) {
			String t1c = t1components.get(i).first();
			String t2c = t2components.get(i).first();
			if(!t1c.equals(t2c)) {
				return false;
			}
		}
		return true;
	}
}
