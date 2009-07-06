package whiley.ast.types;

public class AnyType implements Type {
	public String toString() { return "?"; }
	
	public boolean equals(Object o) {
		return o instanceof AnyType;
	}
	
	public int hashCode() {
		return 3;
	}
}
