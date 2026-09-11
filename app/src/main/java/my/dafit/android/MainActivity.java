package my.dafit.android;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private final Map<String,BluetoothDevice> devices = new LinkedHashMap<>();
    private final Handler handler = new Handler();
    private TextView status, log;
    private LinearLayout deviceList, gattList;
    private Button scanBtn, disconnectBtn;

    private static final UUID CCCD =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice d=result.getDevice();
            String name="Tanpa nama";
            if(result.getScanRecord()!=null && result.getScanRecord().getDeviceName()!=null)
                name=result.getScanRecord().getDeviceName();
            else {
                try { if(d.getName()!=null) name=d.getName(); } catch(SecurityException ignored){}
            }
            addDevice(d,name,result.getRssi());
        }
        @Override public void onScanFailed(int code) {
            setStatus("Scan gagal: "+code);
            addLog("SCAN FAILED: "+code);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        BluetoothManager bm=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        adapter=bm.getAdapter();
        requestPermissionsIfNeeded();
        if(adapter==null) {
            setStatus("Bluetooth tidak disokong");
            scanBtn.setEnabled(false);
        }
    }

    private void buildUi() {
        ScrollView sv=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,24,24,24);
        sv.addView(root);

        TextView title=new TextView(this);
        title.setText("Dafit Android v1.3");
        title.setTextSize(26);
        root.addView(title);

        TextView sub=new TextView(this);
        sub.setText("GOOJODOQ FB033 — BLE Scanner & GATT");
        sub.setTextSize(16);
        root.addView(sub);

        status=new TextView(this);
        status.setText("Status: Sedia");
        status.setTextSize(17);
        status.setPadding(0,16,0,10);
        root.addView(status);

        LinearLayout buttons=new LinearLayout(this);
        scanBtn=new Button(this);
        scanBtn.setText("SCAN BLE");
        scanBtn.setOnClickListener(v->startScan());
        buttons.addView(scanBtn,new LinearLayout.LayoutParams(0,-2,1));

        disconnectBtn=new Button(this);
        disconnectBtn.setText("PUTUS");
        disconnectBtn.setEnabled(false);
        disconnectBtn.setOnClickListener(v->disconnect());
        buttons.addView(disconnectBtn,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(buttons);

        heading(root,"Peranti BLE ditemui");
        deviceList=new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        root.addView(deviceList);

        heading(root,"GATT Services / Characteristics");
        gattList=new LinearLayout(this);
        gattList.setOrientation(LinearLayout.VERTICAL);
        root.addView(gattList);

        heading(root,"Log");
        log=new TextView(this);
        log.setTextSize(12);
        log.setTypeface(android.graphics.Typeface.MONOSPACE);
        root.addView(log);

        setContentView(sv);
    }

    private void heading(LinearLayout p,String s) {
        TextView t=new TextView(this);
        t.setText(s); t.setTextSize(19); t.setPadding(0,20,0,8); p.addView(t);
    }

    private boolean hasBlePermission() {
        if(Build.VERSION.SDK_INT>=31)
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsIfNeeded() {
        if(hasBlePermission()) return;
        if(Build.VERSION.SDK_INT>=31)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},101);
        else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},101);
    }

    @SuppressWarnings("MissingPermission")
    private void startScan() {
        if(!hasBlePermission()){ requestPermissionsIfNeeded(); return; }
        if(adapter==null) return;
        if(!adapter.isEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            setStatus("Sila hidupkan Bluetooth");
            return;
        }
        stopScan();
        devices.clear();
        deviceList.removeAllViews();
        gattList.removeAllViews();
        if(log!=null) log.setText("");
        scanner=adapter.getBluetoothLeScanner();
        if(scanner==null){setStatus("BLE scanner tidak tersedia");return;}
        scanner.startScan(scanCallback);
        setStatus("Scanning BLE…");
        addLog("SCAN START");
        handler.postDelayed(()->stopScan(),15000);
    }

    @SuppressWarnings("MissingPermission")
    private void stopScan() {
        if(scanner!=null){scanner.stopScan(scanCallback);scanner=null;}
        if(status!=null && (gatt==null)) setStatus("Scan tamat — pilih peranti");
    }

    private void addDevice(BluetoothDevice d,String name,int rssi) {
        runOnUiThread(()->{
            String addr;
            try{addr=d.getAddress();}catch(SecurityException e){return;}
            if(devices.containsKey(addr)) return;
            devices.put(addr,d);

            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(12,8,12,8);

            TextView tv=new TextView(this);
            tv.setText(name+"\n"+addr+"\nRSSI: "+rssi+" dBm");
            tv.setTextSize(15);
            row.addView(tv);

            Button c=new Button(this);
            String n=name.toLowerCase(Locale.getDefault());
            c.setText((n.contains("fb033")||n.contains("goojodoq")||n.contains("gts")||n.contains("watch"))
                ?"SAMBUNG FB033":"SAMBUNG");
            c.setOnClickListener(v->connect(d,name));
            row.addView(c);
            deviceList.addView(row);
        });
    }

    @SuppressWarnings("MissingPermission")
    private void connect(BluetoothDevice d,String name) {
        stopScan();
        disconnect();
        setStatus("Menyambung: "+name);
        addLog("CONNECT -> "+name);
        if(Build.VERSION.SDK_INT>=23)
            gatt=d.connectGatt(this,false,gattCallback,BluetoothDevice.TRANSPORT_LE);
        else gatt=d.connectGatt(this,false,gattCallback);
        disconnectBtn.setEnabled(true);
    }

    @SuppressWarnings("MissingPermission")
    private void disconnect() {
        if(gatt!=null){
            try{gatt.disconnect();}catch(Exception ignored){}
            try{gatt.close();}catch(Exception ignored){}
            gatt=null;
        }
        if(disconnectBtn!=null) disconnectBtn.setEnabled(false);
    }

    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int statusCode,int state){
            runOnUiThread(()->{
                if(state==BluetoothProfile.STATE_CONNECTED){
                    setStatus("CONNECTED");
                    addLog("GATT CONNECTED status="+statusCode);
                    try{g.discoverServices();}catch(SecurityException ignored){}
                }else if(state==BluetoothProfile.STATE_DISCONNECTED){
                    setStatus("DISCONNECTED");
                    addLog("GATT DISCONNECTED status="+statusCode);
                    disconnectBtn.setEnabled(false);
                }
            });
        }

        @Override public void onServicesDiscovered(BluetoothGatt g,int statusCode){
            runOnUiThread(()->{
                addLog("SERVICES DISCOVERED status="+statusCode);
                showGatt(g);
            });
        }

        @Override public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,int statusCode){
            byte[] b=c.getValue();
            runOnUiThread(()->{
                addLog("READ "+c.getUuid()+" status="+statusCode);
                addLog("HEX: "+hex(b));
                addLog("ASCII: "+ascii(b));
            });
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){
            byte[] b=c.getValue();
            runOnUiThread(()->{
                addLog("NOTIFY "+c.getUuid());
                addLog("HEX: "+hex(b));
            });
        }
    };

    @SuppressWarnings("MissingPermission")
    private void showGatt(BluetoothGatt g) {
        gattList.removeAllViews();
        for(android.bluetooth.BluetoothGattService s:g.getServices()){
            TextView st=new TextView(this);
            st.setText("SERVICE\n"+s.getUuid());
            st.setTextSize(15); st.setPadding(0,10,0,6);
            gattList.addView(st);

            for(BluetoothGattCharacteristic c:s.getCharacteristics()){
                LinearLayout row=new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                TextView info=new TextView(this);
                info.setText(c.getUuid()+"\nProperties: "+props(c.getProperties()));
                info.setTextSize(13);
                row.addView(info);

                LinearLayout actions=new LinearLayout(this);

                if((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ)!=0){
                    Button r=new Button(this);
                    r.setText("READ");
                    r.setOnClickListener(v->{
                        addLog("READ REQUEST "+c.getUuid());
                        try{g.readCharacteristic(c);}catch(SecurityException ignored){}
                    });
                    actions.addView(r,new LinearLayout.LayoutParams(0,-2,1));
                }

                if((c.getProperties() & (BluetoothGattCharacteristic.PROPERTY_NOTIFY|
                        BluetoothGattCharacteristic.PROPERTY_INDICATE))!=0){
                    Button no=new Button(this);
                    no.setText("NOTIFY ON");
                    no.setOnClickListener(v->{
                        try{
                            boolean ok=g.setCharacteristicNotification(c,true);
                            BluetoothGattDescriptor d=c.getDescriptor(CCCD);
                            if(d!=null){
                                d.setValue((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE)!=0
                                    ?BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                    :BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                g.writeDescriptor(d);
                            }
                            addLog("NOTIFY "+c.getUuid()+" local="+ok);
                        }catch(SecurityException ignored){}
                    });
                    actions.addView(no,new LinearLayout.LayoutParams(0,-2,1));
                }
                row.addView(actions);
                gattList.addView(row);
            }
        }
    }

    private String props(int p){
        StringBuilder s=new StringBuilder();
        if((p&BluetoothGattCharacteristic.PROPERTY_READ)!=0)s.append("READ ");
        if((p&BluetoothGattCharacteristic.PROPERTY_WRITE)!=0)s.append("WRITE ");
        if((p&BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)!=0)s.append("WRITE_NR ");
        if((p&BluetoothGattCharacteristic.PROPERTY_NOTIFY)!=0)s.append("NOTIFY ");
        if((p&BluetoothGattCharacteristic.PROPERTY_INDICATE)!=0)s.append("INDICATE ");
        return s.length()==0?"NONE":s.toString().trim();
    }

    private String hex(byte[] b){
        if(b==null)return "";
        StringBuilder s=new StringBuilder();
        for(byte x:b)s.append(String.format(Locale.US,"%02X ",x&255));
        return s.toString().trim();
    }

    private String ascii(byte[] b){
        if(b==null)return "";
        StringBuilder s=new StringBuilder();
        for(byte x:b){int n=x&255;s.append(n>=32&&n<=126?(char)n:'.');}
        return s.toString();
    }

    private void setStatus(String s){if(status!=null)status.setText("Status: "+s);}
    private void addLog(String s){if(log==null)return;log.append((log.length()>0?"\n":"")+s);}

    @Override protected void onDestroy(){
        stopScan(); disconnect(); super.onDestroy();
    }
}
