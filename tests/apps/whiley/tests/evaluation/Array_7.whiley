void f(int[] a) {
 a[0] = 5;
 print a;
}

void main() {
 int[] b;
 b = [1,2,3];     
 print b;
 f(b);
 print b;
}
