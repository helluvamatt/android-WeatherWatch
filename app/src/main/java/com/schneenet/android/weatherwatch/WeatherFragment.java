package com.schneenet.android.weatherwatch;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.schneenet.android.weatherwatch.models.MessageModel;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.DailyForecast;
import net.aksingh.owmjapis.HourlyForecast;

/**
 * Fragment for displaying a simple weather report (like what is displayed on the watch)
 */
public class WeatherFragment extends Fragment implements InformationService.UpdateHandler, SwipeRefreshLayout.OnRefreshListener
{
	public static WeatherFragment newInstance(int mode)
	{
		WeatherFragment fragment = new WeatherFragment();
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, mode);
		fragment.setArguments(args);
		return fragment;
	}

	private static final String KEY_MODE = "com.schneenet.android.weatherwatch.WeatherFragment.KEY_MODE";
	public static final int MODE_TODAY = 0;
	public static final int MODE_HOURLY_FORECAST = 1;
	public static final int MODE_DAILY_FORECAST = 2;

	private SwipeRefreshLayout mSwipeRefreshLayout;
	private WeatherAdapter mWeatherAdapter;
	private int mMode;

	private InformationService.ServiceBinder mServiceInterface;
	private boolean mServiceBound = false;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mMode = getArguments().getInt(KEY_MODE);
		View v = inflater.inflate(R.layout.fragment_main, container, false);
		ListView cardsListView = (ListView) v.findViewById(R.id.cards_list);
		mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
		mSwipeRefreshLayout.setOnRefreshListener(this);
		mWeatherAdapter = new WeatherAdapter(getActivity());
		SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(mWeatherAdapter);
		animationAdapter.setAbsListView(cardsListView);
		cardsListView.setAdapter(animationAdapter);
		cardsListView.setOnItemClickListener(mWeatherAdapter);
		return v;
	}

	@Override
	public void onStart()
	{
		super.onStart();

		// Bind to service
		Intent serviceIntent = new Intent(getActivity(), InformationService.class);
		getActivity().bindService(serviceIntent, mServiceConnection, 0);
		mSwipeRefreshLayout.setRefreshing(true);
	}

	@Override
	public void onStop()
	{
		super.onStop();

		// Unbind service
		if (mServiceBound)
		{
			mServiceInterface.removeUpdateHandler(this);
			getActivity().unbindService(mServiceConnection);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(KEY_MODE, mMode);
		// TODO Cache text/icons to bundle for restoring later
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		mSwipeRefreshLayout = null;
		mWeatherAdapter = null;
	}

	private ServiceConnection mServiceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mServiceInterface = (InformationService.ServiceBinder) service;
			mServiceInterface.addUpdateHandler(WeatherFragment.this);
			mServiceBound = true;
			onUpdate();
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mServiceInterface = null;
			mServiceBound = false;
		}
	};

	@Override
	public void onUpdate()
	{
		if (mServiceBound && mWeatherAdapter != null)
		{
			mWeatherAdapter.setTempUnit(mServiceInterface.getTempUnit());
			mWeatherAdapter.setWindSpeedUnit(mServiceInterface.getWindSpeedUnit());
			mWeatherAdapter.setPressureUnit(mServiceInterface.getPressureUnit());
			if (!mServiceInterface.hasApiKey())
			{
				mWeatherAdapter.removeAll();
				mWeatherAdapter.setData(0, WeatherAdapter.CardType.MESSAGE, MessageModel.from(getActivity()).withText(R.string.error_no_api_key).build());
			}
			else
			{
				switch (mMode)
				{
					case MODE_DAILY_FORECAST:
						DailyForecast dailyForecast = mServiceInterface.getCachedDailyForecast();
						if (dailyForecast != null)
						{
							int count = dailyForecast.getForecastCount();
							int i;
							for (i = 0; i < count; i++)
							{
								mWeatherAdapter.setData(i, WeatherAdapter.CardType.DAILY_FORECAST, dailyForecast.getForecastInstance(i));
							}
							for (; i < mWeatherAdapter.getCount(); i++)
							{
								mWeatherAdapter.remove(i);
							}
						}
						else if (mServiceInterface.hasLastErrorDailyForecast())
						{
							mWeatherAdapter.removeAll();
							mWeatherAdapter.setData(0, WeatherAdapter.CardType.MESSAGE, MessageModel.from(getActivity()).withText(R.string.error_network).build());
						}
						break;
					case MODE_HOURLY_FORECAST:
						HourlyForecast hourlyForecast = mServiceInterface.getCachedHourlyForecast();
						if (hourlyForecast != null)
						{
							int count = hourlyForecast.getForecastCount();
							int i;
							for (i = 0; i < count; i++)
							{
								mWeatherAdapter.setData(i, WeatherAdapter.CardType.HOURLY_FORECAST, hourlyForecast.getForecastInstance(i));
							}
							for (; i < mWeatherAdapter.getCount(); i++)
							{
								mWeatherAdapter.remove(i);
							}
						}
						else if (mServiceInterface.hasLastErrorHourlyForecase())
						{
							mWeatherAdapter.removeAll();
							mWeatherAdapter.setData(0, WeatherAdapter.CardType.MESSAGE, MessageModel.from(getActivity()).withText(R.string.error_network).build());
						}
						break;
					case MODE_TODAY:
					default:
						CurrentWeather currentWeather = mServiceInterface.getCachedCurrentWeather();
						if (currentWeather != null)
						{
							mWeatherAdapter.setData(0, WeatherAdapter.CardType.CURRENT_CONDITIONS, currentWeather);
						}
						else if (mServiceInterface.hasLastErrorCurrentWeather())
						{
							mWeatherAdapter.setData(0, WeatherAdapter.CardType.MESSAGE, MessageModel.from(getActivity()).withText(R.string.error_network).build());
						}
						break;
				}
			}
			mSwipeRefreshLayout.setRefreshing(false);
		}
	}

	@Override
	public void onRefresh()
	{
		if (mServiceBound)
		{
			mServiceInterface.refreshWeather();
		}
		else
		{
			mSwipeRefreshLayout.setRefreshing(false);
		}
	}
}
