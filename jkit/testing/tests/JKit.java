package jkit.testing.tests;

import org.junit.*;
import jkit.testing.*;

public class JKit extends TestHarness {
	public JKit() {
		super("tests/jkit/src", "java", "tests/jkit/output", "sysout");
	}
												
	@Test public void ClassSignature_1() { runTest("ClassSignature_1"); }
	@Test public void ClassSignature_2() { runTest("ClassSignature_2"); }
	
	@Test public void FieldInit_1() { runTest("FieldInit_1"); }
	@Test public void FieldSignature_1() { runTest("FieldSignature_1"); }
	@Test public void FieldSignature_2() { runTest("FieldSignature_2"); }
	
	@Test public void FreeVariables() { compileFailTest("FreeVariables"); }
	
	@Test public void HeapException_1() { runTest("HeapException_1"); }
	@Test public void ImplicitConversions_1() { runTest("ImplicitConversions_1"); }
	
	@Test public void InfiniteForLoop() { runTest("InfiniteForLoop"); }

	@Test public void TypeBounds_1() { runTest("TypeBounds_1"); }
	@Test public void TypeBounds_2() { compileFailTest("TypeBounds_2"); }
	@Test public void TypeBounds_3() { runTest("TypeBounds_3"); }
	@Test public void TypeBounds_4() { runTest("TypeBounds_4"); }
	@Test public void TypeBounds_5() { runTest("TypeBounds_5"); }	
	// Fails because need to check that a declared intersection type is only
	// between at most one class.
	@Ignore("Known Bug") @Test public void TypeBounds_6() { compileFailTest("TypeBounds_6"); }	
	
	@Test public void TypeBounds_7() { runTest("TypeBounds_7"); }
	
	@Test public void WhileLoop_1() { runTest("WhileLoop_1"); }

	@Test public void Imports_1() { runTest("Imports_1"); }
	@Test public void Imports_2() { runTest("Imports_2"); }
			
	@Test public void JLS_3_3_UnicodeEscapes_1() { runTest("JLS_3_3_UnicodeEscapes_1"); }
	@Test public void JLS_3_10_4_CharacterLiterals_1() { runTest("JLS_3_10_4_CharacterLiterals_1"); }
	
	@Test public void JLS_4_3_2_GetClass_1() { runTest("JLS_4_3_2_GetClass_1"); }
	
	@Test public void JLS_5_1_3_NarrowingConversions_1() { runTest("JLS_5_1_3_NarrowingConversions_1"); }
	@Test public void JLS_5_1_7_BoxingConversion_1() { runTest("JLS_5_1_7_BoxingConversion_1"); }
	@Test public void JLS_5_1_7_BoxingConversion_2() { runTest("JLS_5_1_7_BoxingConversion_2"); }
	@Test public void JLS_5_1_7_BoxingConversion_3() { runTest("JLS_5_1_7_BoxingConversion_3"); }
	@Test public void JLS_5_5_CastingConversion_1() { runTest("JLS_5_5_CastingConversion_1"); }
	@Test public void JLS_5_5_CastingConversion_2() { runTest("JLS_5_5_CastingConversion_2"); }
	@Test public void JLS_5_6_2_BinaryPromotion_1() { runTest("JLS_5_6_2_BinaryPromotion_1"); }

	@Test public void JLS_6_8_6_ConstantNames_1() { runTest("JLS_6_8_6_ConstantNames_1"); }
	@Ignore("Known bug") @Test public void JLS_7_5_3_StaticImport_1() { runTest("JLS_7_5_3_StaticImport_1"); }
	
	@Test public void JLS_8_1_2_GenericTypes_1() { runTest("JLS_8_1_2_GenericTypes_1"); }	
	@Test public void JLS_8_1_2_GenericTypes_2() { runTest("JLS_8_1_2_GenericTypes_2"); }
	@Test public void JLS_8_1_2_GenericTypes_3() { runTest("JLS_8_1_2_GenericTypes_3"); }	
	@Test public void JLS_8_1_2_GenericTypes_4() { runTest("JLS_8_1_2_GenericTypes_4"); }
	@Test public void JLS_8_1_2_GenericTypes_5() { runTest("JLS_8_1_2_GenericTypes_5"); }
	@Test public void JLS_8_1_2_GenericTypes_6() { runTest("JLS_8_1_2_GenericTypes_6"); }
	@Test public void JLS_8_1_2_GenericTypes_7() { runTest("JLS_8_1_2_GenericTypes_7"); }
	@Test public void JLS_8_1_2_GenericTypes_8() { runTest("JLS_8_1_2_GenericTypes_8"); }
	@Test public void JLS_8_1_2_GenericTypes_9() { runTest("JLS_8_1_2_GenericTypes_9"); }
	
