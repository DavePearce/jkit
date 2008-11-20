import java.io.*;

class RAFTest {
    public static void main(String[] args) throws IOException {
        String s;
        RandomAccessFile raf = new RandomAccessFile("RAFTest.out", "rw");
        raf.writeBytes("dlroW olleH");
        if (raf.getFilePointer() != 11) { System.out.println(1);
                                          System.exit(1); }
        raf.writeBytes("Hello World");
        if (raf.length() != 22) { System.out.println(2);
                                  System.exit(2); }
        raf.seek(0);
        s = raf.readLine();
        if (!s.equals("dlroW olleHHello World")) { System.out.println(3);
                                                   System.exit(3); }
        raf.close();
        File f = new File("RAFTest.out");
        if (!f.delete()) { System.out.println(6);
                           System.exit(6); }
        System.out.println(0);
        System.exit(0); }
    
    public RAFTest() { super(); }
}
