package com.schneenet.android.weatherwatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.meteocons_typeface_library.Meteoconcs;
import com.schneenet.android.weatherwatch.models.MessageModel;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.DailyForecast;
import net.aksingh.owmjapis.HourlyForecast;

import java.util.ArrayList;

/**
 * BaseAdapter than displays weather cards of varying layouts
 */
public class WeatherAdapter extends BaseAdapter
{

	private ArrayList<ViewHolder> mData = new ArrayList<>();
	private Context mContext;
	private int mTempUnit;

	public WeatherAdapter(Context ctxt)
	{
		mContext = ctxt;
	}

	public void setTempUnit(int tempUnit)
	{
		mTempUnit = tempUnit;
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
			holder.viewTree = LayoutInflater.from(mContext).inflate(holder.type.getLayoutRes(), parent, false);
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
					IconicsDrawable iconicsDrawable = new IconicsDrawable(mContext, FontAwesome.Icon.faw_exclamation_triangle).colorRes(R.color.colorAccent).sizeRes(R.dimen.default_icon_size);
					messageImageView.setImageDrawable(iconicsDrawable);
					break;
				case DAILY_FORECAST:
					DailyForecast.Forecast dailyForecast = (DailyForecast.Forecast) holder.weatherObj;
					// TODO Populate Daily forecast UI
					break;
				case HOURLY_FORECAST:
					HourlyForecast.Forecast hourlyForecast = (HourlyForecast.Forecast) holder.weatherObj;
					// TODO Populate Hourly forecast UI
					break;
				case CURRENT_CONDITIONS:
				default:
					CurrentWeather weather = (CurrentWeather) holder.weatherObj;
					ImageView iconView = (ImageView) holder.viewTree.findViewById(R.id.weather_icon);
					iconView.setImageDrawable(getWeatherIconDrawable(mContext, getWeatherIcon(weather)));
					TextView conditions = (TextView) holder.viewTree.findViewById(R.id.weather_conditions);
					conditions.setText(weather.getWeatherInstance(0).getWeatherName());
					TextView temperature = (TextView) holder.viewTree.findViewById(R.id.weather_temperature);
					temperature.setText(InformationService.getTemperatureString(mContext, mTempUnit, weather.getMainInstance().getTemperature()));
					TextView city = (TextView) holder.viewTree.findViewById(R.id.weather_city);
					city.setText(weather.getCityName());
					break;
			}
		}
	}

	public static Meteoconcs.Icon getWeatherIcon(CurrentWeather weather)
	{
		String iconName = weather.getWeatherInstance(0).getWeatherIconName();
		int weatherCode = weather.getWeatherInstance(0).getWeatherCode();
		return InformationService.getIconForOWMCode(weatherCode, iconName.endsWith("d"));
	}

	public static IconicsDrawable getWeatherIconDrawable(Context ctxt, String iconName)
	{
		return getWeatherIconDrawable(ctxt, Meteoconcs.Icon.valueOf(iconName));
	}

	public static IconicsDrawable getWeatherIconDrawable(Context ctxt, Meteoconcs.Icon icon)
	{
		return new IconicsDrawable(ctxt, icon).colorRes(R.color.colorPrimary);
	}

	public enum CardType
	{
		MESSAGE(R.layout.message_card),
		CURRENT_CONDITIONS(R.layout.weather_card),
		HOURLY_FORECAST(R.layout.weather_card), // TODO Hourly forecast UI
		DAILY_FORECAST(R.layout.weather_card); // TODO Daily forecast UI

		CardType(int layoutRes)
		{
			mLayoutRes = layoutRes;
		}

		private int mLayoutRes;

		public int getLayoutRes()
		{
			return mLayoutRes;
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
