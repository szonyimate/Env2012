package com.example.env2012;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import java.util.ArrayList;
import java.util.List;

public class ValuesFragment extends Fragment {

    Button startButton;
    GridLayout gridLayout;

    TextView currentView;

    CountDownTimer countDownTimer;

    List<String> inputDemo = new ArrayList<String>();
    String[] splittedValues;
    int stepper = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_values, parent, false);

        startButton = view.findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> startCalculations(currentView));

        gridLayout = view.findViewById(R.id.gridLayout);
        fillDemoList();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        countDownTimer = new CountDownTimer(60100, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (stepper < inputDemo.size()){
                    splitInput(inputDemo.get(stepper));
                    for (int j=0; j<7; j++){
                        currentView = gridLayout.findViewWithTag(Integer.toString(j));
                        if (j==0){
                            calculatePressure(splittedValues[j]);
                        } else {
                            currentView.setText(splittedValues[j]);
                        }
                    }
                    stepper++;
                } else {
                    stepper = 0;
                }
            }

            @Override
            public void onFinish() {

            }
        };

    }

    public void startCalculations(View view) {
        countDownTimer.start();
    }

    public void fillDemoList() {
        inputDemo.add("0C0AD7 050A 1983 70594C 700788_FFFFF6 704739");
        inputDemo.add("0C0ACB 050A 1982 705946 7007BD 702744_FFFFE3");
        inputDemo.add("0C0AE2 050A 1982_FFFFEA 7007E1 702782 70477B");
        inputDemo.add("0C0AD7 050A 1983 70596A_FFFFF2 702783 704754");
        inputDemo.add("0C0AD4 0509 1984 705982 700839_FFFFE8 70478D");
        inputDemo.add("0C0ACC 050B 1982 705996 700884 7027C9_FFFFE3");
        inputDemo.add("0C0AD9 050A 1984_FFFFF9 7008A3 7027D5 7047A5");
        inputDemo.add("0C0ADD 050B 1981 7059E5 7009D6 7028BD_FFFFC8");
        inputDemo.add("0C0AF7 050A 1983_FFFFEB 7009F7 7028E8 70483A");
        inputDemo.add("0C0AEB 0509 1982 705A14_FFFFEA 7028D8 704856");
        inputDemo.add("0C0AFD 050A 1982 7059FE 700A3D_FFFFC4 704873");
        inputDemo.add("0C0B07 050B 1981 705A29 700A77 702911_FFFFEE");
        inputDemo.add("0C0B05 050A 1980_FFFFF4 700A98 702943 704881");
        inputDemo.add("0C0AF1 0508 1982 705A52_FFFFEB 702947 704890");
        inputDemo.add("0C0AEF 0507 1981 705A6A 700AF7_FFFFF5 704897");
        inputDemo.add("0C0AFE 0508 1981 705A52 700B17 70297B_FFFFD9");
        inputDemo.add("0C0B05 050A 1980_FFFFD5 700B30 7029AD 7048E1");
        inputDemo.add("0C0AFD 0508 1980 705A65_FFFFD3 7029CE 7048D4");
        inputDemo.add("0C0B07 0507 1980 705A95 700B9A_FFFFE5 7048DC");
    }

    public String[] splitInput(String input) {
        splittedValues = input.split(" |\\_");
        return splittedValues;
    }

    public String calculatePressure(String hexValue){
        

        return "";
    }
}