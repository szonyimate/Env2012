package com.example.env2012;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class TcpFragment extends Activity {

    TcpClient mTcpClient;
    TextView recieveText;
    TextView sendText;
    TextView serverIp;
    TextView serverPort;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setContentView(R.layout.fragment_tcp);

        recieveText = findViewById(R.id.responseText);
        sendText = findViewById(R.id.inputText);
        serverIp = findViewById(R.id.serverIpText);
        serverPort = findViewById(R.id.serverPortText);

        new ConnectTask().execute("");

        mTcpClient = new TcpClient();

    }

    public void sendRequest(View view) {

        if (mTcpClient != null) {
            mTcpClient.sendMessage(sendText.getText().toString());
        }
    }

    public void messageReceived(String message){
        recieveText.setText(message + "\n");
    }
}
