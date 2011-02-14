package net.rollanwar.android.copier;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;


public class SeeApps extends Activity implements OnItemClickListener, Runnable{

	protected static String title=null;
	protected static String message=null;
	protected static ProgressDialog pd;
	private static AppListAdapter adapter;
	private static Looper loop;
	private static Uri file = null;

    /**
     * Llama a valid y si hay una exepcion en this.ex hace un show(), sino finish(R.string.error_init);<br>
     * De ser todo correcto llama a this.show();
     */
    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(pd != null)
			showDialog();
		show();
    }

    @Override
    protected void onDestroy() {
    	if(pd != null)
    		cleanDialog();
    	super.onDestroy();
    }

    public static final void cleanDialog(){
		if(pd != null)
			if(pd.isShowing())
				pd.dismiss();
	}
	protected final void showDialog(){
		if(message == null)
			message = getString(R.string.geting);
		if(title == null)
			title = getString(R.string.processing);
		pd = ProgressDialog.show(this, title, message, true, false);
	}

	protected void show() {
		setContentView(R.layout.info_apps);
		final ListView l = (ListView) findViewById(R.id.result_list);
		l.setOnItemClickListener(this);
		if(adapter == null){
			message = getString(R.string.load_apps);
			title = getString(R.string.list_apps);
			showDialog();
			message = null;
			title = null;
			update();
		}else
			l.setAdapter(adapter);
	}

	public void update() {
		if(loop == null)
			new Thread(this).start();
	}

	private Handler h = new Handler(){
		public void handleMessage(android.os.Message msg) {
			final ListView l = (ListView) findViewById(R.id.result_list);
			l.setAdapter(adapter);
			cleanDialog();
			pd = null;
			loop.quit();
			loop = null;
		};
	};

	@Override
	public void run() {
		Looper.prepare();
		loop = Looper.myLooper();
		if(adapter == null)
			adapter = new AppListAdapter(this);
		if(file != null){
			File f = new File(file.getPath());
			if(f.isFile()){
				Context context = this.getApplicationContext();
				File path = Environment.getExternalStorageDirectory();
				path = new File(path.getAbsolutePath()+context.getString(R.string.path));
				if(!path.isDirectory())
					path.mkdirs();
				File copia = new File(path.getAbsolutePath(), f.getName());
				try {
					copy(f, copia);
				} catch (Exception e) {
					Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG);
					e.printStackTrace();
				}
			}
		}
		h.sendEmptyMessage(0);
		//Looper.loop();
	}

	@Override
	public void onItemClick(AdapterView<?> adap, View v, int arg2, long arg3) {
		if(adap.getAdapter() instanceof AppListAdapter)
			if(v instanceof AppInfo){
				AppInfo app = (AppInfo)v;
				file = app.getFile();
				showDialog();
				update();
			}
	}

	private static final int COPY_BUFFER_SIZE = 1024;

	private static void copy(File oldFile, File newFile) {
		try {
			FileInputStream input = new FileInputStream(oldFile);
			FileOutputStream output = new FileOutputStream(newFile);

			byte[] buffer = new byte[COPY_BUFFER_SIZE];
			while (true) {
			        int bytes = input.read(buffer);
			        if (bytes <= 0) {
			                break;
			        }
			        output.write(buffer, 0, bytes);
			}
			output.close();
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


