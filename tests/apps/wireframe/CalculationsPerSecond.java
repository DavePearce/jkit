/**
	 * Class used to calculate the number of calculations per second.
	 */
	public class CalculationsPerSecond {
	    private float currentFPS;
	    private long lastFrameTime;
	    private float fpsCounter;
	    private int framesDrawn;
	    private float timeDelta;
	 
	    /**
	     * 1.0f.
	     */
	    public static final float ONE_F = 1.0f;
	 
	    /**
	     * A nano second.
	     */
	    public static final float NANO = 1000000000.0f;
	 
	    /**
	     * Constructor. Sets some default values.
	     */
	    public CalculationsPerSecond() {
	        reset();
	    }
	    
	    /**
	     * Resets values.
	     */
	    public void reset() {
	        lastFrameTime = System.nanoTime();
	        fpsCounter = 0;
	        framesDrawn = 0;
	    }
	 
	    /**
	     * The start of the calculation.
	     */
	    public void startTiming() {
	        long frameTime = System.nanoTime();
	        long elapsed = frameTime - lastFrameTime;
	        lastFrameTime = frameTime;
	        timeDelta = (elapsed / NANO);
	    }
	 
	    /**
	     * The end of the calculation.
	     * @return The calculations per second. 0 if < 1 fps.
	     */
	    public float endTiming() {
	        framesDrawn++;
	 
	        fpsCounter += timeDelta;
	        if (fpsCounter > ONE_F) {
	            currentFPS = framesDrawn / fpsCounter;
	            framesDrawn = 0;
	            fpsCounter -= ONE_F;
	        }
	        return currentFPS;
	    }    
	}