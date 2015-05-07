package com.schneenet.android.weatherwatch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.mikepenz.meteocons_typeface_library.Meteoconcs;
import com.schneenet.android.weatherwatch.utils.WeatherUtils;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.DailyForecast;
import net.aksingh.owmjapis.HourlyForecast;
import net.aksingh.owmjapis.OpenWeatherMap;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InformationService extends Service implements OnSharedPreferenceChangeListener
{
	private final static String TAG = "InformationService";
	private final static UUID PEBBLE_APP_UUID = UUID.fromString("455367bd-36cc-4f1d-8002-ff84b2356582");

	private static final int KEY_WEATHER_TEMPERATURE = 0;
	private static final int KEY_WEATHER_CONDITIONS = 1;
	private static final int KEY_WEATHER_CITY = 2;
	private static final int KEY_WEATHER_ICON = 3;

	private static final int PREF_TEMPUNIT_K = 0;
	private static final int PREF_TEMPUNIT_C = 1;
	private static final int PREF_TEMPUNIT_F = 2;

	private static final int PREF_SPEEDUNIT_MPH = 0;
	private static final int PREF_SPEEDUNIT_KPH = 1;
	private static final int PREF_SPEEDUNIT_KNOTS = 2;
	private static final int PREF_SPEEDUNIT_MPS = 3;

	private static final int PREF_PRESSUREUNIT_INHG = 0;
	private static final int PREF_PRESSUREUNIT_MMHG = 1;
	private static final int PREF_PRESSUREUNIT_HPA = 2;

	private static final int PREF_LOCATIONSOURCE_AUTO = 0;
	//private static final int PREF_LOCATIONSOURCE_NETWORK = 1;
	private static final int PREF_LOCATIONSOURCE_GPS = 2;

	private static final long MIN_LOCATION_TIME = 300000; // 5 minutes 

	// Status
	private ScheduledThreadPoolExecutor mTimerExecutor;
	private ScheduledFuture<?> mUpdateTask;
	private Handler mHandler = new Handler();
	private boolean mSendToPebble;
	private Throwable mLastError_CurrentWeather;
	private Throwable mLastError_HourlyForecast;
	private Throwable mLastError_DailyForecast;

	// Configuration
	private long mDelay = 30; // minutes
	private int mTempUnit = PREF_TEMPUNIT_F; // Default to degrees F
	private int mWindSpeedUnit = PREF_SPEEDUNIT_MPH; // Default to MPH
	private int mPressureUnit = PREF_PRESSUREUNIT_INHG; // Default to in Hg (US)
	private String mApiKey = "";
	private byte mForecastDays = 10; // TODO Configuration for this

	// Cached data:
	private CurrentWeather mCachedWeather;
	private HourlyForecast mCachedHourlyForecast;
	private DailyForecast mCachedDailyForecast;

	// Cached location
	private final Object mCachedLocationLockObject = new Object();
	private Location mCachedLocation;

	// Binding
	private final ServiceBinder mBinder = new ServiceBinder();
	private HashSet<UpdateHandler> mUpdateHandlers = new HashSet<>();

	@Override
	public void onCreate()
	{
		super.onCreate();

		PebbleKit.registerPebbleConnectedReceiver(this, mPebbleConnectedReceiver);
		PebbleKit.registerPebbleDisconnectedReceiver(this, mPebbleDisconnectedReceiver);
		PebbleKit.registerReceivedDataHandler(this, mPebbleDataReceiver);
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

		mTimerExecutor = new ScheduledThreadPoolExecutor(1);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		configureService();
		mBinder.refreshWeather();
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		LocationManager locationService = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationService.removeUpdates(mLocationListener);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	private void configureService()
	{
		// Pull settings from SharedPreferences
		String tempunitKey = getString(R.string.prefs_tempunit_key);
		String updatefreqKey = getString(R.string.prefs_updatefreq_key);
		String apikeyKey = getString(R.string.prefs_apikey_key);
		String locationsourceKey = getString(R.string.prefs_locationsource_key);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mTempUnit = Integer.parseInt(prefs.getString(tempunitKey, Integer.toString(PREF_TEMPUNIT_F)));
		mDelay = Long.parseLong(prefs.getString(updatefreqKey, "30"));
		mApiKey = prefs.getString(apikeyKey, null);
		int locationProvider = Integer.parseInt(prefs.getString(locationsourceKey, Integer.toString(PREF_LOCATIONSOURCE_AUTO)));

		// Make sure defaults are set in the preferences
		if (!prefs.contains(tempunitKey))
		{
			prefs.edit().putString(tempunitKey, Integer.toString(PREF_TEMPUNIT_F)).apply();
		}
		if (!prefs.contains(updatefreqKey))
		{
			prefs.edit().putString(updatefreqKey, "30").apply();
		}
		if (!prefs.contains(locationsourceKey))
		{
			prefs.edit().putString(locationsourceKey, Integer.toString(PREF_LOCATIONSOURCE_AUTO)).apply();
		}

		// Register for location updates
		LocationManager locationService = (LocationManager) getSystemService(LOCATION_SERVICE);
		String provider = LocationManager.NETWORK_PROVIDER;
		if (locationProvider == PREF_LOCATIONSOURCE_GPS || (locationProvider == PREF_LOCATIONSOURCE_AUTO && !locationService.isProviderEnabled(LocationManager.NETWORK_PROVIDER)))
		{
			provider = LocationManager.GPS_PROVIDER;
		}
		locationService.requestLocationUpdates(provider, MIN_LOCATION_TIME, 0, mLocationListener);
	}

	private void sendPebbleUpdate()
	{
		Log.i(getClass().getSimpleName(), "sendCachedUpdate() called...");
		if (mCachedWeather != null)
		{
			if (mSendToPebble)
			{
				// Process icon image
				String iconName = mCachedWeather.getWeatherInstance(0).getWeatherIconName();
				int weatherCode = mCachedWeather.getWeatherInstance(0).getWeatherCode();
				Meteoconcs.Icon icon = getIconForOWMCode(weatherCode, iconName.endsWith("d"));

				// Send cached data to pebble
				PebbleDictionary dict = new PebbleDictionary();
				dict.addString(KEY_WEATHER_CITY, mCachedWeather.getCityName());
				dict.addString(KEY_WEATHER_CONDITIONS, mCachedWeather.getWeatherInstance(0).getWeatherName());
				dict.addString(KEY_WEATHER_TEMPERATURE, getTemperatureString(this, mTempUnit, mCachedWeather.getMainInstance().getTemperature()));
				dict.addInt8(KEY_WEATHER_ICON, (byte) icon.getCharacter());
				PebbleKit.sendDataToPebble(this, PEBBLE_APP_UUID, dict);
			}
		}
		else if (mLastError_CurrentWeather != null)
		{
			PebbleDictionary dict = new PebbleDictionary();
			dict.addString(KEY_WEATHER_CITY, "");
			dict.addString(KEY_WEATHER_CONDITIONS, getString(R.string.error_check_app));
			dict.addString(KEY_WEATHER_TEMPERATURE, getString(R.string.default_na));
			dict.addInt8(KEY_WEATHER_ICON, (byte) Meteoconcs.Icon.met_na.getCharacter());
			PebbleKit.sendDataToPebble(this, PEBBLE_APP_UUID, dict);
		}
		else
		{
			// Send "Loading..." data to pebble
			PebbleDictionary dict = new PebbleDictionary();
			dict.addString(KEY_WEATHER_CITY, "");
			dict.addString(KEY_WEATHER_CONDITIONS, getString(R.string.loading));
			dict.addString(KEY_WEATHER_TEMPERATURE, getString(R.string.default_na));
			dict.addInt8(KEY_WEATHER_ICON, (byte) Meteoconcs.Icon.met_na.getCharacter());
			PebbleKit.sendDataToPebble(this, PEBBLE_APP_UUID, dict);
		}
	}

	private void sendUpdate()
	{
		// Send updates to handlers
		for (final UpdateHandler handler : mUpdateHandlers)
		{
			mHandler.post(new Runnable() {
				@Override
				public void run()
				{
					handler.onUpdate();
				}
			});
		}
	}

	private BroadcastReceiver mPebbleConnectedReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			mSendToPebble = true;
			sendPebbleUpdate();
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
			sendPebbleUpdate();

			// Send ack back to Pebble
			PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
		}
	};

	private LocationListener mLocationListener = new LocationListener()
	{
		@Override
		public void onLocationChanged(Location location)
		{
			synchronized (mCachedLocationLockObject)
			{
				boolean refreshNow = mCachedLocation == null;
				mCachedLocation = location;
				if (refreshNow) mBinder.refreshWeather();
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
			if (!mBinder.hasApiKey())
			{
				Log.w(getClass().getSimpleName(), "No API Key defined.");
				return;
			}

			synchronized (mCachedLocationLockObject)
			{
				if (mCachedLocation == null)
				{
					Log.w(getClass().getSimpleName(), "Don't yet have location.");
					return;
				}
			}

			// Create OpenWeatherMap client
			OpenWeatherMap owm = new OpenWeatherMap(OpenWeatherMap.Units.METRIC, mApiKey);

			// Load current weather
			Log.i(TAG, "Loading current conditions...");
			try
			{
				CurrentWeather cwd = owm.currentWeatherByCoordinates((float) mCachedLocation.getLatitude(), (float) mCachedLocation.getLongitude());
				if (cwd.isValid())
				{
					mCachedWeather = cwd;
					mLastError_CurrentWeather = null;
				}
				else
				{
					Log.w(TAG, "Current conditions response was invalid.");
				}
			}
			catch (Exception ex)
			{
				Log.e(getClass().getSimpleName(), "Failed to get weather data", ex);
				mCachedWeather = null;
				mLastError_CurrentWeather = ex;
			}

			// Load hourly forecast
			Log.i(TAG, "Loading hourly forecast...");
			try
			{
				HourlyForecast hourlyForecast = owm.hourlyForecastByCoordinates((float) mCachedLocation.getLatitude(), (float) mCachedLocation.getLongitude());
				if (hourlyForecast.isValid())
				{
					mCachedHourlyForecast = hourlyForecast;
					mLastError_HourlyForecast = null;
				}
				else
				{
					Log.w(TAG, "Hourly forecast response was invalid.");
				}
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Failed to get hourly forecast data", ex);
				mCachedHourlyForecast = null;
				mLastError_HourlyForecast = ex;
			}

			// Load daily forecast
			Log.i(TAG, "Loading daily forecast...");
			try
			{
				DailyForecast dailyForecast = owm.dailyForecastByCoordinates((float) mCachedLocation.getLatitude(), (float) mCachedLocation.getLongitude(), mForecastDays);
				if (dailyForecast.isValid())
				{
					mCachedDailyForecast = dailyForecast;
					mLastError_DailyForecast = null;
				}
				else
				{
					Log.w(TAG, "Daily forecast response was invalid.");
				}
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Failed to load daily forecast", ex);
				mCachedDailyForecast = null;
				mLastError_DailyForecast = ex;
			}

			sendUpdate();
			sendPebbleUpdate();

			Log.i(TAG, "Done with update");
		}
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		String updatefreqKey = getString(R.string.prefs_updatefreq_key);
		if (key.equals(updatefreqKey) && mUpdateTask != null)
		{
			// Cancel pending weather updates
			mUpdateTask.cancel(false);
		}
		configureService();
		if (key.equals(updatefreqKey))
		{
			mBinder.refreshWeather();
		}
	}

	public static Meteoconcs.Icon getIconForOWMCode(int weatherCode, boolean isDay)
	{
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
				// Storm
				return Meteoconcs.Icon.met_clouds_flash;
			case 300:
			case 301:
			case 302:
			case 310:
			case 311:
			case 312:
			case 313:
			case 314:
			case 321:
				// Drizzle
				return Meteoconcs.Icon.met_drizzle;
			case 500:
			case 501:
			case 502:
			case 503:
			case 504:
			case 520:
			case 521:
			case 522:
			case 531:
				// Rain
				return Meteoconcs.Icon.met_rain;
			case 511:
			case 611:
			case 612:
				// Sleet
				// TODO Sleet (for now, fall down to snow)
			case 600:
			case 601:
			case 615:
			case 616:
			case 620:
			case 621:
				// Snow
				return Meteoconcs.Icon.met_snow;
			case 602:
			case 622:
				// Heavy snow
				return Meteoconcs.Icon.met_snow_heavy;
			case 701:
				// Mist
				return Meteoconcs.Icon.met_mist;
			case 711:
			case 721:
			case 731:
			case 741:
			case 751:
			case 761:
			case 762:
			case 771:
			case 781:
				// Fog
				return Meteoconcs.Icon.met_fog;
			case 800:
				// Clear / sunny
				return isDay ? Meteoconcs.Icon.met_sun : Meteoconcs.Icon.met_moon;
			case 801:
			case 802:
			case 803:
				// Partly cloudy
				return isDay ? Meteoconcs.Icon.met_cloud_sun : Meteoconcs.Icon.met_cloud_moon;
			case 804:
				// Cloudy
				return Meteoconcs.Icon.met_clouds;
			case 903:
				// Cold
				return Meteoconcs.Icon.met_snowflake;
			case 904:
				// Hot
				return Meteoconcs.Icon.met_temperature;
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
				// Wind
				return Meteoconcs.Icon.met_wind;
			case 906:
				// Hail
				return Meteoconcs.Icon.met_hail;
			default:
				return Meteoconcs.Icon.met_na;
		}
	}

	public static String getTemperatureString(Context ctxt, int tempUnit, float rawTemp)
	{
		// Process temperature to send
		String temperature;
		switch (tempUnit)
		{
			case PREF_TEMPUNIT_C:
				temperature = ctxt.getString(R.string.str_degrees_c, rawTemp);
				break;
			case PREF_TEMPUNIT_F:
				double tempF = WeatherUtils.tempCelsiusToFahrenheit(rawTemp);
				temperature = ctxt.getString(R.string.str_degrees_f, tempF);
				break;
			case PREF_TEMPUNIT_K:
			default:
				double tempK = WeatherUtils.tempCelsiusToKelvin(rawTemp);
				temperature = ctxt.getString(R.string.str_degrees_k, tempK);
				break;
		}
		return temperature;
	}

	public static String getWindSpeedString(Context ctxt, int windSpeedUnit, float rawWindSpeed)
	{
		String windSpeed;
		switch (windSpeedUnit)
		{
			case PREF_SPEEDUNIT_MPS:
				windSpeed = ctxt.getString(R.string.str_windspeed_mps, rawWindSpeed);
				break;
			case PREF_SPEEDUNIT_KPH:
				windSpeed = ctxt.getString(R.string.str_windspeed_kph, WeatherUtils.speedMpsToKph(rawWindSpeed));
				break;
			case PREF_SPEEDUNIT_KNOTS:
				windSpeed = ctxt.getString(R.string.str_windspeed_knots, WeatherUtils.speedMpsToKnots(rawWindSpeed));
				break;
			case PREF_SPEEDUNIT_MPH:
			default:
				windSpeed = ctxt.getString(R.string.str_windspeed_mph, WeatherUtils.speedMpsToMph(rawWindSpeed));
				break;
		}
		return windSpeed;
	}

	public static String getPressureString(Context ctxt, int pressureUnit, float rawPressure)
	{
		String pressure;
		switch (pressureUnit)
		{
			case PREF_PRESSUREUNIT_HPA:
				pressure = ctxt.getString(R.string.str_pressure_hpa, rawPressure);
				break;
			case PREF_PRESSUREUNIT_MMHG:
				pressure = ctxt.getString(R.string.str_pressure_mmhg, WeatherUtils.pressureHpaToMmhg(rawPressure));
				break;
			case PREF_PRESSUREUNIT_INHG:
			default:
				pressure = ctxt.getString(R.string.str_pressure_inhg, WeatherUtils.pressureHpaToInhg(rawPressure));
				break;
		}
		return pressure;
	}

	public static String getWindDirectionString(Context context, float degrees)
	{
		String[] strings = context.getResources().getStringArray(R.array.str_winddirection);
		int i = Math.round((degrees - 11.25f) / 22.5f);
		return strings[i];
	}

	public class ServiceBinder extends Binder
	{
		public void refreshWeather()
		{
			Log.i(getClass().getSimpleName(), "refreshWeather() called...");
			// Schedule weather updates with given delay
			mUpdateTask = mTimerExecutor.scheduleWithFixedDelay(mLoadWeatherCommand, 0, mDelay, TimeUnit.MINUTES);
		}

		public void addUpdateHandler(UpdateHandler handler)
		{
			mUpdateHandlers.add(handler);
		}

		public void removeUpdateHandler(UpdateHandler handler)
		{
			mUpdateHandlers.remove(handler);
		}

		public CurrentWeather getCachedCurrentWeather()
		{
			return mCachedWeather;
		}

		public HourlyForecast getCachedHourlyForecast()
		{
			return mCachedHourlyForecast;
		}

		public DailyForecast getCachedDailyForecast()
		{
			return mCachedDailyForecast;
		}

		public boolean hasLastErrorCurrentWeather()
		{
			return mLastError_CurrentWeather != null;
		}

		public Throwable getLastErrorCurrentWeather()
		{
			return mLastError_CurrentWeather;
		}

		public boolean hasLastErrorHourlyForecase()
		{
			return mLastError_HourlyForecast != null;
		}

		public Throwable getLastErrorHourlyForecast()
		{
			return mLastError_HourlyForecast;
		}

		public boolean hasLastErrorDailyForecast()
		{
			return mLastError_DailyForecast != null;
		}

		public Throwable getLastErrorDailyForecast()
		{
			return mLastError_DailyForecast;
		}

		public int getTempUnit()
		{
			return mTempUnit;
		}

		public int getWindSpeedUnit()
		{
			return mWindSpeedUnit;
		}

		public int getPressureUnit()
		{
			return mPressureUnit;
		}

		public boolean hasApiKey()
		{
			return mApiKey != null && !"".equals(mApiKey);
		}
	}

	public interface UpdateHandler
	{
		public void onUpdate();
	}
}
