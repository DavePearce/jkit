public class JLS_XXX_BypassMethod_4 {
    public static interface Inter {
	public Object f();
    }

    public static class Child implements Inter {
	public String f() {
	    return "Hello World";
	}
    }

    public static void main(String[] args) {
	JLS_XXX_BypassMethod_4.Child tc = new JLS_XXX_BypassMethod_4.Child();
	JLS_XXX_BypassMethod_4.Inter ti = tc;
	System.out.println(tc.f());
	System.out.println(ti.f());	    
    }
}
