package wone.testing;

import static wone.Main.*;
import static org.junit.Assert.*;
import org.junit.*;

public class IntegerArithmetic {

	/*
	 * ============= UNSAT TESTS ==============
	 */
	
	@Test public void Unsat_1() { 
		assertTrue(checkUnsat("x > 0 && x <= 0"));
	}
	
	@Test public void Unsat_2() { 
		assertTrue(checkUnsat("x > 0 && x+1 <= 1"));
	}
	
	@Test public void Unsat_3() { 
		assertTrue(checkUnsat("x < y && y < 0 && 0 < x"));
	}
	
	@Test public void Unsat_4() { 
		assertTrue(checkUnsat("x < y && y <= 0 && 0 <= x"));
	}
	
	@Test public void Unsat_5() { 
		assertTrue(checkUnsat("x != y && x >= 0 && y >= 0 && x + y <= 0"));
	}
	
	@Test public void Unsat_6() { 
		assertTrue(checkUnsat("y == 2 * x && x == 5 && y < 10"));
	}
	
	@Test public void Unsat_7() { 
		assertTrue(checkUnsat("(y == 2 || y == -11) && (x == 3 || x == 10) && y >= x"));
	}
	
	@Test public void Unsat_8() { 
		assertTrue(checkUnsat("y == 2*x && (x == 3 || x == 10) && y < x"));
	}
	
	@Ignore("Known Bug") @Test public void Unsat_9() { 
		// this should fail because both x and y are assumed to be integer
		// variables... 
		assertTrue(checkUnsat("y == 2*x && y == 5"));
	}
	
	@Ignore("Known Bug") @Test public void Unsat_10() { 				
		assertTrue(checkUnsat("1 == 3*x + 6*y"));
	}
	
	@Ignore("Known Bug") @Test public void Unsat_11() { 
		assertTrue(checkUnsat("1 == 3*x + 6*y + z && z == 2"));
	}
	
	@Ignore("Known Bug") @Test public void Unsat_12() {
		assertTrue(checkUnsat("1 == 3*x + 3*y  || 2 == 3*x + 3*y"));
	}
	
	@Ignore("Known Bug") @Test public void Unsat_13() {
		assertTrue(checkUnsat("1 <= 3*x + 3*y  || 2 >= 3*x + 3*y"));
	}
	
	@Ignore("Known Bug") @Test public void Unsat_14() {
		assertTrue(checkUnsat("a = 5*q + r && r >= 0 && r < 5 && a == 10002 && r <= 1"));
	}
	
	@Ignore("Known Bug") @Test public void Unsat_15() {
		assertTrue(checkUnsat("(3*x + 3*y == 1) || (3*x + 6*y + 2*z == 1 && z == 2)"));
	}
	
	@Test public void Unsat_16() {
		assertTrue(checkUnsat("$<=0 && 0<=y && 0<=x && x<y && $==(y+x)"));
	}
	
	@Test public void Unsat_17() { 
		assertTrue(checkUnsat("x==y && 0<=x && $<=0 && 0<=y && $==1"));
	}
	
	@Test public void Unsat_18() { 
		assertTrue(checkUnsat("$==(x+y) && x!=y && 0<=x && $<=0 && 0<=y"));
	}
	
	/*
	 * ============= SAT TESTS ==============
	 */
	
	@Test public void Sat_1() { 
		assertFalse(checkUnsat("x >= 0 && x <= 0"));
	}
	
	@Test public void Sat_2() { 
		assertFalse(checkUnsat("x > 0 && x-1 <= 1"));
	}
	
	@Test public void Sat_3() { 
		assertFalse(checkUnsat("y < x && y < 0 && 0 < x"));
	}
	
	@Test public void Sat_4() { 
		assertFalse(checkUnsat("y < x && y <= 0 && 0 <= x"));
	}
	
	@Test public void Sat_5() { 
		assertFalse(checkUnsat("x != y && x >= 0 && y >= 0 && x + y <= 1"));
	}
	
	@Test public void Sat_6a() { 
		assertFalse(checkUnsat("x == 2 * y && x == 6 && y < 10"));
	}
	
	@Test public void Sat_6b() { 
		assertFalse(checkUnsat("x == 2 * y && x == 6 && y == 3"));
	}
	
	@Test public void Sat_7() { 
		assertFalse(checkUnsat("(y == 2 || y == -11) && (x == 3 || x == 10) && x > y"));
	}
	
	@Test public void Sat_8() { 
		assertFalse(checkUnsat("y == 2*x && (x == 3 || x == 10) && x <= y"));
	}
	
	@Test public void Sat_9() { 
		assertFalse(checkUnsat("1 == 3*x + 6*y + z && z == 1"));
	}
	
	
}