	@Test public void JLS_8_1_3_InnerClasses_1() { runTest("JLS_8_1_3_InnerClasses_1"); }
	@Test public void JLS_8_1_3_InnerClasses_2() { runTest("JLS_8_1_3_InnerClasses_2"); }
	@Test public void JLS_8_1_3_InnerClasses_3() { runTest("JLS_8_1_3_InnerClasses_3"); }
	@Test public void JLS_8_1_3_InnerClasses_4() { runTest("JLS_8_1_3_InnerClasses_4"); }	
	@Test public void JLS_8_1_3_InnerClasses_5() { runTest("JLS_8_1_3_InnerClasses_5"); }
	@Test public void JLS_8_1_3_InnerClasses_6() { runTest("JLS_8_1_3_InnerClasses_6"); }
	@Test public void JLS_8_1_3_InnerClasses_7() { runTest("JLS_8_1_3_InnerClasses_7"); }
	@Test public void JLS_8_1_3_InnerClasses_8() { runTest("JLS_8_1_3_InnerClasses_8"); }
	@Test public void JLS_8_1_3_InnerClasses_9() { runTest("JLS_8_1_3_InnerClasses_9"); }
	@Test public void JLS_8_1_3_InnerClasses_10() { runTest("JLS_8_1_3_InnerClasses_10"); }
	// Fails because no support for classes declared inside methods.
	@Ignore("Known Bug") @Test public void JLS_8_1_3_InnerClasses_11() { runTest("JLS_8_1_3_InnerClasses_11"); }
	
	@Ignore("Known Bug") @Test public void JLS_8_1_1_3_StrictClasses_1() { runTest("JLS_8_1_1_3_StrictClasses_1"); }
	
	@Test public void JLS_8_3_2_FieldInit_2() { runTest("JLS_8_3_2_FieldInit_2"); }
	
	@Test public void JLS_8_4_4_GenericMethods_1() { runTest("JLS_8_4_4_GenericMethods_1"); }
	@Test public void JLS_8_4_4_GenericMethods_2() { runTest("JLS_8_4_4_GenericMethods_2"); }
	@Ignore("Known Bug") @Test public void JLS_8_4_4_GenericMethods_3() { runTest("JLS_8_4_4_GenericMethods_3"); }
	@Test public void JLS_8_6_InstanceInitializers_1() { runTest("JLS_8_6_InstanceInitializers_1"); }
	@Test public void JLS_8_7_StaticInitializers_1() { runTest("JLS_8_7_StaticInitializers_1"); }
	@Test public void JLS_8_7_StaticInitializers_2() { runTest("JLS_8_7_StaticInitializers_2"); }
	@Test public void JLS_8_8_7_2_Super_1() { runTest("JLS_8_8_7_2_Super_1"); }
	@Test public void JLS_8_8_7_2_Super_2() { runTest("JLS_8_8_7_2_Super_2"); }
	@Test public void JLS_8_8_7_2_Super_3() { runTest("JLS_8_8_7_2_Super_3"); }
	@Test public void JLS_8_8_7_2_Super_4() { runTest("JLS_8_8_7_2_Super_4"); }	
	
	// Fails because no support for enums as yet!
	@Test public void JLS_8_9_Enums_1() { runTest("JLS_8_9_Enums_1"); }
	@Test public void JLS_8_9_Enums_2() { runTest("JLS_8_9_Enums_2"); }
	@Test public void JLS_8_9_Enums_3() { runTest("JLS_8_9_Enums_3"); }
	
	@Test public void JLS_9_3_ConstantFields_1() { runTest("JLS_9_3_ConstantFields_1"); }
	@Test public void JLS_9_3_ConstantFields_2() { runTest("JLS_9_3_ConstantFields_2"); }
	
