package mocha.util;

import jkit.core.Method;
import jkit.core.FlowGraph.LocalVarDef;

public class Utils {
	
	public static String formatMethod(Method m) {
		String s = m.name() + "(";
		boolean first = true;
		for(LocalVarDef lvd : m.code().localVariables()) {
			if(lvd.isParameter()) {
				if(first) {
					s += lvd.type().toShortString() + " " + lvd.name();
					first = false;
				}
				else {
					s += ", " + lvd.type().toShortString() + " " + lvd.name();
				}
			}
		}
		
		s += ")";
		
		return s;
	}

}
