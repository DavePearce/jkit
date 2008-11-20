class Simpa {
    int[] array1;
    
    byte[] array2;
    
    int start = 5;
    
    Simpa(int dim, int modifier) {
        super();
        array1 = (new int[dim]);
        array2 = (new byte[dim + 3]);
        for (int i = 0; i < dim; i++) { array1[i] = start - modifier + i; }
        for (int i = 0; i < array2.length; i++) { array2[i] = (byte) (start + modifier + i); } }
    
    int sumup() { int sum = 0;
                  for (int i = 0; i < array1.length; i++) { sum = sum + array1[i]; }
                  for (int i = 0; i < array2.length; i++) { sum = sum + array2[i]; }
                  return sum; }
}
