class multchild extends multparent {
    int valuechild;
    
    int valueparent;
    
    int getchild() { return valuechild; }
    
    int getvgrand() { return this.getgrand(); }
    
    public multchild() { super(); }
}
