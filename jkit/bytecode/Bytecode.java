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
// (C) David James Pearce, 2007. 

package jkit.bytecode;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import jkit.jil.tree.Type;
import jkit.util.Pair;

public abstract class Bytecode {
	
	/** 
	 * Translate this Java bytecode into bytes. If the bytecode requires a
	 * constant pool item which is not present in the constantPool map, then the
	 * appropriate CONSTANT_Info object is added, and given the next available
	 * index.
	 * 
	 * @param offset
	 *            Offset of this bytecode
	 * @param labelOffsets
	 *            Offsets of any labels used in branch bytecodes
	 * @param constantPool
	 *            Indices of constant pool items used in various bytecodes (e.g.
	 *            ldc, putfield, etc)
	 * 
	 * @return
	 */
	public abstract byte[] toBytes(int offset, 
						Map<String,Integer> labelOffsets,
						Map<Constant.Info,Integer> constantPool);

	// ===============================
	// ======= JAVA BYTECODES ========
	// ===============================
	
	/**
     * Represents all bytecodes for storing to a local variable, including
     * istore, astore, lstore etc.
     */
	public final static class Store extends Bytecode {
		public final int slot;
		public final Type type;
		public Store(int slot, Type type) {
			this.slot=slot;
			this.type=type;
		}
		public String toString() {
			if(slot >= 0 && slot <= 3) {
				return typeChar(type) + "store_" + slot;
			} else {
				return typeChar(type) + "store " + slot;
			}
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();					
			if(slot >= 0 && slot <= 3) { 
				write_u1(out,ISTORE_0 + (4*typeOffset(type)) + slot); 
			} else {
				write_u1(out,ISTORE + typeOffset(type));
				write_u1(out,slot);
			}		
			return out.toByteArray();
		}
	}
	
	/**
     * Represents all bytecodes for loading from a local variable, including
     * iload, aload, lload etc.
     */
	public final static class Load extends Bytecode {
		public final int slot;
		public final Type type;
		
		public Load(int slot, Type type) {
			this.slot=slot;
			this.type=type;
		}
		
		public String toString() {
			if(slot >= 0 && slot <= 3) {
				return typeChar(type) + "load_" + slot;
			} else {
				return typeChar(type) + "load " + slot;
			}
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();					
			if(slot >= 0 && slot <= 3) { 
				write_u1(out,ILOAD_0 + (4*typeOffset(type)) + slot); 
			} else {
				write_u1(out,ILOAD + typeOffset(type));
				write_u1(out,slot);
			}		
			return out.toByteArray();
		}
	}

	/**
     * Represents the iinc bytecode for incrementing a local variable.
     */
	public final static class Iinc extends Bytecode {
		public final int slot;
		public final int increment;
		
		public Iinc(int slot, int increment) {
			this.slot=slot;
			this.increment = increment;
		}
		
		public String toString() {
			return "iinc " + slot + ", " + increment;
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();					
			write_u1(out,IINC);
			write_u1(out,slot);
			write_i1(out,increment);
			return out.toByteArray();
		}
	}

	/**
	 * Represents the arrayload bytecode. 
	 */
	public static final class ArrayLoad extends Bytecode {
		public final Type.Array type;
		