	@Test public void JLS_10_6_ArrayInitialisers_1() { runTest("JLS_10_6_ArrayInitialisers_1"); }
	@Test public void JLS_10_6_ArrayInitialisers_2() { compileFailTest("JLS_10_6_ArrayInitialisers_2"); }
	@Test public void JLS_10_6_ArrayInitialisers_3() { runTest("JLS_10_6_ArrayInitialisers_3"); }
	@Test public void JLS_10_7_ArrayMembers_1() { runTest("JLS_10_7_ArrayMembers_1"); }
	// Fails because of broken return type for array clone method
	@Ignore("Known Bug") @Test public void JLS_10_7_ArrayMembers_2() { runTest("JLS_10_7_ArrayMembers_2"); }
	
	@Test public void JLS_14_9_IfStatement_1() { runTest("JLS_14_9_IfStatement_1"); }
	@Test public void JLS_14_9_IfStatement_2() { runTest("JLS_14_9_IfStatement_2"); }
	@Test public void JLS_14_9_IfStatement_3() { runTest("JLS_14_9_IfStatement_3"); }
	@Test public void JLS_14_9_IfStatement_4() { runTest("JLS_14_9_IfStatement_4"); }	
	@Test public void JLS_14_9_IfStatement_5() { runTest("JLS_14_9_IfStatement_5"); }
	
	@Test public void JLS_14_10_Assert_1() { runTest("JLS_14_10_Assert_1"); }
	@Test public void JLS_14_11_SwitchStatement_1() { runTest("JLS_14_11_SwitchStatement_1"); }
	@Test public void JLS_14_11_SwitchStatement_2() { runTest("JLS_14_11_SwitchStatement_2"); }
	@Test public void JLS_14_11_SwitchStatement_3() { runTest("JLS_14_11_SwitchStatement_3"); }
	// Fails due to some classpath issue.
	@Ignore("Known Bug") @Test public void JLS_14_11_SwitchStatement_4() { runTest("JLS_14_11_SwitchStatement_4"); }
	@Test public void JLS_14_11_SwitchStatement_5() { runTest("JLS_14_11_SwitchStatement_5"); }
	// fails because of constant used for switch case
	@Ignore("Known Bug") @Test public void JLS_14_11_SwitchStatement_6() { runTest("JLS_14_11_SwitchStatement_6"); }
	// should fail because non-constant uses for switch case
	@Ignore("Known Bug") @Test public void JLS_14_11_SwitchStatement_7() { compileFailTest("JLS_14_11_SwitchStatement_7"); }
	@Test public void JLS_14_12_WhileStatement_1() { runTest("JLS_14_12_WhileStatement_1"); }
	@Test public void JLS_14_14_ForStatement_1() { runTest("JLS_14_14_ForStatement_1"); }
	@Test public void JLS_14_14_ForStatement_2() { runTest("JLS_14_14_ForStatement_2"); }
	@Test public void JLS_14_14_ForStatement_3() { runTest("JLS_14_14_ForStatement_3"); }
	@Test public void JLS_14_14_ForStatement_4() { runTest("JLS_14_14_ForStatement_4"); }
	@Test public void JLS_14_14_ForStatement_5() { runTest("JLS_14_14_ForStatement_5"); }
	@Test public void JLS_14_14_ForStatement_6() { runTest("JLS_14_14_ForStatement_6"); }
	// fails because labelled blocks aren't supported.
	@Ignore("Known Bug") @Test public void JLS_14_14_ForStatement_7() { runTest("JLS_14_14_ForStatement_7"); }
	@Test public void JLS_14_14_ForStatement_8() { runTest("JLS_14_14_ForStatement_8"); }
	@Test public void JLS_14_14_ForStatement_9() { runTest("JLS_14_14_ForStatement_9"); }
	
	@Test public void JLS_14_20_TryStatement_1() { runTest("JLS_14_20_TryStatement_1"); }
	@Test public void JLS_14_20_TryStatement_2() { runTest("JLS_14_20_TryStatement_2"); }
	@Test public void JLS_14_20_TryStatement_3() { runTest("JLS_14_20_TryStatement_3"); }
	@Test public void JLS_14_20_TryStatement_4() { runTest("JLS_14_20_TryStatement_4"); }	
	@Test public void JLS_14_20_TryStatement_5() { runTest("JLS_14_20_TryStatement_5"); }
	@Test public void JLS_14_20_TryStatement_6() { runTest("JLS_14_20_TryStatement_6"); }
	@Test public void JLS_14_20_TryStatement_7() { runTest("JLS_14_20_TryStatement_7"); }
	
