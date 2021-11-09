package com.example.env2012;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient {

    public String TAG = TcpClient.class.getSimpleName();
    public String SERVER_IP = "192.168.1.127";
    public int SERVER_PORT = 20130;

    private String mServerMessage;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;

    // Constructor
    public TcpClient() {
    }

    public void sendMessage(final String message){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.d(TAG,"Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void stopClient() {
        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
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
            Log.e("TCP Client", "C: Connecting...");
            Socket socket = new Socket(serverAddr, SERVER_PORT);
            try {
                mBufferOut = new PrintWriter(socket.getOutputStream());
                Log.e("TCP Client", "C: Sent.");
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                int charsRead = 0; char[] buffer = new char[1024]; //choose your buffer size if you need other than 1024

                while (mRun) {
                    charsRead = mBufferIn.read(buffer);
                    mServerMessage = new String(buffer).substring(0, charsRead);
                    if (mServerMessage != null && mMessageListener != null) {
                        mMessageListener.messageReceived(mServerMessage);}
                    mServerMessage = null;
                }
                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");
            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            } finally {
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