		public ArrayLoad(Type.Array type) { this.type = type; }
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,IALOAD + typeArrayOffset(type.element()));
			return out.toByteArray();
		}
		
		public String toString() { return typeArrayChar(type.element()) + "aload"; }
	}
	
	/**
	 * Represents the arraystore bytecode.
	 */
	public static final class ArrayStore extends Bytecode {
		public final Type.Array type;
		
		public ArrayStore(Type.Array type) { this.type = type; }
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,IASTORE + typeArrayOffset(type.element()));
			return out.toByteArray();
		}
		
		public String toString() { return typeArrayChar(type.element()) + "astore"; }
	}
	
	/**
     * Represents all bytecodes for loading constants. Including, iconst, bipush,
     * sipush, ldc, ldc_w
     */
	public static final class LoadConst extends Bytecode {
		public final Object constant;
		
		public LoadConst(Object constant) {
			assert constant == null || constant instanceof Number || constant instanceof String;
			this.constant = constant; 
		}				
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if(constant instanceof Integer) {
				int v = (Integer) constant;
				if(v >= -1 && v <= 5) { 
					write_u1(out,ICONST_0 + v); 
				} else if(v >= -128 && v <= 127) { 
					write_u1(out,BIPUSH);
					write_u1(out,v);
				} else if(v >= -32768 && v <= 32767) { 
					write_u1(out,SIPUSH);
					write_u2(out,v);
				} else {
					int idx = constantPool.get(new Constant.Integer(v));					
					if(idx < 255) {
						write_u1(out,LDC);					
						write_u1(out,idx);
					} else {
						write_u1(out,LDC_W);					
						write_u2(out,idx);
					}
				}
			} else if(constant instanceof Long) {
				long v = (Long) constant;
				if(v == 0 || v == 1) {
					write_u1(out,LCONST_0 + (int) v);
				} else {
					int idx = constantPool.get(new Constant.Long(v));
					write_u1(out,LDC2_W);
					write_u2(out,idx);
				}
			} else if(constant instanceof Float) {
				float v = (Float) constant;
				if(v == 0.0F) {
					write_u1(out,FCONST_0);
				} else if(v == 1.0F) {
					write_u1(out,FCONST_1);
				} else if(v == 2.0F) {
					write_u1(out,FCONST_2);
				} else {
					int idx = constantPool.get(new Constant.Float(v));
					if(idx < 255) {
						write_u1(out,LDC);
						write_u1(out,idx);
					} else {
						write_u1(out,LDC_W);
						write_u2(out,idx);
					}
				}
			} else if(constant instanceof Double) {
				double v = (Double) constant;
				if(v == 0.0D) {
					write_u1(out,DCONST_0);
				} else if(v == 1.0D) {
					write_u1(out,DCONST_1);
				} else {
					int idx = constantPool.get(new Constant.Double(v));
					write_u1(out,LDC2_W);
					write_u2(out,idx);
				}				
			} else if(constant instanceof String) {
				String v = (String) constant;
				int idx = constantPool.get(new Constant.String(new Constant.Utf8(v)));
				if(idx < 255) {
					write_u1(out,LDC);				
					write_u1(out,idx);
				} else {
					write_u1(out,LDC_W);				
					write_u2(out,idx);
				}
			} else if(constant instanceof Type) {
				Type.Reference ref = (Type.Reference) constant;
				int idx = constantPool.get(Constant.buildClass(ref));
				write_u1(out, LDC_W);
				write_u2(out, idx);
			} else {
				write_u1(out,ACONST_NULL);
			} 
			return out.toByteArray();
		}
		
		public String toString() {
			if(constant instanceof Integer) {
				int v = (Integer) constant;
				if(v >= -1 && v <= 5) { 
					return "iconst_" + v;
				} else if(v >= -128 && v <= 127) { 
					return "bipush " + v;
				} else if(v >= -32768 && v <= 32767) { 					
					return "sipush " + v;
				} else {
					return "ldc " + v;					
				}
			} else if(constant instanceof Long) {
				long v = (Long) constant;
				if(v == 0 || v == 1) {
					return "lconst_" + v;					
				} else {
					return "ldc2_w " + v;					
				}
			} else if(constant instanceof Float) {
				float v = (Float) constant;
				if(v == 0.0F) {
					return "fconst_0";
				} else if(v == 1.0F) {
					return "fconst_1";					
				} else if(v == 2.0F) {
					return "fconst_2";					
				} else {
					return "ldc " + v;					
				}				
			} else if(constant instanceof Double) {
				double v = (Double) constant;
				if(v == 0.0D) {
					return "dconst_0";
				} else if(v == 1.0D) {
					return "dconst_1";
				} else {
					return "ldc2_w " + v;
				}
			} else if(constant instanceof String) {
				String v = (String) constant;
				return "ldc " + v;
			} else {
				return "aconst_null";
			} 
		}
	}
	
	/**
	 * Represents return bytecodes, including ireturn, areturn, etc.
	 */
	public static final class Return extends Bytecode {
		public final Type type;
		public Return(Type type) { this.type = type; }		
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if(type == null) { write_u1(out,RETURN); } 
			else { write_u1(out,IRETURN + typeOffset(type)); }
			return out.toByteArray();
		}
		
		public String toString() {
			if(type == null) { return "return"; } 
			else { return typeChar(type) + "return"; }
		}
	}
	
	/**
     * This does not actually correspond to a bytecode per se. Rather it is an
     * imaginary bytecode which is used to mark the destination of branching
     * instructions. 
     */
	public static final class Label extends Bytecode {
		public final String name;		
		public Label(String name) { this.name = name; }		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			return new byte[0];
		}
		public String toString() { return name + ":"; }
	}
	
	public static final class Neg extends Bytecode {
		public final Type type;
		
		public Neg(Type type) { this.type = type; }
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,INEG + typeOffset(type));
			return out.toByteArray();
		}
		
		public String toString() { return typeChar(type) + "neg"; }
	}
	
	/**
     * This represents all binary operators involving two operands on the stack.
     * Examples include iadd, fsub, ldiv, ishr, lxor, etc.
     */
	public static final class BinOp extends Bytecode {
		public static final int ADD = 0;
		public static final int SUB = 1;
		public static final int MUL = 2;
		public static final int DIV = 3;
		public static final int REM = 4;
		public static final int SHL = 5;
		public static final int SHR = 6;
		public static final int USHR = 7;
		public static final int AND = 8;
		public static final int OR = 9;
		public static final int XOR = 10;
		
		private static final int[] base = {IADD, ISUB, IMUL, IDIV, IREM, ISHL,
				ISHR, IUSHR, IAND, IOR, IXOR};
		private static final String[] str = {"add", "sub", "mul", "div", "rem",
				"shl", "shr", "ushr", "and", "or", "xor"};

		public final Type type;
		public final int op;
		public BinOp(int op, Type type) {
			assert op >= 0 && op <= USHR;
			this.op = op;
			this.type = type;
		}
		public byte[] toBytes(int offset, Map<String, Integer> labelOffsets,
				Map<Constant.Info, Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out, base[op] + typeOffset(type));
			return out.toByteArray();
		}
		public String toString() {
			return typeChar(type) + str[op];
		}
	}
		

	/**
	 * Modifier to indicate get/put/invoke is static 
	 */
	public static final int STATIC = 1;
	/**
	 * Modifier to indicate get/put is non-static 
	 */
	public static final int NONSTATIC = 2;
	/**
	 * Modifier to indicate invoke is virtual 
	 */	
	public static final int VIRTUAL = 2;
	/**
	 * Modifier to indicate invoke is special 
	 */
	public static final int SPECIAL = 3;
	/**
	 * Modifier to indicate invoke is special 
	 */
	public static final int INTERFACE = 4;
	
	/**
	 * This represents the putfield and putstatic bytecodes.
	 */
	public static final class PutField extends Bytecode {
		public final Type.Reference owner;
		public final Type type;
		public final String name;
		public final int mode;
		
		public PutField(Type.Reference owner, String name, Type type, int mode) {
			assert mode >= 1 && mode <= 2;
			this.owner = owner;
			this.type = type;
			this.name = name;	
			this.mode = mode;
		}
		
		public byte[] toBytes(int offset, 
				Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();			
			int idx = constantPool.get(Constant.buildFieldRef(owner, name,
					type));
			if(mode == STATIC) {
				write_u1(out,PUTSTATIC);
			} else {
				write_u1(out,PUTFIELD);
			}
			write_u2(out,idx); 
			return out.toByteArray();
		}
		
		public String toString() {
			if(mode == STATIC) {
				return "putstatic " + owner + "." + name + ":" + type;
			} else {
				return "putfield " + owner + "." + name + ":" + type;
			}
		}
	}	
	
	/**
	 * This represents the getfield and getstatic bytecodes.
	 */
	public static final class GetField extends Bytecode {
		public final Type.Reference owner;
		public final Type type;
		public final String name;
		public final int mode;
		
		public GetField(Type.Reference owner, String name, Type type, int mode) {
			assert mode >= 1 && mode <= 2;
			this.owner = owner;
			this.type = type;
			this.name = name;
			this.mode = mode;
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();			
			int idx = constantPool.get(Constant.buildFieldRef(owner, name,
					type));
			if(mode == STATIC) {
				write_u1(out,GETSTATIC);
			} else {
				write_u1(out,GETFIELD);
			}
			write_u2(out,idx); 
			return out.toByteArray();
		}
		
		public String toString() {
			if(mode == STATIC) {
				return "getstatic " + owner + "." + name + ":" + type;
			} else {
				return "getfield " + owner + "." + name + ":" + type;
			}
		}
	}	
	
	/**
     * This represents the invokevirtual, invokespecial, invokestatic and
     * invokeinterface bytecodes
     */
	public static final class Invoke extends Bytecode {
		public final Type.Clazz owner;
		public final Type.Function type;
		public final String name;
		public final int mode;
		
		public Invoke(Type.Clazz owner, String name, Type.Function type, int mode) {
			assert mode >= 1 && mode <= 3;
			this.owner = owner;
			this.type = type;
			this.name = name;
			this.mode = mode;
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();			
			int idx;
			if(mode != INTERFACE) {
				idx = constantPool.get(Constant.buildMethodRef(owner, name,
					type));
			} else {
				idx = constantPool.get(Constant.buildInterfaceMethodRef(owner, name,
						type));
			}
			if(mode == STATIC) {
				write_u1(out,INVOKESTATIC);				 
			} else if(mode == VIRTUAL) {
				write_u1(out,INVOKEVIRTUAL);
			} else if(mode == SPECIAL){
				write_u1(out,INVOKESPECIAL);	
			} else {
				write_u1(out,INVOKEINTERFACE);
			}
			write_u2(out,idx); 
			if(mode == INTERFACE) {
				int ps = 1; // 1 for the "this" reference!
				for(Type t : type.parameterTypes()) {
					ps += ClassFile.slotSize(t);
				}
				write_u1(out,ps);
				write_u1(out,0);
				return out.toByteArray();
			}			
			return out.toByteArray();
		}
		
		public String toString() {		
			if(mode == STATIC) {
				return "invokestatic " + owner + "." + name + " " + type;
			} else if(mode == VIRTUAL) {
				return "invokevirtual " + owner + "." + name + " " + type;
			} else if(mode == SPECIAL) {
				return "invokespecial " + owner + "." + name + " " + type;
			} else {
				return "invokeinterface " + owner + "." + name + " " + type;
			}
		}
	}
	
	/**
	 * This represents the family of primitive conversion operations, such as
	 * i2f, d2f, l2i etc. Observe that in some cases (e.g. converting from a
	 * long to a byte) several bytecodes will be produced (e.g. l2i,i2b).
	 */
	public static final class Conversion extends Bytecode {
		public final Type.Primitive from;
		public final Type.Primitive to;
		
		public Conversion(Type.Primitive from, Type.Primitive to) {			
			this.from = from;
			this.to = to;
		}	
		
		public byte[] toBytes(int offset, Map<String, Integer> labelOffsets,
				Map<Constant.Info, Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (from instanceof Type.Int || from instanceof Type.Short
					|| from instanceof Type.Byte || from instanceof Type.Char) {
				// i2l, i2f, i2d, i2c, i2b, i2s
				if(to instanceof Type.Long) {
					write_u1(out,I2L);
				} else if(to instanceof Type.Float) {
					write_u1(out,I2F);
				} else if(to instanceof Type.Double) {
					write_u1(out,I2D);
				} else if(to instanceof Type.Char && !(from instanceof Type.Char)) {
					write_u1(out,I2C);
				} else if(to instanceof Type.Short && !(from instanceof Type.Short)) {
					write_u1(out,I2S);
				} else if(to instanceof Type.Byte && !(from instanceof Type.Byte)) {
					write_u1(out,I2B);
				} 
			} else if(from instanceof Type.Long) {
				// l2i, l2f, l2d
				if(to instanceof Type.Char) {
					write_u1(out,L2I);
					write_u1(out,I2C);
				} else if(to instanceof Type.Byte) {
					write_u1(out,L2I);
					write_u1(out,I2B);				
				} else if(to instanceof Type.Short) {
					write_u1(out,L2I);
					write_u1(out,I2S);				
				} if(to instanceof Type.Int) {
					write_u1(out,L2I);
				} else if(to instanceof Type.Float) {
					write_u1(out,L2F);
				} else if(to instanceof Type.Double) {
					write_u1(out,L2D);
				}
			} else if(from instanceof Type.Float) {
				// f2i, f2l, f2d
				if(to instanceof Type.Char) {
					write_u1(out,F2I);
					write_u1(out,I2C);
				} else if(to instanceof Type.Byte) {
					write_u1(out,F2I);
					write_u1(out,I2B);				
				} else if(to instanceof Type.Short) {
					write_u1(out,F2I);
					write_u1(out,I2S);				
				} if(to instanceof Type.Int) {
					write_u1(out,F2I);
				} else if(to instanceof Type.Long) {
					write_u1(out,F2L);
				} else if(to instanceof Type.Double) {
					write_u1(out,F2D);
				}
			} else if(from instanceof Type.Double) {
				// d2i, d2l, d2f
				if(to instanceof Type.Char) {
					write_u1(out,D2I);
					write_u1(out,I2C);
				} else if(to instanceof Type.Byte) {
					write_u1(out,D2I);
					write_u1(out,I2B);				
				} else if(to instanceof Type.Short) {
					write_u1(out,D2I);
					write_u1(out,I2S);				
				} if(to instanceof Type.Int) {
					write_u1(out,D2I);
				} else if(to instanceof Type.Long) {
					write_u1(out,D2L);
				} else if(to instanceof Type.Float) {
					write_u1(out,D2F);
				}
			} 
				
			return out.toByteArray();
		}
		
		public String toString() {
			if(from instanceof Type.Int || from instanceof Type.Short
					|| from instanceof Type.Byte || from instanceof Type.Char) {
				// i2l, i2f, i2d, i2c, i2b, i2s
				if(to instanceof Type.Long) {
					return "i2l";
				} else if(to instanceof Type.Float) {
					return "i2f";
				} else if(to instanceof Type.Double) {
					return "i2d";
				} else if(to instanceof Type.Char && !(from instanceof Type.Char)) {
					return "i2c";
				} else if(to instanceof Type.Short && !(from instanceof Type.Short)) {
					return "i2s";
				} else if(to instanceof Type.Byte && !(from instanceof Type.Byte)) {
					return "i2b";
				}
			} else if(from instanceof Type.Long) {
				// l2i, l2f, l2d
				if(to instanceof Type.Int) {
					return "l2i";
				} else if(to instanceof Type.Float) {
					return "l2f";
				} else if(to instanceof Type.Double) {
					return "l2d";
				}
			} else if(from instanceof Type.Float) {
				// f2i, f2l, f2d
				if(to instanceof Type.Int) {
					return "f2i";
				} else if(to instanceof Type.Long) {
					return "f2l";
				} else if(to instanceof Type.Double) {
					return "f2d";
				}
			} else if(from instanceof Type.Double) {
				// d2i, d2l, d2f
				if(to instanceof Type.Int) {
					return "d2i";
				} else if(to instanceof Type.Long) {
					return "d2l";
				} else if(to instanceof Type.Float) {
					return "d2f";
				}
			}
			throw new RuntimeException("Invalid conversion operator!");
		}
		
	}
	
	/**
     * This class abstracts different kinds of branching statements (e.g. goto,
     * ifacmp, etc). It probably should abstract switches as well, although it
     * currently doesn't,
     */
	public static abstract class Branch extends Bytecode {
		public final String label;
		public Branch(String label) {
			this.label = label;
		}
	}
	
	/**
	 * This class abstracts the unconditional branch bytecode goto.
	 */
	public static class Goto extends Branch {
		public Goto(String label) { super(label); }
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			if (!labelOffsets.keySet().contains(label)) {
				throw new IllegalArgumentException("Unable to resolve label \"" + label
						+ "\" in labelOffsets");
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// here, need to figure out how far away we're going
			int target = labelOffsets.get(label) - offset;
			if(-32768 <= target && target <= 32767) {
				write_u1(out,GOTO);
				write_i2(out,target);
			} else {
				write_u1(out,GOTO_W);
				write_i4(out,target);
			}
			return out.toByteArray();
		}
		public String toString() {
			return "goto " + label;
		}
	}
	
	/**
	 * This represents the bytecodes ifeq, ifne, iflt, ifge, ifgt, ifle.
	 */
	public static class If extends Branch {
		public final static int EQ=0;
		public final static int NE=1;
		public final static int LT=2;
		public final static int GE=3;
		public final static int GT=4;
		public final static int LE=5;
		public final static int NULL=6;
		public final static int NONNULL=7;
		public final static String[] str = { "eq", "ne", "lt", "ge", "gt", ",le", "null", "nonnull" };
		
		public final int cond;
		
		public If(int cond, String label) { 			 
			super(label);
			assert cond >=0 && cond <= LE;			
			this.cond=cond;
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			if (!labelOffsets.keySet().contains(label)) {
				throw new IllegalArgumentException("Unable to resolve label \"" + label
						+ "\" in labelOffsets");
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// here, need to figure out how far away we're going
			int target = labelOffsets.get(label) - offset;
			if(-32768 <= target && target <= 32767) {
				if(cond < NULL) {
					write_u1(out,IFEQ + cond);
				} else {
					write_u1(out,IFNULL + (cond-NULL));
				}
				write_i2(out,target);
			} else {
				throw new RuntimeException("Conditional branch target too far away");
			}
			return out.toByteArray();
		}
		public String toString() {
			return "if" + str[cond] + " " + label;
		}
	}
	
	/**
	 * This represents the bytecodes fcmpl, fcmpg, dcmpl, dcmpg and lcmp
	 */
	public static class Cmp extends Bytecode {
		public final static int EQ=0;
		public final static int LT=1;
		public final static int GT=2;		
		public final static String[] str = { "", "lt", "gt" };
		public final int op;
		public final Type type;
		
		/**
         * Construct a cmp bytecode.
         * 
         * @param type
         *            either Type.Float, Type.Double or Type.Long
         * @param op
         *            op == LT || OP == GT if type == Float or type == Double,
         *            otherwise op == EQ
         */
		public Cmp(Type type, int op){
			assert (type instanceof Type.Double && op >= 1 && op <= 2) ||
				(type instanceof Type.Float && op >= 1 && op <= 2) ||
				(type instanceof Type.Long && op == 0);
			this.type=type;
			this.op = op;
		}
		
		public byte[] toBytes(int offset, Map<String, Integer> labelOffsets,
				Map<Constant.Info, Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (type instanceof Type.Double) {
				if(op == LT) {
					write_u1(out, DCMPL);
				} else {
					write_u1(out, DCMPG);
				}
			} else if (type instanceof Type.Float) {
				if(op == LT) {
					write_u1(out, FCMPL);
				} else {
					write_u1(out, FCMPG);
				}
			} else if (type instanceof Type.Long) {
				write_u1(out, LCMP);
			}
			return out.toByteArray();
		}
		
		public String toString(){
			if(type instanceof Type.Double) {
				if(op == LT) {
					return "dcmpl";
				} else {
					return "fcmpg";
				}
			} else if(type instanceof Type.Float) {
				if(op == LT) {			
					return "fcmpl";
				} else {
					return "fcmpg";
				}
			} else  {
				return "lcmpl";
			} 
		}
	}
	
	/**
	 * This represents the bytecodes ifacmp_XX and ificmp_xx
	 */
	public static class IfCmp extends Branch {
		public final static int EQ=0;
		public final static int NE=1;
		public final static int LT=2;
		public final static int GE=3;
		public final static int GT=4;
		public final static int LE=5;
		public final static String[] str = { "eq", "ne", "lt", "ge", "gt", ",le" };
		
		public final int cond;
		public final Type type;
		
		public IfCmp(int cond, Type type, String label) { 			 
			super(label);		
			assert type instanceof Type.Int || type instanceof Type.Reference;
			assert cond >=0 && ((type instanceof Type.Int && cond <= LE)
					|| (type instanceof Type.Reference && cond <= NE));
			this.cond = cond;
			this.type = type;
		}
		
		public byte[] toBytes(int offset, Map<String, Integer> labelOffsets,
				Map<Constant.Info, Integer> constantPool) {
			assert labelOffsets.containsKey(label);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// here, need to figure out how far away we're going
			int target = labelOffsets.get(label) - offset;
			if (-32768 <= target && target <= 32767) {
				if (type instanceof Type.Primitive) {
					write_u1(out, IF_ICMPEQ + cond);
				} else {
					write_u1(out, IF_ACMPEQ + cond);
				}
				write_i2(out, target);
			} else {
				throw new RuntimeException(
						"Conditional branch target too far away");
			}
			return out.toByteArray();
		}
		
		public String toString() {
			if(type instanceof Type.Int) {
				return "if_icmp" + str[cond] + " " + label;				
			} else {
				return "if_acmp" + str[cond] + " " + label;				
			}			
		}
	}
	
	/**
	 * This represents the bytecodes tableswitch and lookupswitch
	 */
	public static class Switch extends Bytecode {
		
		public final String defaultLabel;
		public final List<Pair<Integer, String>> cases;
		public final int type;
		
		public Switch(String def, List<Pair<Integer, String>> cases) {
			
			this.defaultLabel = def;
			this.cases = cases;
			
			int lo = cases.get(0).first();
			int hi = cases.get(cases.size()-1).first();
			
			int tableSize = 4+4*(hi-lo+1);
			int lookupSize = 8*(cases.size());
			
			if (tableSize < lookupSize) {
				this.type = TABLESWITCH;
			}
			else {
				this.type = LOOKUPSWITCH;
			}
		}
		
		public int getSize(int offset) {
			int padding = 3 - (offset%4);
			if (type == LOOKUPSWITCH) {
				return 1 + padding + 4 + 4 + cases.size() * 8;
			}
			else {
				int lo = cases.get(0).first();
				int hi = cases.get(cases.size()-1).first();
				return 1 + padding + 4 + 8 + 4 * (hi-lo+1);
			}
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			if (!labelOffsets.keySet().contains(defaultLabel)) {
				throw new IllegalArgumentException("Unable to resolve label \""
						+ defaultLabel + "\" in labelOffsets");
			}			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out, type);			
			for (int i = 0; i < (3-(offset%4)); i++) {				
				write_u1(out, 0); // padding
			}			
			
			int def = labelOffsets.get(defaultLabel) - offset;
			write_i4(out, def);
			if (type == LOOKUPSWITCH) {
				write_i4(out, cases.size());
				for (Pair<Integer, String> c : cases) {
					write_i4(out, c.first());
					write_i4(out, labelOffsets.get(c.second()) - offset);
				}
			} else {
				int lo = cases.get(0).first();
				int hi = cases.get(cases.size()-1).first();
				write_i4(out, lo);
				write_i4(out, hi);
				
				int index = 0;
				for (int i = lo; i <= hi; i++) {
					Pair<Integer,String> c = cases.get(index);
					if (c.first() == i) {
						int target = labelOffsets.get(c.second()) - offset;						
						write_i4(out, target);
						index++;
					} else {
						write_i4(out, def);
					}
				}
			}
			return out.toByteArray();
		}
		
		public String toString() {
			String out = "lookupswitch {\n";
			out += "\tdefault\t: "+defaultLabel+"\n";
			for (Pair<Integer, String> c: cases) {
				out += "\t"+c.first()+"\t: "+c.second()+"\n"; 
			}
			out += "}";
			return out;
		}
	}
	
	/**
	 * Represents the pop and pop2 bytecodes
	 */
	public static final class Pop extends Bytecode {
		public final Type type;
		
		public Pop(Type type) { this.type = type; }
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if(ClassFile.slotSize(type) > 1) {
				write_u1(out,POP2);
			} else {
				write_u1(out,POP);
			}
			return out.toByteArray();
		}
		
		public String toString() {			
			if(ClassFile.slotSize(type) > 1) { return "pop2"; } 
			else {
				return "pop";
			}			
		}
	}
	
	/**
	 * Represents the dup and dup2 bytecodes
	 */
	public static final class Dup extends Bytecode {
		public final Type type;
		
		public Dup(Type type) { this.type = type; }
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if(ClassFile.slotSize(type) > 1) {
				write_u1(out,DUP2);
			} else {
				write_u1(out,DUP);
			}
			return out.toByteArray();
		}
		
		public String toString() {			
			if(ClassFile.slotSize(type) > 1) { return "dup2"; } 
			else {
				return "dup";
			}			
		}
	}
	
	/**
	 * Represents the dupx1 bytecode
	 */
	public static final class DupX1 extends Bytecode {
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,DUP_X1);	
			return out.toByteArray();
		}
		
		public String toString() {			
			return "dup_x1";			
		}
	}
	
	/**
	 * Represents the dupx2 bytecode
	 */
	public static final class DupX2 extends Bytecode {
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,DUP_X2);	
			return out.toByteArray();
		}
		
		public String toString() {			
			return "dup_x2";			
		}
	}
	
	/**
	 * Represents the new, newarray and anewarray, multinewarray bytecodes.
	 */
	public static final class New extends Bytecode {
		public final Type type;
		public final int dims;
		
		public New(Type type) {
			assert type instanceof Type.Reference || type instanceof Type.Array;
			this.type = type;
			this.dims = -1;
		}
		
		public New(Type type, int dims) {
			assert type instanceof Type.Reference || type instanceof Type.Array;
			this.type = type;
			this.dims = dims;
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();			
			if(type instanceof Type.Array) {
				Type.Array atype = (Type.Array) type;
				if(dims > 1) {
					int idx = constantPool.get(Constant
								.buildClass((Type.Array) type));
					write_u1(out,MULTIANEWARRAY);
					write_u2(out,idx);
					write_u1(out,dims);
				} else {				
					Type elementType = atype.element();
					
					if(elementType instanceof Type.Reference) {
						int idx = constantPool.get(Constant
								.buildClass((Type.Reference) elementType));
						write_u1(out,ANEWARRAY);
						write_u2(out,idx);
					}
					else if (elementType instanceof Type.Array) {
						int idx = constantPool.get(Constant
								.buildClass((Type.Array) elementType));
						write_u1(out,ANEWARRAY);
						write_u2(out,idx);
					}
					else {
						write_u1(out,NEWARRAY);		
						
						if(elementType instanceof Type.Bool) {
							write_u1(out, T_BOOLEAN);
						} else if(elementType instanceof Type.Char) {
							write_u1(out, T_CHAR);
						} else if(elementType instanceof Type.Byte) {
							write_u1(out, T_BYTE);
						} else if(elementType instanceof Type.Int) {
							write_u1(out, T_INT);
						} else if(elementType instanceof Type.Short) {
							write_u1(out, T_SHORT);
						} else if(elementType instanceof Type.Long) {
							write_u1(out, T_LONG);
						} else if(elementType instanceof Type.Float) {
							write_u1(out, T_FLOAT);
						} else if(elementType instanceof Type.Double) {
							write_u1(out, T_DOUBLE);
						} else {
							throw new RuntimeException("internal failure constructing " + elementType);
						}
					}
				}									
			} else {
			int idx = constantPool.get(Constant
						.buildClass((Type.Reference) type));
				write_u1(out,NEW);
				write_u2(out,idx);
			}
			return out.toByteArray();
		}
		
		public String toString() {			
			if(type instanceof Type.Array) { 
				Type.Array atype = (Type.Array) type;
				Type elementType = atype.element();
				if(dims > 1) {
					return "multianewarray " + type + ", " + dims;
				} else if(elementType instanceof Type.Reference 
						|| elementType instanceof Type.Array) {				
					return "anewarray " + type; 
				} else {
					return "newarray " + type;
				}
			} else {
				return "new " + type;
			}			
		}
	}
	
	/**
	 * Represents the arraylength bytecode
	 */
	public static final class ArrayLength extends Bytecode {				
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,ARRAYLENGTH);
			return out.toByteArray();
		}
		
		public String toString() { return "arraylength"; }
	}
	
	/**
	 * Represents the check cast bytecode.
	 */
	public static final class CheckCast extends Bytecode {
		public final Type type;
		
		/**
		 * Check a reference on the stack has the given type.
		 * 
		 * @param type --- must be either Type.Array or Type.Reference 
		 */
		public CheckCast(Type type) {
			assert type instanceof Type.Reference || type instanceof Type.Array;			
			this.type = type; 
		}
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();			
			int idx;
			
			if(type instanceof Type.Reference) {
				idx = constantPool.get(Constant
						.buildClass((Type.Reference) type));
			} else if (type instanceof Type.Array) {
				idx = constantPool.get(Constant
						.buildClass((Type.Array) type));
			} else {
				throw new RuntimeException("Unhandled constant type: " + type);
			}
			
			write_u1(out,CHECKCAST);
			write_u2(out,idx);
			
			return out.toByteArray();
		}
		
		public String toString() {						
			return "checkcast " + type;					
		}
	}
	
	/**
	 * Represents the instanceof bytecode
	 */
	public static final class InstanceOf extends Bytecode {
		public final Type type;
		
		public InstanceOf(Type type) { this.type = type; }
		
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();			
			
			int idx;
			
			if(type instanceof Type.Reference) {
				idx = constantPool.get(Constant
						.buildClass((Type.Reference) type));
			} else {
				idx = constantPool.get(Constant
						.buildClass((Type.Array) type));
			}			
			
			write_u1(out,INSTANCEOF);
			write_u2(out,idx);
			
			return out.toByteArray();
		}
		
		public String toString() {						
			return "instanceof " + type;					
		}
	}
	
	/**
	 * Represents the nop bytecode.
	 */
	public static final class Nop extends Bytecode {
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,NOP);		
			return out.toByteArray();
		}
		
		public String toString() {						
			return "nop";					
		}
	}
	
	/**
	 * Represents the athrow bytecode.
	 */
	public static final class Throw extends Bytecode {
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,ATHROW);		
			return out.toByteArray();
		}
		
		public String toString() {						
			return "athrow";					
		}
	}
		
	/**
	 * Represents a monitorenter bytecode
	 */
	public static final class MonitorEnter extends Bytecode {
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,MONITORENTER);		
			return out.toByteArray();
		}
		
		public String toString() {						
			return "monitorenter";					
		}
	}
	
	/**
	 * Represents a monitorexit bytecode
	 */
	public static final class MonitorExit extends Bytecode {
		public byte[] toBytes(int offset, Map<String,Integer> labelOffsets,  
				Map<Constant.Info,Integer> constantPool) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			write_u1(out,MONITOREXIT);		
			return out.toByteArray();
		}
		
		public String toString() {						
			return "monitorexit";					
		}
	}
	// ==============================
	// ======= HELPER METHODS =======
	// ==============================
	
	public static String[] get() {
		String[] map = new String[256];
		// initialise the map using reflection!
		try {
			Class<?> c = Class.forName("jkit.util.Bytecode");
			for(java.lang.reflect.Field f : c.getDeclaredFields()) {
				String name = f.getName();
				if(name.contains("STOP")) {
					
				} else {
					// instruction opcode
					int i = f.getInt(null);
					map[i] = name;
				}				
			}
		} catch(IllegalAccessException e) {
			throw new RuntimeException("Illegal Access Exception");
		} catch(ClassNotFoundException e) {
			throw new RuntimeException("Unable to initialise OpcodeMap");
		}
		return map;
	}
	
	private static char typeChar(Type type) {
		if(type instanceof Type.Reference 
				|| type instanceof Type.Null
				|| type instanceof Type.Array) {
			return 'a';
		} else if(type instanceof Type.Int || type instanceof Type.Byte
				|| type instanceof Type.Char || type instanceof Type.Short
				|| type instanceof Type.Bool || type instanceof Type.Byte) {
			return 'i';
		} else if(type instanceof Type.Long) {
			return 'l';
		} else if(type instanceof Type.Float) {
			return 'f';
		} else if(type instanceof Type.Double) {
			return 'd';
		} else {
			throw new RuntimeException("Unknown type encountered (" + type + ")");
		}
	}
	
	private static int typeOffset(Type type) {
		if(type instanceof Type.Reference 
				|| type instanceof Type.Null 
				|| type instanceof Type.Array
				|| type instanceof Type.Variable) {
			return 4;
		} else if(type instanceof Type.Int || type instanceof Type.Byte
				|| type instanceof Type.Char || type instanceof Type.Short
				|| type instanceof Type.Bool || type instanceof Type.Byte) {
			return 0;
		} else if(type instanceof Type.Long) {
			return 1;
		} else if(type instanceof Type.Float) {
			return 2;
		} else if(type instanceof Type.Double) {
			return 3;
		} else {
			throw new RuntimeException("Unknown type encountered (" + type + ")");
		}
	}

	private static int typeArrayOffset(Type type) {
		if (type instanceof Type.Int) {
			return 0;
		} else if (type instanceof Type.Long) {
			return 1;
		} else if(type instanceof Type.Float) {
			return 2;
		} else if(type instanceof Type.Double) {
			return 3;
		} else if(type instanceof Type.Array) {
			return 4;
		} else if (type instanceof Type.Reference
				|| type instanceof Type.Variable
				|| type instanceof Type.Wildcard) {
			return 4; //same as array
		} else if(type instanceof Type.Byte) {
			return 5;
		} else if(type instanceof Type.Bool) {
			return 5; //same as byte
		} else if(type instanceof Type.Char) {
			return 6;		
		} else if(type instanceof Type.Short) {
			return 7;
		} else {
			throw new RuntimeException("Unknown type in array: " + type);
		}
	}
	
	private static char typeArrayChar(Type type) {
		if(type instanceof Type.Byte) {
			return 'b';
		} else if(type instanceof Type.Char) {
			return 'c';		
		} else if(type instanceof Type.Short) {
			return 's';
		} else {
			return typeChar(type);
		}
	}
	
	private static void write_u1(ByteArrayOutputStream output, int w) {
		output.write(w & 0xFF);
	}
	
	private static void write_u2(ByteArrayOutputStream output, int w) {
		output.write((w >> 8) & 0xFF);
		output.write(w & 0xFF);		
	}
	
	private static void write_u4(ByteArrayOutputStream output, long w) {
		output.write((int) (w >> 24) & 0xFF);
		output.write((int) (w >> 16) & 0xFF);
		output.write((int) (w >> 8) & 0xFF);
		output.write((int) (w & 0xFF));						
	}
	
	@SuppressWarnings("unused")
	private static void write_u8(ByteArrayOutputStream output, long w) {
		write_u4(output, ((w >> 32) & 0xFFFFFFFFL));
		write_u4(output, (w & 0xFFFFFFFFL));
	}
	
	private static void write_i1(ByteArrayOutputStream output, int w) {
		output.write(w);								
	}
	
	private static void write_i2(ByteArrayOutputStream output, int w) {		
		output.write(w >> 8);
		output.write(w);
	}
	
	private static void write_i4(ByteArrayOutputStream output, int w) {
		output.write(w >> 24);
		output.write(w >> 16);
		output.write(w >> 8);
		output.write(w);						
	}
			
    // opcodes

    public static final int NOP = 0; 
    public static final int ACONST_NULL = 1; 
    public static final int ICONST_M1 = 2; 
    public static final int ICONST_0 = 3; 
    public static final int ICONST_1 = 4; 
    public static final int ICONST_2 = 5; 
    public static final int ICONST_3 = 6; 
    public static final int ICONST_4 = 7; 
    public static final int ICONST_5 = 8; 
    public static final int LCONST_0 = 9; 
    public static final int LCONST_1 = 10; 
    public static final int FCONST_0 = 11; 
    public static final int FCONST_1 = 12; 
    public static final int FCONST_2 = 13; 
    public static final int DCONST_0 = 14; 
    public static final int DCONST_1 = 15; 
    public static final int BIPUSH = 16; 
    public static final int SIPUSH = 17; 
    public static final int LDC = 18; 
    public static final int LDC_W = 19; 
    public static final int LDC2_W = 20; 
    public static final int ILOAD = 21; 
    public static final int LLOAD = 22; 
    public static final int FLOAD = 23; 
    public static final int DLOAD = 24; 
    public static final int ALOAD = 25; 
    public static final int ILOAD_0 = 26; 
    public static final int ILOAD_1 = 27; 
    public static final int ILOAD_2 = 28; 
    public static final int ILOAD_3 = 29; 
    public static final int LLOAD_0 = 30; 
    public static final int LLOAD_1 = 31; 
    public static final int LLOAD_2 = 32; 
    public static final int LLOAD_3 = 33; 
    public static final int FLOAD_0 = 34; 
    public static final int FLOAD_1 = 35; 
    public static final int FLOAD_2 = 36; 
    public static final int FLOAD_3 = 37; 
    public static final int DLOAD_0 = 38; 
    public static final int DLOAD_1 = 39; 
    public static final int DLOAD_2 = 40; 
    public static final int DLOAD_3 = 41; 
    public static final int ALOAD_0 = 42; 
    public static final int ALOAD_1 = 43; 
    public static final int ALOAD_2 = 44; 
    public static final int ALOAD_3 = 45; 
    public static final int IALOAD = 46; 
    public static final int LALOAD = 47; 
    public static final int FALOAD = 48; 
    public static final int DALOAD = 49; 
    public static final int AALOAD = 50; 
    public static final int BALOAD = 51; 
    public static final int CALOAD = 52; 
    public static final int SALOAD = 53; 
    public static final int ISTORE = 54; 
    public static final int LSTORE = 55; 
    public static final int FSTORE = 56; 
    public static final int DSTORE = 57; 
    public static final int ASTORE = 58; 
    public static final int ISTORE_0 = 59; 
    public static final int ISTORE_1 = 60; 
    public static final int ISTORE_2 = 61; 
    public static final int ISTORE_3 = 62; 
    public static final int LSTORE_0 = 63; 
    public static final int LSTORE_1 = 64; 
    public static final int LSTORE_2 = 65; 
    public static final int LSTORE_3 = 66; 
    public static final int FSTORE_0 = 67; 
    public static final int FSTORE_1 = 68; 
    public static final int FSTORE_2 = 69; 
    public static final int FSTORE_3 = 70; 
    public static final int DSTORE_0 = 71; 
    public static final int DSTORE_1 = 72; 
    public static final int DSTORE_2 = 73; 
    public static final int DSTORE_3 = 74; 
    public static final int ASTORE_0 = 75; 
    public static final int ASTORE_1 = 76; 
    public static final int ASTORE_2 = 77; 
    public static final int ASTORE_3 = 78; 
    public static final int IASTORE = 79; 
    public static final int LASTORE = 80; 
    public static final int FASTORE = 81; 
    public static final int DASTORE = 82; 
    public static final int AASTORE = 83; 
    public static final int BASTORE = 84; 
    public static final int CASTORE = 85; 
    public static final int SASTORE = 86; 
    public static final int POP = 87; 
    public static final int POP2 = 88; 
    public static final int DUP = 89; 
    public static final int DUP_X1 = 90; 
    public static final int DUP_X2 = 91; 
    public static final int DUP2 = 92; 
    public static final int DUP2_X1 = 93; 
    public static final int DUP2_X2 = 94; 
    public static final int SWAP = 95; 
    public static final int IADD = 96; 
    public static final int LADD = 97; 
    public static final int FADD = 98; 
    public static final int DADD = 99; 
    public static final int ISUB = 100; 
    public static final int LSUB = 101; 
    public static final int FSUB = 102; 
    public static final int DSUB = 103; 
    public static final int IMUL = 104; 
    public static final int LMUL = 105; 
    public static final int FMUL = 106; 
    public static final int DMUL = 107; 
    public static final int IDIV = 108; 
    public static final int LDIV = 109; 
    public static final int FDIV = 110; 
    public static final int DDIV = 111; 
    public static final int IREM = 112; 
    public static final int LREM = 113; 
    public static final int FREM = 114; 
    public static final int DREM = 115; 
    public static final int INEG = 116; 
    public static final int LNEG = 117; 
    public static final int FNEG = 118; 
    public static final int DNEG = 119; 
    public static final int ISHL = 120; 
    public static final int LSHL = 121; 
    public static final int ISHR = 122; 
    public static final int LSHR = 123; 
    public static final int IUSHR = 124; 
    public static final int LUSHR = 125; 
    public static final int IAND = 126; 
    public static final int LAND = 127; 
    public static final int IOR = 128; 
    public static final int LOR = 129; 
    public static final int IXOR = 130; 
    public static final int LXOR = 131; 
    public static final int IINC = 132; 
    public static final int I2L = 133; 
    public static final int I2F = 134; 
    public static final int I2D = 135; 
    public static final int L2I = 136; 
    public static final int L2F = 137; 
    public static final int L2D = 138; 
    public static final int F2I = 139; 
    public static final int F2L = 140; 
    public static final int F2D = 141; 
    public static final int D2I = 142; 
    public static final int D2L = 143; 
    public static final int D2F = 144; 
    public static final int I2B = 145; 
    public static final int I2C = 146; 
    public static final int I2S = 147; 
    public static final int LCMP = 148; 
    public static final int FCMPL = 149; 
    public static final int FCMPG = 150; 
    public static final int DCMPL = 151; 
    public static final int DCMPG = 152; 
    public static final int IFEQ = 153; 
    public static final int IFNE = 154; 
    public static final int IFLT = 155; 
    public static final int IFGE = 156; 
    public static final int IFGT = 157; 
    public static final int IFLE = 158; 
    public static final int IF_ICMPEQ = 159; 
    public static final int IF_ICMPNE = 160; 
    public static final int IF_ICMPLT = 161; 
    public static final int IF_ICMPGE = 162; 
    public static final int IF_ICMPGT = 163; 
    public static final int IF_ICMPLE = 164; 
    public static final int IF_ACMPEQ = 165; 
    public static final int IF_ACMPNE = 166; 
    public static final int GOTO = 167; 
    public static final int JSR = 168; 
    public static final int RET = 169; 
    public static final int TABLESWITCH = 170; 
    public static final int LOOKUPSWITCH = 171; 
    public static final int IRETURN = 172; 
    public static final int LRETURN = 173; 
    public static final int FRETURN = 174; 
    public static final int DRETURN = 175; 
    public static final int ARETURN = 176; 
    public static final int RETURN = 177; 
    public static final int GETSTATIC = 178; 
    public static final int PUTSTATIC = 179; 
    public static final int GETFIELD = 180; 
    public static final int PUTFIELD = 181; 
    public static final int INVOKEVIRTUAL = 182; 
    public static final int INVOKESPECIAL = 183; 
    public static final int INVOKESTATIC = 184; 
    public static final int INVOKEINTERFACE = 185; 
    public static final int UNUSED = 186; 
    public static final int NEW = 187; 
    public static final int NEWARRAY = 188; 
    public static final int ANEWARRAY = 189; 
    public static final int ARRAYLENGTH = 190; 
    public static final int ATHROW = 191; 
    public static final int CHECKCAST = 192; 
    public static final int INSTANCEOF = 193; 
    public static final int MONITORENTER = 194; 
    public static final int MONITOREXIT = 195; 
    public static final int WIDE = 196; 
    public static final int MULTIANEWARRAY = 197; 
    public static final int IFNULL = 198;
    public static final int IFNONNULL = 199; 
    public static final int GOTO_W = 200; 
    public static final int JSR_W = 201; 
    public static final int BREAKPOINT = 202; // reserved
    
    public static final int IMPDEP1 = 254;    // reserved
    public static final int IMPDEP2 = 255;    // reserved
    
    // Array Types
    
    public static final int T_BOOLEAN = 4;
    public static final int T_CHAR = 5;
    public static final int T_FLOAT = 6;
    public static final int T_DOUBLE = 7;
    public static final int T_BYTE = 8;
    public static final int T_SHORT = 9;
    public static final int T_INT = 10;
    public static final int T_LONG = 11;        
}
