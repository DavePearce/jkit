interface Inner {
}

public class JLS_15_12_AnonymousClasses_13 {
    public static void main(String[] args) {	
	new Inner() {
	    {
		System.out.println("INNER INTERFACE");
	    }
	};
    }
}
