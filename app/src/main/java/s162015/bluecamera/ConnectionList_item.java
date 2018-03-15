package s162015.bluecamera;

import android.bluetooth.BluetoothDevice;

/**
 * Created by s162161 on 2017/01/10.
 */

public class ConnectionList_item {
    public String mName,mAddress;
    int icon = R.drawable.denpa_p;
    BluetoothDevice device;
    ConnectionList_item(BluetoothDevice de, String str, String str2){
        device = de;
        mName = str;mAddress = str2;
    }
    ConnectionList_item(BluetoothDevice de, String str, String str2, int num){
        device = de;
        mName = str;mAddress = str2;icon = num;
    }

    public BluetoothDevice getDevice(){
        return device;
    }
}
