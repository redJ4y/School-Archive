package application;

import tasks.Task;
import java.awt.image.BufferedImage;
import java.util.Random;

// @author Jared Scholz
public class ImageProcessor extends Task<BufferedImage, Integer> {

    private final ImageConnection client;
    private final Random randGen;

    public ImageProcessor(BufferedImage param, ImageConnection client) {
        super(param);
        this.client = client;
        randGen = new Random();
    }

    @Override
    public void run() {
        System.out.println("Task " + uniqueID + " starting for " + client.getClientAddress());
        notifyAll(0);
        int width = param.getWidth();
        int height = param.getHeight();
        int maxSegmentWidth = width / 12;
        for (int i = 0; i < height; i++) {
            int j = 0;
            while (j < width) {
                // produce segment of random width...
                int segmentEnd = j + randGen.nextInt(maxSegmentWidth) + 10;
                // calculate average color of segment:
                long redSum = 0;
                long greenSum = 0;
                long blueSum = 0;
                int actualWidth = 0; // account for right image border
                for (int k = j; k < segmentEnd && k < width; k++) {
                    int currentRGB = param.getRGB(k, i);
                    redSum += (currentRGB & 0x00FF0000) >>> 16;
                    greenSum += (currentRGB & 0x0000FF00) >>> 8;
                    blueSum += currentRGB & 0x000000FF;
                    actualWidth++;
                }
                int newRGB = 255 << 24 // include alpha
                        | (int) (redSum / actualWidth) << 16
                        | (int) (greenSum / actualWidth) << 8
                        | (int) (blueSum / actualWidth);
                // apply average color to entire segment:
                for (; j < segmentEnd && j < width; j++) {
                    param.setRGB(j, i, newRGB);
                }
                Thread.yield(); // slow down thread to better show progression!
            }
            if (height % 4 == 0) { // send update every 4th row...
                notifyAll((int) (((double) i / height) * 100));
            }
        }
        notifyAll(100);
        client.sendImage(param);
    }
}
