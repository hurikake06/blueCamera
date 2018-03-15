package s162015.bluecamera;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by s162015 on 2017/01/16.
 */

public class SocketReadWrite {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private DataReceptionListener dataReceptionListener;
    private DisConnectCallBack disConnectCallBack;
    private BluetoothSocket mSocket;
    private BluetoothDevice mDevice;
    private boolean isConnecting = true;

    public interface DataReceptionListener{
        void onDataRecepted(SendObject object,SocketReadWrite socketReadWrite);
    }
    public interface DisConnectCallBack{
        void onDisConnect(SocketReadWrite socketReadWrite);
    }
    public interface DataSendCallBack{
        void onDataSend();
    }
    SocketReadWrite(BluetoothDevice device){
        mDevice = device;
    }
    SocketReadWrite(BluetoothSocket socket,@NonNull DataReceptionListener dataReceptionListener,@NonNull DisConnectCallBack disConnectCallBack){
        set(socket,dataReceptionListener,disConnectCallBack);
    }
    public void set(BluetoothSocket socket, @NonNull DataReceptionListener dataReceptionListener,@NonNull DisConnectCallBack disConnectCallBack){
        mSocket = socket;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.dataReceptionListener = dataReceptionListener;
        this.disConnectCallBack = disConnectCallBack;
    }
    private Thread dataReceptThread;
    public void start(){
        Log.i("blueMessage","recept new Thread");
        if(dataReceptThread == null) {
            dataReceptThread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    while (isConnecting) {
                        try {
                            dataReceptionListener.onDataRecepted((SendObject) in.readObject(),SocketReadWrite.this);
                        } catch (ClassNotFoundException ignored) {
                            ignored.printStackTrace();
                        } catch (ClassCastException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (e.getMessage().startsWith("bt socket closed") || e.getMessage().startsWith("Software caused connection abort")) {
                                disConnectCallBack.onDisConnect(SocketReadWrite.this);
                                break;
                            }
                        }
                    }
                    Log.i("socket Recept","end");
                }
            };
            dataReceptThread.start();
        }
    }
    Thread thread;
    public void send(final SendObject obj){
        if(count == 0) {
            if (thread == null || !thread.isAlive()) {
                thread = new Thread() {
                    public void run() {
                        sendObject(obj);
                    }
                };
                thread.start();
            }
        }
    }
    public void send(SendObject.Type type,Object data){
        send(new SendObject(type,data));
    }
    private int count = 0;
    public void sendPriority(final SendObject obj){
        new Thread(){
            public void run() {
                Log.i("socket send","priority");
                count++;
                sendObject(obj);
                Log.i("socket send","priority end");
                count--;
            }
        }.start();
    }

    public void sendPriority(SendObject.Type type,Object data){
        sendPriority(new SendObject(type,data));
    }

    public void sendPriority(final SendObject.Type type, final Object data, @NonNull final DataSendCallBack dataSendCallBack){
        sendPriority(new SendObject(type,data),dataSendCallBack);
    }

    public void sendPriority(final SendObject obj, @NonNull final DataSendCallBack dataSendCallBack){
        new Thread(){
            public void run() {
                count++;
                sendObject(obj);
                count--;
                dataSendCallBack.onDataSend();
            }
        }.start();
    }

    private synchronized void sendObject(final SendObject obj){
        if(out != null) {
            try {
                Log.i("blueMessage", "send");
                out.writeObject(obj);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void cancel(){
        try {
            isConnecting = false;
            if(mSocket != null) mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public String toString(){
        return mDevice.getName();
    }

    public BluetoothDevice getDevice(){
        return mDevice;
    }
}