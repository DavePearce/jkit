package wone.testing;

import static org.junit.Assert.*;
import static wone.Main.checkUnsat;

import org.junit.Test;

/**
 * This set of tests checks uninterpreted functions.
 * @author djp
 *
 */
public class UninterpretedFunctions {
	/*
	 * ============= UNSAT TESTS ==============
	 */
	
	@Test public void Unsat_1() { 
		assertTrue(checkUnsat("f(y) == t1 && f(x) == t2 && x == y && t1 != t2"));
	}
	
	@Test public void Unsat_2() { 
		assertTrue(checkUnsat("f(y) == t1 && f(x) == t2 && x == z+1 && z == y-1 && t1 != t2"));
	}
	
	@Test public void Unsat_3() { 
		assertTrue(checkUnsat("f(x1,y1) == t1 && f(x2,y2) == t2 && x1 == x2 && y1 == y2 && t1 != t2"));
	}
	
	@Test public void Unsat_4() { 
		assertTrue(checkUnsat("f(y) != t1 && f(x) == t1 && x == y"));
	}
	
	@Test
	public void Unsat_5() {
		// this example is taken from a simple whiley program.
		assertTrue(checkUnsat("$9==0 && (length($0)==$1 && $2==0 && $1==3 && get($0,$4)==$5 && get($0,$6)==$7 && $3==1 && $5==2 && $7==3 && get($0,$2)==$3 && $4==1 && $6==2) && length(arr)==$11 && $11<=$10 && arr==$0 && get(arr,$9)==$10"));
	}
	
	/*
	 * ============= SAT TESTS ==============
	 */
	
	@Test public void Sat_1() { 
		assertFalse(checkUnsat("f(y) == t1 && f(x) == t2 && x == y && t1 == t2"));
	}
	
	@Test public void Sat_2() { 
		assertFalse(checkUnsat("f(y) == t1 && f(x) == t2 && x == z+1 && z == y-1 && t1 == t2"));
	}
	
	@Test public void Sat_3() { 
		assertFalse(checkUnsat("f(x1,y1) == t1 && f(x2,y2) == t2 && x1 == x2 && y1 == y2 && t1 == t2"));
	}
	
	@Test public void Sat_4() { 
		assertFalse(checkUnsat("f(y) != t1 && f(x) == t1 && x != y"));
	}
}
