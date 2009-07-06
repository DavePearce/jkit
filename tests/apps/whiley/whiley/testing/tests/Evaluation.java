package whiley.testing.tests;

import org.junit.*;

import whiley.testing.*;

public class Evaluation extends TestHarness {
	public Evaluation() {
		super("tests/evaluation", "tests/evaluation", "sysout", false);
	}
	
	@Test public void Evaluation_1() { runTest("Evaluation_1"); }
	@Test public void Evaluation_3() { runTest("Evaluation_3"); }

	@Test public void Array_1() { runTest("Array_1"); }
	@Test public void Array_2() { runTest("Array_2"); }
	@Test public void Array_3() { runTest("Array_3"); }
	@Test public void Array_4() { runTest("Array_4"); }
	@Test public void Array_5() { runTest("Array_5"); }
	@Test public void Array_6() { runTest("Array_6"); }
	@Test public void Array_7() { runTest("Array_7"); }
	
	@Test public void While_1() { runTest("While_1"); }
	@Test public void While_2() { runTest("While_2"); }
	
	@Test public void Real_1() { runTest("Real_1"); }
	@Test public void Real_2() { runTest("Real_2"); }
}

