package com.example.env2012;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class TcpFragment extends Activity {

    TcpClient mTcpClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        new ConnectTask().execute("");
    }

    @Override
    protected void onStart() {
        super.onStart();

        setContentView(R.layout.fragment_tcp);
    }

}
