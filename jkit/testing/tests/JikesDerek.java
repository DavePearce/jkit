package jkit.testing.tests;

import org.junit.Test;

import jkit.testing.TestHarness;

public class JikesDerek extends TestHarness {
	public JikesDerek() {
		super("tests/jikestst/derek/src", "java",
		      "tests/jikestst/derek/ok/exec", "e");
	}
		
	@Test public void TestArithmetic() { runTest("TestArithmetic"); }
	@Test public void TestArrayAccess() { runTest("TestArrayAccess"); }
	@Test public void TestClassInitializer() { runTest("TestClassInitializer"); }
	@Test public void TestCompare() { runTest("TestCompare"); }
	
	@Test public void TestConstants() { runTest("TestConstants"); }
	@Test public void TestConversions() { runTest("TestConversions"); }
	@Test public void TestFieldAccess() { runTest("TestFieldAccess"); }
	@Test public void TestFinally() { runTest("TestFinally"); }	
	@Test public void TestIO() { runTest("TestIO"); }
	@Test public void TestInstanceOf() { runTest("TestInstanceOf"); }
	@Test public void TestInterfaceCall() { runTest("TestInterfaceCall"); }
	@Test public void TestReturn() { runTest("TestReturn"); }
	@Test public void TestStackAccess() { runTest("TestStackAccess"); }
	@Test public void TestStackTrace() { runTest("TestStackTrace"); }
	@Test public void TestStaticCall() { runTest("TestStaticCall"); }
	@Test public void TestSwitch() { runTest("TestSwitch"); }
	@Test public void TestThrow() { runTest("TestThrow"); }	
	@Test public void TestVirtualCall() { runTest("TestVirtualCall"); }
	@Test public void Sort() { runTest("Sort","100"); }
}
