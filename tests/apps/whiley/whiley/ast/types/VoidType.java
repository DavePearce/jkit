package whiley.ast.types;

public final class VoidType implements Type {
	public String toString() { return "void"; }
	
	public boolean equals(Object o) {
		return o instanceof VoidType;
	}
	
	public int hashCode() {
		return 0;
	}
}
