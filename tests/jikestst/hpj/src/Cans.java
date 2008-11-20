class Cans implements order {
    int costofitem;
    
    int stock;
    
    Cans(int i, int j) { super();
                         costofitem = i;
                         stock = j; }
    
    public int price() { return costofitem; }
    
    public int quantity() { return stock; }
}
