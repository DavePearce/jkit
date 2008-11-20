class Try1 {
    public static void main(String[] args) { int i = 6;
                                             try { i = i + 1; }
                                             finally { i = i * 2; }
                                             System.out.println(i);
                                             System.exit(i); }
    
    public Try1() { super(); }
}
