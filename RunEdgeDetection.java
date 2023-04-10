package edgedetection;

import java.io.IOException;

/**
 * A class that calls the program using the EdgeDetectionUI class
 */

public class RunEdgeDetection {

    /**
     * Example of using the EdgeDetectionUI class
     * 
     * @param args
     * @exception IOException
     */

    public static void main(String[] args) throws IOException {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        new EdgeDetectionUI();
    }
}
