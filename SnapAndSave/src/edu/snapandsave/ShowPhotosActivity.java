package edu.snapandsave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import edu.snapandsave.app.MobiquityApp;

/**
 * Task -- Lists all photos that have been uploaded to that directory, and view selected photos
 * @author amansharma
 *
 */

public class ShowPhotosActivity extends Activity {

	private DropboxAPI<AndroidAuthSession> mDBApi;
	private Handler mUpdateListHandler, mShowPhotoHandler; 
	private ArrayAdapter<String> mListAdpater;
	private ListView mListView;
	private List<String> mPhotoNames = new ArrayList<>();
	private ImageView mPhotoIv;
	private LinearLayout mPhotoLl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_photos);
		
		mDBApi = ((MobiquityApp)getApplication()).getmDBApi();
		mListView = (ListView) findViewById(R.id.all_photos_lv);
		mPhotoIv = (ImageView) findViewById(R.id.photo_iv);
		mPhotoLl = (LinearLayout) findViewById(R.id.photo_ll);
		
		mUpdateListHandler = new Handler();
		mShowPhotoHandler = new Handler();
		
		mPhotoNames = new ArrayList<String>();
		mListAdpater = new ArrayAdapter<>(this, R.layout.list_item_show_photos,
				R.id.photo_name_tv, mPhotoNames);
		mListView.setAdapter(mListAdpater);
		mListView.setOnItemClickListener(mPhotoClickListener);
		
		mPhotoLl.setOnClickListener(onPhotoLayoutClickListener);
		
		getPhotos();
	}


	/*
	 *  get all the photos and update the listview
	 */
	private void getPhotos() {
		Runnable getPhotosRunnable = new Runnable() {
			
			@Override
			public void run() {
				getListOfPhotos();
				updateListView();
			}
		};
		Thread thread = new Thread(getPhotosRunnable);
		thread.start();
	}
	
	private void updateListView() {
		mUpdateListHandler.post(new Runnable() {

			@Override
			public void run() {
				mListAdpater.notifyDataSetChanged();
			}
		});
	}
	
	/*
	 * Read details of all contents in the home directory with-in dropbox
	 */
	private void getListOfPhotos() {
		Entry existingEntries;
		try {
			existingEntries = mDBApi.metadata("/", 50, null, true, null);

			for (Entry e : existingEntries.contents) {
				if (!e.isDeleted) {
					mPhotoNames.add(e.fileName());
				}
			}
		} catch (DropboxException e1) {
			e1.printStackTrace();
		}
	}

	/*
	 * gets file from dropbox and stores it to a temp file 
	 * the file is set to ImageView using Handler
	 */
	protected void getFile(String fileName) {
		try {
			final String url = Environment.getExternalStorageDirectory() + 
					File.separator + "temp_file.jpg";
			File file = new File(url);
			if(!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream outputStream = new FileOutputStream(file);
			mDBApi.getFile(fileName, null, outputStream, null);
			mShowPhotoHandler.post(new Runnable() {
				
				@Override
				public void run() {
					Bitmap bitmap = null;
					try {
						bitmap = BitmapFactory.decodeStream((InputStream)
								new URL("file://" + url).getContent());
					} catch (IOException e) {
						e.printStackTrace();
					} 
					mPhotoIv.setImageBitmap(bitmap);
					mPhotoLl.setVisibility(View.VISIBLE);
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	private OnItemClickListener mPhotoClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, final int position,
				long id) {
			
			Thread thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					getFile(mPhotoNames.get(position));
				}
			});
			thread.start();
		}
	};
	
	private OnClickListener onPhotoLayoutClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			v.setVisibility(View.GONE);
		}
	};
	
}
