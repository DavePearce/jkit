class for2 {
    public static void main(String[] args) { int i;
                                             int j;
                                             int s;
                                             s = 0;
                                             for (i = 0; i < 10; i++) { for (j = 0; j < 15; j++) { s = s + i + j; } }
                                             System.out.println(s);
                                             if (s == 1725) { System.exit(0); } else { System.exit(1); } }
    
    public for2() { super(); }
}
