class cont2 {
    public static void main(String[] args) {
        int i;
        int j;
        int s = 0;
        label: for (i = 0; i < 10; i++) { for (j = 0; j < 15; j++) { s = s + i + j;
                                                                     if (s > 20) continue label;
                                                                     s = s + i + j; } }
        System.out.println(s);
        if (s == 70) { System.exit(0); } else { System.exit(1); } }
    
    public cont2() { super(); }
}
