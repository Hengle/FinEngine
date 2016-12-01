package com.ifinver.finengine;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by iFinVer on 2016/11/15.
 * ilzq@foxmail.com
 */

@SuppressWarnings("deprecation")
public final class CameraUtils {

    private static final String TAG = "CameraUtils";

    private Camera mCamera;
    public int mCameraIndex;
    public int mFrameWidth;
    public int mFrameHeight;
    private int mWindowDegree = 0;
    private boolean mInitialized = false;
    private int mCameraOrientation = 0;

    /*Package*/ CameraUtils() {
        mCameraIndex = Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public void setCameraDegreeByWindowRotation(int windowRotation) {
        switch (windowRotation) {
            case Surface.ROTATION_0:
                mWindowDegree = 0;
                break;
            case Surface.ROTATION_90:
                mWindowDegree = 90;
                break;
            case Surface.ROTATION_180:
                mWindowDegree = 180;
                break;
            case Surface.ROTATION_270:
                mWindowDegree = 270;
                break;
        }
        if (mInitialized) {
            setCameraDispOri();
        }

    }

    public int getCameraOrientation() {
        return mCameraOrientation;
    }

    private void setCameraDispOri() {
        if (mCamera != null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraIndex, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCameraOrientation = (info.orientation + mWindowDegree) % 360;
            } else { // back-facing
                mCameraOrientation = (info.orientation - mWindowDegree + 360) % 360;
            }
            mCameraOrientation = (360 - mCameraOrientation) % 360; // compensate the mirror
            mCamera.setDisplayOrientation(mCameraOrientation);
//            Log.d(TAG, "setCameraDispOri = " + mCameraOrientation);
        }
    }

    /**
     * 传入期望的宽高
     *
     * @return true = 成功
     */
    public boolean init(int width, int height) {
        boolean result = true;
        synchronized (this) {
            mCamera = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                try {
                    mCamera = Camera.open(mCameraIndex);
                    if (mCameraIndex == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        Log.d(TAG, "已打开前置摄像头");
                    } else {
                        Log.d(TAG, "已打开后置摄像头");
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "摄像头 #" + mCameraIndex + "打开失败！: " + e.getLocalizedMessage());
                }
            }

            if (mCamera == null)
                return false;

            try {
                Camera.Parameters params = mCamera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();
                if (sizes != null) {
                    //格式
                    params.setPreviewFormat(ImageFormat.NV21);
                    //大小
                    CameraSize frameSize = calculateCameraFrameSize(sizes, width, height);
                    Log.d(TAG, "预览设置为 " + frameSize.width + "x" + frameSize.height);
                    params.setPreviewSize(frameSize.width, frameSize.height);
                    //帧率
                    List<int[]> support = params.getSupportedPreviewFpsRange();
                    if(support.size() > 0){
                        int[] ints = support.get(0);
                        int min = ints[1];
                        int max = ints[1];
                        params.setPreviewFpsRange(min,max);
                        Log.d(TAG,"帧率设置为:["+min+","+max+"]");
                    }else {
                        Log.e(TAG,"WTF,不能设置帧率");
                    }
                    //优化
                    //三星的机型也有问题，未知的问题机型较多，所以不使用
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100")){
//                        params.setRecordingHint(true);
//                    }
                    //聚焦
                    List<String> FocusModes = params.getSupportedFocusModes();
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                    mCamera.setParameters(params);
                    //记录宽高
                    params = mCamera.getParameters();
                    mFrameWidth = params.getPreviewSize().width;
                    mFrameHeight = params.getPreviewSize().height;
                } else {
                    result = false;
                }
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }
        mInitialized = result;
        return result;
    }


    public boolean startPreview(Context appCtx,SurfaceTexture st) {
        try {
            WindowManager wm = (WindowManager) appCtx.getSystemService(Context.WINDOW_SERVICE);
            setCameraDegreeByWindowRotation(wm.getDefaultDisplay().getRotation());
            mCamera.setPreviewTexture(st);
            mCamera.startPreview();
            setCameraDispOri();
            Log.d(TAG,"开始Camera预览");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public boolean toggleCameraWithoutStart() {
        if (mCamera != null) {
            try {
                stop();
            } catch (Throwable ignored) {
                Log.e(TAG, "调整摄像头时，关闭当前摄像头失败！", ignored);
                return false;
            }
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraIndex) {
                mCameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
            } else {
                mCameraIndex = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
            return init(mFrameWidth, mFrameHeight);
        }
        return false;
    }

    public void stop() {
        synchronized (this) {
            mInitialized = false;
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);

                mCamera.release();
            }
            mCamera = null;
            Log.d(TAG, "摄像机已释放");
        }
    }

    /**
     * 根据期望选择合适宽高
     */
    private CameraSize calculateCameraFrameSize(List<Camera.Size> supportedSizes, int maxAllowedWidth, int maxAllowedHeight) {
        int calcWidth = 0;
        int calcHeight = 0;

        for (Camera.Size size : supportedSizes) {
            int width = size.width;
            int height = size.height;

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = width;
                    calcHeight = height;
                }
            }
        }

        return new CameraSize(calcWidth, calcHeight);
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public int getFrameDegree() {
        return mCameraOrientation;
    }

    private class CameraSize {
        int width;
        int height;

        public CameraSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}