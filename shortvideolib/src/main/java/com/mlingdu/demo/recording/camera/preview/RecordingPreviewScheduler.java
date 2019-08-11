package com.mlingdu.demo.recording.camera.preview;
import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.cgfay.filterlibrary.glfilter.color.bean.DynamicColor;
import com.cgfay.filterlibrary.glfilter.makeup.bean.DynamicMakeup;
import com.cgfay.filterlibrary.glfilter.stickers.StaticStickerNormalFilter;
import com.cgfay.filterlibrary.glfilter.stickers.bean.DynamicSticker;
import com.mlingdu.demo.filter.RenderManager;
import com.mlingdu.demo.recording.camera.preview.RecordingPreviewView.ChangbaRecordingPreviewViewCallback;
import com.mlingdu.demo.recording.camera.preview.VideoCamera.ChangbaVideoCameraCallback;
import com.mlingdu.demo.video.encoder.MediaCodecSurfaceEncoder;

public class RecordingPreviewScheduler
        implements ChangbaVideoCameraCallback, ChangbaRecordingPreviewViewCallback {
    private static final String TAG = "RecordingPreviewScheduler";
    private RecordingPreviewView mPreviewView;
    private VideoCamera mCamera;
    private int mNativeInputTextureId = 0;
    private int mCurrentTexture;
    // 操作锁
    private final Object mSynOperation = new Object();
    // 渲染管理器
    private RenderManager mRenderManager;
    private int mTextureWidth, mTextureHeight;
    // 矩阵
    private final float[] mMatrix = new float[16];
    //activity context
    private Context mContext;

    public RecordingPreviewScheduler(RecordingPreviewView previewView, VideoCamera camera, Context context) {
        mContext = context;
        isStopped = false;
        this.mPreviewView = previewView;
        this.mCamera = camera;
        this.mPreviewView.setCallback(this);
        this.mCamera.setCallback(this);
        mRenderManager = RenderManager.getInstance();
    }

    public void resetStopState() {
        isStopped = false;
    }

    public native void startEncoding(int width, int height, int videoBitRate, int frameRate, boolean useHardWareEncoding, int strategy);

    public native void stopEncoding();

    public int getNumberOfCameras() {
        if (null != mCamera) {
            return mCamera.getNumberOfCameras();
        }
        return -1;
    }

    /**
     * 当切换视频滤镜的时候调用这个方法
     **/
    public void switchPreviewFilter(AssetManager assetManager, PreviewFilterType filterType) {
        switch (filterType) {
            case PREVIEW_COOL:
                switchPreviewFilter(filterType.getValue(), assetManager, "filter/cool_1.acv");
                break;
            case PREVIEW_THIN_FACE:
            case PREVIEW_NONE:
            case PREVIEW_ORIGIN:
            case PREVIEW_WHITENING:
            default:
                switchPreviewFilter(filterType.getValue(), assetManager, "");
                break;
        }
    }

    private native void switchPreviewFilter(int value, AssetManager ass, String filename);

    /**
     * 预览状态、录制状态、暂停录制状态
     **/
    public native void switchPauseRecordingPreviewState();

    public native void switchCommonPreviewState();

    /**
     * 切换摄像头, 底层会在返回来调用configCamera, 之后在启动预览
     **/
    public native void switchCameraFacing();

    private boolean isFirst = true;
    private boolean isSurfaceExsist = false;
    private boolean isStopped = false;
    private int defaultCameraFacingId = CameraInfo.CAMERA_FACING_FRONT;

    @Override
    public void createSurface(Surface surface, int width, int height) {
        startPreview(surface, width, height, defaultCameraFacingId);
        // 渲染器初始化
        mRenderManager.init(mContext);
    }


    private void startPreview(Surface surface, int width, int height, final int cameraFacingId) {
        if (isFirst) {
            prepareEGLContext(surface, width, height, cameraFacingId);
            isFirst = false;
        } else {
            createWindowSurface(surface);
        }
        isSurfaceExsist = true;
    }

    public void startPreview(final int cameraFacingId) {
        try {
            if (null != mPreviewView) {
                SurfaceHolder holder = mPreviewView.getHolder();
                if (null != holder) {
                    Surface surface = holder.getSurface();
                    if (null != surface) {
                        startPreview(surface, mPreviewView.getWidth(), mPreviewView.getHeight(), cameraFacingId);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public native void prepareEGLContext(Surface surface, int width, int height, int cameraFacingId);

    public native void createWindowSurface(Surface surface);

    public native void adaptiveVideoQuality(int maxBitRate, int avgBitRate, int fps);

    public native void hotConfigQuality(int maxBitrate, int avgBitrate, int fps);

    public native void resetRenderSize(int width, int height);

    @Override
    public void surfaceChanged(int width, int height) {
        mRenderManager.setDisplaySize(width, height);
        resetRenderSize(width, height);
    }
    @Override
    public void destroySurface() {
        if (isStopped) {
            this.stopPreview();
        } else {
            this.destroyWindowSurface();
        }
        isSurfaceExsist = false;
        mRenderManager.release();
    }

    public void stop() {
        this.stopEncoding();
        isStopped = true;
        if (!isSurfaceExsist) {
            this.stopPreview();
        }
    }

    private void stopPreview() {
        this.destroyEGLContext();
        isFirst = true;
        isSurfaceExsist = false;
        isStopped = false;
    }

    public native void destroyWindowSurface();

    public native void destroyEGLContext();

    /**
     * 当Camera捕捉到了新的一帧图像的时候会调用这个方法,因为更新纹理必须要在EGLThread中,
     * 所以配合下updateTexImageFromNative使用
     **/
    @Override
    public native void notifyFrameAvailable();

    public void onPermissionDismiss(String tip) {
        Log.i("problem", "onPermissionDismiss : " + tip);
    }

    @Override
    public native void updateTexMatrix(float texMatrix[]);

    private CameraConfigInfo mConfigInfo;

    /**
     * 当底层创建好EGLContext之后，回调回来配置Camera，返回Camera的配置信息，然后在EGLThread线程中回调回来继续做Camera未完的配置以及Preview
     **/
    public CameraConfigInfo configCameraFromNative(int cameraFacingId) {
        defaultCameraFacingId = cameraFacingId;
        mConfigInfo = mCamera.configCameraFromNative(cameraFacingId);
        mTextureWidth = mCamera.getPreviewWidth();
        mTextureHeight = mCamera.getPreviewHeight();
        calculateImageSize(mTextureWidth, mTextureHeight);
        return mConfigInfo;
    }

    /**
     * 当底层EGLThread创建完纹理之后，反射调用，设置给Camera
     **/
    public void startPreviewFromNative(int textureId) {
        mNativeInputTextureId = textureId;
        mCamera.setCameraPreviewTexture(textureId);
    }

    /**
     * 当底层EGLThread更新纹理的时候调用这个方法
     **/
    public void updateTexImageFromNative() {
        mCamera.updateTexImage();
        //对texture做滤镜处理
        mCamera.getTransformMatrix(mMatrix);
        // 绘制渲染
        mCurrentTexture = mRenderManager.drawFrame(mNativeInputTextureId, mMatrix);
        //纹理id通过jni传递下去处理，作为下一级的输入
        //todo native层创建的纹理在java层操作会不会有问题，搜不到答案，试了才知道
    }

    /**
     * 释放掉当前的Camera
     **/
    public void releaseCameraFromNative() {
        mCamera.releaseCamera();
    }

    public void onMemoryWarning(int queueSize) {
        Log.d("problem", "onMemoryWarning called");
    }

    // encoder
    protected MediaCodecSurfaceEncoder surfaceEncoder;
    Surface surface = null;

    public void createMediaCodecSurfaceEncoderFromNative(int width, int height, int bitRate, int frameRate) {
        try {
            surfaceEncoder = new MediaCodecSurfaceEncoder(width, height, bitRate, frameRate);
            surface = surfaceEncoder.getInputSurface();
        } catch (Exception e) {
            Log.e("problem", "createMediaCodecSurfaceEncoder failed");
        }
    }

    public void hotConfigEncoderFromNative(int width, int height, int bitRate, int fps) {
        try {
            if (surfaceEncoder != null) {
                surfaceEncoder.hotConfig(width, height, bitRate, fps);
                surface = surfaceEncoder.getInputSurface();
            }
        } catch (Exception e) {
            Log.e("problem", "hotConfigMediaCodecSurfaceEncoder failed");
        }
    }

    public long pullH264StreamFromDrainEncoderFromNative(byte[] returnedData) {
        return surfaceEncoder.pullH264StreamFromDrainEncoderFromNative(returnedData);
    }

    public long getLastPresentationTimeUsFromNative() {
        return surfaceEncoder.getLastPresentationTimeUs();
    }

    public Surface getEncodeSurfaceFromNative() {
        return surface;
    }

    public void reConfigureFromNative(int targetBitrate) {
        if (null != surfaceEncoder) {
            surfaceEncoder.reConfigureFromNative(targetBitrate);
        }
    }

    public void closeMediaCodecCalledFromNative() {
        if (null != surfaceEncoder) {
            surfaceEncoder.shutdown();
        }
    }

    public native void hotConfig(int bitRate, int fps, int gopSize);
    public native void setBeautifyParam(int key, float value);

    /**
     * 计算imageView 的宽高
     */
    private void calculateImageSize(int width, int height) {
        mRenderManager.setTextureSize(width, height);
    }

    /**
     * 切换边框模糊
     * @param enableEdgeBlur
     */
    void changeEdgeBlurFilter(boolean enableEdgeBlur) {
        synchronized (mSynOperation) {
            mRenderManager.changeEdgeBlurFilter(enableEdgeBlur);
        }
    }

    /**
     * 切换动态滤镜
     * @param color
     */
    void changeDynamicFilter(DynamicColor color) {
        synchronized (mSynOperation) {
            mRenderManager.changeDynamicFilter(color);
        }
    }

    /**
     * 切换动态彩妆
     * @param makeup
     */
    void changeDynamicMakeup(DynamicMakeup makeup) {
        synchronized (mSynOperation) {
            mRenderManager.changeDynamicMakeup(makeup);
        }
    }

    /**
     * 切换动态资源
     * @param color
     */
    void changeDynamicResource(DynamicColor color) {
        synchronized (mSynOperation) {
            mRenderManager.changeDynamicResource(color);
        }
    }

    /**
     * 切换动态资源
     * @param sticker
     */
    void changeDynamicResource(DynamicSticker sticker) {
        synchronized (mSynOperation) {
            mRenderManager.changeDynamicResource(sticker);
        }
    }

    public StaticStickerNormalFilter touchDown(MotionEvent e) {
        //todo
        return null;
    }
}
