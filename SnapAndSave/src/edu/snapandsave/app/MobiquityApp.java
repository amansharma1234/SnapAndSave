package edu.snapandsave.app;

import android.app.Application;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import edu.snapandsave.utils.Constants;

/**
 * Application class
 * @author amansharma
 *
 */
public class MobiquityApp extends Application {

	private DropboxAPI<AndroidAuthSession> mDBApi; 

	@Override
	@SuppressWarnings("deprecation")
	public void onCreate() {
		super.onCreate();
		
		/* Initialize DropBoxAPI */
		AppKeyPair appKeys = new AppKeyPair(Constants.APP_KEY, Constants.APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, Constants.ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
	}

	public DropboxAPI<AndroidAuthSession> getmDBApi() {
		return mDBApi;
	}

	public void setmDBApi(DropboxAPI<AndroidAuthSession> mDBApi) {
		this.mDBApi = mDBApi;
	}
	
	
}
