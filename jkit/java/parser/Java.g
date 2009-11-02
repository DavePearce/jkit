/** A Java 1.5 grammar for ANTLR v3 derived from the spec
 *
 *  This is a very close representation of the spec; the changes
 *  are comestic (remove left recursion) and also fixes (the spec
 *  isn't exactly perfect).  I have run this on the 1.4.2 source
 *  and some nasty looking enums from 1.5, but have not really
 *  tested for 1.5 compatibility.
 *
 *  I built this with: java -Xmx100M org.antlr.Tool java.g 
 *  and got two errors that are ok (for now):
 *  java.g:691:9: Decision can match input such as
 *    "'0'..'9'{'E', 'e'}{'+', '-'}'0'..'9'{'D', 'F', 'd', 'f'}"
 *    using multiple alternatives: 3, 4
 *  As a result, alternative(s) 4 were disabled for that input
 *  java.g:734:35: Decision can match input such as "{'$', 'A'..'Z',
 *    '_', 'a'..'z', '\u00C0'..'\u00D6', '\u00D8'..'\u00F6',
 *    '\u00F8'..'\u1FFF', '\u3040'..'\u318F', '\u3300'..'\u337F',
 *    '\u3400'..'\u3D2D', '\u4E00'..'\u9FFF', '\uF900'..'\uFAFF'}"
 *    using multiple alternatives: 1, 2
 *  As a result, alternative(s) 2 were disabled for that input
 *
 *  You can turn enum on/off as a keyword :)
 *
 *  Version 1.0 -- initial release July 5, 2006 (requires 3.0b2 or higher)
 *
 *  Primary author: Terence Parr, July 2006
 *
 *  Version 1.0.1 -- corrections by Koen Vanderkimpen & Marko van Dooren,
 *      October 25, 2006;
 *      fixed normalInterfaceDeclaration: now uses typeParameters instead
 *          of typeParameter (according to JLS, 3rd edition)
 *      fixed castExpression: no longer allows expression next to type
 *          (according to semantics in JLS, in contrast with syntax in JLS)
 *
 *  Version 1.0.2 -- Terence Parr, Nov 27, 2006
 *      java spec I built this from had some bizarre for-loop control.
 *          Looked weird and so I looked elsewhere...Yep, it's messed up.
 *          simplified.
 *
 *  Version 1.0.3 -- Chris Hogue, Feb 26, 2007
 *      Factored out an annotationName rule and used it in the annotation rule.
 *          Not sure why, but typeName wasn't recognizing references to inner
 *          annotations (e.g. @InterfaceName.InnerAnnotation())
 *      Factored out the elementValue section of an annotation reference.  Created 
 *          elementValuePair and elementValuePairs rules, then used them in the 
 *          annotation rule.  Allows it to recognize annotation references with 
 *          multiple, comma separated attributes.
 *      Updated elementValueArrayInitializer so that it allows multiple elements.
 *          (It was only allowing 0 or 1 element).
 *      Updated localVariableDeclaration to allow annotations.  Interestingly the JLS
 *          doesn't appear to indicate this is legal, but it does work as of at least
 *          JDK 1.5.0_06.
 *      Moved the Identifier portion of annotationTypeElementRest to annotationMethodRest.
 *          Because annotationConstantRest already references variableDeclarator which 
 *          has the Identifier portion in it, the parser would fail on constants in 
 *          annotation definitions because it expected two identifiers.  
 *      Added optional trailing ';' to the alternatives in annotationTypeElementRest.
 *          Wouldn't handle an inner interface that has a trailing ';'.
 *      Swapped the expression and type rule reference order in castExpression to 
 *          make it check for genericized casts first.  It was failing to recognize a
 *          statement like  "Class<Byte> TYPE = (Class<Byte>)...;" because it was seeing
 *          'Class<Byte' in the cast expression as a less than expression, then failing 
 *          on the '>'.
 *      Changed createdName to use typeArguments instead of nonWildcardTypeArguments.
 *          Again, JLS doesn't seem to allow this, but java.lang.Class has an example of
 *          of this construct.
 *      Changed the 'this' alternative in primary to allow 'identifierSuffix' rather than
 *          just 'arguments'.  The case it couldn't handle was a call to an explicit
 *          generic method invocation (e.g. this.<E>doSomething()).  Using identifierSuffix
 *          may be overly aggressive--perhaps should create a more constrained thisSuffix rule?
 * 		
 *  Version 1.0.4 -- Hiroaki Nakamura, May 3, 2007
 *
 *	Fixed formalParameterDecls, localVariableDeclaration, forInit,
 *	and forVarControl to use variableModifier* not 'final'? (annotation)?
 *
 *  Version 1.0.5 -- Terence, June 21, 2007
 *	--a[i].foo didn't work. Fixed unaryExpression
 */
