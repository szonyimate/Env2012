package com.example.env2012;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValuesFragment extends Fragment {

    final double Pt_C = -4.183e-12;

    byte byary[] = new byte[65531];
    byte nyolcbajt[] = new byte[8];
    double fpvect[] = new double[8001];
    char tident[] = new char[8];
    String Tpswd[] = new String[8];

    //CONST
    boolean vanconsol = false;
    boolean demomode = false;
    String cr = "#13" + "#10";
    String demofilnam = "EDemoRom.env";
    Teeprom eedefault = new Teeprom();
    TptParms tptParms = new TptParms();
    int pressvalid = 0;
    int shttmpvalid = 0;
    int ptvalid[] = new int[4];
    long ptlong[] = new long[4];
    long ptzr[] = new long[4];

    //-----------------------------------------------------------------

    double shtmpr = 23.0;
    double shtrhslope = 1.0;
    double[] rpt = {1090.0,1091.0,1092.0,1093.0};
    double[] wpt = {0.091,0.092,0.093,0.094};

    GridLayout gridLayout;

    TextView pressureTextView;
    TextView temp1TextView;
    TextView temp2TextView;
    TextView temp3TextView;
    TextView temp4TextView;
    TextView humidityTextView;
    TextView huSensTTextView;

    CountDownTimer countDownTimer;

    List<String> inputDemo = new ArrayList<String>();
    String[] splittedValues;
    int stepper = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_values, parent, false);

        gridLayout = view.findViewById(R.id.gridLayout);

        temp1TextView = view.findViewById(R.id.t1ValueTextView);
        temp2TextView = view.findViewById(R.id.t2ValueTextView);
        temp3TextView = view.findViewById(R.id.t3ValueTextView);
        temp4TextView = view.findViewById(R.id.t4ValueTextView);
        pressureTextView = view.findViewById(R.id.pressureValueTextView);
        humidityTextView = view.findViewById(R.id.humidityValueTextView);
        huSensTTextView = view.findViewById(R.id.husenstValueTextView);
        fillDemoList();

        // eedefault inicializalas
        for (int i = 0; i < 4; i++){
            tptParms.rnull = 1000 + i;  tptParms.beta = -5.802e-7f; tptParms.alfa = 3.90802e-3f;
            eedefault.ptary[i] = tptParms;
        }
        eedefault.rnorm = 1250;
        eedefault.proffs = 0;
        eedefault.prslope = 1;
        eedefault.shtrhoffs=0;
        eedefault.shtrhslope=1.0f;
        eedefault.shttmpoffs=0;
        eedefault.shttmpslope=1.0f;
        eedefault.equipid[0] = '2'; eedefault.equipid[1] = '#'; eedefault.equipid[2] = 'E'; eedefault.equipid[3] = 'n';
        eedefault.equipid[4] = 'v'; eedefault.equipid[5] = '_'; eedefault.equipid[6] = 'C'; eedefault.equipid[7] = '!';
        eedefault.humcaldat = Long.parseLong("20100101",16);
        eedefault.ptcaldat = Long.parseLong("20100322",16);
        eedefault.prescaldat = Long.parseLong("20100323",16);
        eedefault.crc0 = Long.parseLong("1244E263",16);

        //ptvalid inicializalas
        Arrays.fill(ptvalid,0);
        //ptlong inicializalas
        Arrays.fill(ptlong,Long.parseLong("700000",16));
        //ptzr inicializalas
        Arrays.fill(ptzr,0);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        countDownTimer = new CountDownTimer(600100, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (stepper < inputDemo.size()){
                    splitInput(inputDemo.get(stepper));
                    pressureTextView.setText(calculatePressure(splittedValues[0]));
                    humidityTextView.setText(calculateHumidity(splittedValues[1]));
                    huSensTTextView.setText(calculateShTemperature(splittedValues[2]));
                    temp1TextView.setText(calculateTemperature(0,splittedValues[3]));
                    temp2TextView.setText(calculateTemperature(1,splittedValues[4]));
                    temp3TextView.setText(calculateTemperature(2,splittedValues[5]));
                    temp4TextView.setText(calculateTemperature(3,splittedValues[6]));
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
        inputDemo.add("0C0AD7 050A 1983 6FB007 6FAEB1 6FAD86 6FB500");
        inputDemo.add("0BA3D9 050A 1911 6FB007 6FAEB1 6FAD86 6FB500");
        inputDemo.add("0BA3D5 050A 190F 6FB007 6FAEB1 6FAD86 6FB500");
        inputDemo.add("0C0AD7 050A 1983 6FB007 6FAEB1 6FAD86 6FB500");
        inputDemo.add("0C0AD4 0509 1984 6FB007 6FAEB1 6FAD86 6FB500");
        inputDemo.add("0C0ACC 050B 1982 6FB007 6FAEB1 6FAD86 6FB500");
        inputDemo.add("0C0AD9 050A 1984 6FAD86 7008A3 7027D5 7047A5");
        inputDemo.add("0C0ADD 050B 1981 7059E5 7009D6 7028BD 6FAD86");
        inputDemo.add("0C0AF7 050A 1983 6FAD86 7009F7 7028E8 70483A");
        inputDemo.add("0C0AEB 0509 1982 705A14 6FAD86 7028D8 704856");
        inputDemo.add("0C0AFD 050A 1982 7059FE 700A3D 6FAD86 704873");
        inputDemo.add("0C0B07 050B 1981 705A29 700A77 702911 6FAD86");
        inputDemo.add("0C0B05 050A 1980 6FAD86 700A98 702943 704881");
        inputDemo.add("0C0AF1 0508 1982 705A52 6FAD86 702947 704890");
        inputDemo.add("0C0AEF 0507 1981 705A6A 700AF7 6FAD86 704897");
        inputDemo.add("0C0AFE 0508 1981 705A52 700B17 70297B 6FAD86");
        inputDemo.add("0C0B05 050A 1980 6FAD86 700B30 7029AD 7048E1");
        inputDemo.add("0C0AFD 0508 1980 705A65 6FAD86 7029CE 7048D4");
        inputDemo.add("0C0B07 0507 1980 705A95 700B9A 6FAD86 7048DC");
    }

    public String[] splitInput(String input) {
        splittedValues = input.split(" |\\_");
        return splittedValues;
    }

    public String calculatePressure(String hexValue){
        int intValue = Integer.parseInt(hexValue, 16);
        double pressure = Math.round(intValue*0.98914);
        pressure = (pressure*0.00000000011087363-0.026687718)*pressure+121756.66;
        pressure = pressure * eedefault.prslope + eedefault.proffs;

        intValue = (int) pressure;

        return String.valueOf(intValue);
    }

    public String calculateHumidity(String hexValue){
        int intValue = Integer.parseInt(hexValue, 16);
        double humidity = (0.000001595 * intValue + 0.0367) * intValue - 2.0468;
        humidity = (shtmpr - 25) * (0.01+ 0.00008 * intValue) + humidity;
        humidity = humidity * eedefault.shtrhslope + eedefault.shtrhoffs;

        String humidityValue = String.valueOf((double)Math.round(humidity * 100d) / 100d);

        return humidityValue;
    }

    public String calculateShTemperature(String hexValue){
        int intValue = Integer.parseInt(hexValue,16);
        double temp = (intValue/100-40.1)*eedefault.shttmpslope+eedefault.shttmpoffs;

        return String.valueOf(temp);
    }

    public String calculateTemperature(int pos, String hexValue){
        long input = Long.parseLong(hexValue, 16);
        double xpt, negg, ww;
        rpt[pos] = (float) input/ (float) Long.parseLong("800000",16) * (float) eedefault.rnorm;
        ww = rpt[pos]/eedefault.ptary[pos].rnull-1;
        xpt = (Math.sqrt(Math.pow(eedefault.ptary[pos].alfa, 2)+4*eedefault.ptary[pos].beta*ww)-eedefault.ptary[pos].alfa)/2/eedefault.ptary[pos].beta;

        if (xpt < 0) {
            negg = Pt_C*(xpt - 100) * xpt;
            xpt = (Math.sqrt(Math.pow(eedefault.ptary[pos].alfa, 2)+4*(negg + eedefault.ptary[pos].beta)*ww)-eedefault.ptary[pos].alfa)/2/(negg + eedefault.ptary[pos].beta);
            negg = Pt_C*(xpt - 100) * xpt;
            xpt = (Math.sqrt(Math.pow(eedefault.ptary[pos].alfa, 2)+4*(negg + eedefault.ptary[pos].beta)*ww)-eedefault.ptary[pos].alfa)/2/(negg + eedefault.ptary[pos].beta);
        }

        String tempValue = String.valueOf(xpt);

        return tempValue;
    }

}