int f() ensures $ >= 10 {
 int i;
 i = 0;

 while(i < 10) { 
  print i;
  i = i + 1;
 }

 return i;
}

void main() {
 print f();
}
