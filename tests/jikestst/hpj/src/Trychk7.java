class Trychk7 {
    public static void main(String[] args) {
        int i = 6;
        int x = 1;
        for (int j = 0; j < 2; j++) { try { i = i + 1;
                                            i = other(i); }
                                      catch (ArrayIndexOutOfBoundsException e) { i = i + 1; }
                                      catch (Exception e) { x = x + 10;
                                                            i = i + 1; } }
        System.out.println(i + x);
        System.exit(i + x); }
    
    static int other(int i) throws Exception { int[] a;
                                               try { if (i == 7) { i = otherother(i); } }
                                               catch (java.io.IOException e) { i = 2; }
                                               return i; }
    
    static int otherother(int i) throws Exception {
        int[] a;
        a = (new int[4]);
        try { a[i] = 5;
              if (i == 7) throw new java.io.IOException("wrong value of i"); }
        catch (java.io.IOException e) { System.out.println("in catch of otherother"); }
        return i - 2; }
    
    public Trychk7() { super(); }
}
