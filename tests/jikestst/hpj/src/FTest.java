import java.io.*;

class FTest {
    public static void main(String[] args) throws IOException {
        File f = new File("FTest.out");
        File f1 = new File("FTest1.out");
        FileOutputStream fos = new FileOutputStream("FTest.out");
        PrintStream ps = new PrintStream(fos);
        ps.println("abcdefg");
        ps.close();
        if (!f.exists()) { System.out.println(1);
                           System.exit(1); }
        if (!f.canWrite()) { System.out.println(2);
                             System.exit(2); }
        if (!f.canRead()) { System.out.println(3);
                            System.exit(3); }
        if (!f.isFile()) { System.out.println(4);
                           System.exit(4); }
        if (f.isDirectory()) { System.out.println(5);
                               System.exit(5); }
        if (f.length() != 8) { System.out.println(6);
                               System.exit(6); }
        if (f.isAbsolute()) { System.out.println(7);
                              System.exit(7); }
        if (!f.renameTo(f1)) { System.out.println(8);
                               System.exit(8); }
        if (!f1.delete()) { System.out.println(9); }
        if (f.exists()) { System.out.println(10);
                          System.exit(10); }
        System.out.println(0);
        System.exit(0); }
    
    public FTest() { super(); }
}
