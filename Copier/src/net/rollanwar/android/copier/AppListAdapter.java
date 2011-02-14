package net.rollanwar.android.copier;

import java.util.ArrayList;
import java.util.List;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AppListAdapter extends BaseAdapter {

	private ArrayList<AppInfo> apps = new ArrayList<AppInfo>();

	public AppListAdapter(final SeeApps activity){
		super();
		final PackageManager man = activity.getPackageManager();
		final List<ApplicationInfo> lista = man.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		for (ApplicationInfo app : lista) {
			if((app.flags & ApplicationInfo.FLAG_SYSTEM) != 1){
				apps.add(new AppInfo(activity, app));
			}
		}
	}

	@Override
	public boolean isEnabled(final int position) {
		return apps.get(position).isEnabled();
	}

	@Override
	public boolean areAllItemsEnabled() {
		for (int i=0;i < apps.size();i++)
			if (!isEnabled(i))
				return false;
		return true;
	}

	//No implementadas por BaseAdapter
	@Override
	public int getCount() {
		return apps.size();
	}

	@Override
	public AppInfo getItem(final int arg0) {
		return apps.get(arg0);
	}

	@Override
	public long getItemId(final int arg0) {
		return apps.get(arg0).getId();
	}

	@Override
	public AppInfo getView(final int arg0, final View arg1, final ViewGroup arg2) {
		return apps.get(arg0);
	}
}
