package com.mlingdu.demo.activity;

import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.mlingdu.demo.R;
import com.mlingdu.demo.entity.Music;
import com.mlingdu.demo.fragment.CameraFragment;
import com.mlingdu.demo.fragment.MusicSelectFragment;
import com.mlingdu.demo.util.PermissionUtils;

public class MagicCameraActivity extends AppCompatActivity implements CameraFragment.OnSelectMusicListener,
        MusicSelectFragment.OnMusicSelectedListener{
    static {
        System.loadLibrary("video_engine");
    }

    private static final String FRAGMENT_CAMERA = "fragment_magic_camera";
    private static final String FRAGMENT_MUSIC_SELECT = "fragment_music_select";
    private static final int REQUEST_CODE = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_magic_camera);
        if (null == savedInstanceState) {
            CameraFragment fragment = new CameraFragment();
            fragment.setOnSelectMusicListener(this);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, FRAGMENT_CAMERA)
                    .addToBackStack(FRAGMENT_CAMERA)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }

    @Override
    public void onBackPressed() {
        // 判断fragment栈中的个数，如果只有一个，则表示当前只处于预览主页面点击返回状态
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackEntryCount > 1) {
            getSupportFragmentManager().popBackStack();
        } else if (backStackEntryCount == 1) {
            CameraFragment fragment = (CameraFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_CAMERA);
            if (fragment != null) {
                if (!fragment.onBackPressed()) {
                    finish();
                }
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackEntryCount == 1) {
            CameraFragment fragment = (CameraFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_CAMERA);
            if (fragment != null) {
                fragment.setOnSelectMusicListener(null);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onOpenSelectMusicPage() {
        MusicSelectFragment fragment = new MusicSelectFragment();
        fragment.addOnMusicSelectedListener(this);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container, fragment)
                .addToBackStack(FRAGMENT_MUSIC_SELECT)
                .commit();
    }

    @Override
    public void onMusicSelected(Music music) {
        getSupportFragmentManager().popBackStack(FRAGMENT_CAMERA, 0);
        CameraFragment fragment = (CameraFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_CAMERA);
        if (fragment != null) {
            fragment.setSelectedMusic(music.getSongUrl(), music.getDuration());
        }
    }

    public native String stringFromJNI();
}
