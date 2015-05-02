package com.schneenet.android.weatherwatch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

public class InformationService extends Service implements OnSharedPreferenceChangeListener
{

	public static final String ACTION_RECONFIGURE = "com.schneenet.android.weatherwatch.InformationService.ACTION_RECONFIGURE";

	private final static UUID PEBBLE_APP_UUID = UUID.fromString("455367bd-36cc-4f1d-8002-ff84b2356582");

	private static final int KEY_WEATHER_TEMPERATURE = 0;
	private static final int KEY_WEATHER_CONDITIONS = 1;
	private static final int KEY_WEATHER_CITY = 2;
	private static final int KEY_WEATHER_ICON = 3;

	private static final int ICON_INDEX_LOADING = 0;
	private static final int ICON_INDEX_ERROR = 1;
	private static final int ICON_INDEX_CLEAR_DAY = 2;
	private static final int ICON_INDEX_CLEAR_NIGHT = 3;
	private static final int ICON_INDEX_CLOUDY = 4;
	private static final int ICON_INDEX_FOG = 5;
	private static final int ICON_INDEX_PARTLY_CLOUDY_DAY = 6;
	private static final int ICON_INDEX_PARTLY_CLOUDY_NIGHT = 7;
	private static final int ICON_INDEX_RAIN = 8;
	private static final int ICON_INDEX_SLEET = 9;
	private static final int ICON_INDEX_SNOW = 10;
	private static final int ICON_INDEX_WIND = 11;

	private static final int PREF_TEMPUNIT_K = 0;
	private static final int PREF_TEMPUNIT_C = 1;
	private static final int PREF_TEMPUNIT_F = 2;
	
	private static final int PREF_LOCATIONSOURCE_AUTO = 0;
	//private static final int PREF_LOCATIONSOURCE_NETWORK = 1;
	private static final int PREF_LOCATIONSOURCE_GPS = 2;
	
	private static final long MIN_LOCATION_TIME = 300000; // 5 minutes 

	// Status
	private ScheduledThreadPoolExecutor mTimerExecutor;
	private ScheduledFuture<?> mUpdateTask;
	private boolean mSendToPebble = false;

	// Configuration
	private long mDelay = 30; // minutes
	private int mTempUnit = PREF_TEMPUNIT_F; // Default to degrees F
	private String mApiKey = "";
	private int mLocationProvider = PREF_LOCATIONSOURCE_AUTO;

	// Cached data:
	private int mCachedIconImage = ICON_INDEX_LOADING;
	private String mCachedConditions = "Loading...";
	private String mCachedCity = "N/A";
	private String mCachedTemperature = "N/A";
	
	// Cached location
	private Object mCachedLocationLockObject = new Object();
	private Location mCachedLocation;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		PebbleKit.registerPebbleConnectedReceiver(this, mPebbleConnectedReceiver);
		PebbleKit.registerPebbleDisconnectedReceiver(this, mPebbleDisconnectedReceiver);
		PebbleKit.registerReceivedDataHandler(this, mPebbleDataReceiver);

		registerReceiver(mConfiguredReceiver, new IntentFilter(ACTION_RECONFIGURE));
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		
		mTimerExecutor = new ScheduledThreadPoolExecutor(1);
		configureService();

