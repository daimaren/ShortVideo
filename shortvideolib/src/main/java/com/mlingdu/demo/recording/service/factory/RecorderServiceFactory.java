package com.mlingdu.demo.recording.service.factory;

import com.mlingdu.demo.recording.RecordingImplType;
import com.mlingdu.demo.recording.service.RecorderService;
import com.mlingdu.demo.recording.service.impl.AudioRecordRecorderServiceImpl;

public class RecorderServiceFactory {
	private static RecorderServiceFactory instance = new RecorderServiceFactory();
	private RecorderServiceFactory(){};
	public static RecorderServiceFactory getInstance(){
		return instance;
	}
	
	public RecorderService getRecorderService(RecordingImplType recordingImplType){
		RecorderService result = null;
		switch (recordingImplType) {
		case ANDROID_PLATFORM:
			result = AudioRecordRecorderServiceImpl.getInstance();
			break;
		default:
			break;
		}
		return result;
	}
}