grammar Java;
options {
 output=AST;
 k=2; 
 backtrack=true; 
 memoize=true;
}

tokens {
 UNIT; 
 PACKAGE;
 IMPORT;
 STATIC_IMPORT;
 CLASS;
 INTERFACE;
 ENUM;
 MODIFIERS;
 EXTENDS;
 IMPLEMENTS;
 FIELD;
 METHOD;
 PARAMETER;
 VARARGS;
 TYPE;
 VOID;
 TYPE_PARAMETER;
 THROWS;
 ANNOTATION;
 SUPER;
 INTVAL;
 FLOATVAL;
 DOUBLEVAL;
 CHARVAL;
 STRINGVAL;
 BOOLVAL;
 NULLVAL;
 BLOCK;
 VARDEF;
 ASSERT;
 IF;
 FOR;
 WHILE;
 DOWHILE;
 RETURN;
 THROW;
 BREAK;
 CONTINUE;
 ASSIGN;
 VAR;
 LOR;
 OR;
 LAND;
 AND;
 XOR;
 EQ;
 NEQ;
 INSTANCEOF;
 LTEQ;
 GTEQ;
 LT;
 GT;
 SHL;
 SHR;
 USHR;
 ADD;
 SUB;
 DIV;
 MUL;
 MOD;
 DEREF;
 NEG;
 POSTINC;
 PREINC;
 PREDEC;
 POSTDEC;
 NOT;
 INV;
 CAST;
 SELECTOR;
 INVOKE;
 ARRAYINDEX;
 NEW;
 GETCLASS;
 SYNCHRONIZED;
 INIT;
 TEST;
 STEP;
 LABEL;
 FOREACH;
 TRY;
 CATCH;
 FINALLY;
 CONDEXPR;
 ARRAYVAL;
 SWITCH;
 CASE;
 DEFAULT;
 ARRAYINIT;
 ARGUMENTS;
 LABINOP; // left-associative binary operator
 NONE; // to indicate the absence of a type (for constructors)
 STATIC;
 ENUM_CONSTANT;
 ASSIGNOP;
 PARAMETERS;
 TYPE_PARAMETERS;
}

@lexer::members {
protected boolean enumIsKeyword = true;
	public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
      String text = "?";
      if(e.token != null) {
       text = e.token.getText();
      }
	  throw new SyntaxError("error on \"" + text +"\"",e.line,e.charPositionInLine,text.length());
    } 
}
@lexer::header {
package jkit.java.parser;
import jkit.compiler.SyntaxError;
}
@header {
package jkit.java.parser;
import jkit.compiler.SyntaxError;
}

@rulecatch { 
 catch(RecognitionException e) {  
  throw new SyntaxError("error on \"" + e.token.getText()+"\"",e.line,e.charPositionInLine,e.token.getText().length());
 } 
}

@members {
    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
	  throw new SyntaxError("error on \"" + e.token.getText()+"\"",e.line,e.charPositionInLine,e.token.getText().length());
    }    	
}

// starting point for parsing a java file
compilationUnit 
	:	annotations? packageDeclaration? importDeclaration* typeDeclaration*     
		-> ^(UNIT packageDeclaration? importDeclaration* typeDeclaration*)
	;

packageDeclaration
	:	'package' qualifiedName ';' -> ^(PACKAGE qualifiedName)
	;
	
importDeclaration
	:	'import' (
			'static' i+=Identifier ('.' i+=Identifier)* ('.' i+='*')? ';' -> ^(STATIC_IMPORT $i+)
			| i+=Identifier ('.' i+=Identifier)* ('.' i+='*')? ';' -> ^(IMPORT $i+)
			)
	;
	
typeDeclaration
	:	classOrInterfaceDeclaration
    |   ';'
	;
	
classOrInterfaceDeclaration
	:	modifier* (
		classDeclaration -> ^(CLASS ^(MODIFIERS modifier*) classDeclaration)
		| enumDeclaration -> ^(ENUM ^(MODIFIERS modifier*) enumDeclaration)
		| normalInterfaceDeclaration -> ^(INTERFACE ^(MODIFIERS modifier*) normalInterfaceDeclaration)
		| annotationTypeDeclaration -> ^(ANNOTATION ^(MODIFIERS modifier*) annotationTypeDeclaration)
		) 
	;
	
classDeclaration
	:	'class' Identifier (typeParameters)?
        ('extends' type)? 
        ('implements' typeList)?
        classBody -> ^(Identifier typeParameters?) ^(EXTENDS type?) ^(IMPLEMENTS typeList?) classBody?
	;

typeParameters
	:	'<' t+=typeParameter (',' t+=typeParameter)* '>' -> '<' $t*
	;

typeParameter
	:	Identifier ('extends' bound)? -> ^(TYPE_PARAMETER ^(Identifier ^(EXTENDS bound)?))
	;
		
