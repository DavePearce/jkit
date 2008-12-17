package jkit.java.stages;

import java.util.ArrayList;
import java.util.HashSet;

import jkit.java.Decl.Clazz;
import jkit.jil.Type;
import jkit.util.Pair;

/**
 * This method contains a variety of useful algorithms for deal with Java's type
 * system.
 * 
 * @author djp
 */
public class TypeSystem {

	
	/**
     * This method determines the common super type of the two types supplied.
     * 
     * @param refT1
     * @param refT2
     * @return
     * @throws ClassNotFoundException
     */
	public Type.Clazz findCommonSuperType(Type.Clazz refT1, Type.Clazz refT2)
			throws ClassNotFoundException {
		
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
			} else if (TypeSystem.descriptor(type, false).equals(
					TypeSystem.descriptor(refT2, false))) {
				// There are several cases here:
				//
				// 1) computing common supertype of erased and non-erased types.
                //    e.g. ArrayList<String> and ArrayList. In this case, we employ
                //    the unchecked assumption that they go together (as Java does)
				//
				// 2) computing common supertype of two non-erased types.
                //    e.g. ArrayList<String> and ArrayList<Integer>. In this case,
                //    we can generate ArrayList<?>
				Pair<String,Type[]>[] tClasses = type.classes();
				Pair<String,Type[]>[] rClasses = refT2.classes();
				@SuppressWarnings("unchecked")
				Pair<String,Type[]>[] nClasses = new Pair[tClasses.length];
				boolean failed = false;
				for(int i=0;i!=tClasses.length;++i) {
					Type[] ts = tClasses[i].second();
					Type[] rs = rClasses[i].second();					
					if(ts.length != rs.length) {
						if(ts.length == 0) {
							nClasses[i] = rClasses[i];							
						} else if(rs.length == 0) {
							nClasses[i] = tClasses[i];
						} else {
							// fall through, since this on doesn't work
							failed = true;
							break;
						}
					} else {
						Type[] ns = new Type[rs.length];
						for (int j = 0; j != ts.length; ++j) {
							if(!ts[j].supsetEqOfElem(rs[j]) && !rs[j].supsetEqOfElem(ts[j])) {
								// FIXME: need to deal with bounds here
								ns[j] = Type.wildcardType(new Type[0], new Type[0]);
							} else if(rs[j].supsetEqOfElem(ts[j])){
								ns[j] = rs[j];
							} else {
								ns[j] = ts[j];
							}							
						}
						nClasses[i] = new Pair<String, Type[]>(tClasses[i].first(),ns);
					}		
				}
				if(!failed) {
					return Type.referenceType(type.pkg(),nClasses);
				}				
			}
			closure.add(type);
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
}
