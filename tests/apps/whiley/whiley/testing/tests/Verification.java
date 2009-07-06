package whiley.testing.tests;

import org.junit.*;

import whiley.testing.*;

public class Verification extends TestHarness {
	public Verification() {
		super("tests/verification", "tests/verification", "sysout", true);
	}
	
    @Test public void DivideByZero_1() { compileFailTest("DivideByZero_1"); }
    @Test public void DivideByZero_2() { compileFailTest("DivideByZero_2"); }
	
	@Test public void Invoke_1() { compileFailTest("Invoke_1"); }
	@Test public void Invoke_2() { compileFailTest("Invoke_2"); }

	@Test public void Integer_1() { runTest("Integer_1"); }
	@Test public void Integer_2() { compileFailTest("Integer_2"); }
	
	@Test public void Boolean_1() { runTest("Boolean_1"); }
	@Test public void Boolean_2() { runTest("Boolean_2"); }	

	@Test public void While_1() { compileFailTest("While_1"); }
	@Ignore("Known Bug") @Test public void While_2() { compileFailTest("While_2"); }
	@Test public void While_3() { compileFailTest("While_3"); }
	@Test public void While_4() { runTest("While_4"); }
	
	@Test public void Real_1() { runTest("Real_1"); }
	@Test public void Real_2() { compileFailTest("Real_2"); }
	
	// The following test case generates a non-linear constraint which the
	// fourier-motzkin eliminator cannot handle and, hence, cannot discharge.
	@Ignore("Known Bug") @Test public void Real_3() { runTest("Real_3"); }
	
	@Test public void List_1() { compileFailTest("List_1"); }
	@Test public void List_2() { runTest("List_2"); }
	@Test public void List_3() { runTest("List_3"); }
	@Test public void List_4() { compileFailTest("List_4"); }
	@Test public void List_5() { compileFailTest("List_5"); }
	@Test public void List_6() { runTest("List_6"); }
	@Test public void List_7() { runTest("List_7"); }
	@Test public void List_8() { compileFailTest("List_8"); }
}

