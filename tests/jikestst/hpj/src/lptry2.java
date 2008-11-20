class lptry2 {
    public static void main(String[] args) { int i;
                                             int j;
                                             int[] s = new int[7];
                                             for (i = 0; i < 10; i++) { try { s[i] = i; }
                                                                        catch (Exception e) { System.out.println(0);
                                                                                              System.exit(0); } }
                                             System.exit(1);
                                             System.out.println(1); }
    
    public lptry2() { super(); }
}
