package com.mlingdu.demo.recording.video.service.impl;

import android.content.res.AssetManager;

import com.mlingdu.demo.recording.camera.preview.RecordingPreviewScheduler;
import com.mlingdu.demo.video.encoder.MediaCodecSurfaceEncoder;
import com.mlingdu.demo.recording.camera.exception.CameraParamSettingException;
import com.mlingdu.demo.recording.camera.preview.PreviewFilterType;
import com.mlingdu.demo.recording.exception.AudioConfigurationException;
import com.mlingdu.demo.recording.exception.StartRecordingException;
import com.mlingdu.demo.recording.service.RecorderService;
import com.mlingdu.demo.recording.video.service.MediaRecorderService;

public class MediaRecorderServiceImpl implements MediaRecorderService {

	private RecorderService audioRecorderService;
	private RecordingPreviewScheduler previewScheduler;

	public MediaRecorderServiceImpl(RecorderService recorderService, RecordingPreviewScheduler scheduler) {
		this.audioRecorderService = recorderService;
		this.previewScheduler = scheduler;
	}

	public void switchCamera() throws CameraParamSettingException {
		previewScheduler.switchCameraFacing();
	}
	
	@Override
	public void initMetaData() throws AudioConfigurationException {
		audioRecorderService.initMetaData();
	}

	@Override
	public boolean initMediaRecorderProcessor() {
		return audioRecorderService.initAudioRecorderProcessor();
	}

	@Override
	public boolean start(int width, int height, int videoBitRate, int frameRate, boolean useHardWareEncoding, int strategy) throws StartRecordingException {
		audioRecorderService.start();
		if(useHardWareEncoding){
			if(MediaCodecSurfaceEncoder.IsInNotSupportedList()){
				useHardWareEncoding = false;
			}
		}
		previewScheduler.startEncoding(width, height, videoBitRate, frameRate, useHardWareEncoding, strategy);
		return useHardWareEncoding;
	}

	@Override
	public void destoryMediaRecorderProcessor() {
		audioRecorderService.destoryAudioRecorderProcessor();
	}

	@Override
	public void stop() {
		audioRecorderService.stop();
		previewScheduler.stop();
	}

	@Override
	public int getAudioBufferSize() {
		return audioRecorderService.getAudioBufferSize();
	}

	@Override
	public int getSampleRate() {
		return audioRecorderService.getSampleRate();
	}

	@Override
	public void switchPreviewFilter(AssetManager assetManager, PreviewFilterType filterType) {
		if(null != previewScheduler){
			previewScheduler.switchPreviewFilter(assetManager, filterType);;
		}
	}

	@Override
	public void enableUnAccom() {
		if (audioRecorderService != null) {
			audioRecorderService.enableUnAccom();
		}
	}

}
