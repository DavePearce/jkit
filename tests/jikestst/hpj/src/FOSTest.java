import java.io.*;

class FOSTest {
    public static void main(String[] args) throws IOException {
        FileOutputStream fos = new FileOutputStream("FOSTest.out");
        PrintStream ps = new PrintStream(fos);
        ps.println("Hello World");
        ps.close();
        System.out.println(0);
        System.exit(0); }
    
    public FOSTest() { super(); }
}
