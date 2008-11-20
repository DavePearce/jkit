class mmult {
    static void mmult1(double[][] a, double[][] b, double[][] c, int L, int M, int N) {
        double t;
        for (int i = 1; i <= L; i++) {
            for (int j = 1; j <= N; j++) { t = 0;
                                           for (int k = 1; k <= M; k++) { t = t + a[i][k] * b[k][j]; }
                                           c[i][j] = t; } }
    }
    
    static void mmult2(double[][] a, double[][] b, double[][] c, int L, int M, int N) {
        int i;
        int j;
        int k;
        i = 1;
        while (i <= L) { j = 1;
                         while (j <= N) { c[i][j] = 0;
                                          k = 1;
                                          while (k <= M) { c[i][j] = c[i][j] + a[i][k] * b[k][j];
                                                           k = k + 1; }
                                          j = j + 1; }
                         i = i + 1; } }
    
    public mmult() { super(); }
}
