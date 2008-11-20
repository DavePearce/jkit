import java.io.*;
import java.util.*;
import java.lang.*;

interface I1 {
    public abstract void update();
}

class IVehicle implements I1 {
    int wheelcount;
    
    public void update() { wheelcount++; }
    
    public IVehicle() { super(); }
}

interface I3 {
    public abstract int sum();
}

interface I2 extends I3 {
    public abstract int wheels();
    
    public abstract int setsum(int trip, int load);
}

class ITruck extends IVehicle implements I2 {
    int maxload;
    
    int tripcount;
    
    int width;
    
    public int sum() { return maxload + tripcount; }
    
    public int wheels() { return wheelcount; }
    
    public void update() { super.update();
                           wheelcount = wheelcount + 3; }
    
    public int setsum(int trip, int load) { tripcount = trip;
                                            maxload = load;
                                            return tripcount + maxload; }
    
    public ITruck() { super(); }
}

interface I5 {
    public abstract void addWidth(int x);
}

interface I4 extends I5 {
    public abstract int getWidth();
}

class IWideTruck extends ITruck implements I4 {
    public int getWidth() { return width; }
    
    public void addWidth(int x) { width = width + x; }
    
    public IWideTruck() { super(); }
}

class implement {
    public static void main(String[] args) { I1 i1;
                                             I2 i2;
                                             I3 i3;
                                             I4 i4;
                                             I5 i5;
                                             ITruck t;
                                             int answer;
                                             int i;
                                             t = new ITruck();
                                             i1 = (ITruck) t;
                                             i1.update();
                                             i2 = new ITruck();
                                             i3 = new ITruck();
                                             i = ((I2) i3).setsum(3, 6);
                                             ((I1) i3).update();
                                             answer = i3.sum() + t.wheels();
                                             i = ((I2) i1).setsum(2, 5);
                                             answer = answer + ((I2) t).sum();
                                             t = new IWideTruck();
                                             t.width = 5;
                                             i4 = (IWideTruck) t;
                                             i5 = (I5) t;
                                             if (t instanceof I5) answer = answer + 1;
                                             i5.addWidth(4);
                                             i4.addWidth(15);
                                             answer = answer + i4.getWidth();
                                             i3 = (I3) i5;
                                             answer = answer + i3.sum() + t.setsum(3, 6) + i3.sum();
                                             System.out.println(answer);
                                             System.exit(answer); }
    
    public implement() { super(); }
}
