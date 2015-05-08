package com.schneenet.android.weatherwatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;
import com.schneenet.android.weatherwatch.icons.WeatherWatchIcons;
import com.schneenet.android.weatherwatch.models.MessageModel;
import com.schneenet.android.weatherwatch.utils.WeatherUtils;
import com.schneenet.android.weatherwatch.views.anim.BaseAnimatorListener;

import net.aksingh.owmjapis.AbstractWeather;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.DailyForecast;
import net.aksingh.owmjapis.HourlyForecast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * BaseAdapter than displays weather cards of varying layouts
 */
public class WeatherAdapter extends BaseAdapter implements ListView.OnItemClickListener
{

	private ArrayList<ViewHolder> mData = new ArrayList<>();
	private Context mContext;
	private int mTempUnit;
	private int mWindSpeedUnit;
	private int mPressureUnit;
	private int mExpandedIndex = -1;

	public WeatherAdapter(Context ctxt)
	{
		mContext = ctxt;
	}

	public void setTempUnit(int tempUnit)
	{
		mTempUnit = tempUnit;
	}

	public void setWindSpeedUnit(int windSpeedUnit)
	{
		mWindSpeedUnit = windSpeedUnit;
	}

	public void setPressureUnit(int pressureUnit)
	{
		mPressureUnit = pressureUnit;
	}

	@Override
	public int getCount()
	{
		return mData.size();
	}

	@Override
	public Object getItem(int position)
	{
		ViewHolder holder = mData.get(position);
		return holder != null ? holder.weatherObj : null;
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder = mData.get(position);
		if (holder.viewTree == null)
		{
			holder.viewTree = LayoutInflater.from(mContext).inflate(R.layout.card_layout, parent, false);
			ViewGroup container = (ViewGroup) holder.viewTree.findViewById(R.id.card_container);
			View card = LayoutInflater.from(mContext).inflate(holder.type.getLayoutRes(), container, false);
			container.addView(card);
		}
		populateViewTree(holder);
		return holder.viewTree;
	}

	public void setData(int position, CardType cardType, Object data)
	{
		if (exists(position))
		{
			ViewHolder holder = mData.get(position);
			holder.weatherObj = data;
			if (holder.type != cardType) holder.viewTree = null;
			holder.type = cardType;
			populateViewTree(holder);
		}
		else
		{
			addData(cardType, data);
		}
	}

	public void addData(CardType cardType, Object data)
	{
		mData.add(new ViewHolder(cardType, data));
		notifyDataSetChanged();
	}

	public void remove(int position)
	{
		mData.remove(position);
		notifyDataSetChanged();
	}

	public void removeAll()
	{
		mData.clear();
		notifyDataSetChanged();
	}

	public boolean exists(int position)
	{
		return position > -1 && position < mData.size();
	}

