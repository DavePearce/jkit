void main() {
 int[][] a1;
 int[][] a2;
 
 a1 = [[1,2,3],[0]];
 a2 = a1;
 a2[0] = [3,4,5];

 print a1[0];
 print a1[1];
 print a2[0];
 print a2[1];
}
