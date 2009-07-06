int[] f() {
 return [1,2];
}

void main() {
 int[] a1;
 int[] a2;
 
 a1 = f();
 a2 = f();
 a2[0] = 0;

 print a1[0];
 print a1[1];
 print a2[0];
 print a2[1];
}