		return START_STICKY;
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(mConfiguredReceiver);
		LocationManager locationService = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationService.removeUpdates(mLocationListener);
	}

	private void configureService()
	{
		// Cancel pending weather updates
		if (mUpdateTask != null) mUpdateTask.cancel(false);

		// Pull settings from SharedPreferences
		String tempunitKey = getString(R.string.prefs_tempunit_key);
		String updatefreqKey = getString(R.string.prefs_updatefreq_key);
		String apikeyKey = getString(R.string.prefs_apikey_key);
		String locationsourceKey = getString(R.string.prefs_locationsource_key);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mTempUnit = prefs.getInt(tempunitKey, PREF_TEMPUNIT_F);
		mDelay = prefs.getLong(updatefreqKey, 30);
		mApiKey = prefs.getString(apikeyKey, null);
		mLocationProvider = prefs.getInt(locationsourceKey, PREF_LOCATIONSOURCE_AUTO);
		
		// Make sure defaults are set in the preferences
		if (!prefs.contains(tempunitKey))
		{
			prefs.edit().putInt(tempunitKey, PREF_TEMPUNIT_F).apply();
		}
		if (!prefs.contains(updatefreqKey))
		{
			prefs.edit().putLong(updatefreqKey, 30).apply();
		}
		if (!prefs.contains(locationsourceKey))
		{
			prefs.edit().putInt(locationsourceKey, PREF_LOCATIONSOURCE_AUTO).apply();
		}
		
		// Register for location updates
		LocationManager locationService = (LocationManager) getSystemService(LOCATION_SERVICE);
		String provider = LocationManager.NETWORK_PROVIDER;
		if (mLocationProvider == PREF_LOCATIONSOURCE_GPS || (mLocationProvider == PREF_LOCATIONSOURCE_AUTO && !locationService.isProviderEnabled(LocationManager.NETWORK_PROVIDER)))
		{
			provider = LocationManager.GPS_PROVIDER;
		}
		locationService.requestLocationUpdates(provider, MIN_LOCATION_TIME, 0, mLocationListener);
		
		// Schedule weather updates with given delay
		mUpdateTask = mTimerExecutor.scheduleWithFixedDelay(mLoadWeatherCommand, 0, mDelay, TimeUnit.MINUTES);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// This service does not support binding
		return null;
	}

	private void sendCachedUpdate()
	{
		if (mSendToPebble)
		{
			// Send cached data to pebble
			PebbleDictionary dict = new PebbleDictionary();
			dict.addString(KEY_WEATHER_CITY, mCachedCity);
			dict.addString(KEY_WEATHER_CONDITIONS, mCachedConditions);
			dict.addString(KEY_WEATHER_TEMPERATURE, mCachedTemperature);
			dict.addUint32(KEY_WEATHER_ICON, mCachedIconImage);
			PebbleKit.sendDataToPebble(this, PEBBLE_APP_UUID, dict);
		}
	}

	private BroadcastReceiver mPebbleConnectedReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			mSendToPebble = true;
			sendCachedUpdate();
		}
	};

	private BroadcastReceiver mPebbleDisconnectedReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			mSendToPebble = false;
		}
	};

	private PebbleDataReceiver mPebbleDataReceiver = new PebbleDataReceiver(PEBBLE_APP_UUID)
	{
		@Override
		public void receiveData(final Context context, final int transactionId, final PebbleDictionary data)
		{
			// Handle message
			Log.d(getClass().getSimpleName(), "Received message: " + data.toJsonString());
			sendCachedUpdate();

			// Send ack back to Pebble
			PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
		}
	};

	private BroadcastReceiver mConfiguredReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			configureService();
		}
	};
	
	private LocationListener mLocationListener = new LocationListener()
	{
		@Override
		public void onLocationChanged(Location location)
		{
			synchronized(mCachedLocationLockObject)
			{
				mCachedLocation = location;
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
			// Do nothing
		}

		@Override
		public void onProviderEnabled(String provider)
		{
			// Do nothing
		}

		@Override
		public void onProviderDisabled(String provider)
		{
			// Do nothing
		}
	};
	
	private Runnable mLoadWeatherCommand = new Runnable()
	{
		@Override
		public void run()
		{
			if (mApiKey == null || "".equals(mApiKey))
			{
				Log.w(getClass().getSimpleName(), "No API Key defined.");
				return;
			}

			// Load weather
			try
			{
				URL url = new URL(String.format("%s?lat=%f&long=%f", getString(R.string.str_weather_api_url), mCachedLocation.getLatitude(), mCachedLocation.getLongitude()));
				HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
				httpConn.setRequestProperty("x-api-key", mApiKey);
				InputStream in = new BufferedInputStream(httpConn.getInputStream());
				
				try
				{
					// Process JSON response
					Scanner scanner = new Scanner(in, httpConn.getContentEncoding());
					scanner.useDelimiter("\\A");
					if (scanner.hasNext())
					{
						// Process response
						JSONObject responseObj = new JSONObject(scanner.next());
						JSONArray weatherObjects = (JSONArray) responseObj.get("weather");
						JSONObject weatherObj = (JSONObject) weatherObjects.get(0);
						mCachedConditions = weatherObj.getString("main");
						mCachedCity = responseObj.getString("name");
						
						// Process temperature
						JSONObject mainWeatherObject = (JSONObject) responseObj.get("main");
						double rawTemp = mainWeatherObject.getDouble("temp"); // rawTemp in degrees Kelvin
						
						switch (mTempUnit)
						{
						case PREF_TEMPUNIT_C:
							double tempC = 0;
							mCachedTemperature = getString(R.string.str_degrees_c, tempC);
							break;
						case PREF_TEMPUNIT_F:
							double tempF = 0;
							mCachedTemperature = getString(R.string.str_degrees_f, tempF);
							break;
						case PREF_TEMPUNIT_K:
						default:
							mCachedTemperature = getString(R.string.str_degrees_k, rawTemp);
							break;
						}
						
						// Process icon image
						String icon = weatherObj.getString("icon");
						boolean isDay = icon.endsWith("d");
						int weatherCode = weatherObj.getInt("id");
						switch (weatherCode)
						{
							case 200:
							case 201:
							case 202:
							case 210:
							case 211:
							case 212:
							case 221:
							case 230:
							case 231:
							case 232:
							case 900:
							case 901:
							case 902:
								// TODO Thunderstorm icon
								break;
							case 300:
							case 301:
							case 302:
							case 310:
							case 311:
							case 312:
							case 313:
							case 314:
							case 321:
							case 500:
							case 501:
							case 502:
							case 503:
							case 504:
							case 520:
							case 521:
							case 522:
							case 531:
								mCachedIconImage = ICON_INDEX_RAIN;
								break;
							case 511:
							case 611:
							case 612:
								mCachedIconImage = ICON_INDEX_SLEET;
								break;
							case 600:
							case 601:
							case 602:
							case 615:
							case 616:
							case 620:
							case 621:
							case 622:
								mCachedIconImage = ICON_INDEX_SNOW;
								break;
							case 701:
							case 711:
							case 721:
							case 731:
							case 741:
							case 751:
							case 761:
							case 762:
							case 771:
							case 781:
								mCachedIconImage = ICON_INDEX_FOG;
								break;
							case 800:
								mCachedIconImage = isDay ? ICON_INDEX_CLEAR_DAY : ICON_INDEX_CLEAR_NIGHT;
								break;
							case 801:
							case 802:
							case 803:
								mCachedIconImage = isDay ? ICON_INDEX_PARTLY_CLOUDY_DAY : ICON_INDEX_PARTLY_CLOUDY_NIGHT;
								break;
							case 804:
								mCachedIconImage = ICON_INDEX_CLOUDY;
								break;
							case 903:
								// TODO Cold icon
								break;
							case 904:
								// TODO Hot icon
								break;
							case 905:
							case 951:
							case 952:
							case 953:
							case 954:
							case 955:
							case 956:
							case 957:
							case 958:
							case 959:
							case 960:
							case 961:
							case 962:
								mCachedIconImage = ICON_INDEX_WIND;
								break;
							case 906:
								// TODO Hail icon
								break;
							default:
								mCachedIconImage = ICON_INDEX_ERROR;
								break;
						}
					}
					scanner.close();
				}
				catch (JSONException e)
				{
					Log.e(getClass().getSimpleName(), "Failed to parse weather response", e);
					mCachedIconImage = ICON_INDEX_ERROR;
				}
				finally
				{
					in.close();
				}
			}
			catch (IOException ex)
			{
				Log.e(getClass().getSimpleName(), "Failed to get weather data", ex);
				mCachedIconImage = ICON_INDEX_ERROR;
			}
		}
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		configureService();
	}
}
