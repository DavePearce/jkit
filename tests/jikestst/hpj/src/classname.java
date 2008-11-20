class classname {
    public static void main(String[] args) {
        String s;
        String cln;
        classname x;
        Class cl;
        String[] arr;
        arr = (new String[5]);
        x = new classname();
        cl = x.getClass();
        cln = cl.getName();
        s = "classname";
        int result = 10;
        if (s == cln) result = result + 5; else if (s.equals(cln)) result = result + 0;
        cl = arr.getClass();
        cln = cl.getName();
        s = "[Ljava.lang.String;";
        if (s == cln) { result = result + 50; } else if (!s.equals(cln)) result = result + 30;
        Object ox = givemeone(1);
        if (ox.getClass().isInterface()) result = result + 100;
        ox = givemeone(2);
        if (ox.getClass().isInterface()) result = result + 20;
        System.out.println(result);
        System.exit(result); }
    
    static Object givemeone(int i) { if (i == 1) return new Cans(3, 2); else { Cloneable x = new cost[2];
                                                                               return x; } }
    
    public classname() { super(); }
}
