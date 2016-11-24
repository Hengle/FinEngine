package com.ifinver.myopengles;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.nio.ByteBuffer;

/**
 * Created by iFinVer on 2016/11/16.
 * ilzq@foxmail.com
 */

public class OpenGLActivity extends AppCompatActivity implements CameraHolder.InitCallback, CameraHolder.StopCallback, CameraHolder.BufferCallback {

    private CameraHolder mCameraHolder;
    private DisplayMetrics displayMetrics;
    private TextureRenderView[] textures;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl);

        FrameLayout fm[] = new FrameLayout[4];
        fm[0] = (FrameLayout) findViewById(R.id.av_0);
        fm[1] = (FrameLayout) findViewById(R.id.av_1);
        fm[2] = (FrameLayout) findViewById(R.id.av_2);
        fm[3] = (FrameLayout) findViewById(R.id.av_3);

        mCameraHolder = CameraHolder.getInstance();
        mCameraHolder.setCameraDegreeByWindowRotation(getWindowManager().getDefaultDisplay().getRotation());
        mCameraHolder.setBufferCallback(this);

        int imageFormat = mCameraHolder.getImageFormat();//现在只支持NV21
        textures = new TextureRenderView[4];
        for (int i = 0; i < fm.length; i++) {
            textures[i] = new TextureRenderView(this);
            textures[i].setAspectRatio(TextureRenderView.AR_ASPECT_FILL_PARENT);
            fm[i].addView(textures[i], new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        textures[0].initRenderer(imageFormat, TextureRenderer.FILTER_TYPE_NEGATIVE_COLOR);
        textures[1].initRenderer(imageFormat, TextureRenderer.FILTER_TYPE_CYAN);
        textures[2].initRenderer(imageFormat, TextureRenderer.FILTER_TYPE_FISH_EYE);
        textures[3].initRenderer(imageFormat, TextureRenderer.FILTER_TYPE_GREY_SCALE);

        displayMetrics = getResources().getDisplayMetrics();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toggle_camera:
                if (mCameraHolder != null) {
                    mCameraHolder.toggleCamera(new CameraHolder.ToggleCallback() {
                        @Override
                        public void onToggleCameraComplete(boolean success, int current) {
                            if (success) {
                                Toast.makeText(OpenGLActivity.this, "success", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(OpenGLActivity.this, "error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    Toast.makeText(this, "switching", Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onVideoBuffer(ByteBuffer frameByteBuffer, int frameDegree, int frameWidth, int frameHeight) {
        for (TextureRenderView renderView : textures) {
            renderView.onVideoBuffer(frameByteBuffer, frameDegree, frameWidth, frameHeight);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraHolder.start(displayMetrics.widthPixels, displayMetrics.heightPixels, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraHolder.stop(this);
    }

    @Override
    public void onCameraStarted(boolean success, int mFrameWidth, int mFrameHeight, int imageFormat) {
        for (TextureRenderView renderView : textures) {
            renderView.setVideoSize(mFrameWidth,mFrameHeight);
        }
    }

    @Override
    public void onCameraStopped() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
