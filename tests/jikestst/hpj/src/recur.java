import java.io.*;

class recur {
    static int i = 1;
    
    static PrintStream pp;
    
    public static void main(String[] argv) throws IOException {
        if (i == 1) pp = new PrintStream(new FileOutputStream("recur.out"));
        if (i == 1) fred(argv);
        pp.println("In main"); }
    
    static void fred(String[] argv) throws IOException { pp.println("In fred");
                                                         i = i + 1;
                                                         main(argv); }
    
    public recur() { super(); }
}
