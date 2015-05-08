package com.schneenet.android.weatherwatch;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Simple fragment adapter
 */
public class FragmentAdapter extends FragmentPagerAdapter
{
	private ArrayList<FragmentHolder> mFragmentList = new ArrayList<>();

	public FragmentAdapter(FragmentManager fm)
	{
		super(fm);
	}

	public void add(int id, Fragment fragment)
	{
		mFragmentList.add(new FragmentHolder(id, fragment));
	}

	public int getPositionFromId(int id)
	{
		for (int i = 0; i < mFragmentList.size(); i++)
		{
			if (mFragmentList.get(i).identifer == id)
			{
				return i;
			}
		}
		return -1;
	}

	public int getIdAtPosition(int position)
	{
		if (exists(position))
		{
			return mFragmentList.get(position).identifer;
		}
		return -1;
	}

	public boolean exists(int position)
	{
		return position > -1 && position < mFragmentList.size();
	}

	@Override
	public Fragment getItem(int position)
	{
		return mFragmentList.get(position).fragment;
	}

	@Override
	public int getCount()
	{
		return mFragmentList.size();
	}

	private static class FragmentHolder
	{
		FragmentHolder(int id, Fragment f)
		{
			identifer = id;
			fragment = f;
		}

		private int identifer;
		private Fragment fragment;
	}
}
