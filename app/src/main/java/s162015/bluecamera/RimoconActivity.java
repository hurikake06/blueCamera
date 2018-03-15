
package s162015.bluecamera;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.ACTION_NAME_CHANGED;

public class RimoconActivity extends AppCompatActivity {
    public static File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath()+"/bluetooth");

    private ComentAdapter mComentAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mReceiver;
    private SocketReadWrite nowSocketReadWrite;
    private ArrayList<SocketReadWrite> mSocketReadWriteList = new ArrayList();
    private SocketAdapter mSocketAdapter;
    private ArrayList<ConnectionList_item> mConnectionList = new ArrayList();

    private ImageView preview;
    private boolean isTaking = false;

    private Bitmap mImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("blueMessage","onCreate - Rimocon");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rimocon);
        mComentAdapter = new ComentAdapter(this);
        mSocketAdapter = new SocketAdapter(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mComentAdapter.connectionList.addAll(getPairedDevices());
        setLayout(Layout.selectLayout);
        setmReceiver();
        nowSocketReadWrite = null;

        if(!dir.exists()){
            dir.mkdirs();
        }

    }

    private Handler handler = new Handler();

    private enum Layout{
        selectLayout,previewLayout
    }

    private void changeLayout(final Layout layout){
        handler.post(new Runnable() {
            @Override
            public void run() {
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view;
                switch(layout){
                    case previewLayout:
                        view = layoutInflater.inflate(R.layout.activity_preview_rimocon,null,false);
                        setContentView(view);
                        setLayout(layout);
                        break;
                    case selectLayout:
                        view = layoutInflater.inflate(R.layout.activity_rimocon,null,false);
                        setContentView(view);
                        setLayout(layout);
                        break;
                }
            }
        });
    }

    private void setLayout(final Layout layout){
        switch(layout){
            case selectLayout:
                final ListView listView = ((ListView)findViewById(R.id.ListView_Connect));
                listView.setAdapter(mComentAdapter);
                
                findViewById(R.id.button_scan).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doDiscovery();
                    }
                });

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        /*
                        ConnectionList_item item = mComentAdapter.connectionList.get(position);
                        BluetoothClientThread clientThread = new BluetoothClientThread(RimoconActivity.this,
                                item.mName, item.device, mBluetoothAdapter, new BluetoothClientThread.ReceivedSocketListener() {
                            @Override
                            public void setSocket(final BluetoothSocket receivedSocket) {
                                Log.i("Connection1","socket Recieve");
                                changeLayout(Layout.previewLayout);
                                socketSet(receivedSocket);
                                mSocketReadWrite.sendPriority(SendObject.Type.message,Info.Message.startPreview);
                            }
                        });
                        clientThread.start();
                        */
                        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
                        if(checkedItemPositions.get(position)){
                            view.findViewById(R.id.back_connection_item).setBackgroundResource(R.color.multipleSelectOn);
                        }else{
                            view.findViewById(R.id.back_connection_item).setBackgroundResource(R.color.multipleSelectOff);
                        }
                    }
                });

                findViewById(R.id.button_startTaking).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ArrayList<SocketReadWrite> list = new ArrayList();
                        SparseBooleanArray checkedArray = listView.getCheckedItemPositions();
                        for(int i = 0;i < checkedArray.size();i++){
                            int at = checkedArray.keyAt(i);
                            if(checkedArray.get(at)){
                                list.add(new SocketReadWrite(((ConnectionList_item) listView.getItemAtPosition(at)).getDevice()));
                            }
                        }
                        int size = list.size();
                        if(size == 0){
                            showToast(R.string.select_device);
                        }else{
                            mSocketReadWriteList = list;
                            changeLayout(Layout.previewLayout);
                        }
                    }
                });
                break;
            case previewLayout:
                preview = (ImageView) findViewById(R.id.preview_rimocon);
                findViewById(R.id.button_shutter).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bloadCast(SendObject.Type.message,Info.Message.takePicture);
                        isTaking = true;
                    }
                });
                ListView listView_socket = (ListView)findViewById(R.id.socket_list);
                listView_socket.setAdapter(mSocketAdapter);
                listView_socket.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        changePreview((SocketReadWrite)adapterView.getItemAtPosition(i));
                        showToast((adapterView.getItemAtPosition(i)).toString());
                    }
                });

                startPreview();
                break;
        }
    }

    private void startPreview(){
        for (final SocketReadWrite readWriteSocket:mSocketReadWriteList){
            BluetoothDevice device = readWriteSocket.getDevice();
            BluetoothClientThread clientThread = new BluetoothClientThread(RimoconActivity.this,
                    device.getName(), device, mBluetoothAdapter, new BluetoothClientThread.ReceivedSocketListener() {
                @Override
                public void setSocket(final BluetoothSocket receivedSocket) {
                    Log.i("Connection1","socket Recieve");
                    changeLayout(Layout.previewLayout);
                    socketSet(readWriteSocket,receivedSocket);
                    mSocketAdapter.addItem(readWriteSocket);
                    if(!nowSocketSet(readWriteSocket))readWriteSocket.sendPriority(SendObject.Type.message,Info.Message.endPreview);
                }
            });
            clientThread.start();
        }
    }
    private synchronized boolean nowSocketSet(SocketReadWrite temp){
        if(nowSocketReadWrite == null){
            changePreview(temp);
            return true;
        }else{
            return false;
        }
    }
    private void bloadCast(SendObject.Type type,Object data){
        bloadCast(new SendObject(type,data));
    }
    private void bloadCast(SendObject sendObject){
        for(SocketReadWrite obj:mSocketReadWriteList){
            if(obj != null) {
                obj.sendPriority(sendObject);
            }
        }
    }

    private SocketReadWrite socketSet(SocketReadWrite mSocketReadWrite,final BluetoothSocket socket){
        mSocketReadWrite.set(socket,
                new SocketReadWrite.DataReceptionListener() {
                    @Override
                    public void onDataRecepted(SendObject object,SocketReadWrite socketReadWrite) {
                        dataRecept(object,socketReadWrite);
                    }
                },
                new SocketReadWrite.DisConnectCallBack() {
                    @Override
                    public void onDisConnect(SocketReadWrite socketReadWrite) {
                        socketReadWrite.cancel();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RimoconActivity.this,"切断されました",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
        mSocketReadWrite.start();
        return mSocketReadWrite;
    }

    private void dataRecept(SendObject object,SocketReadWrite socketReadWrite){
        switch (object.type) {
            case message:
                messageRecept((Info.Message) object.data,socketReadWrite);
                break;
            case preview:
                Log.i("blueMessage","preview");
                previewRecept((byte[])object.data,socketReadWrite);
                break;
            case picture:
                pictureRecept((byte[])object.data,socketReadWrite);
                break;
            default:
                Log.i("blueMessage","default");
        }
    }

    private void messageRecept(Info.Message data,SocketReadWrite socketReadWrite){
        Log.i("blueMessage","message - " + data.toString());
    }

    public void changePreview(@NonNull final SocketReadWrite nextSocket){
        if(nowSocketReadWrite != null) {
            nowSocketReadWrite.sendPriority(SendObject.Type.message, Info.Message.endPreview);
        }
        nextSocket.sendPriority(SendObject.Type.message,Info.Message.startPreview);
        nowSocketReadWrite = nextSocket;
        handler.post(new Runnable() {
            @Override
            public void run() {
                setText(R.id.textView,nextSocket.toString());
            }
        });
    }

    private synchronized void previewRecept(final byte[] bytes,SocketReadWrite socketReadWrite){
        if (bytes != null && preview != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(mImage != null){
                        mImage.recycle();
                    }
                    mImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    preview.setImageBitmap(mImage);
                }
            });
        }
    }

    private void pictureRecept(final byte[] bytes,SocketReadWrite socketReadWrite){
        /*if(mImage != null){
            mImage.recycle();
        }
        mImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mImage = CameraPreview.rotateBitmap(mImage,mImage.getWidth(),mImage.getHeight(),90);
        handler.post(new Runnable() {
            @Override
            public void run() {
                preview.setImageBitmap(mImage);
                showToast("pictureRecept");
            }
        });
        */
        new SaveThread(bytes,socketReadWrite.toString()).start();
    }

    private class SaveThread extends Thread{
        private byte[] bytes;
        private String name;

        @Override
        public void run(){
            String path;
            try {
                path = saveJPEG(bytes);
                registAndroidDB(path);
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showToast("保存が失敗しました");
                    }
                });
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showToast(name + " - save");
                }
            });

        }
        SaveThread(byte[] bytes,String name){
            this.bytes = bytes;
            this.name = name;
        }
    }
    public synchronized String saveJPEG(byte[] bytes) throws IOException {
        final String path = dir.getPath()+ "/" + "img_" + System.currentTimeMillis()+".jpg";
        FileOutputStream os;
        os = new FileOutputStream(path,true);
        os.write(bytes);
        os.close();

        return path;
    }
    private void registAndroidDB(String path) {
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put("_data", path);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private Toast toast;
    private void showToast(String comment){
        if(toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(this,comment,Toast.LENGTH_LONG);
        toast.show();
    }

    private void showToast(@StringRes int resId){
        showToast(getString(resId));
    }

    @Override
    public void onPause(){
        Log.i("blueMessage","onPause");
        super.onPause();
        //cancel();
    }
    @Override
    protected void onStop(){
        Log.i("blueMessage","onStop");
        super.onStop();
        //cancel();
    }
    @Override
    protected void onDestroy(){
        Log.i("blueMessage","onDestroy");
        super.onDestroy();
        cancel();
    }

    private void cancel(){
        for(SocketReadWrite obj:mSocketReadWriteList){
            if(obj != null) {
                obj.cancel();
            }
        }
    }

    //ペアリング済みのデバイスを表示
    private ArrayList<ConnectionList_item> getPairedDevices(){
        ArrayList<ConnectionList_item> deviceList = new ArrayList();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(new ConnectionList_item(device,device.getName(),device.getAddress()));
            }
        } else {
            showToast("noDevice");
        }
        return deviceList;
    }
    //デバイスの検知
    private void doDiscovery() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FOUND);
        filter.addAction(ACTION_DISCOVERY_FINISHED);
        //filter.addAction(ACTION_NAME_CHANGED);
        registerReceiver(mReceiver, filter);

        mConnectionList = new ArrayList();
        setText(R.id.button_scan,R.string.scanning);
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            setText(R.id.button_scan,R.string.cancel);
        }

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }
    //キャストの結果もろもろの受付
    private void setmReceiver(){
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // If it's already paired, skip it, because it's been listed already
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        mConnectionList.add(new ConnectionList_item(device,device.getName(),device.getAddress(),R.drawable.denpa_n));
                    }
                    // When discovery is finished, change the Activity title
                } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                    if (mConnectionList.size() == 0) {
                        showToast("no Device");
                    }else{
                        showToast(Integer.toString(mConnectionList.size()));
                        mComentAdapter.connectionList.addAll(mConnectionList);
                        mComentAdapter.notifyDataSetChanged();
                    }
                    setText(R.id.button_scan,R.string.rescan);
                }
            }
        };
    }

    public void setText(@IdRes int id, @StringRes int string){
        setText(id,getString(string));
    }

    public void setText(@IdRes int id,String string){
        TextView view = (TextView) findViewById(id);
        if(view != null){
            view.setText(string);
        }
    }

    class ComentAdapter extends BaseAdapter {
        Context context;
        LayoutInflater layoutInflater = null;
        public ArrayList<ConnectionList_item> connectionList;

        ComentAdapter(Context context){
            this.context = context;
            this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            this.connectionList = new ArrayList();
        }
        @Override
        public int getCount() {
            return connectionList.size();
        }

        @Override
        public Object getItem(int position) {
            return connectionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            convertView = layoutInflater.inflate(R.layout.connection_item_layout,parent,false);
            ConnectionList_item temp = (ConnectionList_item) getItem(position);

            ((TextView)convertView.findViewById(R.id.textView)).setText(temp.mName);
            ((ImageView)convertView.findViewById(R.id.imageView)).setImageResource(temp.icon);

            return convertView;
        }
    }
    class SocketAdapter extends BaseAdapter{
        Context context;
        LayoutInflater layoutInflater = null;
        ArrayList<SocketReadWrite> socketList;
        SocketAdapter(Context context){
            this.context = context;
            this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            this.socketList = new ArrayList();
        }
        @Override
        public int getCount() {
            return socketList.size();
        }

        @Override
        public Object getItem(int position) {
            return socketList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public synchronized void addItem(SocketReadWrite temp){
            socketList.add(temp);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = layoutInflater.inflate(R.layout.socket_item_layout,parent,false);
            SocketReadWrite temp = (SocketReadWrite) getItem(position);

            ((TextView)convertView.findViewById(R.id.socket_name)).setText(temp.toString());

            return convertView;
        }
    }
}
