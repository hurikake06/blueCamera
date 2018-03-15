package s162015.bluecamera;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by s162161 on 2017/01/12.
 */
public class BluetoothServerThread extends Thread {
    //サーバー側の処理
    //UUID：Bluetoothプロファイル毎に決められた値
    private final BluetoothServerSocket servSock;
    static BluetoothAdapter myServerAdapter;
    private Context mContext;
    //UUIDの生成
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public String myNumber;
    private ReceivedSocketListener receivedSocketListener;
    private boolean isWaiting = true;

    public interface ReceivedSocketListener{
        void setSocket(BluetoothSocket receivedSocket);
    }

    //コンストラクタの定義
    public BluetoothServerThread(Context context,String myNum, BluetoothAdapter btAdapter,@NonNull ReceivedSocketListener receivedSocketListener){
        this.receivedSocketListener = receivedSocketListener;
        //各種初期化
        mContext = context;
        BluetoothServerSocket tmpServSock = null;
        myServerAdapter = btAdapter;
        myNumber = myNum;
        try{
            //自デバイスのBluetoothサーバーソケットの取得
            tmpServSock = myServerAdapter.listenUsingRfcommWithServiceRecord("BlueToothSample03", MY_UUID);
        }catch(IOException e){
            e.printStackTrace();
        }
        servSock = tmpServSock;
    }

    public void run(){
        BluetoothSocket receivedSocket = null;
        while(isWaiting){
            if(receivedSocket != null){
                receivedSocketListener.setSocket(receivedSocket);
                try {
                    servSock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            try{
                receivedSocket = servSock.accept();
            }catch(IOException e){
                e.printStackTrace();
                break;
            }
        }
        Log.i("blueMessage","ServSock finish");
    }
    public void cancel() {
        try {
            isWaiting = false;
            servSock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}