import java.io.*;

class DTest {
    public static void main(String[] args) throws IOException { java.util.Date d = new java.util.Date(1000000000000L);
                                                                String s;
                                                                FileOutputStream f = new FileOutputStream("Dates.out");
                                                                PrintStream o = new PrintStream(f);
                                                                s = d.toString();
                                                                o.println(s);
                                                                s = d.toLocaleString();
                                                                o.println(s);
                                                                s = d.toGMTString();
                                                                o.println(s);
                                                                System.out.println(s);
                                                                System.exit(0); }
    
    public DTest() { super(); }
}
