// $ANTLR 3.1 jkit/java/Java.g 2008-12-03 10:12:44

package jkit.java;
import jkit.compiler.SyntaxError;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class JavaLexer extends Lexer {
    public static final int T__197=197;
    public static final int T__139=139;
    public static final int SYNCHRONIZED=78;
    public static final int T__174=174;
    public static final int HexDigit=105;
    public static final int T__196=196;
    public static final int T__144=144;
    public static final int T__122=122;
    public static final int METHOD=15;
    public static final int T__137=137;
    public static final int T__140=140;
    public static final int IMPORT=6;
    public static final int PACKAGE=5;
    public static final int CONTINUE=41;
    public static final int Letter=112;
    public static final int T__138=138;
    public static final int T__173=173;
    public static final int T__119=119;
    public static final int ASSERT=33;
    public static final int T__198=198;
    public static final int T__142=142;
    public static final int T__176=176;
    public static final int FloatTypeSuffix=108;
    public static final int T__118=118;
    public static final int T__135=135;
    public static final int EXTENDS=12;
    public static final int POSTDEC=69;
    public static final int ARRAYVAL=88;
    public static final int SUPER=23;
    public static final int IntegerTypeSuffix=106;
    public static final int ARGUMENTS=93;
    public static final int T__156=156;
    public static final int WS=114;
    public static final int NONE=95;
    public static final int T__159=159;
    public static final int T__177=177;
    public static final int T__158=158;
    public static final int FIELD=14;
    public static final int POSTINC=66;
    public static final int LAND=46;
    public static final int STATIC=96;
    public static final int CATCH=85;
    public static final int MUL=62;
    public static final int T__157=157;
    public static final int UnicodeEscape=110;
    public static final int MODIFIERS=11;
    public static final int CONDEXPR=87;
    public static final int T__143=143;
    public static final int XOR=48;
    public static final int T__193=193;
    public static final int T__141=141;
    public static final int ADD=59;
    public static final int FOREACH=83;
    public static final int TYPE=18;
    public static final int SHL=56;
    public static final int LOR=44;
    public static final int OctalLiteral=103;
    public static final int T__167=167;
    public static final int SELECTOR=73;
    public static final int T__194=194;
    public static final int CAST=72;
    public static final int THROWS=21;
    public static final int LABEL=82;
    public static final int MOD=63;
    public static final int T__191=191;
    public static final int OR=45;
    public static final int DOWHILE=37;
    public static final int BLOCK=31;
    public static final int T__192=192;
    public static final int EscapeSequence=109;
    public static final int INSTANCEOF=51;
    public static final int FloatingPointLiteral=99;
    public static final int NEQ=50;
    public static final int T__175=175;
    public static final int T__117=117;
    public static final int WHILE=36;
    public static final int INVOKE=74;
    public static final int COMMENT=115;
    public static final int T__172=172;
    public static final int T__199=199;
    public static final int GTEQ=53;
    public static final int LABINOP=94;
    public static final int UNIT=4;
    public static final int JavaIDDigit=113;
    public static final int T__170=170;
    public static final int RETURN=38;
    public static final int T__136=136;
    public static final int IF=34;
    public static final int T__171=171;
    public static final int GETCLASS=77;
    public static final int FOR=35;
    public static final int T__189=189;
    public static final int DEFAULT=91;
    public static final int OctalEscape=111;
    public static final int STRINGVAL=28;
    public static final int T__134=134;
    public static final int T__195=195;
    public static final int NEG=65;
    public static final int DEREF=64;
    public static final int SUB=60;
    public static final int NOT=70;
    public static final int TRY=84;
    public static final int T__162=162;
    public static final int T__160=160;
    public static final int T__123=123;
    public static final int STEP=81;
    public static final int T__145=145;
    public static final int T__187=187;
    public static final int INTVAL=24;
    public static final int SHR=57;
    public static final int PREDEC=68;
    public static final int ARRAYINIT=92;
    public static final int STATIC_IMPORT=7;
    public static final int T__186=186;
    public static final int AND=47;
    public static final int T__181=181;
    public static final int T__128=128;
    public static final int NULLVAL=30;
    public static final int PREINC=67;
    public static final int T__161=161;
    public static final int FINALLY=86;
    public static final int T__168=168;
    public static final int T__150=150;
    public static final int Identifier=98;
    public static final int ENUM_CONSTANT=97;
    public static final int T__182=182;
    public static final int EQ=49;
    public static final int NEW=76;
    public static final int BOOLVAL=29;
    public static final int LT=54;
    public static final int T__165=165;
    public static final int T__130=130;
    public static final int T__151=151;
    public static final int LINE_COMMENT=116;
    public static final int CASE=90;
    public static final int INTERFACE=9;
    public static final int CHARVAL=27;
    public static final int HexLiteral=102;
    public static final int INV=71;
    public static final int T__125=125;
    public static final int T__149=149;
    public static final int LTEQ=52;
    public static final int DecimalLiteral=104;
    public static final int T__166=166;
    public static final int BREAK=40;
    public static final int T__132=132;
    public static final int ANNOTATION=22;
    public static final int DIV=61;
    public static final int T__190=190;
    public static final int T__131=131;
    public static final int T__124=124;
    public static final int T__169=169;
    public static final int THROW=39;
    public static final int T__126=126;
    public static final int T__148=148;
    public static final int INIT=79;
    public static final int T__188=188;
    public static final int T__200=200;
    public static final int VARDEF=32;
    public static final int DOUBLEVAL=26;
    public static final int TYPE_PARAMETER=20;
    public static final int ARRAYINDEX=75;
    public static final int ASSIGN=42;
    public static final int T__127=127;
    public static final int VOID=19;
    public static final int T__183=183;
    public static final int T__133=133;
    public static final int FLOATVAL=25;
    public static final int VARARGS=17;
    public static final int T__164=164;
    public static final int T__120=120;
    public static final int USHR=58;
    public static final int ENUM=10;
    public static final int T__163=163;
    public static final int Exponent=107;
    public static final int T__153=153;
    public static final int IMPLEMENTS=13;
    public static final int SWITCH=89;
    public static final int T__185=185;
    public static final int CharacterLiteral=100;
    public static final int T__178=178;
    public static final int GT=55;
    public static final int StringLiteral=101;
    public static final int T__129=129;
    public static final int T__180=180;
    public static final int T__152=152;
    public static final int T__121=121;
    public static final int VAR=43;
    public static final int CLASS=8;
    public static final int T__147=147;
    public static final int T__179=179;
    public static final int EOF=-1;
    public static final int T__154=154;
    public static final int PARAMETER=16;
    public static final int T__184=184;
    public static final int T__155=155;
    public static final int TEST=80;
    public static final int T__146=146;

    protected boolean enumIsKeyword = true;


    // delegates
    // delegators

    public JavaLexer() {;} 
    public JavaLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public JavaLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "jkit/java/Java.g"; }

    // $ANTLR start "T__117"
    public final void mT__117() throws RecognitionException {
        try {
            int _type = T__117;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:11:8: ( 'package' )
            // jkit/java/Java.g:11:10: 'package'
            {
            match("package"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__117"

    // $ANTLR start "T__118"
    public final void mT__118() throws RecognitionException {
        try {
            int _type = T__118;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:12:8: ( ';' )
            // jkit/java/Java.g:12:10: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__118"

    // $ANTLR start "T__119"
    public final void mT__119() throws RecognitionException {
        try {
            int _type = T__119;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:13:8: ( 'import' )
            // jkit/java/Java.g:13:10: 'import'
            {
            match("import"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__119"

    // $ANTLR start "T__120"
    public final void mT__120() throws RecognitionException {
        try {
            int _type = T__120;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:14:8: ( 'static' )
            // jkit/java/Java.g:14:10: 'static'
            {
            match("static"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__120"

    // $ANTLR start "T__121"
    public final void mT__121() throws RecognitionException {
        try {
            int _type = T__121;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:15:8: ( '.' )
            // jkit/java/Java.g:15:10: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__121"

    // $ANTLR start "T__122"
    public final void mT__122() throws RecognitionException {
        try {
            int _type = T__122;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:16:8: ( '*' )
            // jkit/java/Java.g:16:10: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__122"

    // $ANTLR start "T__123"
    public final void mT__123() throws RecognitionException {
        try {
            int _type = T__123;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:17:8: ( 'class' )
            // jkit/java/Java.g:17:10: 'class'
            {
            match("class"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__123"

    // $ANTLR start "T__124"
    public final void mT__124() throws RecognitionException {
        try {
            int _type = T__124;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:18:8: ( 'extends' )
            // jkit/java/Java.g:18:10: 'extends'
            {
            match("extends"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__124"

    // $ANTLR start "T__125"
    public final void mT__125() throws RecognitionException {
        try {
            int _type = T__125;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:19:8: ( 'implements' )
            // jkit/java/Java.g:19:10: 'implements'
            {
            match("implements"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__125"

    // $ANTLR start "T__126"
    public final void mT__126() throws RecognitionException {
        try {
            int _type = T__126;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:20:8: ( '<' )
            // jkit/java/Java.g:20:10: '<'
            {
            match('<'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__126"

    // $ANTLR start "T__127"
    public final void mT__127() throws RecognitionException {
        try {
            int _type = T__127;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:21:8: ( ',' )
            // jkit/java/Java.g:21:10: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__127"

    // $ANTLR start "T__128"
    public final void mT__128() throws RecognitionException {
        try {
            int _type = T__128;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:22:8: ( '>' )
            // jkit/java/Java.g:22:10: '>'
            {
            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__128"

    // $ANTLR start "T__129"
    public final void mT__129() throws RecognitionException {
        try {
            int _type = T__129;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:23:8: ( '&' )
            // jkit/java/Java.g:23:10: '&'
            {
            match('&'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__129"

    // $ANTLR start "T__130"
    public final void mT__130() throws RecognitionException {
        try {
            int _type = T__130;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:24:8: ( '{' )
            // jkit/java/Java.g:24:10: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__130"

    // $ANTLR start "T__131"
    public final void mT__131() throws RecognitionException {
        try {
            int _type = T__131;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:25:8: ( '}' )
            // jkit/java/Java.g:25:10: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__131"

    // $ANTLR start "T__132"
    public final void mT__132() throws RecognitionException {
        try {
            int _type = T__132;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:26:8: ( 'interface' )
            // jkit/java/Java.g:26:10: 'interface'
            {
            match("interface"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__132"

    // $ANTLR start "T__133"
    public final void mT__133() throws RecognitionException {
        try {
            int _type = T__133;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:27:8: ( 'void' )
            // jkit/java/Java.g:27:10: 'void'
            {
            match("void"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__133"

    // $ANTLR start "T__134"
    public final void mT__134() throws RecognitionException {
        try {
            int _type = T__134;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:28:8: ( 'throws' )
            // jkit/java/Java.g:28:10: 'throws'
            {
            match("throws"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__134"

    // $ANTLR start "T__135"
    public final void mT__135() throws RecognitionException {
        try {
            int _type = T__135;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:29:8: ( '[' )
            // jkit/java/Java.g:29:10: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__135"

    // $ANTLR start "T__136"
    public final void mT__136() throws RecognitionException {
        try {
            int _type = T__136;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:30:8: ( ']' )
            // jkit/java/Java.g:30:10: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__136"

    // $ANTLR start "T__137"
    public final void mT__137() throws RecognitionException {
        try {
            int _type = T__137;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:31:8: ( '=' )
            // jkit/java/Java.g:31:10: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__137"

    // $ANTLR start "T__138"
    public final void mT__138() throws RecognitionException {
        try {
            int _type = T__138;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:32:8: ( 'public' )
            // jkit/java/Java.g:32:10: 'public'
            {
            match("public"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__138"

    // $ANTLR start "T__139"
    public final void mT__139() throws RecognitionException {
        try {
            int _type = T__139;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:33:8: ( 'protected' )
            // jkit/java/Java.g:33:10: 'protected'
            {
            match("protected"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__139"

    // $ANTLR start "T__140"
    public final void mT__140() throws RecognitionException {
        try {
            int _type = T__140;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:34:8: ( 'private' )
            // jkit/java/Java.g:34:10: 'private'
            {
            match("private"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__140"

    // $ANTLR start "T__141"
    public final void mT__141() throws RecognitionException {
        try {
            int _type = T__141;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:35:8: ( 'abstract' )
            // jkit/java/Java.g:35:10: 'abstract'
            {
            match("abstract"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__141"

    // $ANTLR start "T__142"
    public final void mT__142() throws RecognitionException {
        try {
            int _type = T__142;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:36:8: ( 'final' )
            // jkit/java/Java.g:36:10: 'final'
            {
            match("final"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__142"

    // $ANTLR start "T__143"
    public final void mT__143() throws RecognitionException {
        try {
            int _type = T__143;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:37:8: ( 'native' )
            // jkit/java/Java.g:37:10: 'native'
            {
            match("native"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__143"

    // $ANTLR start "T__144"
    public final void mT__144() throws RecognitionException {
        try {
            int _type = T__144;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:38:8: ( 'synchronized' )
            // jkit/java/Java.g:38:10: 'synchronized'
            {
            match("synchronized"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__144"

    // $ANTLR start "T__145"
    public final void mT__145() throws RecognitionException {
        try {
            int _type = T__145;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:39:8: ( 'transient' )
            // jkit/java/Java.g:39:10: 'transient'
            {
            match("transient"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__145"

    // $ANTLR start "T__146"
    public final void mT__146() throws RecognitionException {
        try {
            int _type = T__146;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:40:8: ( 'volatile' )
            // jkit/java/Java.g:40:10: 'volatile'
            {
            match("volatile"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__146"

    // $ANTLR start "T__147"
    public final void mT__147() throws RecognitionException {
        try {
            int _type = T__147;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:41:8: ( 'strictfp' )
            // jkit/java/Java.g:41:10: 'strictfp'
            {
            match("strictfp"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__147"

    // $ANTLR start "T__148"
    public final void mT__148() throws RecognitionException {
        try {
            int _type = T__148;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:42:8: ( 'boolean' )
            // jkit/java/Java.g:42:10: 'boolean'
            {
            match("boolean"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__148"

    // $ANTLR start "T__149"
    public final void mT__149() throws RecognitionException {
        try {
            int _type = T__149;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:43:8: ( 'char' )
            // jkit/java/Java.g:43:10: 'char'
            {
            match("char"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__149"

    // $ANTLR start "T__150"
    public final void mT__150() throws RecognitionException {
        try {
            int _type = T__150;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:44:8: ( 'byte' )
            // jkit/java/Java.g:44:10: 'byte'
            {
            match("byte"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__150"

    // $ANTLR start "T__151"
    public final void mT__151() throws RecognitionException {
        try {
            int _type = T__151;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:45:8: ( 'short' )
            // jkit/java/Java.g:45:10: 'short'
            {
            match("short"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__151"

    // $ANTLR start "T__152"
    public final void mT__152() throws RecognitionException {
        try {
            int _type = T__152;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:46:8: ( 'int' )
            // jkit/java/Java.g:46:10: 'int'
            {
            match("int"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__152"

    // $ANTLR start "T__153"
    public final void mT__153() throws RecognitionException {
        try {
            int _type = T__153;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:47:8: ( 'long' )
            // jkit/java/Java.g:47:10: 'long'
            {
            match("long"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__153"

    // $ANTLR start "T__154"
    public final void mT__154() throws RecognitionException {
        try {
            int _type = T__154;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:48:8: ( 'float' )
            // jkit/java/Java.g:48:10: 'float'
            {
            match("float"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__154"

    // $ANTLR start "T__155"
    public final void mT__155() throws RecognitionException {
        try {
            int _type = T__155;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:49:8: ( 'double' )
            // jkit/java/Java.g:49:10: 'double'
            {
            match("double"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__155"

    // $ANTLR start "T__156"
    public final void mT__156() throws RecognitionException {
        try {
            int _type = T__156;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:50:8: ( '?' )
            // jkit/java/Java.g:50:10: '?'
            {
            match('?'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__156"

    // $ANTLR start "T__157"
    public final void mT__157() throws RecognitionException {
        try {
            int _type = T__157;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:51:8: ( 'super' )
            // jkit/java/Java.g:51:10: 'super'
            {
            match("super"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__157"

    // $ANTLR start "T__158"
    public final void mT__158() throws RecognitionException {
        try {
            int _type = T__158;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:52:8: ( '(' )
            // jkit/java/Java.g:52:10: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__158"

    // $ANTLR start "T__159"
    public final void mT__159() throws RecognitionException {
        try {
            int _type = T__159;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:53:8: ( ')' )
            // jkit/java/Java.g:53:10: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__159"

    // $ANTLR start "T__160"
    public final void mT__160() throws RecognitionException {
        try {
            int _type = T__160;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:54:8: ( '...' )
            // jkit/java/Java.g:54:10: '...'
            {
            match("..."); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__160"

    // $ANTLR start "T__161"
    public final void mT__161() throws RecognitionException {
        try {
            int _type = T__161;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:55:8: ( 'null' )
            // jkit/java/Java.g:55:10: 'null'
            {
            match("null"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__161"

    // $ANTLR start "T__162"
    public final void mT__162() throws RecognitionException {
        try {
            int _type = T__162;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:56:8: ( 'true' )
            // jkit/java/Java.g:56:10: 'true'
            {
            match("true"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__162"

    // $ANTLR start "T__163"
    public final void mT__163() throws RecognitionException {
        try {
            int _type = T__163;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:57:8: ( 'false' )
            // jkit/java/Java.g:57:10: 'false'
            {
            match("false"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__163"

    // $ANTLR start "T__164"
    public final void mT__164() throws RecognitionException {
        try {
            int _type = T__164;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:58:8: ( '@' )
            // jkit/java/Java.g:58:10: '@'
            {
            match('@'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__164"

    // $ANTLR start "T__165"
    public final void mT__165() throws RecognitionException {
        try {
            int _type = T__165;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:59:8: ( 'default' )
            // jkit/java/Java.g:59:10: 'default'
            {
            match("default"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__165"

    // $ANTLR start "T__166"
    public final void mT__166() throws RecognitionException {
        try {
            int _type = T__166;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:60:8: ( 'assert' )
            // jkit/java/Java.g:60:10: 'assert'
            {
            match("assert"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__166"

    // $ANTLR start "T__167"
    public final void mT__167() throws RecognitionException {
        try {
            int _type = T__167;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:61:8: ( ':' )
            // jkit/java/Java.g:61:10: ':'
            {
            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__167"

    // $ANTLR start "T__168"
    public final void mT__168() throws RecognitionException {
        try {
            int _type = T__168;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:62:8: ( 'if' )
            // jkit/java/Java.g:62:10: 'if'
            {
            match("if"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__168"

    // $ANTLR start "T__169"
    public final void mT__169() throws RecognitionException {
        try {
            int _type = T__169;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:63:8: ( 'else' )
            // jkit/java/Java.g:63:10: 'else'
            {
            match("else"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__169"

    // $ANTLR start "T__170"
    public final void mT__170() throws RecognitionException {
        try {
            int _type = T__170;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:64:8: ( 'for' )
            // jkit/java/Java.g:64:10: 'for'
            {
            match("for"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__170"

    // $ANTLR start "T__171"
    public final void mT__171() throws RecognitionException {
        try {
            int _type = T__171;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:65:8: ( 'while' )
            // jkit/java/Java.g:65:10: 'while'
            {
            match("while"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__171"

    // $ANTLR start "T__172"
    public final void mT__172() throws RecognitionException {
        try {
            int _type = T__172;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:66:8: ( 'do' )
            // jkit/java/Java.g:66:10: 'do'
            {
            match("do"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__172"

    // $ANTLR start "T__173"
    public final void mT__173() throws RecognitionException {
        try {
            int _type = T__173;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:67:8: ( 'try' )
            // jkit/java/Java.g:67:10: 'try'
            {
            match("try"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__173"

    // $ANTLR start "T__174"
    public final void mT__174() throws RecognitionException {
        try {
            int _type = T__174;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:68:8: ( 'finally' )
            // jkit/java/Java.g:68:10: 'finally'
            {
            match("finally"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__174"

    // $ANTLR start "T__175"
    public final void mT__175() throws RecognitionException {
        try {
            int _type = T__175;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:69:8: ( 'switch' )
            // jkit/java/Java.g:69:10: 'switch'
            {
            match("switch"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__175"

    // $ANTLR start "T__176"
    public final void mT__176() throws RecognitionException {
        try {
            int _type = T__176;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:70:8: ( 'return' )
            // jkit/java/Java.g:70:10: 'return'
            {
            match("return"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__176"

    // $ANTLR start "T__177"
    public final void mT__177() throws RecognitionException {
        try {
            int _type = T__177;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:71:8: ( 'throw' )
            // jkit/java/Java.g:71:10: 'throw'
            {
            match("throw"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__177"

    // $ANTLR start "T__178"
    public final void mT__178() throws RecognitionException {
        try {
            int _type = T__178;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:72:8: ( 'break' )
            // jkit/java/Java.g:72:10: 'break'
            {
            match("break"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__178"

    // $ANTLR start "T__179"
    public final void mT__179() throws RecognitionException {
        try {
            int _type = T__179;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:73:8: ( 'continue' )
            // jkit/java/Java.g:73:10: 'continue'
            {
            match("continue"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__179"

    // $ANTLR start "T__180"
    public final void mT__180() throws RecognitionException {
        try {
            int _type = T__180;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:74:8: ( 'catch' )
            // jkit/java/Java.g:74:10: 'catch'
            {
            match("catch"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__180"

    // $ANTLR start "T__181"
    public final void mT__181() throws RecognitionException {
        try {
            int _type = T__181;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:75:8: ( 'case' )
            // jkit/java/Java.g:75:10: 'case'
            {
            match("case"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__181"

    // $ANTLR start "T__182"
    public final void mT__182() throws RecognitionException {
        try {
            int _type = T__182;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:76:8: ( '+' )
            // jkit/java/Java.g:76:10: '+'
            {
            match('+'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__182"

    // $ANTLR start "T__183"
    public final void mT__183() throws RecognitionException {
        try {
            int _type = T__183;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:77:8: ( '-' )
            // jkit/java/Java.g:77:10: '-'
            {
            match('-'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__183"

    // $ANTLR start "T__184"
    public final void mT__184() throws RecognitionException {
        try {
            int _type = T__184;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:78:8: ( '/' )
            // jkit/java/Java.g:78:10: '/'
            {
            match('/'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__184"

    // $ANTLR start "T__185"
    public final void mT__185() throws RecognitionException {
        try {
            int _type = T__185;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:79:8: ( '&=' )
            // jkit/java/Java.g:79:10: '&='
            {
            match("&="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__185"

    // $ANTLR start "T__186"
    public final void mT__186() throws RecognitionException {
        try {
            int _type = T__186;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:80:8: ( '|=' )
            // jkit/java/Java.g:80:10: '|='
            {
            match("|="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__186"

    // $ANTLR start "T__187"
    public final void mT__187() throws RecognitionException {
        try {
            int _type = T__187;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:81:8: ( '^=' )
            // jkit/java/Java.g:81:10: '^='
            {
            match("^="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__187"

    // $ANTLR start "T__188"
    public final void mT__188() throws RecognitionException {
        try {
            int _type = T__188;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:82:8: ( '%' )
            // jkit/java/Java.g:82:10: '%'
            {
            match('%'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__188"

    // $ANTLR start "T__189"
    public final void mT__189() throws RecognitionException {
        try {
            int _type = T__189;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:83:8: ( '||' )
            // jkit/java/Java.g:83:10: '||'
            {
            match("||"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__189"

    // $ANTLR start "T__190"
    public final void mT__190() throws RecognitionException {
        try {
            int _type = T__190;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:84:8: ( '&&' )
            // jkit/java/Java.g:84:10: '&&'
            {
            match("&&"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__190"

    // $ANTLR start "T__191"
    public final void mT__191() throws RecognitionException {
        try {
            int _type = T__191;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:85:8: ( '|' )
            // jkit/java/Java.g:85:10: '|'
            {
            match('|'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__191"

    // $ANTLR start "T__192"
    public final void mT__192() throws RecognitionException {
        try {
            int _type = T__192;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:86:8: ( '^' )
            // jkit/java/Java.g:86:10: '^'
            {
            match('^'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__192"

    // $ANTLR start "T__193"
    public final void mT__193() throws RecognitionException {
        try {
            int _type = T__193;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:87:8: ( '==' )
            // jkit/java/Java.g:87:10: '=='
            {
            match("=="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__193"

    // $ANTLR start "T__194"
    public final void mT__194() throws RecognitionException {
        try {
            int _type = T__194;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:88:8: ( '!=' )
            // jkit/java/Java.g:88:10: '!='
            {
            match("!="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__194"

    // $ANTLR start "T__195"
    public final void mT__195() throws RecognitionException {
        try {
            int _type = T__195;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:89:8: ( 'instanceof' )
            // jkit/java/Java.g:89:10: 'instanceof'
            {
            match("instanceof"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__195"

    // $ANTLR start "T__196"
    public final void mT__196() throws RecognitionException {
        try {
            int _type = T__196;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:90:8: ( '++' )
            // jkit/java/Java.g:90:10: '++'
            {
            match("++"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__196"

    // $ANTLR start "T__197"
    public final void mT__197() throws RecognitionException {
        try {
            int _type = T__197;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:91:8: ( '--' )
            // jkit/java/Java.g:91:10: '--'
            {
            match("--"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__197"

    // $ANTLR start "T__198"
    public final void mT__198() throws RecognitionException {
        try {
            int _type = T__198;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:92:8: ( '~' )
            // jkit/java/Java.g:92:10: '~'
            {
            match('~'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__198"

    // $ANTLR start "T__199"
    public final void mT__199() throws RecognitionException {
        try {
            int _type = T__199;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:93:8: ( '!' )
            // jkit/java/Java.g:93:10: '!'
            {
            match('!'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__199"

    // $ANTLR start "T__200"
    public final void mT__200() throws RecognitionException {
        try {
            int _type = T__200;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:94:8: ( 'new' )
            // jkit/java/Java.g:94:10: 'new'
            {
            match("new"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__200"

    // $ANTLR start "HexLiteral"
    public final void mHexLiteral() throws RecognitionException {
        try {
            int _type = HexLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:936:12: ( '0' ( 'x' | 'X' ) ( HexDigit )+ ( IntegerTypeSuffix )? )
            // jkit/java/Java.g:936:14: '0' ( 'x' | 'X' ) ( HexDigit )+ ( IntegerTypeSuffix )?
            {
            match('0'); 
            if ( input.LA(1)=='X'||input.LA(1)=='x' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // jkit/java/Java.g:936:28: ( HexDigit )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='0' && LA1_0<='9')||(LA1_0>='A' && LA1_0<='F')||(LA1_0>='a' && LA1_0<='f')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // jkit/java/Java.g:936:28: HexDigit
            	    {
            	    mHexDigit(); 

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);

            // jkit/java/Java.g:936:38: ( IntegerTypeSuffix )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='L'||LA2_0=='l') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // jkit/java/Java.g:936:38: IntegerTypeSuffix
                    {
                    mIntegerTypeSuffix(); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "HexLiteral"

    // $ANTLR start "DecimalLiteral"
    public final void mDecimalLiteral() throws RecognitionException {
        try {
            int _type = DecimalLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:938:16: ( ( '0' | '1' .. '9' ( '0' .. '9' )* ) ( IntegerTypeSuffix )? )
            // jkit/java/Java.g:938:18: ( '0' | '1' .. '9' ( '0' .. '9' )* ) ( IntegerTypeSuffix )?
            {
            // jkit/java/Java.g:938:18: ( '0' | '1' .. '9' ( '0' .. '9' )* )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='0') ) {
                alt4=1;
            }
            else if ( ((LA4_0>='1' && LA4_0<='9')) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // jkit/java/Java.g:938:19: '0'
                    {
                    match('0'); 

                    }
                    break;
                case 2 :
                    // jkit/java/Java.g:938:25: '1' .. '9' ( '0' .. '9' )*
                    {
                    matchRange('1','9'); 
                    // jkit/java/Java.g:938:34: ( '0' .. '9' )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( ((LA3_0>='0' && LA3_0<='9')) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // jkit/java/Java.g:938:34: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);


                    }
                    break;

            }

            // jkit/java/Java.g:938:45: ( IntegerTypeSuffix )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='L'||LA5_0=='l') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // jkit/java/Java.g:938:45: IntegerTypeSuffix
                    {
                    mIntegerTypeSuffix(); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DecimalLiteral"

    // $ANTLR start "OctalLiteral"
    public final void mOctalLiteral() throws RecognitionException {
        try {
            int _type = OctalLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:940:14: ( '0' ( '0' .. '7' )+ ( IntegerTypeSuffix )? )
            // jkit/java/Java.g:940:16: '0' ( '0' .. '7' )+ ( IntegerTypeSuffix )?
            {
            match('0'); 
            // jkit/java/Java.g:940:20: ( '0' .. '7' )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>='0' && LA6_0<='7')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // jkit/java/Java.g:940:21: '0' .. '7'
            	    {
            	    matchRange('0','7'); 

            	    }
            	    break;

            	default :
            	    if ( cnt6 >= 1 ) break loop6;
                        EarlyExitException eee =
                            new EarlyExitException(6, input);
                        throw eee;
                }
                cnt6++;
            } while (true);

            // jkit/java/Java.g:940:32: ( IntegerTypeSuffix )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0=='L'||LA7_0=='l') ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // jkit/java/Java.g:940:32: IntegerTypeSuffix
                    {
                    mIntegerTypeSuffix(); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OctalLiteral"

    // $ANTLR start "HexDigit"
    public final void mHexDigit() throws RecognitionException {
        try {
            // jkit/java/Java.g:943:10: ( ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' ) )
            // jkit/java/Java.g:943:12: ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )
            {
            if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='F')||(input.LA(1)>='a' && input.LA(1)<='f') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "HexDigit"

    // $ANTLR start "IntegerTypeSuffix"
    public final void mIntegerTypeSuffix() throws RecognitionException {
        try {
            // jkit/java/Java.g:946:19: ( ( 'l' | 'L' ) )
            // jkit/java/Java.g:946:21: ( 'l' | 'L' )
            {
            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "IntegerTypeSuffix"

    // $ANTLR start "FloatingPointLiteral"
    public final void mFloatingPointLiteral() throws RecognitionException {
        try {
            int _type = FloatingPointLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:949:5: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( Exponent )? ( FloatTypeSuffix )? | '.' ( '0' .. '9' )+ ( Exponent )? ( FloatTypeSuffix )? | ( '0' .. '9' )+ Exponent | ( '0' .. '9' )+ FloatTypeSuffix | ( '0' .. '9' )+ Exponent FloatTypeSuffix )
            int alt18=5;
            alt18 = dfa18.predict(input);
            switch (alt18) {
                case 1 :
                    // jkit/java/Java.g:949:9: ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( Exponent )? ( FloatTypeSuffix )?
                    {
                    // jkit/java/Java.g:949:9: ( '0' .. '9' )+
                    int cnt8=0;
                    loop8:
                    do {
                        int alt8=2;
                        int LA8_0 = input.LA(1);

                        if ( ((LA8_0>='0' && LA8_0<='9')) ) {
                            alt8=1;
                        }


                        switch (alt8) {
                    	case 1 :
                    	    // jkit/java/Java.g:949:10: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt8 >= 1 ) break loop8;
                                EarlyExitException eee =
                                    new EarlyExitException(8, input);
                                throw eee;
                        }
                        cnt8++;
                    } while (true);

                    match('.'); 
                    // jkit/java/Java.g:949:25: ( '0' .. '9' )*
                    loop9:
                    do {
                        int alt9=2;
                        int LA9_0 = input.LA(1);

                        if ( ((LA9_0>='0' && LA9_0<='9')) ) {
                            alt9=1;
                        }


                        switch (alt9) {
                    	case 1 :
                    	    // jkit/java/Java.g:949:26: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    break loop9;
                        }
                    } while (true);

                    // jkit/java/Java.g:949:37: ( Exponent )?
                    int alt10=2;
                    int LA10_0 = input.LA(1);

                    if ( (LA10_0=='E'||LA10_0=='e') ) {
                        alt10=1;
                    }
                    switch (alt10) {
                        case 1 :
                            // jkit/java/Java.g:949:37: Exponent
                            {
                            mExponent(); 

                            }
                            break;

                    }

                    // jkit/java/Java.g:949:47: ( FloatTypeSuffix )?
                    int alt11=2;
                    int LA11_0 = input.LA(1);

                    if ( (LA11_0=='D'||LA11_0=='F'||LA11_0=='d'||LA11_0=='f') ) {
                        alt11=1;
                    }
                    switch (alt11) {
                        case 1 :
                            // jkit/java/Java.g:949:47: FloatTypeSuffix
                            {
                            mFloatTypeSuffix(); 

                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // jkit/java/Java.g:950:9: '.' ( '0' .. '9' )+ ( Exponent )? ( FloatTypeSuffix )?
                    {
                    match('.'); 
                    // jkit/java/Java.g:950:13: ( '0' .. '9' )+
                    int cnt12=0;
                    loop12:
                    do {
                        int alt12=2;
                        int LA12_0 = input.LA(1);

                        if ( ((LA12_0>='0' && LA12_0<='9')) ) {
                            alt12=1;
                        }


                        switch (alt12) {
                    	case 1 :
                    	    // jkit/java/Java.g:950:14: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt12 >= 1 ) break loop12;
                                EarlyExitException eee =
                                    new EarlyExitException(12, input);
                                throw eee;
                        }
                        cnt12++;
                    } while (true);

                    // jkit/java/Java.g:950:25: ( Exponent )?
                    int alt13=2;
                    int LA13_0 = input.LA(1);

                    if ( (LA13_0=='E'||LA13_0=='e') ) {
                        alt13=1;
                    }
                    switch (alt13) {
                        case 1 :
                            // jkit/java/Java.g:950:25: Exponent
                            {
                            mExponent(); 

                            }
                            break;

                    }

                    // jkit/java/Java.g:950:35: ( FloatTypeSuffix )?
                    int alt14=2;
                    int LA14_0 = input.LA(1);

                    if ( (LA14_0=='D'||LA14_0=='F'||LA14_0=='d'||LA14_0=='f') ) {
                        alt14=1;
                    }
                    switch (alt14) {
                        case 1 :
                            // jkit/java/Java.g:950:35: FloatTypeSuffix
                            {
                            mFloatTypeSuffix(); 

                            }
                            break;

                    }


                    }
                    break;
                case 3 :
                    // jkit/java/Java.g:951:9: ( '0' .. '9' )+ Exponent
                    {
                    // jkit/java/Java.g:951:9: ( '0' .. '9' )+
                    int cnt15=0;
                    loop15:
                    do {
                        int alt15=2;
                        int LA15_0 = input.LA(1);

                        if ( ((LA15_0>='0' && LA15_0<='9')) ) {
                            alt15=1;
                        }


                        switch (alt15) {
                    	case 1 :
                    	    // jkit/java/Java.g:951:10: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt15 >= 1 ) break loop15;
                                EarlyExitException eee =
                                    new EarlyExitException(15, input);
                                throw eee;
                        }
                        cnt15++;
                    } while (true);

                    mExponent(); 

                    }
                    break;
                case 4 :
                    // jkit/java/Java.g:952:7: ( '0' .. '9' )+ FloatTypeSuffix
                    {
                    // jkit/java/Java.g:952:7: ( '0' .. '9' )+
                    int cnt16=0;
                    loop16:
                    do {
                        int alt16=2;
                        int LA16_0 = input.LA(1);

                        if ( ((LA16_0>='0' && LA16_0<='9')) ) {
                            alt16=1;
                        }


                        switch (alt16) {
                    	case 1 :
                    	    // jkit/java/Java.g:952:8: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt16 >= 1 ) break loop16;
                                EarlyExitException eee =
                                    new EarlyExitException(16, input);
                                throw eee;
                        }
                        cnt16++;
                    } while (true);

                    mFloatTypeSuffix(); 

                    }
                    break;
                case 5 :
                    // jkit/java/Java.g:953:9: ( '0' .. '9' )+ Exponent FloatTypeSuffix
                    {
                    // jkit/java/Java.g:953:9: ( '0' .. '9' )+
                    int cnt17=0;
                    loop17:
                    do {
                        int alt17=2;
                        int LA17_0 = input.LA(1);

                        if ( ((LA17_0>='0' && LA17_0<='9')) ) {
                            alt17=1;
                        }


                        switch (alt17) {
                    	case 1 :
                    	    // jkit/java/Java.g:953:10: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt17 >= 1 ) break loop17;
                                EarlyExitException eee =
                                    new EarlyExitException(17, input);
                                throw eee;
                        }
                        cnt17++;
                    } while (true);

                    mExponent(); 
                    mFloatTypeSuffix(); 

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FloatingPointLiteral"

    // $ANTLR start "Exponent"
    public final void mExponent() throws RecognitionException {
        try {
            // jkit/java/Java.g:957:10: ( ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+ )
            // jkit/java/Java.g:957:12: ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // jkit/java/Java.g:957:22: ( '+' | '-' )?
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0=='+'||LA19_0=='-') ) {
                alt19=1;
            }
            switch (alt19) {
                case 1 :
                    // jkit/java/Java.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }

            // jkit/java/Java.g:957:33: ( '0' .. '9' )+
            int cnt20=0;
            loop20:
            do {
                int alt20=2;
                int LA20_0 = input.LA(1);

                if ( ((LA20_0>='0' && LA20_0<='9')) ) {
                    alt20=1;
                }


                switch (alt20) {
            	case 1 :
            	    // jkit/java/Java.g:957:34: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt20 >= 1 ) break loop20;
                        EarlyExitException eee =
                            new EarlyExitException(20, input);
                        throw eee;
                }
                cnt20++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "Exponent"

    // $ANTLR start "FloatTypeSuffix"
    public final void mFloatTypeSuffix() throws RecognitionException {
        try {
            // jkit/java/Java.g:960:17: ( ( 'f' | 'F' | 'd' | 'D' ) )
            // jkit/java/Java.g:960:19: ( 'f' | 'F' | 'd' | 'D' )
            {
            if ( input.LA(1)=='D'||input.LA(1)=='F'||input.LA(1)=='d'||input.LA(1)=='f' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "FloatTypeSuffix"

    // $ANTLR start "CharacterLiteral"
    public final void mCharacterLiteral() throws RecognitionException {
        try {
            int _type = CharacterLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:963:5: ( '\\'' ( EscapeSequence | ~ ( '\\'' | '\\\\' ) ) '\\'' )
            // jkit/java/Java.g:963:9: '\\'' ( EscapeSequence | ~ ( '\\'' | '\\\\' ) ) '\\''
            {
            match('\''); 
            // jkit/java/Java.g:963:14: ( EscapeSequence | ~ ( '\\'' | '\\\\' ) )
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0=='\\') ) {
                alt21=1;
            }
            else if ( ((LA21_0>='\u0000' && LA21_0<='&')||(LA21_0>='(' && LA21_0<='[')||(LA21_0>=']' && LA21_0<='\uFFFE')) ) {
                alt21=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 21, 0, input);

                throw nvae;
            }
            switch (alt21) {
                case 1 :
                    // jkit/java/Java.g:963:16: EscapeSequence
                    {
                    mEscapeSequence(); 

                    }
                    break;
                case 2 :
                    // jkit/java/Java.g:963:33: ~ ( '\\'' | '\\\\' )
                    {
                    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFE') ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }

            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CharacterLiteral"

    // $ANTLR start "StringLiteral"
    public final void mStringLiteral() throws RecognitionException {
        try {
            int _type = StringLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:967:5: ( '\"' ( EscapeSequence | ~ ( '\\\\' | '\"' ) )* '\"' )
            // jkit/java/Java.g:967:8: '\"' ( EscapeSequence | ~ ( '\\\\' | '\"' ) )* '\"'
            {
            match('\"'); 
            // jkit/java/Java.g:967:12: ( EscapeSequence | ~ ( '\\\\' | '\"' ) )*
            loop22:
            do {
                int alt22=3;
                int LA22_0 = input.LA(1);

                if ( (LA22_0=='\\') ) {
                    alt22=1;
                }
                else if ( ((LA22_0>='\u0000' && LA22_0<='!')||(LA22_0>='#' && LA22_0<='[')||(LA22_0>=']' && LA22_0<='\uFFFE')) ) {
                    alt22=2;
                }


                switch (alt22) {
            	case 1 :
            	    // jkit/java/Java.g:967:14: EscapeSequence
            	    {
            	    mEscapeSequence(); 

            	    }
            	    break;
            	case 2 :
            	    // jkit/java/Java.g:967:31: ~ ( '\\\\' | '\"' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop22;
                }
            } while (true);

            match('\"'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "StringLiteral"

    // $ANTLR start "EscapeSequence"
    public final void mEscapeSequence() throws RecognitionException {
        try {
            // jkit/java/Java.g:972:5: ( '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' ) | UnicodeEscape | OctalEscape )
            int alt23=3;
            int LA23_0 = input.LA(1);

            if ( (LA23_0=='\\') ) {
                switch ( input.LA(2) ) {
                case '\"':
                case '\'':
                case '\\':
                case 'b':
                case 'f':
                case 'n':
                case 'r':
                case 't':
                    {
                    alt23=1;
                    }
                    break;
                case 'u':
                    {
                    alt23=2;
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    {
                    alt23=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 23, 1, input);

                    throw nvae;
                }

            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 23, 0, input);

                throw nvae;
            }
            switch (alt23) {
                case 1 :
                    // jkit/java/Java.g:972:9: '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' )
                    {
                    match('\\'); 
                    if ( input.LA(1)=='\"'||input.LA(1)=='\''||input.LA(1)=='\\'||input.LA(1)=='b'||input.LA(1)=='f'||input.LA(1)=='n'||input.LA(1)=='r'||input.LA(1)=='t' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 2 :
                    // jkit/java/Java.g:973:9: UnicodeEscape
                    {
                    mUnicodeEscape(); 

                    }
                    break;
                case 3 :
                    // jkit/java/Java.g:974:9: OctalEscape
                    {
                    mOctalEscape(); 

                    }
                    break;

            }
        }
        finally {
        }
    }
    // $ANTLR end "EscapeSequence"

    // $ANTLR start "OctalEscape"
    public final void mOctalEscape() throws RecognitionException {
        try {
            // jkit/java/Java.g:979:5: ( '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) )
            int alt24=3;
            int LA24_0 = input.LA(1);

            if ( (LA24_0=='\\') ) {
                int LA24_1 = input.LA(2);

                if ( ((LA24_1>='0' && LA24_1<='3')) ) {
                    int LA24_2 = input.LA(3);

                    if ( ((LA24_2>='0' && LA24_2<='7')) ) {
                        int LA24_4 = input.LA(4);

                        if ( ((LA24_4>='0' && LA24_4<='7')) ) {
                            alt24=1;
                        }
                        else {
                            alt24=2;}
                    }
                    else {
                        alt24=3;}
                }
                else if ( ((LA24_1>='4' && LA24_1<='7')) ) {
                    int LA24_3 = input.LA(3);

                    if ( ((LA24_3>='0' && LA24_3<='7')) ) {
                        alt24=2;
                    }
                    else {
                        alt24=3;}
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 24, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 24, 0, input);

                throw nvae;
            }
            switch (alt24) {
                case 1 :
                    // jkit/java/Java.g:979:9: '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 
                    // jkit/java/Java.g:979:14: ( '0' .. '3' )
                    // jkit/java/Java.g:979:15: '0' .. '3'
                    {
                    matchRange('0','3'); 

                    }

                    // jkit/java/Java.g:979:25: ( '0' .. '7' )
                    // jkit/java/Java.g:979:26: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }

                    // jkit/java/Java.g:979:36: ( '0' .. '7' )
                    // jkit/java/Java.g:979:37: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }


                    }
                    break;
                case 2 :
                    // jkit/java/Java.g:980:9: '\\\\' ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 
                    // jkit/java/Java.g:980:14: ( '0' .. '7' )
                    // jkit/java/Java.g:980:15: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }

                    // jkit/java/Java.g:980:25: ( '0' .. '7' )
                    // jkit/java/Java.g:980:26: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }


                    }
                    break;
                case 3 :
                    // jkit/java/Java.g:981:9: '\\\\' ( '0' .. '7' )
                    {
                    match('\\'); 
                    // jkit/java/Java.g:981:14: ( '0' .. '7' )
                    // jkit/java/Java.g:981:15: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }


                    }
                    break;

            }
        }
        finally {
        }
    }
    // $ANTLR end "OctalEscape"

    // $ANTLR start "UnicodeEscape"
    public final void mUnicodeEscape() throws RecognitionException {
        try {
            // jkit/java/Java.g:986:5: ( '\\\\' 'u' HexDigit HexDigit HexDigit HexDigit )
            // jkit/java/Java.g:986:9: '\\\\' 'u' HexDigit HexDigit HexDigit HexDigit
            {
            match('\\'); 
            match('u'); 
            mHexDigit(); 
            mHexDigit(); 
            mHexDigit(); 
            mHexDigit(); 

            }

        }
        finally {
        }
    }
    // $ANTLR end "UnicodeEscape"

    // $ANTLR start "ENUM"
    public final void mENUM() throws RecognitionException {
        try {
            int _type = ENUM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:989:5: ( 'enum' )
            // jkit/java/Java.g:989:7: 'enum'
            {
            match("enum"); 

            if ( !enumIsKeyword ) _type=Identifier;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ENUM"

    // $ANTLR start "Identifier"
    public final void mIdentifier() throws RecognitionException {
        try {
            int _type = Identifier;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:993:5: ( Letter ( Letter | JavaIDDigit )* )
            // jkit/java/Java.g:993:9: Letter ( Letter | JavaIDDigit )*
            {
            mLetter(); 
            // jkit/java/Java.g:993:16: ( Letter | JavaIDDigit )*
            loop25:
            do {
                int alt25=2;
                int LA25_0 = input.LA(1);

                if ( (LA25_0=='$'||(LA25_0>='0' && LA25_0<='9')||(LA25_0>='A' && LA25_0<='Z')||LA25_0=='_'||(LA25_0>='a' && LA25_0<='z')||(LA25_0>='\u00C0' && LA25_0<='\u00D6')||(LA25_0>='\u00D8' && LA25_0<='\u00F6')||(LA25_0>='\u00F8' && LA25_0<='\u1FFF')||(LA25_0>='\u3040' && LA25_0<='\u318F')||(LA25_0>='\u3300' && LA25_0<='\u337F')||(LA25_0>='\u3400' && LA25_0<='\u3D2D')||(LA25_0>='\u4E00' && LA25_0<='\u9FFF')||(LA25_0>='\uF900' && LA25_0<='\uFAFF')) ) {
                    alt25=1;
                }


                switch (alt25) {
            	case 1 :
            	    // jkit/java/Java.g:
            	    {
            	    if ( input.LA(1)=='$'||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z')||(input.LA(1)>='\u00C0' && input.LA(1)<='\u00D6')||(input.LA(1)>='\u00D8' && input.LA(1)<='\u00F6')||(input.LA(1)>='\u00F8' && input.LA(1)<='\u1FFF')||(input.LA(1)>='\u3040' && input.LA(1)<='\u318F')||(input.LA(1)>='\u3300' && input.LA(1)<='\u337F')||(input.LA(1)>='\u3400' && input.LA(1)<='\u3D2D')||(input.LA(1)>='\u4E00' && input.LA(1)<='\u9FFF')||(input.LA(1)>='\uF900' && input.LA(1)<='\uFAFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop25;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "Identifier"

    // $ANTLR start "Letter"
    public final void mLetter() throws RecognitionException {
        try {
            // jkit/java/Java.g:1001:5: ( '\\u0024' | '\\u0041' .. '\\u005a' | '\\u005f' | '\\u0061' .. '\\u007a' | '\\u00c0' .. '\\u00d6' | '\\u00d8' .. '\\u00f6' | '\\u00f8' .. '\\u00ff' | '\\u0100' .. '\\u1fff' | '\\u3040' .. '\\u318f' | '\\u3300' .. '\\u337f' | '\\u3400' .. '\\u3d2d' | '\\u4e00' .. '\\u9fff' | '\\uf900' .. '\\ufaff' )
            // jkit/java/Java.g:
            {
            if ( input.LA(1)=='$'||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z')||(input.LA(1)>='\u00C0' && input.LA(1)<='\u00D6')||(input.LA(1)>='\u00D8' && input.LA(1)<='\u00F6')||(input.LA(1)>='\u00F8' && input.LA(1)<='\u1FFF')||(input.LA(1)>='\u3040' && input.LA(1)<='\u318F')||(input.LA(1)>='\u3300' && input.LA(1)<='\u337F')||(input.LA(1)>='\u3400' && input.LA(1)<='\u3D2D')||(input.LA(1)>='\u4E00' && input.LA(1)<='\u9FFF')||(input.LA(1)>='\uF900' && input.LA(1)<='\uFAFF') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "Letter"

    // $ANTLR start "JavaIDDigit"
    public final void mJavaIDDigit() throws RecognitionException {
        try {
            // jkit/java/Java.g:1018:5: ( '\\u0030' .. '\\u0039' | '\\u0660' .. '\\u0669' | '\\u06f0' .. '\\u06f9' | '\\u0966' .. '\\u096f' | '\\u09e6' .. '\\u09ef' | '\\u0a66' .. '\\u0a6f' | '\\u0ae6' .. '\\u0aef' | '\\u0b66' .. '\\u0b6f' | '\\u0be7' .. '\\u0bef' | '\\u0c66' .. '\\u0c6f' | '\\u0ce6' .. '\\u0cef' | '\\u0d66' .. '\\u0d6f' | '\\u0e50' .. '\\u0e59' | '\\u0ed0' .. '\\u0ed9' | '\\u1040' .. '\\u1049' )
            // jkit/java/Java.g:
            {
            if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='\u0660' && input.LA(1)<='\u0669')||(input.LA(1)>='\u06F0' && input.LA(1)<='\u06F9')||(input.LA(1)>='\u0966' && input.LA(1)<='\u096F')||(input.LA(1)>='\u09E6' && input.LA(1)<='\u09EF')||(input.LA(1)>='\u0A66' && input.LA(1)<='\u0A6F')||(input.LA(1)>='\u0AE6' && input.LA(1)<='\u0AEF')||(input.LA(1)>='\u0B66' && input.LA(1)<='\u0B6F')||(input.LA(1)>='\u0BE7' && input.LA(1)<='\u0BEF')||(input.LA(1)>='\u0C66' && input.LA(1)<='\u0C6F')||(input.LA(1)>='\u0CE6' && input.LA(1)<='\u0CEF')||(input.LA(1)>='\u0D66' && input.LA(1)<='\u0D6F')||(input.LA(1)>='\u0E50' && input.LA(1)<='\u0E59')||(input.LA(1)>='\u0ED0' && input.LA(1)<='\u0ED9')||(input.LA(1)>='\u1040' && input.LA(1)<='\u1049') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "JavaIDDigit"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:1035:5: ( ( ' ' | '\\r' | '\\t' | '\\u000C' | '\\n' ) )
            // jkit/java/Java.g:1035:8: ( ' ' | '\\r' | '\\t' | '\\u000C' | '\\n' )
            {
            if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||(input.LA(1)>='\f' && input.LA(1)<='\r')||input.LA(1)==' ' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:1039:5: ( '/*' ( options {greedy=false; } : . )* '*/' )
            // jkit/java/Java.g:1039:9: '/*' ( options {greedy=false; } : . )* '*/'
            {
            match("/*"); 

            // jkit/java/Java.g:1039:14: ( options {greedy=false; } : . )*
            loop26:
            do {
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( (LA26_0=='*') ) {
                    int LA26_1 = input.LA(2);

                    if ( (LA26_1=='/') ) {
                        alt26=2;
                    }
                    else if ( ((LA26_1>='\u0000' && LA26_1<='.')||(LA26_1>='0' && LA26_1<='\uFFFE')) ) {
                        alt26=1;
                    }


                }
                else if ( ((LA26_0>='\u0000' && LA26_0<=')')||(LA26_0>='+' && LA26_0<='\uFFFE')) ) {
                    alt26=1;
                }


                switch (alt26) {
            	case 1 :
            	    // jkit/java/Java.g:1039:42: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop26;
                }
            } while (true);

            match("*/"); 

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "LINE_COMMENT"
    public final void mLINE_COMMENT() throws RecognitionException {
        try {
            int _type = LINE_COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // jkit/java/Java.g:1043:5: ( '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' )
            // jkit/java/Java.g:1043:7: '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n'
            {
            match("//"); 

            // jkit/java/Java.g:1043:12: (~ ( '\\n' | '\\r' ) )*
            loop27:
            do {
                int alt27=2;
                int LA27_0 = input.LA(1);

                if ( ((LA27_0>='\u0000' && LA27_0<='\t')||(LA27_0>='\u000B' && LA27_0<='\f')||(LA27_0>='\u000E' && LA27_0<='\uFFFE')) ) {
                    alt27=1;
                }


                switch (alt27) {
            	case 1 :
            	    // jkit/java/Java.g:1043:12: ~ ( '\\n' | '\\r' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\f')||(input.LA(1)>='\u000E' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop27;
                }
            } while (true);

            // jkit/java/Java.g:1043:26: ( '\\r' )?
            int alt28=2;
            int LA28_0 = input.LA(1);

            if ( (LA28_0=='\r') ) {
                alt28=1;
            }
            switch (alt28) {
                case 1 :
                    // jkit/java/Java.g:1043:26: '\\r'
                    {
                    match('\r'); 

                    }
                    break;

            }

            match('\n'); 
            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LINE_COMMENT"

    public void mTokens() throws RecognitionException {
        // jkit/java/Java.g:1:8: ( T__117 | T__118 | T__119 | T__120 | T__121 | T__122 | T__123 | T__124 | T__125 | T__126 | T__127 | T__128 | T__129 | T__130 | T__131 | T__132 | T__133 | T__134 | T__135 | T__136 | T__137 | T__138 | T__139 | T__140 | T__141 | T__142 | T__143 | T__144 | T__145 | T__146 | T__147 | T__148 | T__149 | T__150 | T__151 | T__152 | T__153 | T__154 | T__155 | T__156 | T__157 | T__158 | T__159 | T__160 | T__161 | T__162 | T__163 | T__164 | T__165 | T__166 | T__167 | T__168 | T__169 | T__170 | T__171 | T__172 | T__173 | T__174 | T__175 | T__176 | T__177 | T__178 | T__179 | T__180 | T__181 | T__182 | T__183 | T__184 | T__185 | T__186 | T__187 | T__188 | T__189 | T__190 | T__191 | T__192 | T__193 | T__194 | T__195 | T__196 | T__197 | T__198 | T__199 | T__200 | HexLiteral | DecimalLiteral | OctalLiteral | FloatingPointLiteral | CharacterLiteral | StringLiteral | ENUM | Identifier | WS | COMMENT | LINE_COMMENT )
        int alt29=95;
        alt29 = dfa29.predict(input);
        switch (alt29) {
            case 1 :
                // jkit/java/Java.g:1:10: T__117
                {
                mT__117(); 

                }
                break;
            case 2 :
                // jkit/java/Java.g:1:17: T__118
                {
                mT__118(); 

                }
                break;
            case 3 :
                // jkit/java/Java.g:1:24: T__119
                {
                mT__119(); 

                }
                break;
            case 4 :
                // jkit/java/Java.g:1:31: T__120
                {
                mT__120(); 

                }
                break;
            case 5 :
                // jkit/java/Java.g:1:38: T__121
                {
                mT__121(); 

                }
                break;
            case 6 :
                // jkit/java/Java.g:1:45: T__122
                {
                mT__122(); 

                }
                break;
            case 7 :
                // jkit/java/Java.g:1:52: T__123
                {
                mT__123(); 

                }
                break;
            case 8 :
                // jkit/java/Java.g:1:59: T__124
                {
                mT__124(); 

                }
                break;
            case 9 :
                // jkit/java/Java.g:1:66: T__125
                {
                mT__125(); 

                }
                break;
            case 10 :
                // jkit/java/Java.g:1:73: T__126
                {
                mT__126(); 

                }
                break;
            case 11 :
                // jkit/java/Java.g:1:80: T__127
                {
                mT__127(); 

                }
                break;
            case 12 :
                // jkit/java/Java.g:1:87: T__128
                {
                mT__128(); 

                }
                break;
            case 13 :
                // jkit/java/Java.g:1:94: T__129
                {
                mT__129(); 

                }
                break;
            case 14 :
                // jkit/java/Java.g:1:101: T__130
                {
                mT__130(); 

                }
                break;
            case 15 :
                // jkit/java/Java.g:1:108: T__131
                {
                mT__131(); 

                }
                break;
            case 16 :
                // jkit/java/Java.g:1:115: T__132
                {
                mT__132(); 

                }
                break;
            case 17 :
                // jkit/java/Java.g:1:122: T__133
                {
                mT__133(); 

                }
                break;
            case 18 :
                // jkit/java/Java.g:1:129: T__134
                {
                mT__134(); 

                }
                break;
            case 19 :
                // jkit/java/Java.g:1:136: T__135
                {
                mT__135(); 

                }
                break;
            case 20 :
                // jkit/java/Java.g:1:143: T__136
                {
                mT__136(); 

                }
                break;
            case 21 :
                // jkit/java/Java.g:1:150: T__137
                {
                mT__137(); 

                }
                break;
            case 22 :
                // jkit/java/Java.g:1:157: T__138
                {
                mT__138(); 

                }
                break;
            case 23 :
                // jkit/java/Java.g:1:164: T__139
                {
                mT__139(); 

                }
                break;
            case 24 :
                // jkit/java/Java.g:1:171: T__140
                {
                mT__140(); 

                }
                break;
            case 25 :
                // jkit/java/Java.g:1:178: T__141
                {
                mT__141(); 

                }
                break;
            case 26 :
                // jkit/java/Java.g:1:185: T__142
                {
                mT__142(); 

                }
                break;
            case 27 :
                // jkit/java/Java.g:1:192: T__143
                {
                mT__143(); 

                }
                break;
            case 28 :
                // jkit/java/Java.g:1:199: T__144
                {
                mT__144(); 

                }
                break;
            case 29 :
                // jkit/java/Java.g:1:206: T__145
                {
                mT__145(); 

                }
                break;
            case 30 :
                // jkit/java/Java.g:1:213: T__146
                {
                mT__146(); 

                }
                break;
            case 31 :
                // jkit/java/Java.g:1:220: T__147
                {
                mT__147(); 

                }
                break;
            case 32 :
                // jkit/java/Java.g:1:227: T__148
                {
                mT__148(); 

                }
                break;
            case 33 :
                // jkit/java/Java.g:1:234: T__149
                {
                mT__149(); 

                }
                break;
            case 34 :
                // jkit/java/Java.g:1:241: T__150
                {
                mT__150(); 

                }
                break;
            case 35 :
                // jkit/java/Java.g:1:248: T__151
                {
                mT__151(); 

                }
                break;
            case 36 :
                // jkit/java/Java.g:1:255: T__152
                {
                mT__152(); 

                }
                break;
            case 37 :
                // jkit/java/Java.g:1:262: T__153
                {
                mT__153(); 

                }
                break;
            case 38 :
                // jkit/java/Java.g:1:269: T__154
                {
                mT__154(); 

                }
                break;
            case 39 :
                // jkit/java/Java.g:1:276: T__155
                {
                mT__155(); 

                }
                break;
            case 40 :
                // jkit/java/Java.g:1:283: T__156
                {
                mT__156(); 

                }
                break;
            case 41 :
                // jkit/java/Java.g:1:290: T__157
                {
                mT__157(); 

                }
                break;
            case 42 :
                // jkit/java/Java.g:1:297: T__158
                {
                mT__158(); 

                }
                break;
            case 43 :
                // jkit/java/Java.g:1:304: T__159
                {
                mT__159(); 

                }
                break;
            case 44 :
                // jkit/java/Java.g:1:311: T__160
                {
                mT__160(); 

                }
                break;
            case 45 :
                // jkit/java/Java.g:1:318: T__161
                {
                mT__161(); 

                }
                break;
            case 46 :
                // jkit/java/Java.g:1:325: T__162
                {
                mT__162(); 

                }
                break;
            case 47 :
                // jkit/java/Java.g:1:332: T__163
                {
                mT__163(); 

                }
                break;
            case 48 :
                // jkit/java/Java.g:1:339: T__164
                {
                mT__164(); 

                }
                break;
            case 49 :
                // jkit/java/Java.g:1:346: T__165
                {
                mT__165(); 

                }
                break;
            case 50 :
                // jkit/java/Java.g:1:353: T__166
                {
                mT__166(); 

                }
                break;
            case 51 :
                // jkit/java/Java.g:1:360: T__167
                {
                mT__167(); 

                }
                break;
            case 52 :
                // jkit/java/Java.g:1:367: T__168
                {
                mT__168(); 

                }
                break;
            case 53 :
                // jkit/java/Java.g:1:374: T__169
                {
                mT__169(); 

                }
                break;
            case 54 :
                // jkit/java/Java.g:1:381: T__170
                {
                mT__170(); 

                }
                break;
            case 55 :
                // jkit/java/Java.g:1:388: T__171
                {
                mT__171(); 

                }
                break;
            case 56 :
                // jkit/java/Java.g:1:395: T__172
                {
                mT__172(); 

                }
                break;
            case 57 :
                // jkit/java/Java.g:1:402: T__173
                {
                mT__173(); 

                }
                break;
            case 58 :
                // jkit/java/Java.g:1:409: T__174
                {
                mT__174(); 

                }
                break;
            case 59 :
                // jkit/java/Java.g:1:416: T__175
                {
                mT__175(); 

                }
                break;
            case 60 :
                // jkit/java/Java.g:1:423: T__176
                {
                mT__176(); 

                }
                break;
            case 61 :
                // jkit/java/Java.g:1:430: T__177
                {
                mT__177(); 

                }
                break;
            case 62 :
                // jkit/java/Java.g:1:437: T__178
                {
                mT__178(); 

                }
                break;
            case 63 :
                // jkit/java/Java.g:1:444: T__179
                {
                mT__179(); 

                }
                break;
            case 64 :
                // jkit/java/Java.g:1:451: T__180
                {
                mT__180(); 

                }
                break;
            case 65 :
                // jkit/java/Java.g:1:458: T__181
                {
                mT__181(); 

                }
                break;
            case 66 :
                // jkit/java/Java.g:1:465: T__182
                {
                mT__182(); 

                }
                break;
            case 67 :
                // jkit/java/Java.g:1:472: T__183
                {
                mT__183(); 

                }
                break;
            case 68 :
                // jkit/java/Java.g:1:479: T__184
                {
                mT__184(); 

                }
                break;
            case 69 :
                // jkit/java/Java.g:1:486: T__185
                {
                mT__185(); 

                }
                break;
            case 70 :
                // jkit/java/Java.g:1:493: T__186
                {
                mT__186(); 

                }
                break;
            case 71 :
                // jkit/java/Java.g:1:500: T__187
                {
                mT__187(); 

                }
                break;
            case 72 :
                // jkit/java/Java.g:1:507: T__188
                {
                mT__188(); 

                }
                break;
            case 73 :
                // jkit/java/Java.g:1:514: T__189
                {
                mT__189(); 

                }
                break;
            case 74 :
                // jkit/java/Java.g:1:521: T__190
                {
                mT__190(); 

                }
                break;
            case 75 :
                // jkit/java/Java.g:1:528: T__191
                {
                mT__191(); 

                }
                break;
            case 76 :
                // jkit/java/Java.g:1:535: T__192
                {
                mT__192(); 

                }
                break;
            case 77 :
                // jkit/java/Java.g:1:542: T__193
                {
                mT__193(); 

                }
                break;
            case 78 :
                // jkit/java/Java.g:1:549: T__194
                {
                mT__194(); 

                }
                break;
            case 79 :
                // jkit/java/Java.g:1:556: T__195
                {
                mT__195(); 

                }
                break;
            case 80 :
                // jkit/java/Java.g:1:563: T__196
                {
                mT__196(); 

                }
                break;
            case 81 :
                // jkit/java/Java.g:1:570: T__197
                {
                mT__197(); 

                }
                break;
            case 82 :
                // jkit/java/Java.g:1:577: T__198
                {
                mT__198(); 

                }
                break;
            case 83 :
                // jkit/java/Java.g:1:584: T__199
                {
                mT__199(); 

                }
                break;
            case 84 :
                // jkit/java/Java.g:1:591: T__200
                {
                mT__200(); 

                }
                break;
            case 85 :
                // jkit/java/Java.g:1:598: HexLiteral
                {
                mHexLiteral(); 

                }
                break;
            case 86 :
                // jkit/java/Java.g:1:609: DecimalLiteral
                {
                mDecimalLiteral(); 

                }
                break;
            case 87 :
                // jkit/java/Java.g:1:624: OctalLiteral
                {
                mOctalLiteral(); 

                }
                break;
            case 88 :
                // jkit/java/Java.g:1:637: FloatingPointLiteral
                {
                mFloatingPointLiteral(); 

                }
                break;
            case 89 :
                // jkit/java/Java.g:1:658: CharacterLiteral
                {
                mCharacterLiteral(); 

                }
                break;
            case 90 :
                // jkit/java/Java.g:1:675: StringLiteral
                {
                mStringLiteral(); 

                }
                break;
            case 91 :
                // jkit/java/Java.g:1:689: ENUM
                {
                mENUM(); 

                }
                break;
            case 92 :
                // jkit/java/Java.g:1:694: Identifier
                {
                mIdentifier(); 

                }
                break;
            case 93 :
                // jkit/java/Java.g:1:705: WS
                {
                mWS(); 

                }
                break;
            case 94 :
                // jkit/java/Java.g:1:708: COMMENT
                {
                mCOMMENT(); 

                }
                break;
            case 95 :
                // jkit/java/Java.g:1:716: LINE_COMMENT
                {
                mLINE_COMMENT(); 

                }
                break;

        }

    }


    protected DFA18 dfa18 = new DFA18(this);
    protected DFA29 dfa29 = new DFA29(this);
    static final String DFA18_eotS =
        "\7\uffff\1\10\2\uffff";
    static final String DFA18_eofS =
        "\12\uffff";
    static final String DFA18_minS =
        "\2\56\1\uffff\1\53\2\uffff\2\60\2\uffff";
    static final String DFA18_maxS =
        "\1\71\1\146\1\uffff\1\71\2\uffff\1\71\1\146\2\uffff";
    static final String DFA18_acceptS =
        "\2\uffff\1\2\1\uffff\1\4\1\1\2\uffff\1\3\1\5";
    static final String DFA18_specialS =
        "\12\uffff}>";
    static final String[] DFA18_transitionS = {
            "\1\2\1\uffff\12\1",
            "\1\5\1\uffff\12\1\12\uffff\1\4\1\3\1\4\35\uffff\1\4\1\3\1\4",
            "",
            "\1\6\1\uffff\1\6\2\uffff\12\7",
            "",
            "",
            "\12\7",
            "\12\7\12\uffff\1\11\1\uffff\1\11\35\uffff\1\11\1\uffff\1\11",
            "",
            ""
    };

    static final short[] DFA18_eot = DFA.unpackEncodedString(DFA18_eotS);
    static final short[] DFA18_eof = DFA.unpackEncodedString(DFA18_eofS);
    static final char[] DFA18_min = DFA.unpackEncodedStringToUnsignedChars(DFA18_minS);
    static final char[] DFA18_max = DFA.unpackEncodedStringToUnsignedChars(DFA18_maxS);
    static final short[] DFA18_accept = DFA.unpackEncodedString(DFA18_acceptS);
    static final short[] DFA18_special = DFA.unpackEncodedString(DFA18_specialS);
    static final short[][] DFA18_transition;

    static {
        int numStates = DFA18_transitionS.length;
        DFA18_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA18_transition[i] = DFA.unpackEncodedString(DFA18_transitionS[i]);
        }
    }

    class DFA18 extends DFA {

        public DFA18(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 18;
            this.eot = DFA18_eot;
            this.eof = DFA18_eof;
            this.min = DFA18_min;
            this.max = DFA18_max;
            this.accept = DFA18_accept;
            this.special = DFA18_special;
            this.transition = DFA18_transition;
        }
        public String getDescription() {
            return "948:1: FloatingPointLiteral : ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( Exponent )? ( FloatTypeSuffix )? | '.' ( '0' .. '9' )+ ( Exponent )? ( FloatTypeSuffix )? | ( '0' .. '9' )+ Exponent | ( '0' .. '9' )+ FloatTypeSuffix | ( '0' .. '9' )+ Exponent FloatTypeSuffix );";
        }
    }
    static final String DFA29_eotS =
        "\1\uffff\1\55\1\uffff\2\55\1\74\1\uffff\2\55\3\uffff\1\106\2\uffff"+
        "\2\55\2\uffff\1\113\6\55\5\uffff\2\55\1\136\1\140\1\143\1\146\1"+
        "\150\1\uffff\1\152\1\uffff\2\155\4\uffff\5\55\1\166\5\55\3\uffff"+
        "\7\55\3\uffff\3\55\2\uffff\15\55\1\u0099\3\55\17\uffff\1\u009d\1"+
        "\uffff\1\155\5\55\1\u00a5\1\55\1\uffff\23\55\1\u00ba\5\55\1\u00c0"+
        "\2\55\1\u00c3\5\55\1\uffff\3\55\1\uffff\7\55\1\uffff\10\55\1\u00db"+
        "\2\55\1\u00de\1\55\1\u00e0\1\u00e1\1\u00e2\3\55\1\u00e6\1\uffff"+
        "\5\55\1\uffff\1\55\1\u00ed\1\uffff\1\55\1\u00ef\1\55\1\u00f1\17"+
        "\55\1\u0101\1\u0102\1\55\1\u0104\1\uffff\1\55\1\u0106\1\uffff\1"+
        "\55\3\uffff\1\55\1\u010a\1\55\1\uffff\2\55\1\u010f\1\u0110\1\u0111"+
        "\1\55\1\uffff\1\55\1\uffff\1\u0114\1\uffff\2\55\1\u0117\2\55\1\u011a"+
        "\2\55\1\u011d\3\55\1\u0121\2\55\2\uffff\1\u0124\1\uffff\1\55\1\uffff"+
        "\2\55\1\u0128\1\uffff\2\55\1\u012b\1\55\3\uffff\1\u012d\1\55\1\uffff"+
        "\1\u012f\1\55\1\uffff\1\u0131\1\u0132\1\uffff\1\55\1\u0134\1\uffff"+
        "\3\55\1\uffff\2\55\1\uffff\1\55\1\u013b\1\55\1\uffff\2\55\1\uffff"+
        "\1\u013f\1\uffff\1\u0140\1\uffff\1\u0141\2\uffff\1\55\1\uffff\3"+
        "\55\1\u0146\1\55\1\u0148\1\uffff\1\u0149\1\55\1\u014b\3\uffff\1"+
        "\u014c\1\55\1\u014e\1\55\1\uffff\1\55\2\uffff\1\u0151\2\uffff\1"+
        "\u0152\1\uffff\1\u0153\1\55\3\uffff\1\55\1\u0156\1\uffff";
    static final String DFA29_eofS =
        "\u0157\uffff";
    static final String DFA29_minS =
        "\1\11\1\141\1\uffff\1\146\1\150\1\56\1\uffff\1\141\1\154\3\uffff"+
        "\1\46\2\uffff\1\157\1\150\2\uffff\1\75\1\142\2\141\2\157\1\145\5"+
        "\uffff\1\150\1\145\1\53\1\55\1\52\2\75\1\uffff\1\75\1\uffff\2\56"+
        "\4\uffff\1\143\1\142\1\151\1\160\1\163\1\44\1\141\1\156\1\157\1"+
        "\160\1\151\3\uffff\2\141\1\156\1\163\1\164\1\163\1\165\3\uffff\1"+
        "\151\1\162\1\141\2\uffff\2\163\1\156\1\157\1\154\1\162\1\164\1\154"+
        "\1\167\1\157\1\164\1\145\1\156\1\44\1\146\1\151\1\164\17\uffff\1"+
        "\56\1\uffff\1\56\1\153\1\154\1\164\1\166\1\154\1\44\1\164\1\uffff"+
        "\1\164\1\151\1\143\1\162\1\145\1\164\1\163\1\162\1\164\1\143\3\145"+
        "\1\155\1\144\1\141\1\157\1\156\1\145\1\44\1\164\1\145\2\141\1\163"+
        "\1\44\1\151\1\154\1\44\1\154\1\145\1\141\1\147\1\142\1\uffff\1\141"+
        "\1\154\1\165\1\uffff\1\141\1\151\1\145\1\141\1\162\1\145\1\162\1"+
        "\uffff\1\141\1\151\1\143\1\150\1\164\1\162\1\143\1\163\1\44\1\151"+
        "\1\150\1\44\1\156\3\44\1\164\1\167\1\163\1\44\1\uffff\2\162\1\154"+
        "\1\164\1\145\1\uffff\1\166\1\44\1\uffff\1\145\1\44\1\153\1\44\1"+
        "\154\1\165\1\145\1\162\1\147\2\143\2\164\1\155\1\146\1\156\1\143"+
        "\1\164\1\162\2\44\1\150\1\44\1\uffff\1\156\1\44\1\uffff\1\144\3"+
        "\uffff\1\151\1\44\1\151\1\uffff\1\141\1\164\3\44\1\145\1\uffff\1"+
        "\141\1\uffff\1\44\1\uffff\1\145\1\154\1\44\1\156\1\145\1\44\1\164"+
        "\1\145\1\44\1\145\1\141\1\143\1\44\1\146\1\157\2\uffff\1\44\1\uffff"+
        "\1\165\1\uffff\1\163\1\154\1\44\1\uffff\1\145\1\143\1\44\1\171\3"+
        "\uffff\1\44\1\156\1\uffff\1\44\1\164\1\uffff\2\44\1\uffff\1\145"+
        "\1\44\1\uffff\1\156\1\143\1\145\1\uffff\1\160\1\156\1\uffff\1\145"+
        "\1\44\1\145\1\uffff\1\156\1\164\1\uffff\1\44\1\uffff\1\44\1\uffff"+
        "\1\44\2\uffff\1\144\1\uffff\1\164\1\145\1\157\1\44\1\151\1\44\1"+
        "\uffff\1\44\1\164\1\44\3\uffff\1\44\1\163\1\44\1\146\1\uffff\1\172"+
        "\2\uffff\1\44\2\uffff\1\44\1\uffff\1\44\1\145\3\uffff\1\144\1\44"+
        "\1\uffff";
    static final String DFA29_maxS =
        "\1\ufaff\1\165\1\uffff\1\156\1\171\1\71\1\uffff\1\157\1\170\3\uffff"+
        "\1\75\2\uffff\1\157\1\162\2\uffff\1\75\1\163\1\157\1\165\1\171\2"+
        "\157\5\uffff\1\150\1\145\1\53\1\55\1\57\1\174\1\75\1\uffff\1\75"+
        "\1\uffff\1\170\1\146\4\uffff\1\143\1\142\1\157\1\160\1\164\1\ufaff"+
        "\1\162\1\156\1\157\1\160\1\151\3\uffff\2\141\1\156\2\164\1\163\1"+
        "\165\3\uffff\1\154\1\162\1\171\2\uffff\2\163\1\156\1\157\1\154\1"+
        "\162\1\164\1\154\1\167\1\157\1\164\1\145\1\156\1\ufaff\1\146\1\151"+
        "\1\164\17\uffff\1\146\1\uffff\1\146\1\153\1\154\1\164\1\166\1\157"+
        "\1\ufaff\1\164\1\uffff\1\164\1\151\1\143\1\162\1\145\1\164\1\163"+
        "\1\162\1\164\1\143\3\145\1\155\1\144\1\141\1\157\1\156\1\145\1\ufaff"+
        "\1\164\1\145\2\141\1\163\1\ufaff\1\151\1\154\1\ufaff\1\154\1\145"+
        "\1\141\1\147\1\142\1\uffff\1\141\1\154\1\165\1\uffff\1\141\1\151"+
        "\1\145\1\141\1\162\1\145\1\162\1\uffff\1\141\1\151\1\143\1\150\1"+
        "\164\1\162\1\143\1\163\1\ufaff\1\151\1\150\1\ufaff\1\156\3\ufaff"+
        "\1\164\1\167\1\163\1\ufaff\1\uffff\2\162\1\154\1\164\1\145\1\uffff"+
        "\1\166\1\ufaff\1\uffff\1\145\1\ufaff\1\153\1\ufaff\1\154\1\165\1"+
        "\145\1\162\1\147\2\143\2\164\1\155\1\146\1\156\1\143\1\164\1\162"+
        "\2\ufaff\1\150\1\ufaff\1\uffff\1\156\1\ufaff\1\uffff\1\144\3\uffff"+
        "\1\151\1\ufaff\1\151\1\uffff\1\141\1\164\3\ufaff\1\145\1\uffff\1"+
        "\141\1\uffff\1\ufaff\1\uffff\1\145\1\154\1\ufaff\1\156\1\145\1\ufaff"+
        "\1\164\1\145\1\ufaff\1\145\1\141\1\143\1\ufaff\1\146\1\157\2\uffff"+
        "\1\ufaff\1\uffff\1\165\1\uffff\1\163\1\154\1\ufaff\1\uffff\1\145"+
        "\1\143\1\ufaff\1\171\3\uffff\1\ufaff\1\156\1\uffff\1\ufaff\1\164"+
        "\1\uffff\2\ufaff\1\uffff\1\145\1\ufaff\1\uffff\1\156\1\143\1\145"+
        "\1\uffff\1\160\1\156\1\uffff\1\145\1\ufaff\1\145\1\uffff\1\156\1"+
        "\164\1\uffff\1\ufaff\1\uffff\1\ufaff\1\uffff\1\ufaff\2\uffff\1\144"+
        "\1\uffff\1\164\1\145\1\157\1\ufaff\1\151\1\ufaff\1\uffff\1\ufaff"+
        "\1\164\1\ufaff\3\uffff\1\ufaff\1\163\1\ufaff\1\146\1\uffff\1\172"+
        "\2\uffff\1\ufaff\2\uffff\1\ufaff\1\uffff\1\ufaff\1\145\3\uffff\1"+
        "\144\1\ufaff\1\uffff";
    static final String DFA29_acceptS =
        "\2\uffff\1\2\3\uffff\1\6\2\uffff\1\12\1\13\1\14\1\uffff\1\16\1\17"+
        "\2\uffff\1\23\1\24\7\uffff\1\50\1\52\1\53\1\60\1\63\7\uffff\1\110"+
        "\1\uffff\1\122\2\uffff\1\131\1\132\1\134\1\135\13\uffff\1\54\1\130"+
        "\1\5\7\uffff\1\105\1\112\1\15\3\uffff\1\115\1\25\21\uffff\1\120"+
        "\1\102\1\121\1\103\1\136\1\137\1\104\1\106\1\111\1\113\1\107\1\114"+
        "\1\116\1\123\1\125\1\uffff\1\126\10\uffff\1\64\42\uffff\1\70\3\uffff"+
        "\1\127\7\uffff\1\44\24\uffff\1\71\5\uffff\1\66\2\uffff\1\124\27"+
        "\uffff\1\41\2\uffff\1\101\1\uffff\1\65\1\133\1\21\3\uffff\1\56\6"+
        "\uffff\1\55\1\uffff\1\42\1\uffff\1\45\17\uffff\1\43\1\51\1\uffff"+
        "\1\7\1\uffff\1\100\3\uffff\1\75\4\uffff\1\32\1\46\1\57\2\uffff\1"+
        "\76\2\uffff\1\67\2\uffff\1\26\2\uffff\1\3\3\uffff\1\4\2\uffff\1"+
        "\73\3\uffff\1\22\2\uffff\1\62\1\uffff\1\33\1\uffff\1\47\1\uffff"+
        "\1\74\1\1\1\uffff\1\30\6\uffff\1\10\3\uffff\1\72\1\40\1\61\4\uffff"+
        "\1\37\1\uffff\1\77\1\36\1\uffff\1\31\1\27\1\uffff\1\20\2\uffff\1"+
        "\35\1\11\1\117\2\uffff\1\34";
    static final String DFA29_specialS =
        "\u0157\uffff}>";
    static final String[] DFA29_transitionS = {
            "\2\56\1\uffff\2\56\22\uffff\1\56\1\47\1\54\1\uffff\1\55\1\46"+
            "\1\14\1\53\1\33\1\34\1\6\1\41\1\12\1\42\1\5\1\43\1\51\11\52"+
            "\1\36\1\2\1\11\1\23\1\13\1\32\1\35\32\55\1\21\1\uffff\1\22\1"+
            "\45\1\55\1\uffff\1\24\1\27\1\7\1\31\1\10\1\25\2\55\1\3\2\55"+
            "\1\30\1\55\1\26\1\55\1\1\1\55\1\40\1\4\1\20\1\55\1\17\1\37\3"+
            "\55\1\15\1\44\1\16\1\50\101\uffff\27\55\1\uffff\37\55\1\uffff"+
            "\u1f08\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff"+
            "\u092e\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\57\20\uffff\1\61\2\uffff\1\60",
            "",
            "\1\64\6\uffff\1\62\1\63",
            "\1\67\13\uffff\1\65\1\70\1\uffff\1\71\1\uffff\1\66",
            "\1\72\1\uffff\12\73",
            "",
            "\1\100\6\uffff\1\76\3\uffff\1\75\2\uffff\1\77",
            "\1\102\1\uffff\1\103\11\uffff\1\101",
            "",
            "",
            "",
            "\1\105\26\uffff\1\104",
            "",
            "",
            "\1\107",
            "\1\110\11\uffff\1\111",
            "",
            "",
            "\1\112",
            "\1\114\20\uffff\1\115",
            "\1\120\7\uffff\1\116\2\uffff\1\117\2\uffff\1\121",
            "\1\122\3\uffff\1\124\17\uffff\1\123",
            "\1\125\2\uffff\1\127\6\uffff\1\126",
            "\1\130",
            "\1\132\11\uffff\1\131",
            "",
            "",
            "",
            "",
            "",
            "\1\133",
            "\1\134",
            "\1\135",
            "\1\137",
            "\1\141\4\uffff\1\142",
            "\1\144\76\uffff\1\145",
            "\1\147",
            "",
            "\1\151",
            "",
            "\1\73\1\uffff\10\154\2\73\12\uffff\3\73\21\uffff\1\153\13\uffff"+
            "\3\73\21\uffff\1\153",
            "\1\73\1\uffff\12\156\12\uffff\3\73\35\uffff\3\73",
            "",
            "",
            "",
            "",
            "\1\157",
            "\1\160",
            "\1\162\5\uffff\1\161",
            "\1\163",
            "\1\165\1\164",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\167\20\uffff\1\170",
            "\1\171",
            "\1\172",
            "\1\173",
            "\1\174",
            "",
            "",
            "",
            "\1\175",
            "\1\176",
            "\1\177",
            "\1\u0081\1\u0080",
            "\1\u0082",
            "\1\u0083",
            "\1\u0084",
            "",
            "",
            "",
            "\1\u0085\2\uffff\1\u0086",
            "\1\u0087",
            "\1\u0088\23\uffff\1\u0089\3\uffff\1\u008a",
            "",
            "",
            "\1\u008b",
            "\1\u008c",
            "\1\u008d",
            "\1\u008e",
            "\1\u008f",
            "\1\u0090",
            "\1\u0091",
            "\1\u0092",
            "\1\u0093",
            "\1\u0094",
            "\1\u0095",
            "\1\u0096",
            "\1\u0097",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\24"+
            "\55\1\u0098\5\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08"+
            "\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\u009a",
            "\1\u009b",
            "\1\u009c",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\73\1\uffff\10\154\2\73\12\uffff\3\73\35\uffff\3\73",
            "",
            "\1\73\1\uffff\12\156\12\uffff\3\73\35\uffff\3\73",
            "\1\u009e",
            "\1\u009f",
            "\1\u00a0",
            "\1\u00a1",
            "\1\u00a3\2\uffff\1\u00a2",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\4\55"+
            "\1\u00a4\25\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55"+
            "\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\u00a6",
            "",
            "\1\u00a7",
            "\1\u00a8",
            "\1\u00a9",
            "\1\u00aa",
            "\1\u00ab",
            "\1\u00ac",
            "\1\u00ad",
            "\1\u00ae",
            "\1\u00af",
            "\1\u00b0",
            "\1\u00b1",
            "\1\u00b2",
            "\1\u00b3",
            "\1\u00b4",
            "\1\u00b5",
            "\1\u00b6",
            "\1\u00b7",
            "\1\u00b8",
            "\1\u00b9",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00bb",
            "\1\u00bc",
            "\1\u00bd",
            "\1\u00be",
            "\1\u00bf",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00c1",
            "\1\u00c2",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00c4",
            "\1\u00c5",
            "\1\u00c6",
            "\1\u00c7",
            "\1\u00c8",
            "",
            "\1\u00c9",
            "\1\u00ca",
            "\1\u00cb",
            "",
            "\1\u00cc",
            "\1\u00cd",
            "\1\u00ce",
            "\1\u00cf",
            "\1\u00d0",
            "\1\u00d1",
            "\1\u00d2",
            "",
            "\1\u00d3",
            "\1\u00d4",
            "\1\u00d5",
            "\1\u00d6",
            "\1\u00d7",
            "\1\u00d8",
            "\1\u00d9",
            "\1\u00da",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00dc",
            "\1\u00dd",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00df",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00e3",
            "\1\u00e4",
            "\1\u00e5",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u00e7",
            "\1\u00e8",
            "\1\u00e9",
            "\1\u00ea",
            "\1\u00eb",
            "",
            "\1\u00ec",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u00ee",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00f0",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u00f2",
            "\1\u00f3",
            "\1\u00f4",
            "\1\u00f5",
            "\1\u00f6",
            "\1\u00f7",
            "\1\u00f8",
            "\1\u00f9",
            "\1\u00fa",
            "\1\u00fb",
            "\1\u00fc",
            "\1\u00fd",
            "\1\u00fe",
            "\1\u00ff",
            "\1\u0100",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0103",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0105",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0107",
            "",
            "",
            "",
            "\1\u0108",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\22"+
            "\55\1\u0109\7\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08"+
            "\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\u010b",
            "",
            "\1\u010c",
            "\1\u010d",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\13"+
            "\55\1\u010e\16\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08"+
            "\55\u1040\uffff\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e"+
            "\55\u10d2\uffff\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0112",
            "",
            "\1\u0113",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0115",
            "\1\u0116",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0118",
            "\1\u0119",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u011b",
            "\1\u011c",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u011e",
            "\1\u011f",
            "\1\u0120",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0122",
            "\1\u0123",
            "",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0125",
            "",
            "\1\u0126",
            "\1\u0127",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0129",
            "\1\u012a",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u012c",
            "",
            "",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u012e",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0130",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0133",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\u0135",
            "\1\u0136",
            "\1\u0137",
            "",
            "\1\u0138",
            "\1\u0139",
            "",
            "\1\u013a",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u013c",
            "",
            "\1\u013d",
            "\1\u013e",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "\1\u0142",
            "",
            "\1\u0143",
            "\1\u0144",
            "\1\u0145",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0147",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u014a",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u014d",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u014f",
            "",
            "\1\u0150",
            "",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            "\1\u0154",
            "",
            "",
            "",
            "\1\u0155",
            "\1\55\13\uffff\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32"+
            "\55\105\uffff\27\55\1\uffff\37\55\1\uffff\u1f08\55\u1040\uffff"+
            "\u0150\55\u0170\uffff\u0080\55\u0080\uffff\u092e\55\u10d2\uffff"+
            "\u5200\55\u5900\uffff\u0200\55",
            ""
    };

    static final short[] DFA29_eot = DFA.unpackEncodedString(DFA29_eotS);
    static final short[] DFA29_eof = DFA.unpackEncodedString(DFA29_eofS);
    static final char[] DFA29_min = DFA.unpackEncodedStringToUnsignedChars(DFA29_minS);
    static final char[] DFA29_max = DFA.unpackEncodedStringToUnsignedChars(DFA29_maxS);
    static final short[] DFA29_accept = DFA.unpackEncodedString(DFA29_acceptS);
    static final short[] DFA29_special = DFA.unpackEncodedString(DFA29_specialS);
    static final short[][] DFA29_transition;

    static {
        int numStates = DFA29_transitionS.length;
        DFA29_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA29_transition[i] = DFA.unpackEncodedString(DFA29_transitionS[i]);
        }
    }

    class DFA29 extends DFA {

        public DFA29(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 29;
            this.eot = DFA29_eot;
            this.eof = DFA29_eof;
            this.min = DFA29_min;
            this.max = DFA29_max;
            this.accept = DFA29_accept;
            this.special = DFA29_special;
            this.transition = DFA29_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__117 | T__118 | T__119 | T__120 | T__121 | T__122 | T__123 | T__124 | T__125 | T__126 | T__127 | T__128 | T__129 | T__130 | T__131 | T__132 | T__133 | T__134 | T__135 | T__136 | T__137 | T__138 | T__139 | T__140 | T__141 | T__142 | T__143 | T__144 | T__145 | T__146 | T__147 | T__148 | T__149 | T__150 | T__151 | T__152 | T__153 | T__154 | T__155 | T__156 | T__157 | T__158 | T__159 | T__160 | T__161 | T__162 | T__163 | T__164 | T__165 | T__166 | T__167 | T__168 | T__169 | T__170 | T__171 | T__172 | T__173 | T__174 | T__175 | T__176 | T__177 | T__178 | T__179 | T__180 | T__181 | T__182 | T__183 | T__184 | T__185 | T__186 | T__187 | T__188 | T__189 | T__190 | T__191 | T__192 | T__193 | T__194 | T__195 | T__196 | T__197 | T__198 | T__199 | T__200 | HexLiteral | DecimalLiteral | OctalLiteral | FloatingPointLiteral | CharacterLiteral | StringLiteral | ENUM | Identifier | WS | COMMENT | LINE_COMMENT );";
        }
    }
 

}