package whiley.ast.types;

public final class BoolType implements Type {
	public String toString() { return "bool"; }
	
	public boolean equals(Object o) {
		return o instanceof BoolType;
	}
	
	public int hashCode() {
		return 2;
	}
}
