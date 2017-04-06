package com.Infiniteloop.TogetherPlay.dj;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * The implementation of a ServerSocketHandler used by the Wi-Fi P2P group owner.
 * Inspired by https://github.com/bryan-y88/Musics_Around
 */

public class ServerSocketHandler extends Thread {
    ServerSocket serverSocket = null;
    private static final String TAG = "ServerSocketHandler";
    public static final int SERVER_PORT = 9001;
    public static final int SERVER_CALLBACK = 103;

    // Use a 10 second time out to receive an ack message
    public static final int ACK_TIMEOUT = 10000;

    // A HashSet of all client socket connections and their corresponding output
    // streams for writing
    private HashSet<Socket> connections;

    private boolean needReset = true;
    private Handler handler;

    // For splitting command messages, it should be a character that cannot be
    // used in a file name, all commands have to end with a delimiter
    public static final String CMD_DELIMITER = ";";

    private static final int BUFFER_SIZE = 256;

    // Commands the server can send out to the clients
    public static final String PLAY_CMD = "PLAY";
    public static final String STOP_CMD = "STOP";
    public static final String SYNC_CMD = "SYNC";

    public ServerSocketHandler(Handler handler) throws IOException {
        this.handler = handler;
        connections = new HashSet<Socket>();
        establishSocket();
    }

