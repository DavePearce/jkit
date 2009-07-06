void main() {
 int i;
 i = 0;
 while(i < 10) invariant i < 10 {
  print i;
  i = i + 1;
 }

 i = 0;
 while(i < 10) {
  print i;
  i = i + 1;
 }
}
