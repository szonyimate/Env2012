package com.example.env2012;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient {

    public String SERVER_IP = "";
    public int SERVER_PORT = 0;

    // message to send to the server
    private String mServerMessage;
    // sends message recieved notifications
    private OnMessageReceived mMessageListener = null;
    // server will continue running while this is true
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    // Constructor
    public TcpClient(String serverIP, int serverPort, OnMessageReceived listener) {
        SERVER_IP = serverIP;
        SERVER_PORT = serverPort;
        mMessageListener = listener;
    }

    public void sendMessage(String message) {
        if (mBufferOut != null) {
            try {
                mBufferOut.write(message);
                mBufferOut.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;

        if (mBufferOut != null) {
            try {
                mBufferOut.flush();
                mBufferOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run() {
        mRun = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            //Log.e("TCP Client", "C: Connecting...");
            Socket socket = new Socket(serverAddr, SERVER_PORT);
            try {
                mBufferOut = new PrintWriter(socket.getOutputStream());
                //Log.e("TCP Client", "C: Sent.");
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                int charsRead = 0; char[] buffer = new char[1024]; //choose your buffer size if you need other than 1024

                while (mRun) {
                    charsRead = mBufferIn.read(buffer);
                    mServerMessage = new String(buffer).substring(0, charsRead);
                    if (mServerMessage != null && mMessageListener != null) {
                        mMessageListener.messageReceived(mServerMessage);}
                    mServerMessage = null;

                }

            } catch (Exception e) {
                //Log.e("TCP", "S: Error", e);
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
        }

    }

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}