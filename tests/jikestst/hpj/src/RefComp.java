import java.io.*;
import java.util.*;
import java.lang.*;

class RefComp {
    public static void main(String[] argv) { int[] a = null;
                                             int[] b = null;
                                             int accum = 0;
                                             for (int i = 0; a == null; i++) { if (a == null) ; else accum++;
                                                                               if (b != null) accum += 2;
                                                                               a = (new int[1]);
                                                                               if (a == null) accum += 4;
                                                                               b = a;
                                                                               if (a == b) ; else accum += 8;
                                                                               if (b != a) accum += 16;
                                                                               if (i > 2) { accum = accum * 2;
                                                                                            break; } }
                                             System.out.println(accum);
                                             System.exit(accum); }
    
    public RefComp() { super(); }
}
