class CNotFound extends Exception {
    Truck t;
    
    CNotFound(Truck t) { super(" " + t.serial);
                         this.t = t; }
}