bound
	:	t+=type ('&' t+=type)* -> $t*
	;

enumDeclaration
	:	ENUM Identifier ('implements' typeList)? enumBody -> Identifier ^(IMPLEMENTS typeList?) enumBody?
	;
	
enumBody
	:	'{' enumConstants? ','? enumBodyDeclarations? '}' -> enumConstants? enumBodyDeclarations?
	;

enumConstants
	:	enumConstant (',' enumConstant)* -> enumConstant+
	;
	
enumConstant
	:	annotations? Identifier (arguments)? (classBody)? -> ^(ENUM_CONSTANT annotations? Identifier (arguments)? (classBody)?)
	;
	
enumBodyDeclarations
	:	';' (classBodyDeclaration)* -> classBodyDeclaration*
	;
	
normalInterfaceDeclaration
	:	'interface' Identifier typeParameters? ('extends' typeList)? interfaceBody -> ^(Identifier typeParameters?) ^(EXTENDS) ^(IMPLEMENTS typeList?) interfaceBody?
	;
	
typeList
	:	type (','! type)*
	;
	
classBody
	:	'{'! classBodyDeclaration* '}'!
	;
	
interfaceBody
	:	'{'! interfaceBodyDeclaration* '}'!
	;

classBodyDeclaration
	:	';'!
	|	'static' block -> ^(STATIC block)
	|	'static'? block -> block
	|	modifier* (
	    genericMethodOrConstructorDecl -> ^(METHOD ^(MODIFIERS modifier*) genericMethodOrConstructorDecl)
     	|	methodDeclaration -> ^(METHOD ^(MODIFIERS modifier*) ^(TYPE_PARAMETERS) methodDeclaration)
     	|	fieldDeclaration -> ^(FIELD ^(MODIFIERS modifier*) fieldDeclaration)
    	|	lc='void' Identifier voidMethodDeclaratorRest -> ^(METHOD ^(MODIFIERS modifier*) ^(TYPE_PARAMETERS) Identifier ^(TYPE VOID[$lc]) voidMethodDeclaratorRest?)
    	|	Identifier constructorDeclaratorRest -> ^(METHOD ^(MODIFIERS modifier*) ^(TYPE_PARAMETERS) Identifier ^(NONE) constructorDeclaratorRest)
    	|	normalInterfaceDeclaration -> ^(INTERFACE ^(MODIFIERS modifier*) normalInterfaceDeclaration)
      	|	annotationTypeDeclaration -> ^(ANNOTATION ^(MODIFIERS modifier*) annotationTypeDeclaration)
    	|	classDeclaration -> ^(CLASS ^(MODIFIERS modifier*) classDeclaration)
    	|   enumDeclaration -> ^(ENUM ^(MODIFIERS modifier*) enumDeclaration)
    )
	;
	
genericMethodOrConstructorDecl
	:	typeParameters genericMethodOrConstructorRest -> ^(TYPE_PARAMETERS typeParameters) genericMethodOrConstructorRest
	;
	
genericMethodOrConstructorRest
	:	type Identifier methodDeclaratorRest -> Identifier type methodDeclaratorRest?
	|	lc='void' Identifier methodDeclaratorRest -> Identifier ^(TYPE VOID[$lc]) methodDeclaratorRest?
	|	Identifier constructorDeclaratorRest -> Identifier ^(NONE) constructorDeclaratorRest
	;

methodDeclaration
	:	type Identifier methodDeclaratorRest -> Identifier type methodDeclaratorRest?
	;

fieldDeclaration
	:	type variableDeclarators ';' -> type variableDeclarators
	;
		
interfaceBodyDeclaration
	:	modifier* (
		constantDeclaration -> ^(FIELD ^(MODIFIERS modifier*) constantDeclaration)
		| type Identifier interfaceMethodDeclaratorRest -> ^(METHOD ^(MODIFIERS modifier*) ^(TYPE_PARAMETERS) Identifier type interfaceMethodDeclaratorRest?)
		| interfaceGenericMethodDecl -> ^(METHOD ^(MODIFIERS modifier*) interfaceGenericMethodDecl)
		| lc='void' Identifier voidInterfaceMethodDeclaratorRest -> ^(METHOD ^(MODIFIERS modifier*) ^(TYPE_PARAMETERS) Identifier ^(TYPE VOID[$lc]) voidInterfaceMethodDeclaratorRest?)
		| normalInterfaceDeclaration -> ^(INTERFACE ^(MODIFIERS modifier*) normalInterfaceDeclaration) 
      	| annotationTypeDeclaration -> ^(ANNOTATION ^(MODIFIERS modifier*) annotationTypeDeclaration)
		| classDeclaration -> ^(CLASS ^(MODIFIERS modifier*) classDeclaration)
    	|   enumDeclaration -> ^(ENUM ^(MODIFIERS modifier*) enumDeclaration)
		)
	|	';'!
	;	
	
