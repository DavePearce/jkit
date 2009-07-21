// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

package jkit.testing.tests;

import org.junit.*;

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
	
	// Fails only because output doesn't contain line numbers 
	@Ignore("Known Bug") @Test public void TestStackAccess() { runTest("TestStackAccess"); }
	// Fails only because output doesn't contain line numbers
	@Ignore("Known Bug") @Test public void TestStackTrace() { runTest("TestStackTrace"); }
	@Test public void TestStaticCall() { runTest("TestStaticCall"); }
	@Test public void TestSwitch() { runTest("TestSwitch"); }
	@Test public void TestThrow() { runTest("TestThrow"); }	
	@Test public void TestVirtualCall() { runTest("TestVirtualCall"); }
	@Test public void Sort() { runTest("Sort","100"); }
}
