package cn.nfcsolution.resistance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;

public class MyApplication extends Application {
	private static MyApplication mApplication;
	public static String Request_URL = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mApplication = this;
		Request_URL = getRequestURL(this); // Attain the HTTP Request URL.
		CrashReport.initCrashReport(mApplication, "b400e84083", true); // Transfer crash via back-end Bug system.
	}
	
	public static MyApplication getApplication() {
		return mApplication;
	}
	
	public static String globalURL() {
		if (Request_URL == null) {
			Request_URL = getRequestURL(getApplication());
		}
		return Request_URL;
	}
	
	public static String getRequestURL(final Context context) {
		final Properties pro = new Properties();
		InputStream fis = null;
		try {
			fis = context.getAssets().open("system.properties");
			pro.load(fis);
			final String url = pro.getProperty("JSONRPC_BASE");
			return url;
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (final IOException e) {
				fis = null;
				e.printStackTrace();
			}
		}
		return Request_URL;
	}
}
