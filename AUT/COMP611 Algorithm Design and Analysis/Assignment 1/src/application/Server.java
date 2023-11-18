package application;

import tasks.ThreadPool;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

// @author Jared Scholz
public class Server {

    public static final int PORT = 18181;

    private final ThreadPool threadPool;
    private boolean stopRequested;

    public Server() {
        threadPool = new ThreadPool(4); // 4 worker threads
        stopRequested = false;
    }

    /* Process an image recieved from a client - used by ImageConnection */
    public void processImage(BufferedImage image, ImageConnection client) {
        ImageProcessor processorTask = new ImageProcessor(image, client);
        processorTask.addListener(new ProgressObserver(client));
        threadPool.perform(processorTask);
    } // now ImageProcessor and ProgressObserver communicate directly with ImageConnection

    public void startServer() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started at " + InetAddress.getLocalHost() + " on port " + PORT);
        } catch (IOException e) {
            System.err.println("Server cannot listen on port: " + e);
            System.exit(-1);
        }
        try {
            stopRequested = false;
            while (!stopRequested) {
                Socket socket = serverSocket.accept(); // blocks until a connection is made
                System.out.println("Connection made with " + socket.getInetAddress());
                ImageConnection connection = new ImageConnection(socket, this);
                Thread thread = new Thread(connection); // allow for unlimited connection threads...
                thread.start(); // (these are low-cost threads that are primarily blocking)
            }
            serverSocket.close();
            threadPool.destroyPool(); // clean up
        } catch (IOException e) {
            System.err.println("Cannot accept client connection: " + e);
        }
    }

    public void requestStop() { // functions only after the next connection is made!
        stopRequested = true;
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }
}