	private void populateViewTree(ViewHolder holder)
	{
		if (holder.viewTree != null)
		{
			switch (holder.type)
			{
				case MESSAGE:
					MessageModel messageModel = (MessageModel) holder.weatherObj;
					TextView messageTextView = (TextView) holder.viewTree.findViewById(R.id.message_text);
					messageTextView.setText(messageModel.getMessage());
					messageTextView.setTextColor(messageModel.getTextColor());
					ImageView messageImageView = (ImageView) holder.viewTree.findViewById(R.id.message_icon);
					IconicsDrawable iconicsDrawable = new IconicsDrawable(mContext, FontAwesome.Icon.faw_exclamation_triangle).colorRes(R.color.accent).sizeRes(R.dimen.default_icon_size);
					messageImageView.setImageDrawable(iconicsDrawable);
					break;
				case DAILY_FORECAST:
					DailyForecast.Forecast dailyForecast = (DailyForecast.Forecast) holder.weatherObj;
					ImageView dailyForecastTitleIcon = (ImageView) holder.viewTree.findViewById(R.id.forecast_title_icon);
					dailyForecastTitleIcon.setImageDrawable(new IconicsDrawable(mContext, FontAwesome.Icon.faw_calendar_o).colorRes(R.color.primary));
					TextView dailyForecastTitleView = (TextView) holder.viewTree.findViewById(R.id.forecast_time_display);
					SimpleDateFormat dateFormat = new SimpleDateFormat(mContext.getString(R.string.daily_forecast_date_format));
					dailyForecastTitleView.setText(dateFormat.format(dailyForecast.getDateTime()));
					ImageView dailyForecastIconView = (ImageView) holder.viewTree.findViewById(R.id.forecast_icon);
					dailyForecastIconView.setImageDrawable(getWeatherIconDrawable(mContext, getWeatherIcon(dailyForecast.getWeatherInstance(0))));
					TextView dailyForecastConditions = (TextView) holder.viewTree.findViewById(R.id.forecast_conditions);
					dailyForecastConditions.setText(dailyForecast.getWeatherInstance(0).getWeatherName());
					TextView dailyForecastTempHi = (TextView) holder.viewTree.findViewById(R.id.forecast_temperature_high);
					dailyForecastTempHi.setText(InformationService.getTemperatureString(mContext, mTempUnit, dailyForecast.getTemperatureInstance().getMaximumTemperature()));
					TextView dailyForecastTempLo = (TextView) holder.viewTree.findViewById(R.id.forecast_temperature_low);
					dailyForecastTempLo.setText(InformationService.getTemperatureString(mContext, mTempUnit, dailyForecast.getTemperatureInstance().getMinimumTemperature()));
					break;
				case HOURLY_FORECAST:
					HourlyForecast.Forecast hourlyForecast = (HourlyForecast.Forecast) holder.weatherObj;
					ImageView hourlyForecastTitleIcon = (ImageView) holder.viewTree.findViewById(R.id.weather_title_icon);
					hourlyForecastTitleIcon.setImageDrawable(new IconicsDrawable(mContext, FontAwesome.Icon.faw_bolt).colorRes(R.color.primary));
					TextView hourlyForecastTitleView = (TextView) holder.viewTree.findViewById(R.id.weather_time_display);
					Date hourlyForecastDate = hourlyForecast.getDateTime();
					String timeDisplay = "";
					if (!WeatherUtils.isSameDay(hourlyForecastDate, new Date()))
					{
						timeDisplay += new SimpleDateFormat(mContext.getString(R.string.daily_forecast_date_format)).format(hourlyForecastDate) + " ";
					}
					timeDisplay += DateFormat.getTimeInstance(DateFormat.SHORT).format(hourlyForecastDate);
					hourlyForecastTitleView.setText(timeDisplay);
					ImageView hourlyForecastIconView = (ImageView) holder.viewTree.findViewById(R.id.weather_icon);
					hourlyForecastIconView.setImageDrawable(getWeatherIconDrawable(mContext, getWeatherIcon(hourlyForecast.getWeatherInstance(0))));
					TextView hourlyForecastConditions = (TextView) holder.viewTree.findViewById(R.id.weather_conditions);
					hourlyForecastConditions.setText(hourlyForecast.getWeatherInstance(0).getWeatherName());
					TextView hourlyForecastTemperature = (TextView) holder.viewTree.findViewById(R.id.weather_temperature);
					hourlyForecastTemperature.setText(InformationService.getTemperatureString(mContext, mTempUnit, hourlyForecast.getMainInstance().getTemperature()));
					TextView hourlyForecastCity = (TextView) holder.viewTree.findViewById(R.id.weather_city);
					hourlyForecastCity.setVisibility(View.GONE);
					break;
				case CURRENT_CONDITIONS:
				default:
					CurrentWeather weather = (CurrentWeather) holder.weatherObj;
					ImageView titleIcon = (ImageView) holder.viewTree.findViewById(R.id.weather_title_icon);
					titleIcon.setImageDrawable(new IconicsDrawable(mContext, FontAwesome.Icon.faw_sun_o).colorRes(R.color.primary));
					TextView titleView = (TextView) holder.viewTree.findViewById(R.id.weather_time_display);
					new Iconics.IconicsBuilder().ctx(mContext).on(titleView).build();
					ImageView iconView = (ImageView) holder.viewTree.findViewById(R.id.weather_icon);
					iconView.setImageDrawable(getWeatherIconDrawable(mContext, getWeatherIcon(weather.getWeatherInstance(0))));
					TextView conditions = (TextView) holder.viewTree.findViewById(R.id.weather_conditions);
					conditions.setText(weather.getWeatherInstance(0).getWeatherName());
					TextView temperature = (TextView) holder.viewTree.findViewById(R.id.weather_temperature);
					temperature.setText(InformationService.getTemperatureString(mContext, mTempUnit, weather.getMainInstance().getTemperature()));
					TextView city = (TextView) holder.viewTree.findViewById(R.id.weather_city);
					city.setText(weather.getCityName());
					TextView fieldWind = (TextView) holder.viewTree.findViewById(R.id.field_wind);
					CurrentWeather.Wind wind = weather.getWindInstance();
					String windStr =
							InformationService.getWindSpeedString(mContext, mWindSpeedUnit, wind.getWindSpeed()) +
							" " +
							InformationService.getWindDirectionString(mContext, wind.getWindDegree());
					fieldWind.setText(windStr);
					DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT);
					TextView fieldSunrise = (TextView) holder.viewTree.findViewById(R.id.field_sunrise);
					fieldSunrise.setText(formatter.format(weather.getSysInstance().getSunriseTime()));
					TextView fieldSunset = (TextView) holder.viewTree.findViewById(R.id.field_sunset);
					fieldSunset.setText(formatter.format(weather.getSysInstance().getSunsetTime()));
					TextView fieldHumidity = (TextView) holder.viewTree.findViewById(R.id.field_humidity);
					fieldHumidity.setText(mContext.getString(R.string.field_humidity, weather.getMainInstance().getHumidity()));
					TextView fieldPressure = (TextView) holder.viewTree.findViewById(R.id.field_pressure);
					fieldPressure.setText(InformationService.getPressureString(mContext, mPressureUnit, weather.getMainInstance().getPressure()));
					break;
			}
		}
	}

	public static WeatherWatchIcons.Icon getWeatherIcon(AbstractWeather.Weather weather)
	{
		String iconName = weather.getWeatherIconName();
		int weatherCode = weather.getWeatherCode();
		return InformationService.getIconForOWMCode(weatherCode, iconName.endsWith("d"));
	}

	public static IconicsDrawable getWeatherIconDrawable(Context ctxt, String iconName)
	{
		return getWeatherIconDrawable(ctxt, WeatherWatchIcons.Icon.valueOf(iconName));
	}

	public static IconicsDrawable getWeatherIconDrawable(Context ctxt, WeatherWatchIcons.Icon icon)
	{
		return new IconicsDrawable(ctxt, icon).colorRes(R.color.primary);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if (exists(position))
		{
			ViewHolder holder = mData.get(position);
			if (holder.type.isExpandable())
			{
				if (exists(mExpandedIndex))
				{
					ViewHolder oldHolder = mData.get(mExpandedIndex);
					final View detailsView = oldHolder.viewTree.findViewById(R.id.details_view);
					ValueAnimator valueAnimator = ValueAnimator.ofInt(detailsView.getHeight(), 0);
					valueAnimator.setInterpolator(new DecelerateInterpolator());
					valueAnimator.setDuration(100);
					valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
					{
						@Override
						public void onAnimationUpdate(ValueAnimator animation)
						{
							detailsView.getLayoutParams().height = (int) animation.getAnimatedValue();
							detailsView.requestLayout();
						}
					});
					valueAnimator.addListener(new BaseAnimatorListener()
					{
						@Override
						public void onAnimationEnd(Animator animation)
						{
							detailsView.setVisibility(View.GONE);
						}
					});
					valueAnimator.start();
				}
				if (position == mExpandedIndex)
				{
					mExpandedIndex = -1;
					return;
				}

				final View detailsView = holder.viewTree.findViewById(R.id.details_view);
				detailsView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				int height = detailsView.getMeasuredHeight();
				detailsView.getLayoutParams().height = 0;
				detailsView.setVisibility(View.VISIBLE);
				ValueAnimator valueAnimator = ValueAnimator.ofInt(0, height);
				valueAnimator.setInterpolator(new DecelerateInterpolator());
				valueAnimator.setDuration(100);
				valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
				{
					@Override
					public void onAnimationUpdate(ValueAnimator animation)
					{
						detailsView.getLayoutParams().height = (int) animation.getAnimatedValue();
						detailsView.requestLayout();
					}
				});
				valueAnimator.start();
				mExpandedIndex = position;
			}
		}
	}

	public enum CardType
	{
		MESSAGE(R.layout.message_card, false),
		CURRENT_CONDITIONS(R.layout.weather_card, true),
		HOURLY_FORECAST(R.layout.weather_card, false), // TODO Hourly forecast UI
		DAILY_FORECAST(R.layout.forecast_card, false);

		CardType(int layoutRes, boolean expandable)
		{
			mLayoutRes = layoutRes;
			mExpandable = expandable;
		}

		private int mLayoutRes;
		private boolean mExpandable;

		public int getLayoutRes()
		{
			return mLayoutRes;
		}

		public boolean isExpandable()
		{
			return mExpandable;
		}
	}

	private static class ViewHolder
	{
		ViewHolder(CardType cardType, Object data)
		{
			type = cardType;
			weatherObj = data;
		}

		private CardType type;
		private View viewTree;
		private Object weatherObj;
	}
}
