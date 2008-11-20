import java.lang.*;

class AbsClassMain {
    public static void main(String[] args) { AbsClass0 x = new AbsClass1();
                                             int i = x.foo(5);
                                             System.out.println(i);
                                             System.exit(i); }
    
    public AbsClassMain() { super(); }
}
