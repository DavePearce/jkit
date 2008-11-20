class Try2 {
    public static void main(String[] args) { int i = 6;
                                             try { i = i + 1; }
                                             finally { try { i = i * 2; }
                                                       finally { i = i + 3; } }
                                             System.out.println(i);
                                             System.exit(i); }
    
    public Try2() { super(); }
}
