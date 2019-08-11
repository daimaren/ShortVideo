package com.mlingdu.demo.recording.video.service.factory;

import com.mlingdu.demo.recording.RecordingImplType;
import com.mlingdu.demo.recording.camera.preview.RecordingPreviewScheduler;
import com.mlingdu.demo.recording.service.RecorderService;
import com.mlingdu.demo.recording.service.impl.AudioRecordRecorderServiceImpl;
import com.mlingdu.demo.recording.video.service.MediaRecorderService;
import com.mlingdu.demo.recording.video.service.impl.MediaRecorderServiceImpl;

public class MediaRecorderServiceFactory {
	private static MediaRecorderServiceFactory instance = new MediaRecorderServiceFactory();
	private MediaRecorderServiceFactory() {}
	public static MediaRecorderServiceFactory getInstance() {
		return instance;
	}

	public MediaRecorderService getRecorderService(RecordingPreviewScheduler scheduler, RecordingImplType recordingImplType) {
		RecorderService recorderService = getAudioRecorderService(recordingImplType);
		MediaRecorderService result = new MediaRecorderServiceImpl(recorderService, scheduler);
		return result;
	}

	protected RecorderService getAudioRecorderService(
			RecordingImplType recordingImplType) {
		RecorderService recorderService = null;
		switch (recordingImplType) {
		case ANDROID_PLATFORM:
			recorderService = AudioRecordRecorderServiceImpl.getInstance();
			break;
		default:
			break;
		}
		return recorderService;
	}

}
