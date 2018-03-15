package s162015.bluecamera;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CameraActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private FrameLayout preview;
    CameraPreview mCameraPreview;
    private SocketReadWrite mSocketReadWrite;
    BluetoothServerThread mServerThread;
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            mSocketReadWrite.sendPriority(SendObject.Type.picture, bytes);
            isTaking = false;
            camera.startPreview();
        }
    };

    private boolean isPreviewSend = false;
    private boolean isTaking = false;
    private int jpgQuality = 80;

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
            //mCameraPreview.onePreview();
            mCameraPreview.takePicture(pictureCallback);
            Log.i("CameraActivity","autoFocus");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        findViewById(R.id.button_re).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                waiting();
            }
        });
        preview = (FrameLayout)findViewById(R.id.preview_camera);

        if(Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{"android.permission.CAMERA"}, 0);
        }

        mCameraPreview = new CameraPreview(CameraActivity.this);
        mCameraPreview.setOnAutoFocusCallback(mAutoFocusCallback);
        mCameraPreview.setOnPreviewListener(new CameraPreview.OnPreviewListener() {
            long currentTimeMillis = System.currentTimeMillis();
            @Override
            public void onGetBmp(Bitmap bmp) {
                if(mSocketReadWrite != null) {
                        long nowTimeMillis = System.currentTimeMillis();
                        if (nowTimeMillis > currentTimeMillis + 20) {
                            if (isPreviewSend) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bmp.compress(Bitmap.CompressFormat.JPEG, jpgQuality, baos);
                                byte[] bytes = baos.toByteArray();
                                mSocketReadWrite.send(SendObject.Type.preview, bytes);
                                Log.i("blueMessage", "preview size:" + bytes.length);
                            }
                            currentTimeMillis = nowTimeMillis;
                        }
                    }
                }
        });
        /*
        mCameraPreview.setOnePreviewCallBack(new CameraPreview.OnePreviewCallBack() {
            @Override
            public void getYUV(byte[] data, int width, int height) {
                Bitmap bmp = CameraPreview.getBitmapImageFromYUV(data,width,height,60);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] bytes = baos.toByteArray();
                mSocketReadWrite.sendPriority(SendObject.Type.picture, bytes);
                isTaking = false;
            }
        });
        */
        preview.addView(mCameraPreview);
        //serverStart();
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
        Log.i("blueMessage","onStart");
        super.onStart();
        serverStart();
    }

    private void serverStart(){
        serverStop();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mServerThread = new BluetoothServerThread(
                CameraActivity.this, "BluetoothSample", mBluetoothAdapter,
                new BluetoothServerThread.ReceivedSocketListener() {
                    @Override
                    public void setSocket(final BluetoothSocket receivedSocket) {
                        Log.i("blueMessage","socket Recieve");
                        socketSet(receivedSocket);
                    }
                });
        mServerThread.start();
        Log.i("blueMessage","Server start");
    }

    @Override
    protected void onResume(){
        Log.i("blueMessage","onResume");
        super.onResume();
    }

    @Override
    protected void onPause(){
        Log.i("blueMessage","onPause");
        super.onPause();
        //serverStop();
    }

    @Override
    protected void onDestroy(){
        Log.i("blueMessage","onDestroy");
        super.onDestroy();
        //serverStop();
    }

    @Override
    protected void onStop(){
        Log.i("blueMessage","onStop");
        super.onStop();
        serverStop();
    }

    private void serverStop(){
        if(mServerThread != null) {
            mServerThread.cancel();
            Log.i("blueMessage","Server cancel");
        }
        mServerThread = null;
        if(mSocketReadWrite != null){
            mSocketReadWrite.cancel();
        }
    }

    private Handler handler = new Handler();


    private void socketSet(final BluetoothSocket socket){
        mSocketReadWrite = new SocketReadWrite(socket,
                new SocketReadWrite.DataReceptionListener() {
                    @Override
                    public void onDataRecepted(SendObject object,SocketReadWrite socketReadWrite) {
                        dataRecept(object);
                    }
                },
                new SocketReadWrite.DisConnectCallBack() {
                    @Override
                    public void onDisConnect(SocketReadWrite socketReadWrite) {
                        Log.i("blueMessage","disConnect");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(CameraActivity.this,"切断されました",Toast.LENGTH_SHORT);
                            }
                        });
                        isPreviewSend = false;
                        serverStart();
                    }
                });
        mSocketReadWrite.start();
    }

    private void dataRecept(SendObject object){
        switch (object.type) {
            case message:
                messageRecept((Info.Message) object.data);
                break;
            default:
                Log.i("blueMessage","default");
        }
    }

    private void messageRecept(Info.Message data){
        switch(data){
            case startPreview:
                isPreviewSend = true;
                break;
            case takePicture:
                if(!isTaking) {
                    mCameraPreview.autoFocus();
                    isTaking = true;
                }
                break;
            case endPreview:
                isPreviewSend = false;
                break;
        }
        Log.i("blueMessage","message - " + data.toString());
    }
    //自信がデバイス検知されるのを有効化
    private void waiting(){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(intent);
    }

    //ファイルのデータを取得
    public byte[] file2byteArray(File tempFile){
        final long fileSize = tempFile.length();
        final int byteSize = (int) fileSize;
        byte[] bytes = new byte[byteSize];
        try {
            RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
            try {
                raf.readFully(bytes);
            } finally {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}