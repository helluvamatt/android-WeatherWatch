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
	private ArrayList<Fragment> mFragmentList = new ArrayList<>();

	public FragmentAdapter(FragmentManager fm)
	{
		super(fm);
	}

	public ArrayList<Fragment> getFragmentList()
	{
		return mFragmentList;
	}

	@Override
	public Fragment getItem(int position)
	{
		return mFragmentList.get(position);
	}

	@Override
	public int getCount()
	{
		return mFragmentList.size();
	}
}
