package whiley.ast.types;

public final class IntType implements Type {
	public String toString() { return "int"; }
	
	public boolean equals(Object o) {
		return o instanceof IntType;
	}
	
	public int hashCode() {
		return 1;
	}
}
