package com.example.env2012;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private final BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    private SerialService service;

    //------------------------------------

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

    double shtmpr = 23.0;
    double shtrhslope = 1.0;
    double[] rpt = {1090.0,1091.0,1092.0,1093.0};
    double[] wpt = {0.091,0.092,0.093,0.094};

    Button connectButton;
    Button startButton;
    Button stopButton;
    GridLayout gridLayout;

    TextView pressureTextView;
    TextView temp1TextView;
    TextView temp2TextView;
    TextView temp3TextView;
    TextView temp4TextView;
    TextView humidityTextView;
    TextView huSensTTextView;

    TextView lineTextView;

    CountDownTimer countDownTimer;

    String[] splittedValues;
    int stepper = 0;

    //------------------------------------

    //private TextView receiveText;
    //private TextView sendText;
    private ControlLines controlLines;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean controlLinesEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    public ValuesFragment valuesFragment = new ValuesFragment();
    public List<String> lines;
    String line = new String();

    public TerminalFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Constants.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = getArguments().getInt("baud");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_GRANT_USB));
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
        if(controlLinesEnabled && controlLines != null && connected == Connected.True)
            controlLines.start();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(broadcastReceiver);
        if(controlLines != null)
            controlLines.stop();
        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));
        controlLines = new ControlLines(view);
         */
        View view = inflater.inflate(R.layout.fragment_values, container, false);

        lines = new ArrayList<String>();

        connectButton = view.findViewById(R.id.connectButton);
        connectButton.setOnClickListener(v -> send("A1X"));

        stopButton = view.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> send("A2X"));

        gridLayout = view.findViewById(R.id.gridLayout);

        temp1TextView = view.findViewById(R.id.t1ValueTextView);
        temp2TextView = view.findViewById(R.id.t2ValueTextView);
        temp3TextView = view.findViewById(R.id.t3ValueTextView);
        temp4TextView = view.findViewById(R.id.t4ValueTextView);
        pressureTextView = view.findViewById(R.id.pressureValueTextView);
        humidityTextView = view.findViewById(R.id.humidityValueTextView);
        huSensTTextView = view.findViewById(R.id.husenstValueTextView);

        lineTextView = view.findViewById(R.id.lineTextView);

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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
        menu.findItem(R.id.controlLines).setChecked(controlLinesEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            //receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } /* else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } */else if (id == R.id.controlLines) {
            controlLinesEnabled = !controlLinesEnabled;
            item.setChecked(controlLinesEnabled);
            if (controlLinesEnabled) {
                controlLines.start();
            } else {
                controlLines.stop();
            }
            return true;
        } else if (id == R.id.sendBreak) {
            try {
                usbSerialPort.setBreak(true);
                Thread.sleep(100);
                status("send BREAK");
                usbSerialPort.setBreak(false);
            } catch (Exception e) {
                status("send BREAK failed: " + e.getMessage());
            }
            return true;
        } else if (id == R.id.listValues) {
            try {
                Fragment fragment = new ValuesFragment();
                getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
            } catch (Exception e) {
                status("Fragment creation failed: " + e.getMessage());
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        connect(null);
    }

    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(Constants.INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        connected = Connected.Pending;
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), usbConnection, usbSerialPort);
            service.connect(socket);
            // usb connect is not asynchronous. connect-success and connect-error are returned immediately from socket.connect
            // for consistency to bluetooth/bluetooth-LE app use same SerialListener and SerialService classes
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        controlLines.stop();
        service.disconnect();
        usbSerialPort = null;
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //receiveText.append(spn);
            service.write(data);
        } catch (SerialTimeoutException e) {
            status("write timeout: " + e.getMessage());
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        if(hexEnabled) {
        } else {
            String msg = new String(data);
            if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {

                    lines.add(line.substring(3,line.length()-2));

                    if (lines.size() > 2) {
                        try {
                            splitInput(lines.get(lines.size()-1));
                            pressureTextView.setText(calculatePressure(splittedValues[0]));
                            humidityTextView.setText(calculateHumidity(splittedValues[1]));
                            huSensTTextView.setText(calculateShTemperature(splittedValues[2]));
                            temp1TextView.setText(calculateTemperature(0,splittedValues[3]));
                            temp2TextView.setText(calculateTemperature(1,splittedValues[4]));
                            temp3TextView.setText(calculateTemperature(2,splittedValues[5]));
                            temp4TextView.setText(calculateTemperature(3,splittedValues[6]));
                        } catch (Exception e) {
                            Toast.makeText(service, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }


                    }

                    /*
                    Editable edt = lineTextView.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");

                     */
                    line = "";
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            line += TextUtil.toCaretString(msg, newline.length() != 0).toString();
        }
    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
        if(controlLinesEnabled)
            controlLines.start();
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    class ControlLines {
        private static final int refreshInterval = 500; // msec

        private final Handler mainLooper;
        private final Runnable runnable;
        private final LinearLayout frame;
        private final ToggleButton rtsBtn, ctsBtn, dtrBtn, dsrBtn, cdBtn, riBtn;

        ControlLines(View view) {
            mainLooper = new Handler(Looper.getMainLooper());
            runnable = this::run; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks

            frame = view.findViewById(R.id.controlLines);
            rtsBtn = view.findViewById(R.id.controlLineRts);
            ctsBtn = view.findViewById(R.id.controlLineCts);
            dtrBtn = view.findViewById(R.id.controlLineDtr);
            dsrBtn = view.findViewById(R.id.controlLineDsr);
            cdBtn = view.findViewById(R.id.controlLineCd);
            riBtn = view.findViewById(R.id.controlLineRi);
            rtsBtn.setOnClickListener(this::toggle);
            dtrBtn.setOnClickListener(this::toggle);
        }

        private void toggle(View v) {
            ToggleButton btn = (ToggleButton) v;
            if (connected != Connected.True) {
                btn.setChecked(!btn.isChecked());
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            String ctrl = "";
            try {
                if (btn.equals(rtsBtn)) { ctrl = "RTS"; usbSerialPort.setRTS(btn.isChecked()); }
                if (btn.equals(dtrBtn)) { ctrl = "DTR"; usbSerialPort.setDTR(btn.isChecked()); }
            } catch (IOException e) {
                status("set" + ctrl + " failed: " + e.getMessage());
            }
        }

        private void run() {
            if (connected != Connected.True)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getControlLines();
                rtsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RTS));
                ctsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CTS));
                dtrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DTR));
                dsrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DSR));
                cdBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CD));
                riBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RI));
                mainLooper.postDelayed(runnable, refreshInterval);
            } catch (IOException e) {
                status("getControlLines() failed: " + e.getMessage() + " -> stopped control line refresh");
            }
        }

        void start() {
            frame.setVisibility(View.VISIBLE);
            if (connected != Connected.True)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
                if (!controlLines.contains(UsbSerialPort.ControlLine.RTS)) rtsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CTS)) ctsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DTR)) dtrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DSR)) dsrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CD))   cdBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.RI))   riBtn.setVisibility(View.INVISIBLE);
                run();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "getSupportedControlLines() failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        void stop() {
            frame.setVisibility(View.GONE);
            mainLooper.removeCallbacks(runnable);
            rtsBtn.setChecked(false);
            ctsBtn.setChecked(false);
            dtrBtn.setChecked(false);
            dsrBtn.setChecked(false);
            cdBtn.setChecked(false);
            riBtn.setChecked(false);
        }
    }

}