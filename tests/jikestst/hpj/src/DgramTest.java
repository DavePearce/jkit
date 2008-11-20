import java.net.*;
import java.lang.*;
import java.io.*;

class DgramTest {
    static Dgram2Thread dgram2_t = null;
    
    static Dgram1Thread dgram1_t = null;
    
    static int dgram2RC = -1;
    
    static int dgram1RC = -1;
    
    public static void main(String[] argv) {
        try { dgram1_t = new Dgram1Thread(null);
              dgram1_t.start();
              dgram2_t = new Dgram2Thread(null);
              dgram2_t.start();
              dgram2_t.join(10000);
              dgram1_t.join(10000);
              if (dgram2RC == 0 && dgram1RC == 0) { System.out.println(0);
                                                    System.exit(0); } else { System.out.println(1);
                                                                             System.exit(1); } }
        catch (Exception e) { System.out.println("Exception in DgramTest" + e);
                              System.out.println(1);
                              System.exit(1); }
        return; }
    
    public DgramTest() { super(); }
}
