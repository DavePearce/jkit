class Trychk1 {
    public static void main(String[] args) { int i = 6;
                                             int x = 1;
                                             for (int j = 0; j < 2; j++) { try { i = i + 1;
                                                                                 i = other(i); }
                                                                           catch (RuntimeException e) { x = x + 5; }
                                                                           catch (Exception e) { x = x + 10;
                                                                                                 i = 2; } }
                                             System.out.println(i + x);
                                             System.exit(i + x); }
    
    static int other(int i) throws Exception { int[] a;
                                               a = (new int[3]);
                                               if (i > 5) throw new Exception("stuff");
                                               a[i] = 5;
                                               return a[i]; }
    
    public Trychk1() { super(); }
}
