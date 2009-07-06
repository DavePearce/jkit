package whiley.ast.types;

public final class ListType implements Type {
	private Type element;
	
	public ListType(Type element) {
		this.element = element;
	}
	
	public Type element() {
		return element;
	}
	
	public boolean equals(Object o) {
		if(!(o instanceof ListType)) {
			return false;
		}
		ListType at = (ListType) o;
		return at.element.equals(element);
	}
	
	public int hashCode() {
		return element.hashCode() * 123;
	}
	
	public String toString() {
		return element.toString() + "[]";
	}
}
