package edu.snapandsave;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;

import edu.snapandsave.app.MobiquityApp;

/**
 * Home Activity
 * @author amansharma
 *
 */
public class HomeActivity extends Activity {

	private static final int REQUEST_IMAGE_CAPTURE = -1, REQUEST_TAKE_PHOTO = 1;
	private DropboxAPI<AndroidAuthSession> mDBApi;
	private String mCurrentPhotoPath, mImageFileName;
	
	private MediaRecorder mMediaRecorder;
	private boolean recording = false;
	private Handler showSuccessHandler;
	
	private LocationManager mLocationManager;
	private String mLocationProvider = "";
	private Location mLastKnownLocation; 


	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
			
		/* Task -- Allows a user to authenticate with their Dropbox credentials */
		mDBApi = ((MobiquityApp)getApplication()).getmDBApi();
		if(!mDBApi.getSession().isLinked()) {
			mDBApi.getSession().startOAuth2Authentication(getBaseContext());
		}
		
		showSuccessHandler = new Handler();
		
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		boolean isNetworkProviderAvail = 
				mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		
		
		if(isNetworkProviderAvail) { 
			mLocationProvider = LocationManager.NETWORK_PROVIDER;
		} else {
			boolean isGPSProvider = 
					mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			if(isGPSProvider) {
				mLocationProvider = LocationManager.GPS_PROVIDER;
			}
		}
		
		mLocationManager.requestLocationUpdates(
				mLocationProvider, 10000, 0, locationListener);
	}
	
	protected void onResume() {
	    super.onResume();

	    if (mDBApi.getSession().authenticationSuccessful()) {
	        try {
	            mDBApi.getSession().finishAuthentication();
	            String accessToken = mDBApi.getSession().getOAuth2AccessToken();
	            System.out.println(accessToken);
	        } catch (IllegalStateException e) {
	        }
	    }
	}
	
	/**
	 * Checks if the resultCode is correct and save the file to dropbox
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == REQUEST_IMAGE_CAPTURE) {
			String city = getNearestCity();
			saveToDropBox(city);
		}
	}
	

	/**
	 * Task -- Allows a user to take a photo
	 * 
	 * launches camera to take photo
	 * @param v
	 */
	public void takePhoto(View v) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
	        File photoFile = null;
	        try {
	            photoFile = createFile();
	        } catch (IOException ex) {
	        }

	        if (photoFile != null) {
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
	                    Uri.fromFile(photoFile));
	            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
	        }
	    }
	} 
	
	/**
	 * Launch Show Photos Activity
	 * @param v
	 */
	public void showPhotos(View v) {
		Intent showPhotosIntent = new Intent(this,ShowPhotosActivity.class);
		startActivity(showPhotosIntent);
	}
	
	
	/**
	 * get nearest city with respect to the latitude and longitude
	 * 
	 * Task -- Title of the photo should contain city where it was taken
	 * @return
	 */
	private String getNearestCity() {
		mLastKnownLocation = mLocationManager.getLastKnownLocation(mLocationProvider);
		if(mLastKnownLocation == null) {
			mLastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		if(mLastKnownLocation != null) {
			Geocoder gcd = new Geocoder(this, Locale.getDefault());
			List<Address> addresses;
			try {
				addresses = gcd.getFromLocation(mLastKnownLocation.getLatitude(), 
						mLastKnownLocation.getLongitude(), 1);
				if (addresses.size() > 0)  {
					return addresses.get(0).getLocality();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return "";
	}
	
	/**
	 * creates photo file with a timeStamp in the name
	 * @return
	 * @throws IOException
	 */
	@SuppressLint("SimpleDateFormat")
	private File createFile() throws IOException {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		mImageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES);
		File image = File.createTempFile(
				mImageFileName,  
				".jpg",         
				storageDir      
				);

		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}
	
	/**
	 *  Save file to dropbox. The file name has cityname prepended
	 *  to it.
	 *  
	 *  Task -- Uploads the photo to a directory on Dropbox
	 */
	private void saveToDropBox(final String cityName) {
		Runnable saveFileRunnable = new Runnable() {

			@Override
			public void run() {
				File file = new File(mCurrentPhotoPath);
				try { 
					saveCoordinates(mCurrentPhotoPath);
					FileInputStream inputStream = new FileInputStream(file);
					final Entry response = mDBApi.putFile(cityName + "_" + mImageFileName 
							+ ".jpg", inputStream,
							file.length(), null, null);
					if(response != null) {
						showToast(response.rev);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};

		// thread to initiate file save
		Thread thread = new Thread(saveFileRunnable);
		thread.start();
	}
	
	/**
	 * Shows a Toast for successful File upload
	 * @param rev
	 */
	private void showToast(final String rev) {
		this.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(),
						"File uploaded to Dropbox" + ": revision - " 
								+ rev, 
								Toast.LENGTH_SHORT).show();
			}
		});
//		showSuccessHandler.post(new Runnable() {
// 
//			@Override
//			public void run() {
//				// show toast on success
//				Toast.makeText(getApplicationContext(),
//						"File uploaded to Dropbox" + ": revision - " 
//								+ rev, 
//								Toast.LENGTH_SHORT).show();
//			}
//		});
	}

	
	/**
	 * Task -- Records the GPS coordinates of the photograph
	 */
	private void saveCoordinates(String fileName) { 
		try {
			if(mLastKnownLocation != null) {
				ExifInterface exif = new ExifInterface(fileName);
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE
						, mLastKnownLocation.getLatitude() + "");
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, 
						mLastKnownLocation.getLongitude() + "");
				exif.saveAttributes();
				Log.d("LOCATION - LAT: ", mLastKnownLocation.getLatitude() + "");
				Log.d("LOCATION - LONG: ", mLastKnownLocation.getLongitude() + "");

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			Log.d("LOCATION", location + "");
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	    
	};
	
	/**
	 * Task -- Allow user to record sound clips in addition to photographs
	 * 
	 * Partially Done
	 * Unable to test the recording feature on Emulator 
	 */
	@SuppressLint("SimpleDateFormat")
	private void setUpMediaRecorder() {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String clipFileName = "CLIP_" + timeStamp + "_" + ".mp4";
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mMediaRecorder.setOutputFile(clipFileName); 
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	}
	
	public void record(View v) {
		recording = !recording;
		if(recording) {
			setUpMediaRecorder();
			((Button)v).setText("Stop recording");
			try {
				mMediaRecorder.prepare();
			} catch (IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mMediaRecorder.start();
		} else {
			((Button)v).setText("Start recording");
			mMediaRecorder.stop();
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
	}

}
