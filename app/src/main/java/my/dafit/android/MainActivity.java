package my.dafit.android;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("Dafit Android v1.2");
        title.setTextSize(26);

        TextView subtitle = new TextView(this);
        subtitle.setText("GOOJODOQ FB033");
        subtitle.setTextSize(18);

        status = new TextView(this);
        status.setText("Status: Sedia");
        status.setTextSize(17);

        Button scan = new Button(this);
        scan.setText("SCAN BLUETOOTH");
        scan.setOnClickListener(v -> checkBluetooth());

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(status);
        layout.addView(scan);

        setContentView(layout);
    }

    private void checkBluetooth() {

        BluetoothManager manager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        BluetoothAdapter adapter = manager.getAdapter();

        if (adapter == null) {
            status.setText("Status: Bluetooth tidak disokong");
            return;
        }

        if (!adapter.isEnabled()) {
            status.setText("Status: Bluetooth belum dihidupkan");
        } else {
            status.setText("Status: Bluetooth OK");
        }
    }
}
