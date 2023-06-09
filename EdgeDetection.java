package edgedetection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;

/**
 * Class that processes images using the Scharr, Sobel and Prewitt algorithms
 */

public class EdgeDetection {

    public static final String HORIZONTAL = "Horizontal Filter";
    public static final String VERTICAL = "Vertical Filter";
    public static final String SOBEL_VERTICAL = "Sobel Vertical Filter";
    public static final String SOBEL_HORIZONTAL = "Sobel Horizontal Filter";
    public static final String SCHARR_VERTICAL = "Scharr Vertical Filter";
    public static final String SCHARR_HORIZONTAL = "Scharr Horizontal Filter";
    public static final String CANNY_EDGE_DETECTION = "Canny Algorithm";
    public static final double HIGHER_THRESHOLD = 0.15 * 294;
    public static final double LOWER_THRESHOLD = 0.03 * 294;
    private static final double[][] VERTICAL_MASK = { { 1, 0, -1 }, { 1, 0, -1 }, { 1, 0, -1 } };
    private static final double[][] HORIZONTAL_MASK = { { 1, 1, 1 }, { 0, 0, 0 }, { -1, -1, -1 } };
    private static final double[][] SOBEL_MASK_VERTICAL = { { 1, 0, -1 }, { 2, 0, -2 }, { 1, 0, -1 } };
    private static final double[][] SOBEL_MASK_HORIZONTAL = { { 1, 2, 1 }, { 0, 0, 0 }, { -1, -2, -1 } };
    private static final double[][] SCHARR_MASK_VERTICAL = { { 3, 0, -3 }, { 10, 0, -10 }, { 3, 0, -3 } };
    private static final double[][] SCHARR_MASK_HORIZONTAL = { { 3, 10, 3 }, { 0, 0, 0 }, { -3, -10, -3 } };

    /**
     * Method of type final declaring a HashMap that takes variables of type String
     * and a two-dimensional array
     */

    private final HashMap<String, double[][]> maskMap;

    public EdgeDetection() {
        maskMap = buildMaskMap();
    }

    /**
     * @param bufferedImage
     * @param selectedFilter
     * @param lowerThresholdValue
     * @param higherThresholdValue
     * @return output
     */

    public File detectEdges(BufferedImage bufferedImage, String selectedFilter, double lowerThresholdValue,
            double higherThresholdValue) throws IOException {
        double[][] mixedPixels = new double[bufferedImage.getWidth()][bufferedImage.getHeight()];
        File output = null;
        if (selectedFilter.equals(CANNY_EDGE_DETECTION)) {
            Canny cannyAlgorithm = new Canny(lowerThresholdValue, higherThresholdValue);
            output = cannyAlgorithm.detectEdges(bufferedImage);
        } else {
            double[][][] image = transformImageToArray(bufferedImage);
            double[][] filter = maskMap.get(selectedFilter);
            mixedPixels = applyMix(bufferedImage.getWidth(), bufferedImage.getHeight(), image, filter);
            output = createImageFromMatrix(bufferedImage, mixedPixels);
        }
        return output;
    }

    /**
     * Method changes image to vector
     * 
     * @param bufferedImage
     * @return image
     */

    private double[][][] transformImageToArray(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        double[][][] image = new double[3][height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color color = new Color(bufferedImage.getRGB(j, i));
                image[0][i][j] = color.getRed();
                image[1][i][j] = color.getGreen();
                image[2][i][j] = color.getBlue();
            }
        }
        return image;
    }

    /**
     * The method combines image kernels with a filter
     * 
     * @param width
     * @param height
     * @param image
     * @param filter
     * @return finalMix
     */

    private double[][] applyMix(int width, int height, double[][][] image, double[][] filter) {
        edgedetection.Mix mix = new edgedetection.Mix();
        double[][] redMix = mix.mixNext(image[0], height, width, filter, 3, 3);
        double[][] greenMix = mix.mixNext(image[1], height, width, filter, 3, 3);
        double[][] blueMix = mix.mixNext(image[2], height, width, filter, 3, 3);
        double[][] finalMix = new double[redMix.length][redMix[0].length];
        for (int i = 0; i < redMix.length; i++) {
            for (int j = 0; j < redMix[i].length; j++) {
                finalMix[i][j] = redMix[i][j] + greenMix[i][j] + blueMix[i][j];
            }
        }
        return finalMix;
    }

    /**
     * The method recreates an image from an array
     * 
     * @param originalImage
     * @param imageRGB
     * @return outputFile o
     * @exception IOException
     * @see IOException
     */

    private File createImageFromMatrix(BufferedImage originalImage, double[][] imageRGB) throws IOException {
        BufferedImage createNewImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < imageRGB.length; i++) {
            for (int j = 0; j < imageRGB[i].length; j++) {
                Color color = new Color(transformRGB(imageRGB[i][j]),
                        transformRGB(imageRGB[i][j]),
                        transformRGB(imageRGB[i][j]));
                createNewImage.setRGB(j, i, color.getRGB());
            }
        }
        String g = "outputimage" + LocalTime.now();
        g = g.replace('.', '_').replace(':', '_');
        g = ".\\" + g + ".jpg";
        File outputFile = new File(g);
        ImageIO.write(createNewImage, "jpg", outputFile);
        return outputFile;
    }

    /**
     * Method changes read values to RGB scale
     * 
     * @param value
     * @return
     */

    private int transformRGB(double value) {
        if (value < 0.0) {
            value = -value;
        }
        if (value > 255) {
            return 255;
        } else {
            return (int) value;
        }
    }

    /**
     * The method implements a HashMap that takes objects associated with the mask
     * and a two-dimensional array of type double containing the mask parameters
     * 
     * @return HashMap
     */

    private HashMap<String, double[][]> buildMaskMap() {
        HashMap<String, double[][]> maskMap;
        maskMap = new HashMap<>();

        maskMap.put(VERTICAL, VERTICAL_MASK);
        maskMap.put(HORIZONTAL, HORIZONTAL_MASK);
        maskMap.put(SOBEL_VERTICAL, SOBEL_MASK_VERTICAL);
        maskMap.put(SOBEL_HORIZONTAL, SOBEL_MASK_HORIZONTAL);
        maskMap.put(SCHARR_VERTICAL, SCHARR_MASK_VERTICAL);
        maskMap.put(SCHARR_HORIZONTAL, SCHARR_MASK_HORIZONTAL);
        return maskMap;
    }
}
