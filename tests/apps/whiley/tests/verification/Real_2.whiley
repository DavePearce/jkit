real g(real x) requires x <= 0.5, ensures $ <= 0.16 {
  return x / 3;
}

void main() {
 print g(0.234);
}
