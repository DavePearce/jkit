class lptry1 {
    public static void main(String[] args) { int i;
                                             int j;
                                             int[] s = new int[7];
                                             try { for (i = 0; s[i] < 10; i++) { j = i + i; } }
                                             catch (Exception e) { System.out.println(0);
                                                                   System.exit(0); }
                                             System.out.println(1);
                                             System.exit(1); }
    
    public lptry1() { super(); }
}
