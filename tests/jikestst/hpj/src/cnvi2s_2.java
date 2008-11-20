class cnvi2s_2 {
    public static void main(String[] args) { int i;
                                             short s;
                                             i = -32768;
                                             s = (short) i;
                                             System.out.println(2 + (byte) (s >> 15));
                                             System.exit(2 + (byte) (s >> 15)); }
    
    public cnvi2s_2() { super(); }
}
