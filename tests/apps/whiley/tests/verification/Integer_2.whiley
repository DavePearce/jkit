int f() ensures 2*$==1 {
 return 1;
}

void main() {
 print f();
}
