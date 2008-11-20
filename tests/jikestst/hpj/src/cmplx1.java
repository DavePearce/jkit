class cmplx1 {
    public static void main(String[] args) { int i;
                                             int s;
                                             int j = "abc".length();
                                             s = 0;
                                             for (i = 0; i < 10 && j > 2; i++) { s = s + j; }
                                             System.out.println(s);
                                             if (s == 30) { System.exit(0); } else { System.exit(1); } }
    
    public cmplx1() { super(); }
}