methodDeclaratorRest
	:	formalParameters 
        ('throws' typeList)?
        (   methodBody -> ^(PARAMETERS formalParameters?) ^(THROWS typeList?) methodBody
        |   ';' -> ^(PARAMETERS formalParameters?) ^(THROWS typeList?)
        )
	;
	
voidMethodDeclaratorRest
	:	formalParameters ('throws' typeList)?
        (   methodBody -> ^(PARAMETERS formalParameters?) ^(THROWS typeList?) methodBody
        |   ';' -> ^(PARAMETERS formalParameters?) ^(THROWS typeList?)
        )
	;
	
interfaceMethodDeclaratorRest
	:	formalParameters ('throws' typeList)? ';' -> ^(PARAMETERS formalParameters?) ^(THROWS typeList?)
	;
	
interfaceGenericMethodDecl
	:	typeParameters 
		(type Identifier interfaceMethodDeclaratorRest -> ^(TYPE_PARAMETERS typeParameters) Identifier type interfaceMethodDeclaratorRest
		| lc='void' Identifier interfaceMethodDeclaratorRest -> ^(TYPE_PARAMETERS typeParameters) Identifier ^(TYPE VOID[$lc]) interfaceMethodDeclaratorRest
		)
	;
	
voidInterfaceMethodDeclaratorRest
	:	formalParameters ('throws' typeList)? ';' -> ^(PARAMETERS formalParameters?) ^(THROWS typeList?)
	;
	
constructorDeclaratorRest
	:	formalParameters ('throws' typeList)? methodBody -> ^(PARAMETERS formalParameters?) ^(THROWS typeList?) methodBody
	;

	
variableDeclarators
	:	i+=variableDeclarator (',' i+=variableDeclarator)* -> $i+
	;

variableDeclarator
	:	Identifier variableDeclaratorRest -> ^(Identifier variableDeclaratorRest?)
	;
	
variableDeclaratorRest
	:	('[' ']')+ ('=' variableInitializer)? -> '['+ variableInitializer?
	|	'=' variableInitializer -> variableInitializer
	|
	;

constantDeclaration
	:	type constantDeclarators ';' -> type constantDeclarators
	;

constantDeclarator
	:	Identifier constantDeclaratorRest -> ^(Identifier constantDeclaratorRest)
	;	
	
constantDeclarators
	:	i+=constantDeclarator (',' i+=constantDeclarator)* -> $i+
	;
	
constantDeclaratorRest
	:	('[' ']')* '=' variableInitializer -> '['* variableInitializer
	;
	
variableDeclaratorId
	:	Identifier ('[' ']')*
	;

variableInitializer
	:	arrayInitializer
    |   expression
	;
	
arrayInitializer
	:	'{' (variableInitializer (',' variableInitializer)* (',')? )? '}' -> ^(ARRAYVAL variableInitializer*)
	;

modifier
    :   annotation
    |   'public'
    |   'protected'
    |   'private'
    |   'static'
    |   'abstract'
    |   'final'
    |   'native'
    |   'synchronized'
    |   'transient'
    |   'volatile'
    |   'strictfp'
    ;

packageOrTypeName
	:	Identifier ('.' Identifier)*
	;

enumConstantName
    :   Identifier
    ;

typeName
	:   Identifier
    |   packageOrTypeName '.' Identifier
	;

type
	:	i+=refComponent ('.' i+=refComponent)* ('[' ']')* -> ^(TYPE $i+ ('[')*)
	|	primitiveType ('[' ']')* -> ^(TYPE primitiveType ('[')*)
	;

refComponent
    : Identifier (typeArguments)? -> ^(Identifier typeArguments?)
    ;

primitiveType
    :   'boolean'
    |	'char'
    |	'byte'
    |	'short'
    |	'int'
    |	'long'
    |	'float'
    |	'double'
    ;

variableModifier
	:	'final'
    |   annotation
	;

typeArguments
	:	'<' t+=typeArgument (',' t+=typeArgument)* '>' -> $t+
	;
	
typeArgument
	:	type
	|	'?' ( 
			('extends' type)? -> ^(TYPE ^('?' ^(EXTENDS type)?))
	        | ('super' type)? -> ^(TYPE ^('?' ^(SUPER type)?))
	        )
	;
	
qualifiedNameList
	:	qualifiedName (','! qualifiedName)*
	;
	
formalParameters
	:	'(' formalParameterDecls? ')' -> formalParameterDecls?
	;
	
formalParameterDecls
	:	variableModifier* type (
        variableDeclaratorId (',' formalParameterDecls)? -> ^(PARAMETER ^(MODIFIERS variableModifier*) type variableDeclaratorId) formalParameterDecls?
        | '...' variableDeclaratorId -> ^(VARARGS ^(MODIFIERS variableModifier*) type variableDeclaratorId)
        )
    ;

	
