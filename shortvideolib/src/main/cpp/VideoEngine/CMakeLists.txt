set(PATH_TO_PRE_BUILT ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/armeabi-v7a)

add_definitions("
-DGL_GLEXT_PROTOTYPES
-DEGL_EGLEXT_PROTOTYPES
")

# 添加头文件路径
include_directories(../thirdparty/ffmpeg/include/)
include_directories(/)
include_directories(common/)
include_directories(camera_preview/)
include_directories(livecore/)
include_directories(livecore/common/)
include_directories(livecore/audio_encoder/)
include_directories(livecore/consumer/)
include_directories(livecore/platform_dependent/)
include_directories(livecore/publisher/)
include_directories(music_decoder/)
include_directories(recorder_processor/)
include_directories(video_consumer/)
include_directories(video_consumer/libaudio_encoder/)
include_directories(video_consumer/libmusic_merger/)
include_directories(video_consumer/libvideo_consumer/)
include_directories(video_encoder/)
include_directories(video_encoder/soft_encoder/)
include_directories(video_encoder/hw_encoder/)

# 添加源文件
file(GLOB FILES_TEST "*.cpp")
file(GLOB FILES_JNI_LAYER "android/*.cpp")
file(GLOB FILES_LIB_COMMON "common/*.cpp")
file(GLOB FILES_LIB_COMMON_EGL_CORE "common/egl_core/*.cpp")
file(GLOB FILES_LIB_COMMON_EGL_CORE "common/egl_core/*.cpp")
file(GLOB FILES_LIB_COMMON_RESAMPLER "common/libresampler/*.cpp")
file(GLOB FILES_LIB_COMMON_MSG_Q "common/message_queue/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA "common/opengl_media/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA_RENDER "common/opengl_media/render/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA_TEXTURE "common/opengl_media/texture/*.cpp")
file(GLOB FILES_LIB_COMMON_OPENGL_MEDIA_TEX_COPIER "common/opengl_media/texture_copier/*.cpp")
file(GLOB FILES_LIB_CAMERA_PREVIEW "camera_preview/*.cpp")
file(GLOB FILES_LIB_LIVE_CORE_COMMON "livecore/common/*.cpp")
file(GLOB FILES_LIB_LIVE_CORE_AUDIOENCODER "livecore/audio_encoder/*.cpp")
file(GLOB FILES_LIB_LIVE_CORE_CONSUMER "livecore/consumer/*.cpp")
file(GLOB FILES_LIB_LIVE_CORE_PUBLISHER "livecore/publisher/*.cpp")
file(GLOB FILES_LIB_MUSIC_DECODER "music_decoder/*.cpp")
file(GLOB FILES_LIB_RECORDER_PROCESSOR "recorder_processor/*.cpp")
file(GLOB FILES_LIB_VIDEO_CONSUMER_A_ENCODER "video_consumer/libaudio_encoder/*.cpp")
file(GLOB FILES_LIB_VIDEO_CONSUMER_MUSIC_MERGER "video_consumer/libmusic_merger/*.cpp")
file(GLOB FILES_LIB_VIDEO_CONSUMER "video_consumer/libvideo_consumer/*.cpp")
file(GLOB FILES_LIB_VIDEO_ENCODER "video_encoder/*.cpp")
file(GLOB FILES_LIB_VIDEO_ENCODER_HW "video_encoder/hw_encoder/*.cpp")
file(GLOB FILES_LIB_VIDEO_ENCODER_X264 "video_encoder/soft_encoder/*.cpp")
file(GLOB FILES_LIB_VIDEO_ENCODER_SOFT_COLOR "video_encoder/soft_encoder/color_conversion/color_conversion_armv7a.cpp")

add_library(video_engine SHARED
        ${FILES_TEST}
        ${FILES_JNI_LAYER}
        ${FILES_LIB_COMMON}
        ${FILES_LIB_COMMON_EGL_CORE}
        ${FILES_LIB_COMMON_MSG_Q}
        ${FILES_LIB_COMMON_RESAMPLER}
        ${FILES_LIB_COMMON_OPENGL_MEDIA}
        ${FILES_LIB_COMMON_OPENGL_MEDIA_RENDER}
        ${FILES_LIB_COMMON_OPENGL_MEDIA_TEXTURE}
        ${FILES_LIB_COMMON_OPENGL_MEDIA_TEX_COPIER}
        ${FILES_LIB_CAMERA_PREVIEW}
        ${FILES_LIB_LIVE_CORE_COMMON}
        ${FILES_LIB_LIVE_CORE_AUDIOENCODER}
        ${FILES_LIB_LIVE_CORE_CONSUMER}
        ${FILES_LIB_LIVE_CORE_PUBLISHER}
        ${FILES_LIB_MUSIC_DECODER}
        ${FILES_LIB_RECORDER_PROCESSOR}
        ${FILES_LIB_VIDEO_CONSUMER_A_ENCODER}
        ${FILES_LIB_VIDEO_CONSUMER_MUSIC_MERGER}
        ${FILES_LIB_VIDEO_CONSUMER}
        ${FILES_LIB_VIDEO_ENCODER}
        ${FILES_LIB_VIDEO_ENCODER_HW}
        ${FILES_LIB_VIDEO_ENCODER_X264}
        ${FILES_LIB_VIDEO_ENCODER_SOFT_COLOR}
        )

target_link_libraries(video_engine
        # 引入系统的动态库
        log
        android
        GLESv2
        EGL
        z
        OpenSLES
        # 引入ffmpeg库
        ${PATH_TO_PRE_BUILT}/libavfilter.a
        ${PATH_TO_PRE_BUILT}/libavformat.a
        ${PATH_TO_PRE_BUILT}/libavcodec.a
        ${PATH_TO_PRE_BUILT}/libpostproc.a
        ${PATH_TO_PRE_BUILT}/libswresample.a
        ${PATH_TO_PRE_BUILT}/libswscale.a
        ${PATH_TO_PRE_BUILT}/libavutil.a
        ${PATH_TO_PRE_BUILT}/libpostproc.a
        ${PATH_TO_PRE_BUILT}/libfdk-aac.a
        ${PATH_TO_PRE_BUILT}/libvo-aacenc.a
        ${PATH_TO_PRE_BUILT}/libx264.a
        ${PATH_TO_PRE_BUILT}/libsox.a
        )

