package com.schneenet.android.weatherwatch;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

public class MainActivity extends AppCompatActivity implements Drawer.OnDrawerItemClickListener, ViewPager.OnPageChangeListener
{
	private static final int ITEM_ID_TODAY = 0;
	private static final int ITEM_ID_HOURLY_FORECAST = 1;
	private static final int ITEM_ID_DAILY_FORECAST = 2;
	private static final int ITEM_ID_SETTINGS = 3;

	private Toolbar mToolbar;
	private Drawer.Result mDrawerResult;

	private FragmentAdapter mFragmentAdapter;
	private ViewPager mPager;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mPager = (ViewPager) findViewById(R.id.fragment_pager);
		mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		setSupportActionBar(mToolbar);

		mFragmentAdapter = new FragmentAdapter(getFragmentManager());
		mFragmentAdapter.add(ITEM_ID_TODAY, WeatherFragment.newInstance(WeatherFragment.MODE_TODAY));
		mFragmentAdapter.add(ITEM_ID_HOURLY_FORECAST, WeatherFragment.newInstance(WeatherFragment.MODE_HOURLY_FORECAST));
		mFragmentAdapter.add(ITEM_ID_DAILY_FORECAST, WeatherFragment.newInstance(WeatherFragment.MODE_DAILY_FORECAST));
		mFragmentAdapter.add(ITEM_ID_SETTINGS, SettingsFragment.newInstance());
		mPager.setAdapter(mFragmentAdapter);
		mPager.setOnPageChangeListener(this);

		mDrawerResult = new Drawer()
				.withActivity(this)
				.withToolbar(mToolbar)
				.addDrawerItems(
						new PrimaryDrawerItem().withName(R.string.action_weather).withIdentifier(ITEM_ID_TODAY).withIcon(FontAwesome.Icon.faw_sun_o),
						new PrimaryDrawerItem().withName(R.string.action_hourly_forecast).withIdentifier(ITEM_ID_HOURLY_FORECAST).withIcon(FontAwesome.Icon.faw_bolt),
						new PrimaryDrawerItem().withName(R.string.action_daily_forecast).withIdentifier(ITEM_ID_DAILY_FORECAST).withIcon(FontAwesome.Icon.faw_calendar_o),
						new DividerDrawerItem(),
						new PrimaryDrawerItem().withName(R.string.action_settings).withIdentifier(ITEM_ID_SETTINGS).withIcon(GoogleMaterial.Icon.gmd_settings)
				)
				.withOnDrawerItemClickListener(this)
				.withSavedInstance(savedInstanceState)
				.withFireOnInitialOnClick(true)
				.build();

		Intent serviceIntent = new Intent(this, InformationService.class);
		startService(serviceIntent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		mDrawerResult.saveInstanceState(outState);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long ignoreThisId, IDrawerItem drawerItem)
	{
		if (drawerItem != null)
		{
			// update the main content by replacing fragments
			int pagerPosition = mFragmentAdapter.getPositionFromId(drawerItem.getIdentifier());
			mPager.setCurrentItem(pagerPosition, true);

			// update
			setTitleFromDrawerItem(drawerItem);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
	{
		// Do nothing
	}

	@Override
	public void onPageSelected(int position)
	{
		// Only if we have a Drawer.Result
		if (mDrawerResult != null)
		{
			// Tell the drawer we have changed
			int drawerPosition = mDrawerResult.getPositionFromIdentifier(mFragmentAdapter.getIdAtPosition(position));
			mDrawerResult.setSelection(drawerPosition, false);

			// update the title
			setTitleFromDrawerItem(mDrawerResult.getDrawerItems().get(drawerPosition));
		}
	}

	@Override
	public void onPageScrollStateChanged(int state)
	{
		// Do nothing
	}

	private void setTitleFromDrawerItem(IDrawerItem drawerItem)
	{
		if (drawerItem instanceof Nameable)
		{
			getSupportActionBar().setTitle(((Nameable) drawerItem).getNameRes());
		}
	}
}
