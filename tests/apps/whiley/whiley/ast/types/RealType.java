package whiley.ast.types;

public final class RealType implements Type {
	public String toString() { return "real"; }
	
	public boolean equals(Object o) {
		return o instanceof RealType;
	}
	
	public int hashCode() {
		return 1;
	}
}
