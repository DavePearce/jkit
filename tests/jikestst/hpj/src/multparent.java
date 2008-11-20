class multparent extends multgrand {
    int valueparent;
    
    int getparent() { return valueparent; }
    
    void setparent() { valueparent = 99; }
    
    int getgrand() { return valuegrand + 1; }
    
    public multparent() { super(); }
}
