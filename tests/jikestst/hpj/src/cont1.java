class cont1 {
    public static void main(String[] args) { int i;
                                             int s = 0;
                                             label: for (i = 0; i < 10; i++) { s = s + i;
                                                                               if (s > 20) continue label;
                                                                               s = s + i; }
                                             System.out.println(s);
                                             if (s == 55) { System.exit(0); } else { System.exit(1); } }
    
    public cont1() { super(); }
}
