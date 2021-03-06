package com.github.sachil.uplayer.ui;

import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.ExpandableListView;

import com.github.sachil.uplayer.R;
import com.github.sachil.uplayer.player.MusicService;
import com.github.sachil.uplayer.ui.adapter.NavAdapter;
import com.github.sachil.uplayer.ui.content.PatchedExpandableListView;
import com.github.sachil.uplayer.ui.message.ActionMessage;
import com.github.sachil.uplayer.ui.message.BrowseMessage;
import com.github.sachil.uplayer.ui.message.DeviceMessage;
import com.github.sachil.uplayer.upnp.UpnpUnity;
import com.github.sachil.uplayer.upnp.dmc.BrowseCallback;
import com.github.sachil.uplayer.upnp.dms.ContentTree;

import de.greenrobot.event.EventBus;

public class NavManager implements ExpandableListView.OnChildClickListener,
		View.OnClickListener {

	private static final String TAG = NavManager.class.getSimpleName();
	private Context mContext = null;
	private DrawerLayout mDrawerLayout = null;
	private PatchedExpandableListView mRenderer = null;
	private PatchedExpandableListView mServer = null;
	private PatchedExpandableListView mMedia = null;
	private NavAdapter mRendererAdapter = null;
	private NavAdapter mServerAdapter = null;
	private NavAdapter mMediaAdapter = null;
	private List<Device> mRendererList = null;
	private List<Device> mServerList = null;
	private List<DIDLObject> mMediaList = null;

	public NavManager(Context context, View contentView) {
		mContext = context;
		mRendererList = new ArrayList<>();
		mServerList = new ArrayList<>();
		mMediaList = new ArrayList<>();
		if (!EventBus.getDefault().isRegistered(this))
			EventBus.getDefault().register(this);
		createView(contentView);
	}

	@Override
	public boolean onChildClick(ExpandableListView listView, View view,
			int groupPosition, int childPosition, long id) {

		listView.collapseGroup(groupPosition);
		switch (listView.getId()) {

		case R.id.nav_renderer:
			UpnpUnity.CURRENT_RENDERER = (Device) mRendererAdapter
					.getChild(groupPosition, childPosition);
			MusicService.getInstance().getController()
					.changeDevice(UpnpUnity.CURRENT_RENDERER);
			mRendererAdapter.refresh(UpnpUnity.CURRENT_RENDERER, null);
			break;

		case R.id.nav_library:
			UpnpUnity.CURRENT_SERVER = (Device) mServerAdapter
					.getChild(groupPosition, childPosition);
			mServerAdapter.refresh(UpnpUnity.CURRENT_SERVER, null);
			loadMedia((Device) mServerAdapter.getChild(groupPosition,
					childPosition));
			break;

		case R.id.nav_media:

			UpnpUnity.CURRENT_CONTAINER = (Container) mMediaAdapter
					.getChild(groupPosition, childPosition);
			mMediaAdapter.refresh(UpnpUnity.CURRENT_CONTAINER, null);
			mDrawerLayout.closeDrawers();
			EventBus.getDefault()
					.post(new ActionMessage(R.id.nav_media, 0, null));
			break;
		}
		return false;
	}

	@Override
	public void onClick(View view) {
		EventBus.getDefault().post(new ActionMessage(view.getId(), 0, null));
		mDrawerLayout.closeDrawers();
	}

	public void onEventMainThread(DeviceMessage message) {

		if (message.getDeviceType() == DeviceMessage.DEVICE_TYPE.DMR) {
			if (message.isAdd()) {
				int position = mRendererList.indexOf(message.getDevice());
				if (position >= 0) {
					mRendererList.remove(position);
					mRendererList.add(position, message.getDevice());
				} else
					mRendererList.add(message.getDevice());
			} else {
				if (mRendererList.contains(message.getDevice()))
					mRendererList.remove(message.getDevice());
			}
			mRendererAdapter.refresh(UpnpUnity.CURRENT_RENDERER, mRendererList);
		} else {
			if (message.isAdd()) {
				int position = mServerList.indexOf(message.getDevice());
				if (position >= 0) {
					mServerList.remove(position);
					mServerList.add(position, message.getDevice());
				} else
					mServerList.add(message.getDevice());
			} else {
				if (mServerList.contains(message.getDevice()))
					mServerList.remove(message.getDevice());
			}
			mServerAdapter.refresh(UpnpUnity.CURRENT_SERVER, mServerList);
			loadMedia((Device) mServerAdapter.getChild(0, 0));
		}
	}

	public void onEventMainThread(BrowseMessage message) {
		if (message.isRootNode()) {
			mMediaList = message.getItems();
			UpnpUnity.CURRENT_CONTAINER = (Container) mMediaList.get(0);
			EventBus.getDefault()
					.post(new ActionMessage(R.id.nav_media, 0, null));
			mMediaAdapter.refresh(UpnpUnity.CURRENT_CONTAINER, mMediaList);
		}
	}

	public void clean() {
		if (EventBus.getDefault().isRegistered(this))
			EventBus.getDefault().unregister(this);
	}

	private void createView(View contentView) {
		mDrawerLayout = (DrawerLayout) contentView
				.findViewById(R.id.main_drawer);
		mRenderer = (PatchedExpandableListView) contentView
				.findViewById(R.id.nav_renderer);
		mRendererAdapter = new NavAdapter(mContext);
		mRenderer.setAdapter(mRendererAdapter);
		mRenderer.setOnChildClickListener(this);
		mServer = (PatchedExpandableListView) contentView
				.findViewById(R.id.nav_library);
		mServerAdapter = new NavAdapter(mContext);
		mServer.setAdapter(mServerAdapter);
		mServer.setOnChildClickListener(this);
		mMedia = (PatchedExpandableListView) contentView
				.findViewById(R.id.nav_media);
		mMediaAdapter = new NavAdapter(mContext);
		mMedia.setAdapter(mMediaAdapter);
		mMedia.setOnChildClickListener(this);

		contentView.findViewById(R.id.nav_settings).setOnClickListener(this);
		contentView.findViewById(R.id.nav_exit).setOnClickListener(this);

	}

	private void loadMedia(Device device) {
		Service service = device.findService(
				new UDAServiceType(UpnpUnity.SERVICE_CONTENT_DIRECTORY));
		if (service != null) {
			boolean isLocal = device instanceof RemoteDevice ? false : true;
			UpnpUnity.UPNP_SERVICE.getControlPoint()
					.execute(new BrowseCallback(service, ContentTree
							.getRootContentNode(isLocal).getContainer(),
					isLocal));
		}
	}
}
