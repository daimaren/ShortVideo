package com.mlingdu.demo.recording.camera.preview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.mlingdu.demo.entity.CalculateType;
import com.mlingdu.demo.recording.camera.exception.CameraParamSettingException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoCamera {
	private static final String TAG = "VideoCamera";
	// 16:9的默认宽高(理想值)
	public static final int DEFAULT_16_9_WIDTH = 1280;
	public static final int DEFAULT_16_9_HEIGHT = 720;

	public static int VIDEO_WIDTH = 640;
	public static int DEFAULT_VIDEO_WIDTH = 640;
	public static int VIDEO_HEIGHT = 480;
	public static int DEFAULT_VIDEO_HEIGHT = 480;
	public static int videoFrameRate = 24;

	public static void forcePreviewSize_640_480() {
		VIDEO_WIDTH = 640;
		VIDEO_HEIGHT = 480;
		videoFrameRate = 15;
	}
	public static void forcePreviewSize_1280_720() {
		VIDEO_WIDTH = 1280;
		VIDEO_HEIGHT = 720;
		videoFrameRate = 24;
	}

	private Camera mCamera;
	private int mPreviewWidth = VIDEO_WIDTH;
	private int mPreviewHeight = VIDEO_HEIGHT;
	private SurfaceTexture mCameraSurfaceTexture;
	private Context mContext;

	public VideoCamera(Context context) {
		this.mContext = context;
	}

	public Camera getCamera() {
		return mCamera;
	}

	public CameraConfigInfo configCameraFromNative(int cameraFacingId) {
		if (null != mCamera) {
			releaseCamera();
		}
		if (cameraFacingId >= getNumberOfCameras()) {
			cameraFacingId = 0;
		}
		try {
			return setUpCamera(cameraFacingId);
		} catch (CameraParamSettingException e) {
			mCallback.onPermissionDismiss(e.getMessage());
		}
		int degress = 270;
		int previewWidth = VIDEO_WIDTH;
		int previewHeight = VIDEO_HEIGHT;

		Camera.Parameters parameters = mCamera.getParameters();
		Camera.Size size = calculatePerfectSize(parameters.getSupportedPreviewSizes(),
				DEFAULT_16_9_WIDTH, DEFAULT_16_9_HEIGHT, CalculateType.Lower);
		if(null != mCamera){
			try {
				mPreviewWidth = size.width;
				mPreviewHeight = size.height;
				parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
				mCamera.setParameters(parameters);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new CameraConfigInfo(degress, previewWidth, previewHeight, cameraFacingId);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void setCameraPreviewTexture(int textureId) {
		Log.i(TAG, "setCameraPreviewTexture...");
		mCameraSurfaceTexture = new SurfaceTexture(textureId);
		try {
			mCamera.setPreviewTexture(mCameraSurfaceTexture);
			mCameraSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
				@Override
				public void onFrameAvailable(SurfaceTexture surfaceTexture) {
					if (null != mCallback) {
//						Log.d("RecordingPublisher", "surfaceTexture time stamp is "+surfaceTexture.getTimestamp()/1000000000.0f);
						mCallback.notifyFrameAvailable();
					}
				}
			});
			mCamera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void updateTexImage() {
		// Log.i(TAG, "updateTexImage...");
		try {
			if (null != mCameraSurfaceTexture) {
				mCameraSurfaceTexture.updateTexImage();
				
				//去掉这个没用的调用
//				float[] mTmpMatrix = new float[16];
//				mCameraSurfaceTexture.getTransformMatrix(mTmpMatrix);
//				
//				if (null != mCallback) {
//					mCallback.updateTexMatrix(mTmpMatrix);
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getTransformMatrix(float[] mtx) {
		mCameraSurfaceTexture.getTransformMatrix(mtx);
	}

	public int getNumberOfCameras() {
		return Camera.getNumberOfCameras();
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void releaseCamera() {
		try {
			if (mCameraSurfaceTexture != null) {
				// this causes a bunch of warnings that appear harmless but might
				// confuse someone:
				// W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has
				// been abandoned!
				mCameraSurfaceTexture.release();
				mCameraSurfaceTexture = null;
			}
			if (null != mCamera) {
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private CameraConfigInfo setUpCamera(final int id) throws CameraParamSettingException {
//		 forcePreviewSize_640_480();
		forcePreviewSize_1280_720();
		// printStackTrace(CameraLoader.class);
		try {
			// 1、开启Camera
			try {
				mCamera = getCameraInstance(id);
			} catch (CameraParamSettingException e) {
				throw e;
			}
			boolean mHasPermission = hasPermission();
			if (!mHasPermission) {
				throw new CameraParamSettingException("拍摄权限被禁用或被其他程序占用, 请确认后再录制");
			}
			Parameters parameters = mCamera.getParameters();

			// 2、设置预览照片的图像格式
			List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
			if (supportedPreviewFormats.contains(ImageFormat.NV21)) {
				parameters.setPreviewFormat(ImageFormat.NV21);
			} else {
				throw new CameraParamSettingException("视频参数设置错误:设置预览图像格式异常");
			}

			// 3、设置预览照片的尺寸
			List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
			int previewWidth = VIDEO_WIDTH;
			int previewHeight = VIDEO_HEIGHT;
			boolean isSupportPreviewSize = isSupportPreviewSize(supportedPreviewSizes, previewWidth, previewHeight);

			if (isSupportPreviewSize) {
				parameters.setPreviewSize(previewWidth, previewHeight);
			} else {
				previewWidth = DEFAULT_VIDEO_WIDTH;
				previewHeight = DEFAULT_VIDEO_HEIGHT;
				isSupportPreviewSize = isSupportPreviewSize(
						supportedPreviewSizes, previewWidth, previewHeight);
				if (isSupportPreviewSize) {
					VIDEO_WIDTH = DEFAULT_VIDEO_WIDTH;
					VIDEO_HEIGHT = DEFAULT_VIDEO_HEIGHT;
					parameters.setPreviewSize(previewWidth, previewHeight);
				} else {
					throw new CameraParamSettingException("视频参数设置错误:设置预览的尺寸异常");
				}
			}
			//下面这行设置 有可能导致 返回的图像尺寸和预期不一致
//			parameters.setRecordingHint(true);

			// 4、设置视频记录的连续自动对焦模式
			if (parameters.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
				parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			}

			try {
				mCamera.setParameters(parameters);
			} catch (Exception e) {
				throw new CameraParamSettingException("视频参数设置错误");
			}

			int degress = getCameraDisplayOrientation((Activity) mContext, id);
			int cameraFacing = getCameraFacing(id);
			return new CameraConfigInfo(degress, previewWidth, previewHeight, cameraFacing);
		} catch (Exception e) {
			throw new CameraParamSettingException(e.getMessage());
		}
	}

	private boolean hasPermission() {
		boolean mHasPermission = true;
		if (null == mCamera) {
			mHasPermission = false;
		} else {
			try {
				Class<? extends Camera> class1 = mCamera.getClass();
				Field filed = class1.getDeclaredField("mHasPermission");
				if (null != filed) {
					filed.setAccessible(true);
					mHasPermission = (Boolean) filed.get(mCamera);
				}
			} catch (Exception e1) {
			}
		}
		return mHasPermission;
	}

	private boolean isSupportPreviewSize(List<Size> supportedPreviewSizes, int previewWidth, int previewHeight) {
		boolean isSupportPreviewSize = false;
		for (Size size : supportedPreviewSizes) {
			if (previewWidth == size.width && previewHeight == size.height) {
				isSupportPreviewSize = true;
				break;
			}
		}
		return isSupportPreviewSize;
	}

	/** A safe way to get an instance of the Camera object. */
	private Camera getCameraInstance(final int id) throws CameraParamSettingException {
		Log.i("problem", "getCameraInstance id is" + id);
		Camera c = null;
		try {
			c = Camera.open(id);
		} catch (Exception e) {
			throw new CameraParamSettingException("拍摄权限被禁用或被其他程序占用, 请确认后再录制");
		}
		return c;
	}

	public static int getCameraFacing(final int cameraId) {
		int result;
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			result = 1;
		} else { // back-facing
			result = 0;
		}
		return result;
	}
	public static int getCameraDisplayOrientation(final Activity activity, final int cameraId) {
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int result;
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	private ChangbaVideoCameraCallback mCallback;

	public interface ChangbaVideoCameraCallback {
		public void onPermissionDismiss(String tip);
		
		public void notifyFrameAvailable();
		
		public void updateTexMatrix(float texMatrix[]);
	}

	public void setCallback(ChangbaVideoCameraCallback callback) {
		this.mCallback = callback;
	}

	public int getPreviewHeight() {
		return mPreviewHeight;
	}

	public int getPreviewWidth() {
		return mPreviewWidth;
	}

	/**
	 * 计算最完美的Size
	 * @param sizes
	 * @param expectWidth
	 * @param expectHeight
	 * @return
	 */
	private static Camera.Size calculatePerfectSize(List<Camera.Size> sizes, int expectWidth,
													int expectHeight, CalculateType calculateType) {
		sortList(sizes); // 根据宽度进行排序

		// 根据当前期望的宽高判定
		List<Camera.Size> bigEnough = new ArrayList<>();
		List<Camera.Size> noBigEnough = new ArrayList<>();
		for (Camera.Size size : sizes) {
			if (size.height * expectWidth / expectHeight == size.width) {
				if (size.width > expectWidth && size.height > expectHeight) {
					bigEnough.add(size);
				} else {
					noBigEnough.add(size);
				}
			}
		}
		// 根据计算类型判断怎么如何计算尺寸
		Camera.Size perfectSize = null;
		switch (calculateType) {
			// 直接使用最小值
			case Min:
				// 不大于期望值的分辨率列表有可能为空或者只有一个的情况，
				// Collections.min会因越界报NoSuchElementException
				if (noBigEnough.size() > 1) {
					perfectSize = Collections.min(noBigEnough, new CompareAreaSize());
				} else if (noBigEnough.size() == 1) {
					perfectSize = noBigEnough.get(0);
				}
				break;

			// 直接使用最大值
			case Max:
				// 如果bigEnough只有一个元素，使用Collections.max就会因越界报NoSuchElementException
				// 因此，当只有一个元素时，直接使用该元素
				if (bigEnough.size() > 1) {
					perfectSize = Collections.max(bigEnough, new CompareAreaSize());
				} else if (bigEnough.size() == 1) {
					perfectSize = bigEnough.get(0);
				}
				break;

			// 小一点
			case Lower:
				// 优先查找比期望尺寸小一点的，否则找大一点的，接受范围在0.8左右
				if (noBigEnough.size() > 0) {
					Camera.Size size = Collections.max(noBigEnough, new CompareAreaSize());
					if (((float)size.width / expectWidth) >= 0.8
							&& ((float)size.height / expectHeight) > 0.8) {
						perfectSize = size;
					}
				} else if (bigEnough.size() > 0) {
					Camera.Size size = Collections.min(bigEnough, new CompareAreaSize());
					if (((float)expectWidth / size.width) >= 0.8
							&& ((float)(expectHeight / size.height)) >= 0.8) {
						perfectSize = size;
					}
				}
				break;

			// 大一点
			case Larger:
				// 优先查找比期望尺寸大一点的，否则找小一点的，接受范围在0.8左右
				if (bigEnough.size() > 0) {
					Camera.Size size = Collections.min(bigEnough, new CompareAreaSize());
					if (((float)expectWidth / size.width) >= 0.8
							&& ((float)(expectHeight / size.height)) >= 0.8) {
						perfectSize = size;
					}
				} else if (noBigEnough.size() > 0) {
					Camera.Size size = Collections.max(noBigEnough, new CompareAreaSize());
					if (((float)size.width / expectWidth) >= 0.8
							&& ((float)size.height / expectHeight) > 0.8) {
						perfectSize = size;
					}
				}
				break;
		}
		// 如果经过前面的步骤没找到合适的尺寸，则计算最接近expectWidth * expectHeight的值
		if (perfectSize == null) {
			Camera.Size result = sizes.get(0);
			boolean widthOrHeight = false; // 判断存在宽或高相等的Size
			// 辗转计算宽高最接近的值
			for (Camera.Size size : sizes) {
				// 如果宽高相等，则直接返回
				if (size.width == expectWidth && size.height == expectHeight
						&& ((float) size.height / (float) size.width) == 0.5625f) {
					result = size;
					break;
				}
				// 仅仅是宽度相等，计算高度最接近的size
				if (size.width == expectWidth) {
					widthOrHeight = true;
					if (Math.abs(result.height - expectHeight) > Math.abs(size.height - expectHeight)
							&& ((float) size.height / (float) size.width) == 0.5625f) {
						result = size;
						break;
					}
				}
				// 高度相等，则计算宽度最接近的Size
				else if (size.height == expectHeight) {
					widthOrHeight = true;
					if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth)
							&& ((float) size.height / (float) size.width) == 0.5625f) {
						result = size;
						break;
					}
				}
				// 如果之前的查找不存在宽或高相等的情况，则计算宽度和高度都最接近的期望值的Size
				else if (!widthOrHeight) {
					if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth)
							&& Math.abs(result.height - expectHeight) > Math.abs(size.height - expectHeight)
							&& ((float) size.height / (float) size.width) == 0.5625f) {
						result = size;
					}
				}
			}
			perfectSize = result;
		}
		return perfectSize;
	}
	/**
	 * 分辨率由大到小排序
	 * @param list
	 */
	private static void sortList(List<Camera.Size> list) {
		Collections.sort(list, new CompareAreaSize());
	}

	/**
	 * 比较器
	 */
	private static class CompareAreaSize implements Comparator<Size> {
		@Override
		public int compare(Camera.Size pre, Camera.Size after) {
			return Long.signum((long) pre.width * pre.height -
					(long) after.width * after.height);
		}
	}
}
