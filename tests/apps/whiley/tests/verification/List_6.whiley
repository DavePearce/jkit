void f(int[] x, int i) requires i < |x| && i >= 0 {
 int y;
 int z;
 y = x[i];
 z = x[i];
 assert y == z;
}

void main() {
 int[] arr;
 
 arr = [1,2,3];

 f(arr,2);

 print arr[0];
}
