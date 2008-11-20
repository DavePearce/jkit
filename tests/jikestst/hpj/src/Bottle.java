class Bottle implements cost, foreign {
    int costofitem;
    
    String madeIn;
    
    String[] languagesOnLabel;
    
    Bottle(int i, String country, String[] langs) { super();
                                                    costofitem = i;
                                                    madeIn = country;
                                                    languagesOnLabel = langs; }
    
    public int price() { return costofitem; }
    
    public boolean hasEnglishLabel() {
        int i;
        for (i = 0; i < languagesOnLabel.length; i++) { if (languagesOnLabel[i].equals("English")) return true; }
        return false; }
    
    public String getCountryName() { return madeIn; }
}
