real g(int x) {
  return x / 3.0;
}

int f(int x, int y) requires x>=0 && y>0 {
 print g(x);
}

void main() {
 f(1,2);
}
