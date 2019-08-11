package com.mlingdu.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mlingdu.demo.R;
import com.mlingdu.demo.entity.Song;

public class SongSelectionActivity extends Activity {

	private Button song_1;
	private Button song_2;
	private Button song_3;
	
	public static final String SONG_ID = "song_id";
	public static final int ACCOMPANY_TYPE = 0;
	public static final int BGM_TYPE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.song_selection_layout);
		findView();
		bindListener();
	}
	private void findView() {
		song_1 = (Button) findViewById(R.id.song_1);
		song_2 = (Button) findViewById(R.id.song_2);
		song_3 = (Button) findViewById(R.id.song_3);
	}
	private void bindListener() {
		song_1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				Song song = new Song();
				song.setArtist("Pascal Letoublon");
				song.setSongId(1);
				song.setName("Friendships");
				song.setType(BGM_TYPE);
				bundle.putSerializable(SONG_ID, song);
				intent.putExtras(bundle);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		song_2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				Song song = new Song();
				song.setArtist("DJ");
				song.setSongId(199);
				song.setName("Take Me To Infinity");
				song.setType(ACCOMPANY_TYPE);
				bundle.putSerializable(SONG_ID, song);
				intent.putExtras(bundle);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		song_3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				Song song = new Song();
				song.setArtist("周笔畅");
				song.setSongId(3);
				song.setName("最美的期待");
				song.setType(BGM_TYPE);
				bundle.putSerializable(SONG_ID, song);
				intent.putExtras(bundle);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
	}
	
}
