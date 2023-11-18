package application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

// @author Jared Scholz
public class ImageConnection implements Runnable {

    private final Socket socket;
    private final Server server;
    private boolean stopRequested;
    private DataOutputStream outputStream;

    public ImageConnection(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        stopRequested = false;
        outputStream = null;
        try {
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Server error: " + e);
        }
    }

    public synchronized void sendImage(BufferedImage image) {
        if (!socket.isClosed()) {
            try {
                outputStream.writeChar('i'); // send identification first
                outputStream.flush();
                // send image as a byte array along with the size:
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", byteStream); // write image to the byte array
                byte[] size = ByteBuffer.allocate(4).putInt(byteStream.size()).array(); // courtesy of StackOverflow (link below)
                outputStream.write(size); // send the size of the byte array first
                outputStream.write(byteStream.toByteArray()); // send the byte array
                outputStream.flush();
            } catch (IOException e) {
                System.err.println("Server error: " + e);
            }
        }
    }

    public synchronized void sendProgress(Integer progress) {
        if (!socket.isClosed()) {
            try {
                outputStream.writeChar('p'); // send identification first
                outputStream.flush();
                outputStream.writeInt(progress);
                outputStream.flush();
            } catch (IOException e) {
                System.err.println("Server error: " + e);
            }
        }
    }

    @Override
    public void run() { // wait for input from the client
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            stopRequested = false;
            while (!stopRequested && !socket.isClosed()) {
                /* Code from StackOverflow https://stackoverflow.com/a/25096332 below */
                byte[] sizeArray = new byte[4];
                inputStream.readFully(sizeArray); // input stream blocks until data becomes available...
                int size = ByteBuffer.wrap(sizeArray).asIntBuffer().get();
                byte[] imageArray = new byte[size];
                inputStream.readFully(imageArray);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageArray));
                /* End code from StackOverflow */
                server.processImage(image, this);
            }
            outputStream.close(); // clean up
        } catch (IOException e) {
            System.out.println("Client " + socket.getInetAddress() + " disconnected");
        }
    }

    public void requestStop() { // functions only after the next image is recieved!
        stopRequested = true;
    }

    public InetAddress getClientAddress() {
        return socket.getInetAddress();
    }
}