    @Override
    public void run() {
        // Thread will terminate when someone disconnects the server
        while (serverSocket != null) {
            try {
                // Let the UI thread control the server
                handler.obtainMessage(SERVER_CALLBACK, this).sendToTarget();

                // Always check if the server socket is still ok to function
                establishSocket();

                // A blocking operation to accept incoming client connections
                Socket clientSocket = serverSocket.accept();
                Log.d(TAG, "Accepted another client");

                syncClientTime(clientSocket);

                connections.add(clientSocket);
            }
            catch (IOException e) {
                Log.e(TAG, "Could not communicate to client socket.");
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        needReset = true;

                        // Close all client socket connections
                        for (Socket s : connections) {
                            s.close();
                        }

                        // Empty the stored connection list
                        connections = new HashSet<Socket>();
                    }
                } catch (IOException e1) {
                    Log.e(TAG, "Could not close all client sockets.");
                    disconnectServer();
                }
                break;
            }
        }
    }

    public void establishSocket() {
        // Only setup a new server socket if something went wrong
        if (!needReset) {
            return;
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
            serverSocket = new ServerSocket(SERVER_PORT);
            Log.d(TAG, "Socket Started");
            needReset = false;
        } catch (IOException e) {
            Log.e(TAG, "Could not start server socket.");
        }
    }

    private void syncClientTime(Socket clientSocket) throws IOException {
        Log.d(TAG, "Started syncing time.");

        // Initialize the network latency trackers
        long prevLatency = 0;
        long currLatency = 0;
        long sendTime;

        // This is the minimum latency we are willing to accept, we have to
        // relax this requirement if the network is poor
        long ACCEPTABLE_LATENCY = 50;

        InputStream iStream = clientSocket.getInputStream();
        OutputStream oStream = clientSocket.getOutputStream();

        boolean success = false;
        boolean ackReceived = false;

        clientSocket.setSoTimeout(ACK_TIMEOUT);

        while (!success) {
            // See if we can reach time synchronization within 7 attempts
            for (int i = 0; i < 7; i++) {
                // Preparing command to send, this should be as fast as possible
                // ***********Warning: time sensitive code!***********
                String command = SYNC_CMD + CMD_DELIMITER;

                // Use this to measure the latency
                sendTime = System.currentTimeMillis();

                // Compensating time sync with network latencies:
                // assume our time sync message reaches our client in
                // approximately half of the send and receive time
                command += String.valueOf(currLatency / 2
                        + System.currentTimeMillis())
                        + CMD_DELIMITER;
                oStream.write(command.getBytes());
                // ***********End of time sensitive code************

                while (!ackReceived) {
                    // Clear the buffer before reading
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytes;

                    // Read from the InputStream and determine the network
                    // latency, this will not block forever as the read timeout
                    // has been set
                    bytes = iStream.read(buffer);
                    if (bytes != -1) {
                        // Check for the correct acknowledge message, we don't want
                        // to respond to any other messages other than the SYNC ack from the client
                        String recMsg = new String(buffer);

                        String[] cmdString = recMsg.split(CMD_DELIMITER);

                        if (cmdString[0].equals(SYNC_CMD) && cmdString.length > 1) {
                            ackReceived = true;

                            // Let's hope that the current communication
                            // latencies is within our acceptable latency when
                            // compared to the previous communication latency
                            prevLatency = currLatency;

                            // Just to make the method call similar to client's,
                            // to improve the accuracy of the client receive
                            // time is half of the round trip delay
                            Long.parseLong(cmdString[1]);

                            currLatency = System.currentTimeMillis() - sendTime;

                            // Can this wrap around? Producing a negative number?
                            if (currLatency < 0) {
                                currLatency *= -1;
                            }

                            // Comparing latency jitters:
                            // if this round of latency is acceptable, then the
                            // previously sent time should be reasonable enough
                            // to be used to sync the time for our clients
                            if (Math.abs(currLatency - prevLatency) < ACCEPTABLE_LATENCY) {
                                success = true;
                                Log.d(TAG, "Accepted latency: " + ACCEPTABLE_LATENCY);
                                break;
                            }
                        }
                    }
                    // Socket read timed out, so treat it as an ack has been
                    // received and exit this while loop and send another
                    // message
                    else {
                        ackReceived = true;
                        Log.d(TAG, "Socket read timed out.");
                    }
                }

                Log.d(TAG, "Command Sent: " + command + ", and retrieved network latency of " +
                        currLatency + " ms.");

                if (success) {
                    break;
                }
            }

            // Still can't get a satisfactory result, let's relax our requirement by 2 folds
            ACCEPTABLE_LATENCY *= 2;

            // We have to call it quits some time
            if (ACCEPTABLE_LATENCY > 10000) {
                success = true;
            }
        }
    }

    public void sendPlay(String fileName, long playTime, int playPosition) {
        if (fileName == null) {
            return;
        }

        /*Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    for (Socket s : connections) {
                        try {
                            syncClientTime(s);
                        } catch (IOException e) {
                            Log.e(TAG, "Could not communicate to client socket.");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            thread.join(); // Wait for thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        String command = PLAY_CMD + CMD_DELIMITER + fileName + CMD_DELIMITER
                + String.valueOf(playTime) + CMD_DELIMITER
                + String.valueOf(playPosition) + CMD_DELIMITER;

        Log.d(TAG, "Sending command: " + command);

        for (Socket s : connections) {
            sendCommand(s, command);
        }
    }

    public void sendStop() {
        String command = STOP_CMD + CMD_DELIMITER;

        Log.d(TAG, "Sending command: " + command);

        for (Socket s : connections) {
            sendCommand(s, command);
        }
    }

    private void sendCommand(Socket clientSocket, String command) {
        if (clientSocket == null) {
            return;
        }

        // Automatically update the client connections, making sure the client
        // sockets are always "fresh"
        if (clientSocket.isClosed()) {
            connections.remove(clientSocket);
            clientSocket = null;
            return;
        }

        try {
            // Get the corresponding output stream from the socket
            OutputStream oStream = clientSocket.getOutputStream();
            oStream.write(command.getBytes());
            Log.d(TAG, "Command Sent: " + command);
        } catch (IOException e) {
            try {
                // This client socket is no longer valid, remove it from the list
                clientSocket.close();
                connections.remove(clientSocket);
                clientSocket = null;
            } catch (IOException e1) {
                Log.e(TAG, "Can't remove invalid client socket.");
            }

            Log.e(TAG, "Can't send command to client: " + command);
        }
    }

    public void disconnectClients() {
        // Close all client socket connections
        for (Socket s : connections) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Empty the stored connection list
        connections = new HashSet<Socket>();
    }

    public void disconnectServer() {
        needReset = true;

        // Must disconnect all clients first
        disconnectClients();

        // Empty the client connection list
        connections = null;
        connections = new HashSet<Socket>();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = null;
    }
}
