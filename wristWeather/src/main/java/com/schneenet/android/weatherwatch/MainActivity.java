package com.schneenet.android.weatherwatch;

import android.app.FragmentTransaction;
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

public class MainActivity extends AppCompatActivity implements Drawer.OnDrawerItemClickListener
{
	private Toolbar mToolbar;
	private Drawer.Result mDrawerResult;

	private ViewPager mPager;
	private FragmentAdapter mFragmentAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mPager = (ViewPager) findViewById(R.id.fragment_pager);
		mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		setSupportActionBar(mToolbar);

		mFragmentAdapter = new FragmentAdapter(getFragmentManager());
		mFragmentAdapter.getFragmentList().add(WeatherFragment.newInstance(WeatherFragment.MODE_TODAY));
		mFragmentAdapter.getFragmentList().add(WeatherFragment.newInstance(WeatherFragment.MODE_FORECAST));
		mFragmentAdapter.getFragmentList().add(SettingsFragment.newInstance());
		mPager.setAdapter(mFragmentAdapter);

		mDrawerResult = new Drawer()
				.withActivity(this)
				.withToolbar(mToolbar)
				.addDrawerItems(
						new PrimaryDrawerItem().withName(R.string.action_weather).withIdentifier(R.id.action_weather).withIcon(FontAwesome.Icon.faw_sun_o),
						new PrimaryDrawerItem().withName(R.string.action_forecast).withIdentifier(R.id.action_forecast).withIcon(FontAwesome.Icon.faw_bolt),
						new DividerDrawerItem(),
						new PrimaryDrawerItem().withName(R.string.action_settings).withIdentifier(R.id.action_settings).withIcon(GoogleMaterial.Icon.gmd_settings)
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
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem drawerItem)
	{
		if (drawerItem != null)
		{
			if (drawerItem instanceof Nameable)
			{
				mToolbar.setTitle(((Nameable) drawerItem).getNameRes());
			}

			// update the main content by replacing fragments
			switch (drawerItem.getIdentifier())
			{
				case R.id.action_settings:
					mPager.setCurrentItem(2);
					break;
				case R.id.action_forecast:
					mPager.setCurrentItem(1);
					break;
				case R.id.action_weather:
				default:
					mPager.setCurrentItem(0);
					break;
			}
		}
	}
}
