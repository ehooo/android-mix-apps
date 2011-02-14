package net.rollanwar.android.copier;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class AppInfo extends LinearLayout{
	private ApplicationInfo app = null;
	public AppInfo(final SeeApps context, ApplicationInfo app) {
		super(context);
		this.app = app;

		final PackageManager man = context.getPackageManager();
		setOrientation(HORIZONTAL);
		setBackgroundColor(Color.WHITE);
		
		final ImageView img = new ImageView(context);
		img.setImageDrawable(app.loadIcon(man));
		img.setLayoutParams(new LayoutParams(48, 48));
		addView(img);
		final TextView txt = new TextView(context);
		txt.setGravity(Gravity.CENTER_HORIZONTAL);
		txt.setText(app.loadLabel(man));
		txt.setTextColor(Color.BLACK);
		txt.setPadding(txt.getPaddingLeft()+5, txt.getPaddingTop(), txt.getPaddingRight(), txt.getPaddingBottom());
		addView(txt);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(MotionEvent.ACTION_DOWN == event.getAction())
			setBackgroundColor(Color.YELLOW);
		else
			setBackgroundColor(Color.WHITE);
		return super.onTouchEvent(event);
	}

	public Uri getFile() {
		return Uri.parse("file://"+this.app.publicSourceDir);
	}
}
