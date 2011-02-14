package net.rollanwar.localizame;

import net.rollanwar.localizame.R;
import net.rollanwar.localizame.clases.Settings;
import net.rollanwar.localizame.clases.Utils;
import net.rollanwar.localizame.graphic.CheckPass;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class LocalizaMe extends Activity implements OnClickListener {
    /** Called when the activity is first created. */

	private static Settings settings = null;

	public LocalizaMe(){
		super();
		if(settings == null)//Aseguramos que la instancia de Settings existe para no perder los datos
			settings = new Settings();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		setSettings();
		saveSettings();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		//Cargando Settings
        final SharedPreferences preferences = getSharedPreferences(Utils.packageLocalizaMe, Context.MODE_WORLD_WRITEABLE);
    	Settings.load(preferences);

		if(Settings.isForeceSignal()){
			new CheckPass(this);
		}else{
			setView();
		}
    }

	public void setView(){
		setContentView(R.layout.main);
		final Button button = (Button) findViewById(R.id.running);
		if(button != null)
	    	button.setOnClickListener(this);
		viewSettings();
	}

	public void viewSettings(){
		CheckBox check = (CheckBox) findViewById(R.id.sendcoordinates);
		if(check != null){
			check.setChecked(Settings.isSendCoordinates());
		}
		check = (CheckBox) findViewById(R.id.docall);
		if(check != null){
			check.setChecked(Settings.isDoCall());
		}
		/*
		check = (CheckBox) findViewById(R.id.dovideocall);
		if(check != null){
			check.setChecked(Settings.isDoVideoCall());
		}//*/

		check = (CheckBox) findViewById(R.id.onlycontact);
		if(check != null){
			check.setChecked(Settings.isOnlyContact());
		}
		check = (CheckBox) findViewById(R.id.forecesignal);
		if(check != null){
			check.setChecked(Settings.isForeceSignal());
		}

		EditText pass = (EditText) findViewById(R.id.signal);
		if(pass != null){
			pass.setText(Settings.getSignal());
		}

		final Button button = (Button) findViewById(R.id.running);
		if(button != null){
	    	if (Settings.isRunning())
	    		button.setText(R.string.stop);
	    	else
	    		button.setText(R.string.start);
		}
	}

	public void setSettings() {
		CheckBox check = (CheckBox) findViewById(R.id.sendcoordinates);
		if(check != null){
			Settings.setSendCoordinates(check.isChecked());
		}
		check = (CheckBox) findViewById(R.id.docall);
		if(check != null){
			Settings.setDoCall(check.isChecked());
		}
		/*
		check = (CheckBox) findViewById(R.id.dovideocall);
		if(check != null){
			Settings.setDoVideoCall(check.isChecked());
		}//*/

		check = (CheckBox) findViewById(R.id.onlycontact);
		if(check != null){
			Settings.setOnlyContact(check.isChecked());
		}
		check = (CheckBox) findViewById(R.id.forecesignal);
		if(check != null){
			Settings.setForeceSignal(check.isChecked());
		}

		EditText pass = (EditText) findViewById(R.id.signal);
		if(pass != null){
			Editable text = pass.getText();
			Settings.setSignal( text.toString() );
		}
	}

	public void saveSettings() {
		final SharedPreferences preferences = getSharedPreferences(Utils.packageLocalizaMe, Context.MODE_WORLD_WRITEABLE);
		Settings.save(preferences);
	}

	@Override
	public void onClick(View v) {
		setSettings();
		Settings.setRunning(!Settings.isRunning());
		saveSettings();
		viewSettings();
	}
}