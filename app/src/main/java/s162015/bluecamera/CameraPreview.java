package s162015.bluecamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by s162015 on 2017/01/10.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
    public final static int CAMERA_MODE = Camera.CameraInfo.CAMERA_FACING_BACK;
    public final static int BMP_QUALITY = 70;

    private Camera mCamera;
    boolean isTaken = false;

    private Camera.ShutterCallback mShutterListener = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
        }
    };
    public interface OnPreviewListener{
        void onGetBmp(Bitmap bmp);
    }
    private OnPreviewListener onPreviewListener;
    public void setOnPreviewListener(OnPreviewListener onPreviewListener){
        this.onPreviewListener = onPreviewListener;
    }
    private Camera.PreviewCallback mPreviewListener = new Camera.PreviewCallback(){
        boolean flag = true;
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            //if(flag) {
                //flag = false;
                int w = camera.getParameters().getPreviewSize().width;
                int h = camera.getParameters().getPreviewSize().height;
                Bitmap bmp = getBitmapImageFromYUV(bytes, w, h , BMP_QUALITY);
                if (onPreviewListener != null) onPreviewListener.onGetBmp(bmp);
                //flag = true;
            //}
        }
    };
    public interface OnePreviewCallBack{
        void getYUV(byte[] data, int width, int height);
    }
    private OnePreviewCallBack onePreviewCallBack = null;
    public void setOnePreviewCallBack(OnePreviewCallBack onePreviewCallBack) {
        this.onePreviewCallBack = onePreviewCallBack;
    }

    private Camera.PreviewCallback onePreviewListener = new Camera.PreviewCallback(){
        //OnShotPreview時のbyte[]が渡ってくる
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            int w = camera.getParameters().getPreviewSize().width;
            int h = camera.getParameters().getPreviewSize().height;

            if(onePreviewCallBack != null){
                onePreviewCallBack.getYUV(bytes,w,h);
            }
            cameraPreviewStart();

        }
    };
    public static Bitmap getBitmapImageFromYUV(byte[] data, int width, int height,int bmpQuority) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), bmpQuority, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return rotateBitmap(bmp,width,height,90);
    }
    static Bitmap temp;
    public synchronized static Bitmap rotateBitmap(Bitmap bmp,int width,int height,int angle){
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        if(temp != null){
            temp.recycle();
            temp = null;
        }
        temp = Bitmap.createBitmap(bmp,0,0,width,height,mat,true);
        bmp.recycle();
        return temp;
    }

    public CameraPreview(Context context){
        super(context);
        initialize();
    }

    private void initialize(){
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void cameraOpen(){
        int numberOfCameras = Camera.getNumberOfCameras();
        int cameraId = -1;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for(int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CAMERA_MODE) {
                cameraId = i;
                break;
            }
        }
        this.mCamera = Camera.open(cameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{
            cameraOpen();
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.setDisplayOrientation(90);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        cameraPreviewStart();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    public void takePicture(String name){
        if(!isTaken) {
            isTaken = true;
            mCamera.takePicture(mShutterListener, null, new PictureListener(name));
        }
    }

    public void takePicture(String name,@NonNull PictureSavedListener pictureSavedListener){
        mCamera.takePicture(mShutterListener,null,new PictureListener(name,pictureSavedListener));
    }
    public void takePicture(@NonNull Camera.PictureCallback pictureCallback){
        mCamera.takePicture(mShutterListener,null,pictureCallback);
    }

    private class PictureListener implements Camera.PictureCallback{
        private String path;
        private PictureSavedListener mPictureSavedListener;
        public PictureListener(String path,PictureSavedListener pictureSavedListener){
            this(path);
            mPictureSavedListener = pictureSavedListener;
        }
        public PictureListener(String path){
            this.path = path;
        }
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data != null) {
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(path,true);
                    os.write(data);
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cameraPreviewStart();
            if(mPictureSavedListener != null)mPictureSavedListener.onSaved(path);
            isTaken = false;
        }
    }
    //AutoFocusした時
    private Camera.AutoFocusCallback autoFocusCallback;
    public void setOnAutoFocusCallback(Camera.AutoFocusCallback temp) {
        autoFocusCallback = temp;
    }
    public void autoFocus(){
        mCamera.autoFocus(autoFocusCallback);
    }
    public void autoFocus(Camera.AutoFocusCallback temp) {
        mCamera.autoFocus(temp);
    }

    public interface PictureSavedListener{
        void onSaved(String path);
    }
    public void cameraPreviewStart(){
        mCamera.setPreviewCallback(mPreviewListener);
        mCamera.startPreview();
    }
    public void onePreview(){
        mCamera.setOneShotPreviewCallback(onePreviewListener);
    }

}