	@Test public void JLS_15_6_ArrayAssignment_1() { runTest("JLS_15_6_ArrayAssignment_1"); }
	@Test public void JLS_15_7_EvaluationOrder_1() { runTest("JLS_15_7_EvaluationOrder_1"); }
	@Test public void JLS_15_7_EvaluationOrder_2() { runTest("JLS_15_7_EvaluationOrder_2"); }
	@Test public void JLS_15_7_EvaluationOrder_3() { runTest("JLS_15_7_EvaluationOrder_3"); }
	@Test public void JLS_15_9_5_AnonymousClasses_1() { runTest("JLS_15_9_5_AnonymousClasses_1"); }
	@Test public void JLS_15_9_5_AnonymousClasses_2() { runTest("JLS_15_9_5_AnonymousClasses_2"); }
	@Test public void JLS_15_9_5_AnonymousClasses_3() { runTest("JLS_15_9_5_AnonymousClasses_3"); }
	@Test public void JLS_15_9_5_AnonymousClasses_4() { runTest("JLS_15_9_5_AnonymousClasses_4"); }
	@Test public void JLS_15_9_5_AnonymousClasses_5() { runTest("JLS_15_9_5_AnonymousClasses_5"); }
	@Test public void JLS_15_9_5_AnonymousClasses_6() { runTest("JLS_15_9_5_AnonymousClasses_6"); }
	@Test public void JLS_15_9_5_AnonymousClasses_7() { runTest("JLS_15_9_5_AnonymousClasses_7"); }
	@Test public void JLS_15_9_5_AnonymousClasses_8() { runTest("JLS_15_9_5_AnonymousClasses_8"); }
	@Test public void JLS_15_9_5_AnonymousClasses_9() { runTest("JLS_15_9_5_AnonymousClasses_9"); }	
	@Test public void JLS_15_9_5_AnonymousClasses_10() { runTest("JLS_15_9_5_AnonymousClasses_10"); }
	@Test public void JLS_15_9_5_AnonymousClasses_11() { runTest("JLS_15_9_5_AnonymousClasses_11"); }
	@Test public void JLS_15_9_5_AnonymousClasses_12() { runTest("JLS_15_9_5_AnonymousClasses_12"); }
	@Test public void JLS_15_9_5_AnonymousClasses_13() { runTest("JLS_15_9_5_AnonymousClasses_13"); }
	@Test public void JLS_15_12_VarArgs_1() { runTest("JLS_15_12_VarArgs_1"); }
	@Test public void JLS_15_12_VarArgs_2() { runTest("JLS_15_12_VarArgs_2"); }
	@Test public void JLS_15_12_VarArgs_3() { compileFailTest("JLS_15_12_VarArgs_3"); }
	@Test public void JLS_15_12_VarArgs_4() { runTest("JLS_15_12_VarArgs_4"); }
	@Test public void JLS_15_12_VarArgs_5() { runTest("JLS_15_12_VarArgs_5"); }
	@Test public void JLS_15_12_VarArgs_6() { runTest("JLS_15_12_VarArgs_6"); }		
	@Test public void JLS_15_13_ArrayAccess_1() { runTest("JLS_15_13_ArrayAccess_1"); }
	@Test public void JLS_15_13_ArrayAccess_2() { runTest("JLS_15_13_ArrayAccess_2"); }
	@Test public void JLS_15_13_ArrayAccess_3() { runTest("JLS_15_13_ArrayAccess_3"); }
	@Test public void JLS_15_13_ArrayAccess_4() { runTest("JLS_15_13_ArrayAccess_4"); }
	@Test public void JLS_15_13_ArrayAccess_5() { runTest("JLS_15_13_ArrayAccess_5"); }
	@Test public void JLS_15_13_ArrayAccess_6() { runTest("JLS_15_13_ArrayAccess_6"); }
	
