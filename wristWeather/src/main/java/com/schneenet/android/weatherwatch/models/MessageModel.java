package com.schneenet.android.weatherwatch.models;

import android.content.Context;

import com.schneenet.android.weatherwatch.R;

/**
 * Simple model for WeatherAdapter.CardType.MESSAGE
 */
public class MessageModel
{
	public static Builder from(Context ctxt)
	{
		return new Builder(ctxt);
	}

	private CharSequence mMessage;
	private int mTextColor;

	private MessageModel(CharSequence text, int textColor)
	{
		mMessage = text;
		mTextColor = textColor;
	}

	public CharSequence getMessage()
	{
		return mMessage;
	}

	public int getTextColor()
	{
		return mTextColor;
	}

	public static class Builder
	{
		private Context mCtxt;
		private CharSequence mText;
		private int mTextColor;

		private Builder(Context ctxt)
		{
			mCtxt = ctxt;
			mText = "";
			mTextColor = mCtxt.getResources().getColor(R.color.primary_text);
		}

		public Builder withText(int stringRes)
		{
			mText = mCtxt.getString(stringRes);
			return this;
		}

		public Builder withText(CharSequence text)
		{
			mText = text;
			return this;
		}

		public Builder withTextColor(int color)
		{
			mTextColor = color;
			return this;
		}

		public Builder withTextColorRes(int colorRes)
		{
			mTextColor = mCtxt.getResources().getColor(colorRes);
			return this;
		}

		public MessageModel build()
		{
			return new MessageModel(mText, mTextColor);
		}

	}
}
