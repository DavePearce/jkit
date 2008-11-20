class Trychk6 {
    public static void main(String[] args) { int i = 1;
                                             int x = 1;
                                             int k;
                                             k = x + i;
                                             try { i = i + 1;
                                                   i = other(i);
                                                   if (i == 5) throw new Exception("stuff"); }
                                             catch (RuntimeException e) { x = x + 5; }
                                             catch (Exception e) { x = x + i + 10; }
                                             finally { i = i + 1; }
                                             if (x != 16) i = i + 5;
                                             if (k != 2) i = i + 3;
                                             System.out.println(i);
                                             System.exit(i); }
    
    static int other(int i) throws Exception { int[] a;
                                               a = (new int[3]);
                                               if (i > 5) throw new Exception("stuff");
                                               a[i] = 5;
                                               return a[i]; }
    
    public Trychk6() { super(); }
}