methodBody
	:	block -> block
	;

qualifiedName 
	:	i+=Identifier ('.' i+=Identifier)* -> $i+
	;
	
literal	
	:   integerLiteral -> ^(INTVAL integerLiteral)
    |   FloatingPointLiteral -> ^(FLOATVAL FloatingPointLiteral)
    |   CharacterLiteral -> ^(CHARVAL CharacterLiteral)
    |   StringLiteral -> ^(STRINGVAL StringLiteral)
    |   booleanLiteral -> ^(BOOLVAL booleanLiteral)
    |   'null' -> ^(NULLVAL)
	;

integerLiteral
    :   HexLiteral
    |   OctalLiteral
    |   DecimalLiteral
    ;

booleanLiteral
    :   'true' 
    |   'false'
    ;

// ANNOTATIONS

annotations
	:	annotation+
	;

annotation
	:	lc='@' annotationName ('(' elementValuePairs? ')')? -> ^(ANNOTATION[$lc] annotationName elementValuePairs?)
	;
	
annotationName
	: Identifier ('.' Identifier)*
	;
	
elementValuePairs
	: elementValuePair (',' elementValuePair)*
	;
	
elementValuePair
	: (Identifier '=') elementValue -> ^(ASSIGN ^(VAR Identifier) elementValue)
	| elementValue -> elementValue
	;
	
elementValue
	:	conditionalExpression
	|   annotation
	|   elementValueArrayInitializer
	;
	
elementValueArrayInitializer
	:	'{' (elementValue (',' elementValue )*)? '}' -> ^(ARRAYVAL elementValue+)
	;
	
annotationTypeDeclaration
	:	'@' 'interface' Identifier annotationTypeBody -> Identifier annotationTypeBody?
	;
	
annotationTypeBody
	:	'{'! (annotationTypeElementDeclarations)? '}'!
	;
	
annotationTypeElementDeclarations
	:	(annotationTypeElementDeclaration)*
	;
	
annotationTypeElementDeclaration
	:	(modifier)* 
		(
			type 
			(
				annotationMethodRest ';' -> ^(METHOD ^(MODIFIERS modifier*) type annotationMethodRest)
				| annotationConstantRest ';' -> ^(FIELD ^(MODIFIERS modifier*) type annotationConstantRest)
			)
			| classDeclaration ';'? -> ^(CLASS ^(MODIFIERS modifier*) classDeclaration)
			| enumDeclaration ';'? -> ^(ENUM ^(MODIFIERS modifier*) enumDeclaration)
			| normalInterfaceDeclaration ';'? -> ^(INTERFACE ^(MODIFIERS modifier*) normalInterfaceDeclaration)
			| annotationTypeDeclaration ';'? -> ^(ANNOTATION ^(MODIFIERS modifier*) annotationTypeDeclaration)
		)
	;
	
annotationMethodRest
 	:	Identifier '('! ')'! (defaultValue)?
 	;
 	
annotationConstantRest
 	:	variableDeclarators
 	;
 	
defaultValue
 	:	'default'! elementValue
 	;

// STATEMENTS / BLOCKS

block
	:	'{' blockStatement* '}' -> ^(BLOCK blockStatement*)
	;
	
blockStatement
	:	localVariableDeclaration
	|	classOrInterfaceDeclaration
   	|	statement
	;
	
localVariableDeclaration
	:	variableModifier* type variableDeclarators ';' -> ^(VARDEF ^(MODIFIERS variableModifier*) type variableDeclarators)
	;
	
statement
	: block
    | lc='assert' expression (':' expression)? ';' -> ^(ASSERT[$lc] expression expression?)
    | lc='if' parExpression statement (options {k=1;}:'else' statement)? -> ^(IF[$lc] parExpression statement statement?)
    | lc='for' '(' forControl ')' statement -> ^(FOR[$lc] forControl statement)
    | lc='while' parExpression statement -> ^(WHILE[$lc] ^(TEST parExpression) statement)
    | lc='do' statement 'while' parExpression ';' -> ^(DOWHILE[$lc] ^(TEST parExpression) statement)
    | lc='try' block
      (	catches fc='finally' block -> ^(TRY[$lc] block catches ^(FINALLY[$fc] block))
      | catches -> ^(TRY[$lc] block catches)
      | fc='finally' block -> ^(TRY[$lc] block ^(FINALLY[$fc] block))
      )
    | lc='switch' parExpression '{' switchBlockStatementGroups '}' -> ^(SWITCH[$lc] parExpression switchBlockStatementGroups)
    | lc='synchronized' parExpression block -> ^(SYNCHRONIZED[$lc] parExpression block)
    | lc='return' expression? ';' -> ^(RETURN[$lc] expression?)
    | lc='throw' expression ';' -> ^(THROW[$lc] expression)
    | lc='break' Identifier? ';' -> ^(BREAK[$lc] Identifier?)
    | lc='continue' Identifier? ';' -> ^(CONTINUE[$lc] Identifier?)
    | ';' -> ^(BLOCK)	
    | statementExpression ';' -> statementExpression
    | lc=Identifier ':' statement -> ^(LABEL[$lc] Identifier statement)
	;

