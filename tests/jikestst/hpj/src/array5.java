class array5 {
    public static void main(String[] args) { 
	int[] a;
	int i;
	int j;
	try {
	    a = (new int[5]);
	    i = 4;
	    a[i] = 3;
	    j = a[i];
	    i = -21767345;
	    a[i] = 9;
	    System.out.println(5);
	    System.exit(5); 
	} catch(ArrayIndexOutOfBoundsException e) {
	    System.out.println("ArrayIndexOutOfBoundsException: " + e.getMessage());
	}
    }
    
    public array5() { super(); }
}
