void f(int[] x) {
 int z;
 z = |x|;
 print z;
 print x[z-1];
}

void main() {
 int[] arr;
 
 arr = [1,2,3];

 f(arr);
}