catches
	:	catchClause (catchClause)*
	;
	
catchClause
	:	lc='catch' '(' formalParameter ')' block -> ^(CATCH[$lc] ^(PARAMETER formalParameter) block)
	;

formalParameter
	:	variableModifier* type variableDeclaratorId
	;
		
switchBlockStatementGroups
	:	(switchBlockStatementGroup)*
	;
	
switchBlockStatementGroup
	:	switchLabel blockStatement* -> ^(switchLabel ^(BLOCK blockStatement*))
	;
	
switchLabel
	:	lc='case' constantExpression ':' -> ^(CASE[$lc] constantExpression)
	|	lc='case' enumConstantName ':' -> ^(CASE[$lc] enumConstantName)
	|	lc='default' ':' -> ^(DEFAULT)
	;
	
moreStatementExpressions
	:	(',' statementExpression)*
	;

forControl
options {k=3;} // be efficient for common case: for (ID ID : ID) ...
	:	forVarControl -> ^(FOREACH forVarControl)
	|	forInit? ';' fc=expression? ';' fu=forUpdate? -> ^(INIT forInit?) ^(TEST expression?) ^(STEP forUpdate?)
	;

forInit
	:	variableModifier* type variableDeclarators -> ^(VARDEF ^(MODIFIERS variableModifier*) type variableDeclarators)
	|	expressionList
	;
	
forVarControl
	:	variableModifier* type Identifier ':' expression -> ^(VARDEF ^(MODIFIERS variableModifier*) type Identifier) expression
	;

forUpdate
	:	expressionList
	;

// EXPRESSIONS

parExpression
	:	'('! expression ')'!
	;
	
expressionList
    :   expression (','! expression)*
    ;

statementExpression
	:	expression
	;
	
constantExpression
	:	expression
	;
	
expression
	:	conditionalExpression 
		(
			'=' expression -> ^(ASSIGN conditionalExpression expression)
			| lc='+' '=' expression -> ^(ASSIGNOP[$lc] ADD conditionalExpression expression)
			| lc='-' '=' expression -> ^(ASSIGNOP[$lc] SUB conditionalExpression expression)
			| lc='*' '=' expression -> ^(ASSIGNOP[$lc] MUL conditionalExpression expression)
			| lc='/' '=' expression -> ^(ASSIGNOP[$lc] DIV conditionalExpression expression)			
			| lc='&=' expression -> ^(ASSIGNOP[$lc] AND conditionalExpression expression)
			| lc='|=' expression -> ^(ASSIGNOP[$lc] OR conditionalExpression expression)
			| lc='^=' expression -> ^(ASSIGNOP[$lc] XOR conditionalExpression expression)			
			| lc='%' '=' expression -> ^(ASSIGNOP[$lc] MOD conditionalExpression expression)
			| lc='<' '<' '=' expression -> ^(ASSIGNOP[$lc] SHL conditionalExpression expression)
			| lc='>' '>' '=' expression -> ^(ASSIGNOP[$lc] SHR conditionalExpression expression)
			| lc='>' '>' '>' '=' expression -> ^(ASSIGNOP[$lc] USHR conditionalExpression expression)
			| -> conditionalExpression
		) 
	;
	
conditionalExpression
    :   conditionalOrExpression 
    	( 
    		'?' expression ':' expression -> ^(CONDEXPR conditionalOrExpression expression expression)
    		| -> conditionalOrExpression
    	)
	;

conditionalOrExpression
    :   conditionalAndExpression 
    	( 
		    ('||' conditionalOrExpression) -> ^(LOR conditionalAndExpression conditionalOrExpression)
		    | -> conditionalAndExpression
	    )
	;

conditionalAndExpression
    :   inclusiveOrExpression 
    	( 
    		('&&' conditionalAndExpression) -> ^(LAND inclusiveOrExpression conditionalAndExpression)
    		| -> inclusiveOrExpression    	
    	) 
	;

inclusiveOrExpression
    :   exclusiveOrExpression
        (
    	    ('|' inclusiveOrExpression) -> ^(OR exclusiveOrExpression inclusiveOrExpression)
			| -> exclusiveOrExpression
	    )    
	;

exclusiveOrExpression
    :   andExpression 
    ( 
    	('^' exclusiveOrExpression) -> ^(XOR andExpression exclusiveOrExpression)
	    | -> andExpression
    )
	;

