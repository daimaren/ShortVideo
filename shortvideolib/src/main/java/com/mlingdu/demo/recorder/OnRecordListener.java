package com.mlingdu.demo.recorder;

/**
 * 录制监听器
 */
public interface OnRecordListener {
    // 录制已经开始
    void onRecordStarted();

    // 录制时间改变
    void onRecordProgressChanged(final long duration);

    // 录制已经结束
    void onRecordFinish();
}
