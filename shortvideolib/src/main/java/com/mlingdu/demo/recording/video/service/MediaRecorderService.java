package com.mlingdu.demo.recording.video.service;

import android.content.res.AssetManager;

import com.mlingdu.demo.recording.camera.exception.CameraParamSettingException;
import com.mlingdu.demo.recording.camera.preview.PreviewFilterType;
import com.mlingdu.demo.recording.exception.AudioConfigurationException;
import com.mlingdu.demo.recording.exception.StartRecordingException;

public interface MediaRecorderService {

	public void switchCamera() throws CameraParamSettingException;

	/**
	 * 初始化录音器的硬件部分
	 */
	public void initMetaData() throws AudioConfigurationException;

	/**
	 * 初始化我们后台处理数据部分(处理音频、处理视频)
	 */
	public boolean initMediaRecorderProcessor();

	/**
	 * 销毁我们的后台处理数据部分
	 */
	public void destoryMediaRecorderProcessor();

	/**
	 * 开始录音
	 */
	public boolean start(int width, int height, int videoBitRate, int frameRate, boolean useHardWareEncoding, int strategy) throws StartRecordingException;

	/**
	 * 结束录音
	 */
	public void stop();
	
	/**
	 * 获得后台处理数据部分的buffer大小
	 */
	public int getAudioBufferSize();

	public int getSampleRate();

	public void switchPreviewFilter(AssetManager assetManager, PreviewFilterType filterType);
	
	public void enableUnAccom();

}
