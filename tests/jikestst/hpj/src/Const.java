public final class Const {
    public static void main(String[] argv) { int i = ifunc(12);
                                             System.out.println(i);
                                             System.exit(i); }
    
    static int ifunc(int ii) { return ii + 1; }
    
    public Const() { super(); }
}