	@Test public void JLS_15_14_PostfixOperators_1() { runTest("JLS_15_14_PostfixOperators_1"); }
	@Test public void JLS_15_14_PostfixOperators_2() { runTest("JLS_15_14_PostfixOperators_2"); }
	@Test public void JLS_15_14_PostfixOperators_3() { runTest("JLS_15_14_PostfixOperators_3"); }	
	@Test public void JLS_15_17_BinaryOperators_1() { runTest("JLS_15_17_BinaryOperators_1"); }
	@Test public void JLS_15_17_BinaryOperators_2() { runTest("JLS_15_17_BinaryOperators_2"); }
	@Test public void JLS_15_17_1_MultiplicationOperator_1() { runTest("JLS_15_17_1_MultiplicationOperator_1"); }	
	@Test public void JLS_15_17_1_MultiplicationOperator_2() { runTest("JLS_15_17_1_MultiplicationOperator_2"); }
	@Test public void JLS_15_17_2_DivisionOperator_1() { runTest("JLS_15_17_2_DivisionOperator_1"); }
	@Test public void JLS_15_17_2_DivisionOperator_2() { runTest("JLS_15_17_2_DivisionOperator_2"); }
	@Test public void JLS_15_17_3_RemainderOperator_1() { runTest("JLS_15_17_3_RemainderOperator_1"); }
	@Test public void JLS_15_17_3_RemainderOperator_2() { runTest("JLS_15_17_3_RemainderOperator_2"); }
	@Test public void JLS_15_18_2_AdditionOperator_1() { runTest("JLS_15_18_2_AdditionOperator_1"); }
	@Test public void JLS_15_18_2_SubtractionOperator_1() { runTest("JLS_15_18_2_SubtractionOperator_1"); }	
	@Test public void JLS_15_18_2_SubtractionOperator_2() { runTest("JLS_15_18_2_SubtractionOperator_2"); }
	@Test public void JLS_15_19_ShiftOperators_1() { runTest("JLS_15_19_ShiftOperators_1"); }
	@Test public void JLS_15_19_ShiftOperators_2() { runTest("JLS_15_19_ShiftOperators_2"); }
	@Test public void JLS_15_20_2_InstanceofOperator_1() { runTest("JLS_15_20_2_InstanceofOperator_1"); }	
	@Test public void JLS_15_20_2_InstanceofOperator_2() { compileFailTest("JLS_15_20_2_InstanceofOperator_2"); }
	@Test public void JLS_15_24_ConditionalOr_1() { runTest("JLS_15_24_ConditionalOr_1"); }
	@Test public void JLS_15_25_ConditionalOperator_1() { runTest("JLS_15_25_ConditionalOperator_1"); }
	@Test public void JLS_15_25_ConditionalOperator_2() { runTest("JLS_15_25_ConditionalOperator_2"); }
	@Test public void JLS_15_25_ConditionalOperator_3() { runTest("JLS_15_25_ConditionalOperator_3"); }
	@Test public void JLS_15_25_ConditionalOperator_4() { runTest("JLS_15_25_ConditionalOperator_4"); }
	@Test public void JLS_15_26_2_CompoundAssignmentOperators_1() { runTest("JLS_15_26_2_CompoundAssignmentOperators_1"); }	

	// Fails because: need definite assignment test
	@Ignore("Known Bug") @Test public void JLS_16_3_DefiniteAssignment_1() { compileFailTest("JLS_16_3_DefiniteAssignment_1"); }
	@Ignore("Known Bug") @Test public void JLS_16_3_DefiniteAssignment_2() { compileFailTest("JLS_16_3_DefiniteAssignment_2"); }
	@Ignore("Known Bug") @Test public void JLS_16_3_DefiniteAssignment_3() { compileFailTest("JLS_16_3_DefiniteAssignment_3"); }
	@Ignore("Known Bug") @Test public void JLS_16_3_DefiniteAssignment_4() { compileFailTest("JLS_16_3_DefiniteAssignment_4"); }
	@Ignore("Known Bug") @Test public void JLS_16_DefiniteUnassignment_1() { runTest("JLS_16_DefiniteUnassignment_1"); }
	@Ignore("Known Bug") @Test public void JLS_16_DefiniteUnassignment_2() { compileFailTest("JLS_16_DefiniteUnassignment_2"); }
	@Ignore("Known Bug") @Test public void JLS_16_DefiniteUnassignment_3() { compileFailTest("JLS_16_DefiniteUnassignment_3"); }
	@Ignore("Known Bug") @Test public void JLS_16_DefiniteUnassignment_4() { compileFailTest("JLS_16_DefiniteUnassignment_4"); }

	@Test public void JLS_XXX_BypassMethod_1() { runTest("JLS_XXX_BypassMethod_1"); }
	@Test public void JLS_XXX_BypassMethod_2() { runTest("JLS_XXX_BypassMethod_2"); }
	@Test public void JLS_XXX_BypassMethod_3() { runTest("JLS_XXX_BypassMethod_3"); }
}

