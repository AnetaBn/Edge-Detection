package edgedetection;

/**
 * Class handles image splicing operation with mask
 */

public class Mix {

    /**
     * The method takes an image in grayscale, determines the kernel and position,
     * and applies the splice at the selected position
     * 
     * @param input
     * @param x
     * @param y
     * @param k
     * @param kernelHeight
     * @param kernelWidth
     * @return output
     */

    public static double pixelMix(double[][] input, int x, int y, double[][] k, int kernelWidth, int kernelHeight) {
        double output = 0;
        for (int i = 0; i < kernelWidth; ++i) {
            for (int j = 0; j < kernelHeight; ++j) {
                output = output + (input[x + i][y + j] * k[i][j]);
            }
        }
        return output;
    }

    /**
     * The method takes a 2D array of gray levels and a kernel and applies a splice
     * over the image area defined by width and height
     * 
     * @param input
     * @param width
     * @param height
     * @param kernel
     * @param kernelHeight
     * @param kernelWidth
     * @return
     */

    public static double[][] mix2D(double[][] input, int width, int height, double[][] kernel, int kernelWidth,
            int kernelHeight) {
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        double[][] output = new double[smallWidth][smallHeight];
        for (int i = 0; i < smallWidth; ++i) {
            for (int j = 0; j < smallHeight; ++j) {
                output[i][j] = 0;
            }
        }
        for (int i = 0; i < smallWidth; ++i) {
            for (int j = 0; j < smallHeight; ++j) {
                output[i][j] = pixelMix(input, i, j, kernel, kernelWidth, kernelHeight);
            }
        }
        return output;
    }

    /**
     * The method takes a 2D array of gray levels and a kernel and applies a splice
     * over the image area
     * defined by width and height and returns a portion of the output image.
     * 
     * @param input
     * @param width
     * @param height
     * @param kernel
     * @param kernelHeight
     * @param kernelWidth
     * @return
     */

    public static double[][] mix2DEdge(double[][] input, int width, int height, double[][] kernel, int kernelWidth,
            int kernelHeight) {
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        int top = kernelHeight / 2;
        int left = kernelWidth / 2;

        double[][] small = mix2D(input, width, height, kernel, kernelWidth, kernelHeight);
        double large[][] = new double[width][height];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                large[i][j] = 0;
            }
        }
        for (int j = 0; j < smallHeight; ++j) {
            for (int i = 0; i < smallWidth; ++i) {
                large[i + left][j + top] = small[i][j];
            }
        }
        return large;
    }

    /**
     * The method applies mix2DEdge to the input array
     * 
     * @param input
     * @param width
     * @param height
     * @param kernel
     * @param kernelHeight
     * @param kernelWidth
     * @return
     */

    public double[][] mixNext(double[][] input, int width, int height, double[][] kernel, int kernelWidth,
            int kernelHeight) {
        double[][] newInput = input.clone();
        double[][] output = input.clone();

        for (int i = 0; i < 1; ++i) {
            output = mix2DEdge(newInput, width, height, kernel, kernelWidth, kernelHeight);
            newInput = output.clone();
        }
        return output;
    }
}