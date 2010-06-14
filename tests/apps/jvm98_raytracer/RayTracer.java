/*
 * @(#)RayTracer.java	1.14 06/17/98
 *
 * RayTracer.java
 * The RayTracer class is the starting class for the program. It creates the
 * Scene, renders it and draws it to the screen.
 *
 * Lines that are commented out can be uncommented to draw the resulting 
 * scene on the screen.
 *
 * Modified by Don McCauley - IBM 02/18/98 (DWM)
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

/**
 * class RayTracer
 */
public class RayTracer
{
int threadCount = 0;

int width   = 20;
int height  = 30;
String name = "input/time-test.model";
Canvas canvas;

//  private static Frame theWindow;

    /**
     * main
     * @param args
     */

  static int iterations = 2;


  public long inst_main(String[] argv) { 
    run(argv);
    return 0;
    }



    public void run(String args[]) {
    
        int nthreads = 1;

        if( args.length == 4 )
            nthreads = Integer.parseInt(args[3]);
  
        if (args.length >= 3) {
            width  = Integer.parseInt(args[0]);
            height = Integer.parseInt(args[1]);
            name   = args[2];
        }

        canvas = new Canvas(height, height);

        if( nthreads == 0 )
            nthreads = 1;    // *** change to actual # of CPUs


        if( nthreads == 1 ) {

            //new Runner( this, 0, 1 ).run();
            Runner runner = new Runner( this, 0, 1 ); /* DWM */
            runner.start();                           /* DWM */
            try {                                     /* DWM */
               runner.join();                         /* DWM */
            } catch (java.lang.InterruptedException ie) {}  /* DWM */ 
            runner = null;                            /* DWM */                

        } else {
                
            Runner[] runners = new Runner[nthreads];

            for( int i = 0 ; i < nthreads ; i++ ) {
                 runners[i] = new Runner( this, i, nthreads );
                 threadCount++;
                 runners[i].start();
            }


            for( int i = 0 ; i < nthreads ; i++ ) {  
                try {
                    runners[i].join();
                } catch( InterruptedException x ) {}
                runners[i] = null;                    /* DWM */
            }
          
        }
	/*
        if (Context.getVerify()) {
            canvas.WriteDiag();
        }
	*/
  
    }

}

class Runner extends Thread {

    RayTracer parent;
    int section;
    int nsections;

    public Runner( RayTracer parent, int section, int nsections ) {
        this.parent    = parent;
        this.section   = section;
        this.nsections = nsections;        
    }
        
    public void run() {
        new Scene(parent.name).RenderScene(parent.canvas, parent.width, section, nsections);
        parent.threadCount--;
    }
    
}

