package edgedetection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;

/**
 * Class for detect edges by Canny's algorithm
 */
public class Canny {
    private static final double[][] xGradientKernel = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };
    private static final double[][] yGradientKernel = { { 1, 2, 1 }, { 0, 0, 0 }, { -1, -2, -1 } };
    public static final double[][] gaussianKernel = { { 2. / 159, 4. / 159, 5. / 159, 4. / 159, 2. / 159 },
            { 4. / 159, 9. / 159, 12. / 159, 9. / 159, 4. / 159 },
            { 5. / 159, 12. / 159, 15. / 159, 12. / 159, 5. / 159 },
            { 4. / 159, 9. / 159, 12. / 159, 9. / 159, 4. / 159 },
            { 2. / 159, 4. / 159, 5. / 159, 4. / 159, 2. / 159 } };
    private double lowerThreshold;
    private double higherThreshold;

    /**
     * A method to set the higher and lower threshold values of the Canny class
     * 
     * @param lowerThresholdValue
     * @param higherThresholdValue
     */
    public Canny(double lowerThresholdValue, double higherThresholdValue) {
        this.lowerThreshold = lowerThresholdValue;
        this.higherThreshold = higherThresholdValue;
    }

    /**
     * The main method which is a sequence of consecutive steps of the algorithm
     * 
     * @param sourceImage
     * @return
     * @throws IOException
     */
    public File detectEdges(BufferedImage sourceImage) throws IOException {
        double[][][] pixelArray = convertToArray(sourceImage);
        double[][] grayscaleArray = convertToGrayscale(pixelArray);
        double[][] denoisedArray = applyKernel(grayscaleArray, gaussianKernel);
        double[][] xGradient = applyKernel(denoisedArray, xGradientKernel);
        double[][] yGradient = applyKernel(denoisedArray, yGradientKernel);
        double[][] magnitude = computeMagnitude(xGradient, yGradient);
        int[][] direction = computeDirection(xGradient, yGradient);
        double[][] suppressedMagnitude = nonMaximumSuppression(direction, magnitude);
        double[][] thresholdFlags = setStrengthFlag(suppressedMagnitude);
        double[][] connected = checkWeakPixelConnection(thresholdFlags, suppressedMagnitude);
        return createImageFromMatrix(connected);
    }

    /**
     * A method to convert an image into a three-channel array containing pixel
     * values
     * 
     * @param image
     * @return
     */
    private double[][][] convertToArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        double[][][] pixelArray = new double[width][height][3];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color color = new Color(image.getRGB(i, j));
                pixelArray[i][j][0] = color.getRed();
                pixelArray[i][j][1] = color.getGreen();
                pixelArray[i][j][2] = color.getBlue();
            }
        }
        return pixelArray;
    }

    /**
     * A method to convert an image to grayscale
     * 
     * @param pixelArray
     * @return
     */
    private double[][] convertToGrayscale(double[][][] pixelArray) {
        int width = pixelArray.length;
        int height = pixelArray[0].length;
        double[][] grayscaleArray = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double gray = (pixelArray[i][j][0] + pixelArray[i][j][1] + pixelArray[i][j][2]) / 3;
                grayscaleArray[i][j] = gray;
            }
        }
        return grayscaleArray;
    }

    /**
     * A method to create a matrix filled with zeros
     * 
     * @param width
     * @param height
     * @return
     */
    private double[][] createZerosArray(int width, int height) {
        double[][] array = new double[width][height];
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                array[i][j] = 0;
            }
        }
        return array;
    }

    /**
     * A method to create a border using a mirror image of the image's border pixels
     * 
     * @param smallArray
     * @param kernel
     * @return
     */
    private double[][] createPaddedArray(double[][] smallArray, double[][] kernel) {
        int smallArrayWidth = smallArray.length;
        int smallArrayHeight = smallArray[0].length;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int gapWidth = (int) ((kernelWidth) / 2.0);
        int gapHeight = (int) ((kernelHeight) / 2.0);
        int width = smallArrayWidth + 2 * gapWidth;
        int height = smallArrayHeight + 2 * gapHeight;

        double[][] paddedArray = new double[width][height];
        for (int i = 0; i < gapWidth; ++i) {
            for (int j = 0; j < gapHeight; ++j) {
                // top left corner
                paddedArray[i][j] = smallArray[gapWidth - 1 - i][gapHeight - 1 - j];
                // top right corner
                paddedArray[width - i - 1][j] = smallArray[(smallArrayWidth) - gapWidth + i][gapHeight - 1 - j];
                // bottom left corner
                paddedArray[i][height - j - 1] = smallArray[gapWidth - i - 1][(smallArrayHeight) - gapHeight + i - 1];
                // bottom right corner
                paddedArray[width - i - 1][height - j
                        - 1] = smallArray[(smallArrayWidth) - gapWidth + i][(smallArrayHeight) - gapHeight + i];
            }
        }
        // top row
        for (int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = 0; j < gapHeight; ++j) {
                paddedArray[i][j] = smallArray[i - gapWidth][gapHeight - 1 - j];
            }
        }
        // left row
        for (int i = 0; i < gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[i][j] = smallArray[gapWidth - 1 - i][j - gapHeight];
            }
        }
        // right row
        for (int i = 0; i < gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[width - 1 - i][j] = smallArray[smallArrayWidth - gapWidth - 1 + i][j - gapHeight];
            }
        }
        // bottom row
        for (int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = 0; j < gapHeight; ++j) {
                paddedArray[i][height - 1 - j] = smallArray[i - gapWidth][smallArrayHeight - gapHeight - 1 + j];
            }
        }
        // inside
        for (int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[i][j] = smallArray[i - gapWidth][j - gapHeight];
            }
        }
        return paddedArray;
    }

    /**
     * Method to create padding and apply convolution for the entire image
     * 
     * @param pixelArray
     * @param kernel
     * @return
     */
    private double[][] applyKernel(double[][] pixelArray, double[][] kernel) {
        double[][] biggerPixelArray = createPaddedArray(pixelArray, kernel);
        return applyConvolution(biggerPixelArray, kernel);
    }

    /**
     * Method to apply convolution to the entire image
     * 
     * @param input
     * @param kernel
     * @return
     */
    private double[][] applyConvolution(double[][] input, double[][] kernel) {
        int width = input.length;
        int height = input[0].length;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int gapWidth = (int) ((kernelWidth) / 2.0);
        int gapHeight = (int) ((kernelHeight) / 2.0);
        double[][] output = new double[width - 2 * gapWidth][height - 2 * gapHeight];
        for (int i = gapWidth; i < width - gapWidth; ++i) {
            for (int j = gapHeight; j < height - gapHeight; ++j) {
                output[i - gapWidth][j - gapHeight] = returnConvValue(input, i, j, kernel);
            }
        }
        return output;
    }

    /**
     * A method to calculate the value of a specific pixel value,
     * being the result of splicing an image with a mask.
     * 
     * @param input
     * @param x
     * @param y
     * @param kernel
     * @return
     */
    private double returnConvValue(double[][] input, int x, int y, double[][] kernel) {
        double output = 0;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int gapWidth = (int) ((kernelWidth) / 2.0);
        int gapHeight = (int) ((kernelHeight) / 2.0);
        for (int i = 0; i < kernel.length; ++i) {
            for (int j = 0; j < kernel[0].length; ++j) {
                output = output + (input[x + i - gapWidth][y + j - gapHeight] * kernel[i][j]);
            }
        }
        return output;
    }

    /**
     * A method to calculate the intensity of an image gradient
     * 
     * @param xGradient
     * @param yGradient
     * @return
     */
    private double[][] computeMagnitude(double[][] xGradient, double[][] yGradient) {
        int width = xGradient.length;
        int height = xGradient[0].length;
        double[][] magnitude = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double pixelMagnitude = Math.sqrt(Math.pow(xGradient[i][j], 2) + Math.pow(yGradient[i][j], 2));
                magnitude[i][j] = pixelMagnitude;
            }
        }
        return magnitude;
    }

    /**
     * A method to return the direction of the edge
     * rounded to one of four possible values - 0°, 45°, 90°, 135°.
     * 
     * @param xGradient
     * @param yGradient
     * @return
     */
    private int[][] computeDirection(double[][] xGradient, double[][] yGradient) {
        int width = xGradient.length;
        int height = xGradient[0].length;
        int roundedDirection = 0;
        int[][] direction = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double pixelDirection = Math.atan2(yGradient[i][j], xGradient[i][j]);
                roundedDirection = roundDirection(pixelDirection);
                direction[i][j] = roundedDirection;
            }
        }
        return direction;
    }

    /**
     * 
     * @param pixelDirection direction value for a given pixel from -pi to pi
     * @return
     */
    private int roundDirection(double pixelDirection) {
        double absDirection = Math.abs(pixelDirection);
        int roundedDirection = 0;
        absDirection = Math.toDegrees(absDirection);
        if ((absDirection >= 0 && absDirection < 22.5) || (absDirection >= 157.5 && absDirection <= 180)) {
            roundedDirection = 0;
        } else if (absDirection >= 22.5 && absDirection < 67.5) {
            roundedDirection = 45;
        } else if (absDirection >= 67.5 && absDirection < 112.5) {
            roundedDirection = 90;
        } else if (absDirection >= 112.5 && absDirection < 157.5) {
            roundedDirection = 135;
        }
        return roundedDirection;
    }

    /**
     * A method that allows you to shrink the detected edges by zeroing out the
     * non-maximum pixels
     * 
     * @param direction
     * @param magnitude
     * @return
     */
    private double[][] nonMaximumSuppression(int[][] direction, double[][] magnitude) {
        int width = magnitude.length;
        int height = magnitude[0].length;
        double[][] suppressedMagnitude = createZerosArray(width, height);

        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                if (direction[i][j] == 0) {
                    if ((magnitude[i][j] > magnitude[i - 1][j]) && (magnitude[i][j] > magnitude[i + 1][j])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                } else if (direction[i][j] == 45) {
                    if ((magnitude[i][j] > magnitude[i - 1][j - 1]) && (magnitude[i][j] > magnitude[i + 1][j + 1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                } else if (direction[i][j] == 90) {
                    if ((magnitude[i][j] > magnitude[i][j + 1]) && (magnitude[i][j] > magnitude[i][j - 1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                } else if (direction[i][j] == 135) {
                    if ((magnitude[i][j] > magnitude[i - 1][j + 1]) && (magnitude[i][j] > magnitude[i + 1][j - 1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
            }
        }
        return suppressedMagnitude;
    }

    /**
     * A method to set one of three pixel "strength" values
     * 
     * @param suppressedMagnitude
     * @return Strenght value matrix for each pixel:
     *         1 for a pixel above a higher threshold
     *         0.5 for the pixel above the lower threshold and below the higher
     *         threshold
     *         0 for the pixel below the lower threshold
     */
    private double[][] setStrengthFlag(double[][] suppressedMagnitude) {
        int width = suppressedMagnitude.length;
        int height = suppressedMagnitude[0].length;
        double[][] thresholdFlags = new double[width][height];
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                if (suppressedMagnitude[i][j] >= higherThreshold) {
                    thresholdFlags[i][j] = 1;
                } else if (suppressedMagnitude[i][j] >= lowerThreshold) {
                    thresholdFlags[i][j] = 0.5;
                } else if (suppressedMagnitude[i][j] < lowerThreshold) {
                    thresholdFlags[i][j] = 0;
                }
            }
        }
        return thresholdFlags;
    }

    /**
     * A method that sets the maximum brightness for:
     * - pixels labeled 1,
     * - pixels labeled 0.5, which have a strength of 1 in the immediate
     * neighborhood.
     * 
     * @param thresholdFlags
     * @param suppressedMagnitude
     * @return
     */
    private double[][] checkWeakPixelConnection(double[][] thresholdFlags, double[][] suppressedMagnitude) {
        int width = suppressedMagnitude.length;
        int height = suppressedMagnitude[0].length;
        double[][] connected = createZerosArray(width, height);
        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                if (thresholdFlags[i][j] == 0) {
                    connected[i][j] = 0;
                } else if (thresholdFlags[i][j] == 1) {
                    connected[i][j] = 255;
                } else if (thresholdFlags[i][j] == 0.5) {
                    if ((thresholdFlags[i - 1][j] == 1) || (thresholdFlags[i + 1][j] == 1)) {
                        connected[i][j] = 255;
                    } else if ((thresholdFlags[i][j - 1] == 1) || (thresholdFlags[i][j + 1] == 1)) {
                        connected[i][j] = 255;
                    } else if ((thresholdFlags[i - 1][j - 1] == 1) || (thresholdFlags[i + 1][j + 1] == 1)) {
                        connected[i][j] = 255;
                    } else if ((thresholdFlags[i - 1][j + 1] == 1) || (thresholdFlags[i + 1][j - 1] == 1)) {
                        connected[i][j] = 255;
                    }
                }
            }
        }

        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                if (connected[i][j] == 0.5) {
                    connected[i][j] = 0;
                }
            }
        }
        double[][] connectedPixels = new double[width][height];
        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                connectedPixels[i - 1][j - 1] = connected[i][j];
            }
        }
        return connectedPixels;
    }

    /**
     * A method to create an image file from an array of pixel values
     * 
     * @param array
     * @return
     * @throws IOException
     */
    private File createImageFromMatrix(double[][] array) throws IOException {
        int width = array.length;
        int height = array[0].length;
        BufferedImage edgeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = (int) array[i][j];
                Color color = new Color(pixel, pixel, pixel);
                edgeImage.setRGB(i, j, color.getRGB());
            }
        }
        String g = "outputimage" + LocalTime.now();
        g = g.replace('.', '_').replace(':', '_');
        g = ".\\" + g + ".jpg";
        File outputFile = new File(g);
        ImageIO.write(edgeImage, "jpg", outputFile);
        return outputFile;
    }
}
