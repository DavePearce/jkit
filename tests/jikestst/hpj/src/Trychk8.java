import java.io.*;

class Trychk8 {
    public static void main(String[] args) throws Exception { foo();
                                                              System.out.println(0);
                                                              System.exit(0); }
    
    static void foo() throws Exception { try { bar(); }
                                         catch (Exception e) { System.out.println(0);
                                                               System.exit(0); }
                                         System.out.println(0);
                                         System.exit(0); }
    
    static void bar() throws Exception { try { try { throw new Exception(); }
                                               catch (IOException ie) { System.out.println(0);
                                                                        System.exit(0); } }
                                         catch (Exception e) { System.out.println(1);
                                                               System.exit(1); } }
    
    public Trychk8() { super(); }
}