andExpression
    :   equalityExpression 
    ( 
    	('&' andExpression) -> ^(AND equalityExpression andExpression)
    	| -> equalityExpression
    )
	;

equalityExpression
    :   instanceOfExpression 
    ( 
    	('==' instanceOfExpression) -> ^(EQ instanceOfExpression instanceOfExpression)
    	| ('!=' instanceOfExpression) -> ^(NEQ instanceOfExpression instanceOfExpression)
    	| -> instanceOfExpression
    )
	;

instanceOfExpression
    :   relationalExpression 
    (
    	('instanceof' type) -> ^(INSTANCEOF relationalExpression type)
    	| -> relationalExpression
    )
	;

relationalExpression
    :   shiftExpression 
    ( 
    	('<' '=' shiftExpression) -> ^(LTEQ shiftExpression shiftExpression)
    	| ('<' shiftExpression) -> ^(LT shiftExpression shiftExpression)
    	| ('>' '=' shiftExpression) -> ^(GTEQ shiftExpression shiftExpression)
    	| ('>' shiftExpression) -> ^(GT shiftExpression shiftExpression)
    	| -> shiftExpression
    )
	;
	
shiftExpression
    :   additiveExpression 
    ( 
    	((i+='<' '<'|i+='>' '>') additiveExpression)+ -> ^(LABINOP additiveExpression ($i additiveExpression)+)
    	// I don't think you can use '>>>' in a repetive fashion (e.g. x >>> y >>> z)
    	| ('>' '>' '>' additiveExpression)+ -> ^(USHR additiveExpression additiveExpression+)
	    | -> additiveExpression
	)
	;

additiveExpression
    :   multiplicativeExpression 
    ( 
    	((i+='+'|i+='-') multiplicativeExpression)+ -> ^(LABINOP multiplicativeExpression ($i multiplicativeExpression)+)
    	| -> multiplicativeExpression
    )
	;

multiplicativeExpression
    :   unaryExpression 
    ( 
    	( (i+='*'|i+='/'|i+='%') unaryExpression)+ -> ^(LABINOP unaryExpression ($i unaryExpression)+)
    	| -> unaryExpression
    )
	;
	
unaryExpression
    :   '+' unaryExpression -> unaryExpression
    |	'-' unaryExpression -> ^(NEG unaryExpression)
    |   '++' unaryExpression -> ^(PREINC unaryExpression)
    |   '--' unaryExpression -> ^(PREDEC unaryExpression)
    |   unaryExpressionNotPlusMinus -> unaryExpressionNotPlusMinus    	
    ;
   
unaryExpressionNotPlusMinus
    :   '~' unaryExpression -> ^(INV unaryExpression)
    | 	'!' unaryExpression -> ^(NOT unaryExpression)
    |   castExpression
	|   primary 
		(
			selector+ 
			(
				'++' -> ^(POSTINC ^(SELECTOR primary selector+))
				|'--' -> ^(POSTDEC ^(SELECTOR primary selector+))
				| -> ^(SELECTOR primary selector+)
			)
			| '++' -> ^(POSTINC primary)
			| '--' -> ^(POSTDEC primary)
			| -> primary
		)
    ;

castExpression
    :  '(' primitiveType ')' unaryExpression -> ^(CAST ^(TYPE primitiveType) unaryExpression)
    |  '(' type ')' unaryExpressionNotPlusMinus -> ^(CAST type unaryExpressionNotPlusMinus)
    |  '(' expression ')' unaryExpressionNotPlusMinus -> // FIXME: WHAT DOES THIS RULE DO?
    ;

primary
    :	parExpression
	|   nonWildcardTypeArguments explicitGenericInvocationSuffix -> ^(INVOKE ^(TYPE_PARAMETER nonWildcardTypeArguments) explicitGenericInvocationSuffix)
    |   literal
    |   'new' nonWildcardTypeArguments? primitiveType
    	(
		    ('[' expression ']')+ ('[' ']')* -> ^(NEW ^(TYPE primitiveType '['*) expression*)
		    | ('[' ']')+ arrayInitializer -> ^(ARRAYINIT ^(TYPE primitiveType '['*) arrayInitializer)
		) 
    |   'new' nonWildcardTypeArguments? (i+=refComponent ('.' i+=refComponent)*)
    	(
		    ('[' expression ']')+ ('[' ']')* -> ^(NEW ^(TYPE $i+ '['*) expression*)
		    | ('[' ']')+ arrayInitializer -> ^(ARRAYINIT ^(TYPE $i+ '['*) arrayInitializer)
		    | classCreatorRest -> ^(NEW ^(TYPE $i+) classCreatorRest?)
		) 
    |   type '.' 'class' -> ^(GETCLASS type)    
    |   lc='void' '.' 'class' -> ^(GETCLASS ^(TYPE VOID[$lc]))
    | 	'super' 
		(
			arguments -> ^(INVOKE 'super' arguments?)
			| -> ^(VAR 'super')
		)
    |   Identifier 
    	(
    		arguments -> ^(INVOKE Identifier arguments?)
    		| -> ^(VAR Identifier)
    	)
	;

