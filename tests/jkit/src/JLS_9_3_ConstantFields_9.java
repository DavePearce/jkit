public class JLS_9_3_ConstantFields_9 {
    public static final int constant = 123;

    public static final byte[] arr1 = {
	1,2,3,4,5,constant
    };

    public static void main(String[] args) {
	byte[] arr2 = {
	    constant-1,12,22,28
	};

	for(byte b : arr1) {
	    System.out.println(b);
	}

	for(byte b : arr2) {
	    System.out.println(b);
	}
    };
}
