package com.mlingdu.demo.recording.video;

import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.mlingdu.demo.Videostudio;
import com.mlingdu.demo.entity.RecordItem;
import com.mlingdu.demo.recorder.OnRecordListener;
import com.mlingdu.demo.recorder.RecordTimer;
import com.mlingdu.demo.recording.camera.preview.RecordingPreviewScheduler;
import com.mlingdu.demo.recording.service.PlayerService.OnCompletionListener;
import com.mlingdu.demo.recording.RecordingImplType;
import com.mlingdu.demo.recording.exception.InitPlayerFailException;
import com.mlingdu.demo.recording.exception.InitRecorderFailException;
import com.mlingdu.demo.recording.exception.RecordingStudioException;
import com.mlingdu.demo.recording.exception.RecordingStudioNullArgumentException;
import com.mlingdu.demo.recording.exception.StartRecordingException;
import com.mlingdu.demo.recording.service.factory.PlayerServiceFactory;
import com.mlingdu.demo.recording.service.impl.AudioRecordRecorderServiceImpl;
import com.mlingdu.demo.recording.video.service.factory.MediaRecorderServiceFactory;
import com.mlingdu.demo.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CommonVideoRecordingStudio extends VideoRecordingStudio {

    protected Handler mTimeHandler;
    protected int latency = -1;
    protected OnCompletionListener onComletionListener;

    // 是否处于录制状态
    private boolean isRecording;
    // 精确倒计时
    private RecordTimer mRecordTimer;
    // 倒计时数值
    private long mMaxMillisSeconds = 30 * 1000;
    // 50毫秒读取一次
    private long mCountDownInterval = 50;

    // 当前走过的时长，有可能跟视频的长度不一致
    // 在最后一秒内点击录制，倒计时走完，但录制的视频立即停止
    // 这样的话，最后一段显示的视频长度跟当前走过的时长并不相等
    private long mCurrentDuration = 0;
    // 记录最后一秒点击时剩余的时长
    private long mLastSecondLeftTime = 0;
    // 记录是否最后点击停止
    private boolean mLastSecondStop = false;
    // 是否需要处理最后一秒的情况
    private boolean mProcessLastSecond = true;
    // 计时器是否停止
    private boolean mTimerFinish = false;
    // 分段视频列表
    private LinkedList<RecordItem> mVideoList = new LinkedList<RecordItem>();
    // 录制监听器
    private OnRecordListener mRecordListener;

    public CommonVideoRecordingStudio(RecordingImplType recordingImplType, Handler mTimeHandler,
                                      OnCompletionListener onComletionListener, RecordingStudioStateCallback recordingStudioStateCallback) {
        super(recordingImplType, recordingStudioStateCallback);
        // 伴奏的初始化
        this.latency = -1;
        this.onComletionListener = onComletionListener;
        this.mTimeHandler = mTimeHandler;
    }

    @Override
    public void initRecordingResource(RecordingPreviewScheduler scheduler) throws RecordingStudioException {
        /**
         * 这里一定要注意顺序，先初始化record在初始化player，因为player中要用到recorder中的samplerateSize
         **/
        if (scheduler == null) {
            throw new RecordingStudioNullArgumentException("null argument exception in initRecordingResource");
        }

        scheduler.resetStopState();
        recorderService = MediaRecorderServiceFactory.getInstance().getRecorderService(scheduler, recordingImplType);
        if (recorderService != null) {
            recorderService.initMetaData();
        }
        if (recorderService != null && !recorderService.initMediaRecorderProcessor()) {
            throw new InitRecorderFailException();
        }
        // 初始化伴奏带额播放器 实例化以及init播放器
        playerService = PlayerServiceFactory.getInstance().getPlayerService(onComletionListener,
                RecordingImplType.ANDROID_PLATFORM, mTimeHandler);
        if (playerService != null) {
            boolean result = playerService.setAudioDataSource(AudioRecordRecorderServiceImpl.SAMPLE_RATE_IN_HZ);
            if (!result) {
                throw new InitPlayerFailException();
            }
        }
    }

    @Override
    public void startVideoRecording(String outputPath, int bitRate, int videoWidth, int videoHeight, int audioSampleRate, int qualityStrategy, int adaptiveBitrateWindowSizeInSecs, int adaptiveBitrateEncoderReconfigInterval, int adaptiveBitrateWarCntThreshold, int adaptiveMinimumBitrate, int adaptiveMaximumBitrate, boolean useHardWareEncoding) {
        // 初始化计时器
        initTimer();
        super.startVideoRecording(outputPath, bitRate, videoWidth, videoHeight, audioSampleRate, qualityStrategy,
                adaptiveBitrateWindowSizeInSecs, adaptiveBitrateEncoderReconfigInterval,
                adaptiveBitrateWarCntThreshold, adaptiveMinimumBitrate, adaptiveMaximumBitrate, useHardWareEncoding);
        // 开始倒计时
        startTimer();
        isRecording = true;
    }

    public boolean isPlayingAccompany() {
        boolean ret = false;
        if (null != playerService) {
            ret = playerService.isPlayingAccompany();
        }
        return ret;
    }

    protected int startConsumer(final String outputPath, final int videoWidth, final int videoHeight, final int audioSampleRate,
                                int qualityStrategy, int adaptiveBitrateWindowSizeInSecs, int adaptiveBitrateEncoderReconfigInterval,
                                int adaptiveBitrateWarCntThreshold, int adaptiveMinimumBitrate,
                                int adaptiveMaximumBitrate) {
        qualityStrategy = ifQualityStrayegyEnable(qualityStrategy);
        return Videostudio.getInstance().startVideoRecord(outputPath,
                videoWidth, videoHeight, VIDEO_FRAME_RATE, COMMON_VIDEO_BIT_RATE,
                audioSampleRate, audioChannels, audioBitRate,
                qualityStrategy, adaptiveBitrateWindowSizeInSecs, adaptiveBitrateEncoderReconfigInterval,
                adaptiveBitrateWarCntThreshold,adaptiveMinimumBitrate,adaptiveMaximumBitrate, recordingStudioStateCallback);
    }

    private int ifQualityStrayegyEnable(int qualityStrategy) {
    		qualityStrategy =  (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) ? 0 : qualityStrategy;
    		return qualityStrategy;
    }

    protected boolean startProducer(final int videoWidth, int videoHeight, boolean useHardWareEncoding, int strategy) throws StartRecordingException {
        if (playerService != null) {
            playerService.start();
        }
        if (recorderService != null) {
            return recorderService.start(videoWidth, videoHeight, VideoRecordingStudio.getInitializeVideoBitRate(), VIDEO_FRAME_RATE, useHardWareEncoding, strategy);
        }
        return false;
    }

    @Override
    public void destroyRecordingResource() {
        // 销毁伴奏播放器
        if (playerService != null) {
            playerService.stop();
            playerService = null;
        }
        super.destroyRecordingResource();
    }

    /**
     * 播放一个新的伴奏
     **/
    public void startAccompany(String musicPath) {
        if (null != playerService) {
            playerService.startAccompany(musicPath);
        }
    }

    /**
     * 停止播放
     **/
    public void stopAccompany() {
        if (null != playerService) {
            playerService.stopAccompany();
        }
    }

    /**
     * 暂停播放
     **/
    public void pauseAccompany() {
        if (null != playerService) {
            playerService.pauseAccompany();
        }
    }

    /**
     * 继续播放
     **/
    public void resumeAccompany() {
        if (null != playerService) {
            playerService.resumeAccompany();
        }
    }

    /**
     * 设置伴奏的音量大小
     **/
    public void setAccompanyVolume(float volume, float accompanyMax) {
        if (null != playerService) {
            playerService.setVolume(volume, accompanyMax);
        }
    }
    // -------------------------------------- 短视频分段管理 ---------------------------------

    /**
     * 判断是否正在录制
     * @return
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * 获取录制的总时长
     * @return
     */
    public int getDuration() {
        int duration = 0;
        if (mVideoList != null) {
            for (RecordItem recordItem : mVideoList) {
                duration += recordItem.getDuration();
            }
        }
        return duration;
    }

    /**
     * 添加分段视频
     * @param path      视频路径
     * @param duration  视频时长
     */
    public void addSubVideo(String path, int duration) {
        if (mVideoList == null) {
            mVideoList = new LinkedList<RecordItem>();
        }
        RecordItem recordItem = new RecordItem();
        recordItem.mediaPath = path;
        recordItem.duration = duration;
        mVideoList.add(recordItem);
    }

    /**
     * 移除当前分段视频
     */
    public void removeLastSubVideo() {
        RecordItem recordItem = mVideoList.get(mVideoList.size() - 1);
        mVideoList.remove(recordItem);
        if (recordItem != null) {
            recordItem.delete();
            mVideoList.remove(recordItem);
        }
    }

    /**
     * 删除所有分段视频
     */
    public void removeAllSubVideo() {
        if (mVideoList != null) {
            for (RecordItem part : mVideoList) {
                part.delete();
            }
            mVideoList.clear();
        }
    }

    /**
     * 获取分段视频路径
     * @return
     */
    public List<String> getSubVideoPathList() {
        if (mVideoList == null || mVideoList.isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> mediaPaths = new ArrayList<String>();
        for (int i = 0; i < mVideoList.size(); i++) {
            mediaPaths.add(i, mVideoList.get(i).getMediaPath());
        }
        return mediaPaths;
    }

    /**
     * 获取分段视频数量
     * @return
     */
    public int getNumberOfSubVideo() {
        return mVideoList.size();
    }

    /**
     * 删除一段已记录的时长
     */
    public void deleteRecordDuration() {
        resetDuration();
        resetLastSecondStop();
    }

    /**
     * 取消录制
     */
    public void cancelRecording() {
        cancelTimerWithoutSaving();
    }

    /**
     * 设置录制监听器
     * @param listener
     * @return
     */
    public CommonVideoRecordingStudio setOnRecordListener(OnRecordListener listener) {
        mRecordListener = listener;
        return this;
    }

    /**
     * 重置当前走过的时长
     */
    public void resetDuration() {
        mCurrentDuration = 0;
    }

    /**
     * 重置最后一秒停止标志
     */
    private void resetLastSecondStop() {
        mLastSecondStop = false;
    }

    /**
     * 获取显示的时间文本
     * @return
     */
    public String getVisibleDurationString() {
        return StringUtils.generateMillisTime((int) getVisibleDuration());
    }

    /**
     * 获取显示的时长
     */
    public long getVisibleDuration() {
        return getVisibleDuration( false);
    }

    /**
     * 获取显示的时长
     * @param finish    是否完成
     * @return
     */
    private long getVisibleDuration(boolean finish) {
        if (finish) {
            return mMaxMillisSeconds;
        } else {
            long time = getDuration() + mCurrentDuration;
            if (time > mMaxMillisSeconds) {
                time = mMaxMillisSeconds;
            }
            return time;
        }
    }

    // 倒计时Handler
    @SuppressWarnings("HandlerLeak")
    private Handler mTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            cancelCountDown();
        }
    };

    // --------------------------------------- 计时器操作 ------------------------------------------
    /**
     * 初始化倒计时
     */
    private void initTimer() {

        cancelCountDown();

        mRecordTimer = new RecordTimer(mMaxMillisSeconds, mCountDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!mTimerFinish) {
                    // 获取视频总时长
                    int previousDuration = getDuration();
                    // 获取当前分段视频走过的时间
                    mCurrentDuration = mMaxMillisSeconds - millisUntilFinished;
                    // 如果总时长够设定的最大时长，则需要停止计时
                    if (previousDuration + mCurrentDuration >= mMaxMillisSeconds) {
                        mCurrentDuration = mMaxMillisSeconds - previousDuration;
                        mTimerFinish = true;
                    }
                    // 计时回调
                    if (mRecordListener != null) {
                        mRecordListener.onRecordProgressChanged(getVisibleDuration());
                    }
                    // 是否需要结束计时器
                    if (mTimerFinish) {
                        mTimerHandler.sendEmptyMessage(0);
                    }
                }
            }

            @Override
            public void onFinish() {
                mTimerFinish = true;
                if (mRecordListener != null) {
                    mRecordListener.onRecordProgressChanged(getVisibleDuration(true));
                }
            }
        };
    }

    /**
     * 开始倒计时
     */
    private void startTimer() {
        if (mRecordTimer != null) {
            mRecordTimer.start();
        }
    }

    /**
     * 停止倒计时
     */
    public void stopTimer() {
        isRecording = false;
        // 重置最后一秒停止标志
        mLastSecondStop = false;
        // 判断是否需要处理最后一秒的情况
        if (mProcessLastSecond) {
            // 如果在下一次计时器回调之前剩余时间小于1秒，则表示是最后一秒内点击了停止
            if (getAvailableTime() + mCountDownInterval < 1000) {
                mLastSecondStop = true;
                mLastSecondLeftTime = getAvailableTime();
            }
        }
        // 如果不是最后一秒，则立即停止
        if (!mLastSecondStop) {
            cancelCountDown();
        }
    }

    /**
     * 取消倒计时，不保存走过的时长、停止标志、剩余时间等
     */
    private void cancelTimerWithoutSaving() {
        cancelCountDown();
        resetDuration();
        resetLastSecondStop();
        mLastSecondLeftTime = 0;
    }

    /**
     * 取消倒计时
     */
    public void cancelCountDown() {
        if (mRecordTimer != null) {
            mRecordTimer.cancel();
            mRecordTimer = null;
        }
        // 复位结束标志
        mTimerFinish = false;
    }

    /**
     * 获取剩余时间
     * @return
     */
    private long getAvailableTime() {
        return mMaxMillisSeconds - getDuration() - mCurrentDuration;
    }

    /**
     * 获取当前实际时长 (跟显示的时长不一定不一样)
     * @return
     */
    private long getRealDuration() {
        // 如果是最后一秒内点击，则计时器走过的时长要比视频录制的时长短一些，需要减去多余的时长
        if (mLastSecondLeftTime > 0) {
            long realTime = mCurrentDuration - mLastSecondLeftTime;
            mLastSecondLeftTime = 0;
            return realTime;
        }
        return mCurrentDuration;
    }
}
