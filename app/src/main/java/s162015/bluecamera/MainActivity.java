package s162015.bluecamera;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    Class connect_mode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            ((ImageView)findViewById(R.id.imageButton_Bluetooth)).setImageResource(R.drawable.button_off);
            Toast.makeText(this,"adapter is null",Toast.LENGTH_SHORT).show();
        }
        View buttonControl = findViewById(R.id.button_control);
        buttonControl.setOnClickListener(new Button_Connect());
        View buttonCamera = findViewById(R.id.button_camera);
        buttonCamera.setOnClickListener(new Button_Connect());


        findViewById(R.id.imageButton_Bluetooth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled()) {
                        toastShow("off");
                        mBluetoothAdapter.disable();
                        setImageBlueToothEnable(false);
                    } else {
                        toastShow("on");
                        mBluetoothAdapter.enable();
                        setImageBlueToothEnable(true);
                    }
                }
            }
        });
    }
    private Toast toast;
    private void toastShow(String text){
        if(toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(this,text, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("permission", "permitted");
                } else {
                    Log.i("permission", "not permitted");
                }
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    Intent intent = new Intent(MainActivity.this,connect_mode);
                    startActivity(intent);
                } else {
                    // User did not enable Bluetooth or an error occured
                }
                break;
            default:
                break;
        }
    }

    class Button_Connect implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_control:
                    connect_mode = RimoconActivity.class;
                    break;
                case R.id.button_camera:
                    connect_mode = CameraActivity.class;
                    break;
                default:
                    Toast.makeText(MainActivity.this, "Button_Error", Toast.LENGTH_SHORT).show();
                    break;
            }

            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, connect_mode);
                startActivity(intent);
            } else {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, 1);
            }
        }
    }

    private void checkBluetooth(){
        if(mBluetoothAdapter != null) {
            setImageBlueToothEnable(mBluetoothAdapter.isEnabled());
        }else{
            ((ImageView) findViewById(R.id.imageButton_Bluetooth)).setImageResource(R.drawable.button_off);
        }
    }
    private void setImageBlueToothEnable(boolean enable){
        if(enable) ((ImageView) findViewById(R.id.imageButton_Bluetooth)).setImageResource(R.drawable.button_on);
            else ((ImageView) findViewById(R.id.imageButton_Bluetooth)).setImageResource(R.drawable.button_off);
    }
}