innerCreator
	:	Identifier classCreatorRest -> ^(TYPE Identifier) classCreatorRest?
	;

classCreatorRest
	:	arguments classBody? -> arguments? classBody?
	;
	
explicitGenericInvocation
	:	nonWildcardTypeArguments explicitGenericInvocationSuffix
	;
	
nonWildcardTypeArguments
	:	'<' typeList '>' -> typeList
	;
	
explicitGenericInvocationSuffix
	:	'super' superSuffix -> 'super' superSuffix?
	|   Identifier arguments -> Identifier arguments?
	;
	
superSuffix
	:	arguments -> arguments?
	|   '.' Identifier (arguments)?
	;
	
selector
	:	'.' 'super' arguments -> ^(INVOKE 'super' arguments?)
	|   '.' 'new' (nonWildcardTypeArguments)? innerCreator -> ^(NEW innerCreator) 
	| 	'.' nonWildcardTypeArguments explicitGenericInvocationSuffix -> ^(INVOKE ^(TYPE_PARAMETER nonWildcardTypeArguments) explicitGenericInvocationSuffix)
	|	'.' Identifier 
		(
			arguments -> ^(INVOKE Identifier arguments?)
			| -> ^(DEREF Identifier)
		)
	|   '[' expression ']' -> ^(ARRAYINDEX expression)
	;

arguments
	:	'('! expressionList? ')'!
	;

// LEXER

HexLiteral : '0' ('x'|'X') HexDigit+ IntegerTypeSuffix? ;

DecimalLiteral : ('0' | '1'..'9' '0'..'9'*) IntegerTypeSuffix? ;

OctalLiteral : '0' ('0'..'7')+ IntegerTypeSuffix? ;

fragment
HexDigit : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
IntegerTypeSuffix : ('l'|'L') ;

FloatingPointLiteral
    :   ('0'..'9')+ '.' ('0'..'9')* Exponent? FloatTypeSuffix?
    |   '.' ('0'..'9')+ Exponent? FloatTypeSuffix?
    |   ('0'..'9')+ Exponent
    |	('0'..'9')+ FloatTypeSuffix
    |   ('0'..'9')+ Exponent FloatTypeSuffix
	;

fragment
Exponent : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
FloatTypeSuffix : ('f'|'F'|'d'|'D') ;

CharacterLiteral
    :   '\'' ( EscapeSequence | ~('\''|'\\') ) '\''
    ;

StringLiteral
    :  '"' ( EscapeSequence | ~('\\'|'"') )* '"'
    ;

fragment
EscapeSequence
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UnicodeEscape
    |   OctalEscape
    ;

fragment
OctalEscape
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

ENUM:	'enum' {if ( !enumIsKeyword ) $type=Identifier;}
	;
	
Identifier 
    :   Letter (Letter|JavaIDDigit)*
    ;

/**I found this char range in JavaCC's grammar, but Letter and Digit overlap.
   Still works, but...
 */
fragment
Letter
    :  '\u0024' |
       '\u0041'..'\u005a' |
       '\u005f' |
       '\u0061'..'\u007a' |
       '\u00c0'..'\u00d6' |
       '\u00d8'..'\u00f6' |
       '\u00f8'..'\u00ff' |
       '\u0100'..'\u1fff' |
       '\u3040'..'\u318f' |
       '\u3300'..'\u337f' |
       '\u3400'..'\u3d2d' |
       '\u4e00'..'\u9fff' |
       '\uf900'..'\ufaff'
    ;

fragment
JavaIDDigit
    :  '\u0030'..'\u0039' |
       '\u0660'..'\u0669' |
       '\u06f0'..'\u06f9' |
       '\u0966'..'\u096f' |
       '\u09e6'..'\u09ef' |
       '\u0a66'..'\u0a6f' |
       '\u0ae6'..'\u0aef' |
       '\u0b66'..'\u0b6f' |
       '\u0be7'..'\u0bef' |
       '\u0c66'..'\u0c6f' |
       '\u0ce6'..'\u0cef' |
       '\u0d66'..'\u0d6f' |
       '\u0e50'..'\u0e59' |
       '\u0ed0'..'\u0ed9' |
       '\u1040'..'\u1049'
   ;

WS  :  (' '|'\r'|'\t'|'\u000C'|'\n') {$channel=HIDDEN;}
    ;

COMMENT
    :   '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    ;

LINE_COMMENT
    : '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    ;
