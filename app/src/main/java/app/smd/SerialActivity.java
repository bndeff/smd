package app.smd;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SerialActivity extends AppCompatActivity {

    private PersistedProjectList pl = null;
    private Button btnRefresh;
    private Button btnUpload;
    private Button btnDownload;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private TextView txtCommStatus;
    private TextView txtUploadProject;
    private TextView txtDownloadProject;
    private UsbManager usbManager;
    private UsbDeviceConnection usbConnection;
    private UsbSerialPort usbPort;
    private BroadcastReceiver broadcastReceiver;
    private int deviceStatus = 0;
    private boolean askPermission = true;

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    public static final int ST_UNKNOWN = 0;
    public static final int ST_NO_DEVICE = 1;
    public static final int ST_NO_PERMISSION = 2;
    public static final int ST_PENDING_PERMISSION = 3;
    public static final int ST_CONNECTING = 4;
    public static final int ST_CONNECTION_FAILED = 5;
    public static final int ST_CONNECTED = 6;
    public static final int ST_SENDING = 7;
    public static final int ST_RECEIVING = 8;
    public static final int ST_HANDSHAKE_FAILED = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial);
        pl = new PersistedProjectList(this);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnDownload = (Button) findViewById(R.id.btnDownload);
        btnPrev = (ImageButton) findViewById(R.id.btnPrev);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        txtCommStatus = (TextView) findViewById(R.id.txtCommStatus);
        txtUploadProject = (TextView) findViewById(R.id.txtUploadProject);
        txtDownloadProject = (TextView) findViewById(R.id.txtDownloadProject);
        btnRefresh.setOnClickListener(v -> refreshDevice());
        btnUpload.setOnClickListener(v -> uploadProject());
        btnDownload.setOnClickListener(v -> downloadProject());
        btnPrev.setOnClickListener(v -> prevProject());
        btnNext.setOnClickListener(v -> nextProject());

        deviceStatus = 0;
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                if (action.equals(INTENT_ACTION_GRANT_USB)) {
                    unregisterReceiver(broadcastReceiver);
                    askPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    refreshDevice();
                }
            }
        };

        refreshUI();
        refreshDevice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pl != null) pl.persistState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pl != null) pl.loadState();
        refreshUI();
        refreshDevice();
    }

    private String statusString(int status) {
        switch (status) {
            case ST_NO_DEVICE:
                return "No device found";
            case ST_NO_PERMISSION:
                return "No permission";
            case ST_PENDING_PERMISSION:
                return "Waiting for permission";
            case ST_CONNECTING:
                return "Connecting";
            case ST_CONNECTION_FAILED:
                return "Connection failed";
            case ST_CONNECTED:
                return "Connected";
            case ST_SENDING:
                return "Sending";
            case ST_RECEIVING:
                return "Receiving";
            case ST_HANDSHAKE_FAILED:
                return "Handshake failed";
            default:
                return "Unknown";
        }
    }

    private void setDeviceStatus(int status) {
        runOnUiThread(() -> {
            deviceStatus = status;
            txtCommStatus.setText(statusString(status));
            btnRefresh.setEnabled(status != ST_CONNECTING && status != ST_SENDING &&
                    status != ST_RECEIVING && status != ST_PENDING_PERMISSION);
            btnUpload.setEnabled(status == ST_CONNECTED);
            btnDownload.setEnabled(status == ST_CONNECTED);
        });
    }

    private void refreshDevice() {
        new Thread(() -> {
            setDeviceStatus(ST_CONNECTING);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
            if (availableDrivers.isEmpty()) {
                setDeviceStatus(ST_NO_DEVICE);
                return;
            }
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDevice device = driver.getDevice();
            usbConnection = usbManager.openDevice(device);
            if (usbConnection == null) {
                if (!usbManager.hasPermission(device)) {
                    if (askPermission) {
                        setDeviceStatus(ST_PENDING_PERMISSION);
                        registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
                        PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this,
                                0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                        usbManager.requestPermission(device, usbPermissionIntent);
                    } else {
                        setDeviceStatus(ST_NO_PERMISSION);
                    }
                } else {
                    setDeviceStatus(ST_CONNECTION_FAILED);
                }
                return;
            }
            usbPort = driver.getPorts().get(0);
            try {
                usbPort.open(usbConnection);
                usbPort.setParameters(9600, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                setDeviceStatus(ST_CONNECTION_FAILED);
                return;
            }
            if (serialRequest("ping\n", 16).equals("pong:73743031\n")) {
                setDeviceStatus(ST_CONNECTED);
            } else {
                setDeviceStatus(ST_HANDSHAKE_FAILED);
            }
        }).start();
    }

    private String serialRequest(String request, int maxLen) {
        try {
            byte[] buffer = new byte[maxLen];
            for(int i=0; i<10; ++i) {
                if(usbPort.read(buffer, 20) == 0) break;
            }
            setDeviceStatus(ST_SENDING);
            usbPort.write(request.getBytes("UTF-8"), 3000);
            setDeviceStatus(ST_RECEIVING);
            int bufLen = usbPort.read(buffer, 3000);
            return (new String(buffer, "UTF-8")).substring(0, bufLen);
        } catch (IOException e) {
            return "error:io";
        }
    }

    private void uploadProject() {
        new Thread(() -> {
            if (deviceStatus != ST_CONNECTED) return;
            StateMachine sm = pl.getMachine();
            if (serialRequest(sm.getProgram() + "\n", 16).equals("ok\n")) {
                setDeviceStatus(ST_CONNECTED);
            } else {
                setDeviceStatus(ST_HANDSHAKE_FAILED);
            }
            refreshUI();
        }).start();
    }

    private void downloadProject() {
        new Thread(() -> {
            if (deviceStatus != ST_CONNECTED) return;
            String program = serialRequest("save\n", 2050).trim();
            if (program.startsWith(StateMachine.magic)) {
                setDeviceStatus(ST_CONNECTED);
                pl.importProject(genProjectName(), program);
            } else {
                setDeviceStatus(ST_HANDSHAKE_FAILED);
            }
            refreshUI();
        }).start();
    }

    private void prevProject() {
        int sel = pl.getSelIndex() - 1;
        if (sel < 0) sel = pl.getProjectCount() - 1;
        pl.selectProject(sel);
        refreshUI();
    }

    private void nextProject() {
        int sel = pl.getSelIndex() + 1;
        if (sel >= pl.getProjectCount()) sel = 0;
        pl.selectProject(sel);
        refreshUI();
    }

    private void refreshUI() {
        runOnUiThread(() -> {
            StateMachine sm = pl.getMachine();
            txtUploadProject.setText(sm.getName());
            txtDownloadProject.setText(genProjectName());
        });
    }

    private String genProjectName() {
        return String.format(Locale.US, "Project #%d", pl.getProjectCount() + 1);
    }

}