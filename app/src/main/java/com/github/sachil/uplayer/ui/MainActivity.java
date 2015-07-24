package com.github.sachil.uplayer.ui;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.meta.Device;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.github.sachil.uplayer.R;
import com.github.sachil.uplayer.UplayerUnity;
import com.github.sachil.uplayer.upnp.dmc.DeviceRegistryListener;
import com.github.sachil.uplayer.upnp.dmr.MediaRenderer;
import com.github.sachil.uplayer.upnp.dms.ContentGenerator;
import com.github.sachil.uplayer.upnp.dms.MediaServer;

public class MainActivity extends AppCompatActivity
		implements ServiceConnection {

	private static final String TAG = MainActivity.class.getSimpleName();
	private Context mContext = null;
	private DrawerLayout mDrawerLayout = null;
	private Toolbar mToolbar = null;
	private RecyclerView mRecyclerView = null;
	private NavigationView mNavigationView = null;
	private NavManager mNavmanager = null;
	private AndroidUpnpService mUpnpService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Fresco.initialize(this);
		setContentView(R.layout.activity_main);
		mContext = this;
		mContext = this;
		UplayerUnity.setContext(mContext);
		initView();
		bindService(new Intent(this, AndroidUpnpServiceImpl.class), this,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		mNavmanager.clean();
		super.onDestroy();
	}

	@Override
	public void onServiceConnected(ComponentName componentName,
			IBinder iBinder) {
		Log.e(TAG, "Service is connected.");
		mUpnpService = (AndroidUpnpService) iBinder;
		mNavmanager = new NavManager(mContext,
				findViewById(android.R.id.content), mUpnpService);
		MediaServer server = new MediaServer(mContext,
				UplayerUnity.getInetAddress(mContext));
		mUpnpService.getRegistry().addDevice(server.getDevice());
		MediaRenderer renderer = new MediaRenderer(mContext);
		mUpnpService.getRegistry().addDevice(renderer.getDevice());
		ContentGenerator.prepareAudio(mContext, server);

		DeviceRegistryListener listener = new DeviceRegistryListener();
		for (Device device : mUpnpService.getRegistry().getDevices())
			listener.refreshDevice(device, true);
		mUpnpService.getRegistry().addListener(listener);
		mUpnpService.getControlPoint().search();
	}

	@Override
	public void onServiceDisconnected(ComponentName componentName) {
		Log.e(TAG, "Service is disconnected.");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_settings:

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void initView() {

		mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer);
		((CollapsingToolbarLayout) findViewById(R.id.main_toolbar_layout))
				.setTitle(getString(R.string.app_name));
		mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
				mDrawerLayout, mToolbar, R.string.app_name, R.string.app_name);
		setSupportActionBar(mToolbar);
		toggle.syncState();
		mDrawerLayout.setDrawerListener(toggle);
		mRecyclerView = (RecyclerView) findViewById(R.id.main_recyclerview);
		mNavigationView = (NavigationView) findViewById(R.id.main_navigation);
	}
}