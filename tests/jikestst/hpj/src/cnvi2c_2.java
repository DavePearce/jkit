class cnvi2c_2 {
    public static void main(String[] args) { int i;
                                             char c;
                                             i = -32768;
                                             c = (char) i;
                                             System.out.println((byte) (c >> 15));
                                             System.exit((byte) (c >> 15)); }
    
    public cnvi2c_2() { super(); }
}
