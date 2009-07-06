package whiley.testing.tests;

import org.junit.*;

import whiley.testing.*;

public class Checking extends TestHarness {
	public Checking() {
		super("tests/checking", "tests/checking", "sysout", false);
	}
	
	@Test public void Variables_1() { compileFailTest("Variables_1"); }
	@Test public void Variables_2() { compileFailTest("Variables_2"); }
	@Test public void Variables_3() { compileFailTest("Variables_3"); }
	@Test public void Variables_4() { compileFailTest("Variables_4"); }
	@Test public void Variables_5() { compileFailTest("Variables_5"); }
	@Test public void Variables_6() { compileFailTest("Variables_6"); }
	@Test public void Variables_7() { runTest("Variables_7"); }	
	
	@Test public void Return_1() { compileFailTest("Return_1"); }	
	@Test public void Return_2() { compileFailTest("Return_2"); }
	@Test public void Return_3() { compileFailTest("Return_3"); }
	@Test public void Return_4() { compileFailTest("Return_4"); }
	@Test public void Return_5() { compileFailTest("Return_5"); }
	@Test public void Return_6() { compileFailTest("Return_6"); }
	@Test public void Return_7() { compileFailTest("Return_7"); }
	@Test public void Return_8() { compileFailTest("Return_8"); }
	@Test public void Return_9() { compileFailTest("Return_9"); }
	@Test public void Return_10() { compileFailTest("Return_10"); }
	@Test public void Return_11() { compileFailTest("Return_11"); }
	
	@Test public void Assign_1() { compileFailTest("Assign_1"); }	
	@Test public void Assign_2() { compileFailTest("Assign_2"); }
	@Test public void Assign_3() { compileFailTest("Assign_3"); }
	@Test public void Assign_4() { compileFailTest("Assign_4"); }
	@Test public void Assign_5() { compileFailTest("Assign_5"); }
	@Test public void Assign_6() { compileFailTest("Assign_6"); }
	@Test public void Assign_7() { compileFailTest("Assign_7"); }
	
	@Test public void Void_1() { compileFailTest("Void_1"); }
	@Test public void Void_2() { compileFailTest("Void_2"); }
	
	@Test public void If_1() { compileFailTest("If_1"); }
	@Test public void If_2() { compileFailTest("If_2"); }
	@Test public void If_3() { compileFailTest("If_3"); }
	
	@Test public void While_1() { compileFailTest("While_1"); }
	@Test public void Assert_1() { compileFailTest("Assert_1"); }
	
	@Test public void Array_1() { compileFailTest("Array_1"); }
	@Test public void Array_2() { compileFailTest("Array_2"); }
	@Test public void Array_3() { compileFailTest("Array_3"); }
	@Test public void Array_4() { compileFailTest("Array_4"); }
	@Test public void Array_5() { compileFailTest("Array_5"); }
	@Test public void Array_6() { compileFailTest("Array_6"); }
	
	@Test public void Real_1() { compileFailTest("Real_1"); }
	@Test public void Real_2() { compileFailTest("Real_2"); }
	@Test public void Real_3() { compileFailTest("Real_3"); }
	@Test public void Real_4() { compileFailTest("Real_4"); }
	@Test public void Real_5() { compileFailTest("Real_5"); }
	@Test public void Real_6() { compileFailTest("Real_6"); }
}

