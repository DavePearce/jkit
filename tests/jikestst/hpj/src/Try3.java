class Try3 {
    public static void main(String[] args) { int i;
                                             int j;
                                             j = 0;
                                             for (i = 0; i < 5; i++) { try { j = j + 1; }
                                                                       finally { j = j * 2;
                                                                                 if (i > 1) { break; } } }
                                             System.out.println(j);
                                             System.exit(j); }
    
    public Try3() { super(); }
}
