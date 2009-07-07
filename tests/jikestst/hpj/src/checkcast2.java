class checkcast2 {
    public static void main(String[] aa) { 
	Truck[] ta;
	int[] ii;
	int result = 0;
	Object xx;
	Vehicle[] vv;
	try {
	    ta = (new Truck[5]);
	    ta[1] = new Truck();
	    ta[2] = new Pickup();
	    ii = (new int[3]);
	    vv = ta;
	    ((Truck[]) vv)[1].maxLoad = 5;
	    xx = ta;
	    ((Vehicle[]) xx)[2].numberpassengers = 6;
	    ((Pickup[]) xx)[1].maxLoad = ((Pickup[]) xx)[1].maxLoad + 10;
	    if (ta[1].maxLoad != 15) { System.out.println(13);
		System.exit(13); }
	} catch(ClassCastException e) {
	    System.out.println("ClassCastException!");
	}
	System.out.println(11);
	System.exit(11); }
    
    public checkcast2() { super(); }
}
