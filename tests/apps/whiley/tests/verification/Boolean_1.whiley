int f(int x, int y) requires x>=0 && y>=0, ensures $>0 {
    bool a;
    a = x == y;
    if(a) {
     return 1;
    } else {
     return x + y;
    }
}

int g(int x, int y) requires x>=0 && y>=0, ensures $>0 {
    bool a;
    a = x >= y;
    if(!a) {
     return x + y;
    } else {
     return 1;
    }
}


void main() {
 print 1;
}
