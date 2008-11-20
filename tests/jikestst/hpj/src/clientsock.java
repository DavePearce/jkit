import java.net.*;
import java.lang.*;
import java.io.*;

class clientsock {
    public static void main(String[] argv) {
        try { InetAddress server_ia = InetAddress.getByName(null);
              Socket s = new Socket(server_ia, 6655);
              if (s == null) { System.out.println("socket returned null");
                               System.exit(1); }
              int localPort = s.getLocalPort();
              int remotePort = s.getPort();
              System.out.println("Client: have Socket: localPort=" + localPort + " remotePort=" + remotePort);
              InetAddress ia = s.getInetAddress();
              System.out.println("Client: connected to InetAddress = " + ia);
              OutputStream os = s.getOutputStream();
              PrintStream ps = new PrintStream(os);
              ps.println("Hello World");
              ps.println("Disney World");
              ps.println("Web Wide World");
              ps.println("Goodbye Cruel World");
              ps.close();
              InputStream is = s.getInputStream(); }
        catch (Exception e) { System.out.println("Exception in clientsock" + e); }
        System.exit(0); }
    
    public clientsock() { super(); }
}
