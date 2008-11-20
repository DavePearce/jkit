class ques {
    int x;
    
    int multy_g(int w, int y, int z) { x = z + (w > y ? w : y);
                                       return x; }
    
    int multy_l(int w, int y, int z) { x = z + (w < y ? w : y);
                                       return x; }
    
    int multy_e(int w, int y, int z) { x = z + (w == y ? z : y);
                                       return x; }
    
    int multy_n(int w, int y, int z) { x = z + (w != y ? z : y);
                                       return x; }
    
    int multy_le(int w, int y, int z) { x = z + (w <= y ? w : y);
                                        return x; }
    
    int multy_ge(int w, int y, int z) { x = z + (w >= y ? w : y);
                                        return x; }
    
    public ques() { super(); }
}
