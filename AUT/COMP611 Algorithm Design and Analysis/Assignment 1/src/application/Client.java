package application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

// @author Jared Scholz
public class Client {

    public static final String HOST_NAME = "localhost";
    public static final int HOST_PORT = 18181;

    private final ClientView view;
    private Socket socket;
    private boolean stopRequested;

    public Client() {
        view = new ClientView(this);
        socket = null;
        stopRequested = false;
        view.display();
    }

    public void sendImage(BufferedImage image) { // used by the view thread
        try {
            // send image as a byte array along with the size:
            OutputStream outputStream = socket.getOutputStream();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", byteStream); // write image to the byte array
            byte[] size = ByteBuffer.allocate(4).putInt(byteStream.size()).array(); // courtesy of StackOverflow (link below)
            outputStream.write(size); // send the size of the byte array first
            outputStream.write(byteStream.toByteArray()); // send the byte array
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Client error: " + e);
        }
    }

    public void startClient() {
        try {
            socket = new Socket(HOST_NAME, HOST_PORT);
        } catch (IOException e) {
            System.err.println("Client could not make connection: " + e);
            System.exit(-1);
        }
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            stopRequested = false;
            while (!stopRequested && !socket.isClosed()) { // listen to server on main thread
                // input stream blocks until data becomes available...
                char identification = inputStream.readChar();
                if (identification == 'p') { // progress detected:
                    view.updateProgress(inputStream.readInt());
                } else if (identification == 'i') { // image detected:
                    /* Code from StackOverflow https://stackoverflow.com/a/25096332 below */
                    byte[] sizeArray = new byte[4];
                    inputStream.readFully(sizeArray);
                    int size = ByteBuffer.wrap(sizeArray).asIntBuffer().get();
                    byte[] imageArray = new byte[size];
                    inputStream.readFully(imageArray);
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageArray));
                    /* End code from StackOverflow */
                    view.updateOutput(image);
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e);
        }
    }

    public void requestStop() {
        stopRequested = true;
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.startClient();
    }
}
