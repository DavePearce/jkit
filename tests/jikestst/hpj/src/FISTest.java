import java.io.*;

class FISTest {
    public static void main(String[] args) throws IOException {
        String s;
        FileOutputStream fos = new FileOutputStream("FISTest.out");
        PrintStream ps = new PrintStream(fos);
        ps.println("Hello World");
        ps.println("Disney World");
        ps.println("Web Wide World");
        ps.println("Goodbye Cruel World");
        ps.close();
        FileInputStream fis = new FileInputStream("FISTest.out");
        DataInputStream dis = new DataInputStream(fis);
        s = dis.readLine();
        if (!s.equals("Hello World")) { System.out.println(1);
                                        System.exit(1); }
        if (fis.available() != 48) { System.out.println(2);
                                     System.exit(2); }
        s = dis.readLine();
        if (!s.equals("Disney World")) { System.out.println(3);
                                         System.exit(3); }
        if (fis.skip(15) != 15) { System.out.println(4);
                                  System.exit(4); }
        s = dis.readLine();
        if (!s.equals("Goodbye Cruel World")) { System.out.println(5);
                                                System.exit(5); }
        fis.close();
        File f = new File("FISTest.out");
        if (!f.delete()) { System.out.println(1);
                           System.exit(1); }
        System.out.println(0);
        System.exit(0); }
    
    public FISTest() { super(); }
}
