class Food implements cost {
    int costofitem;
    
    Food(int i) { super();
                  costofitem = i + 2; }
    
    public int price() { return costofitem; }
}
