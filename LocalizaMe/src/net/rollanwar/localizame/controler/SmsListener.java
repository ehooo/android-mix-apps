package net.rollanwar.localizame.controler;

import java.util.ArrayList;
import net.rollanwar.localizame.clases.Action;
import net.rollanwar.localizame.clases.Settings;
import net.rollanwar.localizame.clases.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SmsListener extends BroadcastReceiver {

	private static Settings settings = null;

	public SmsListener(){
		super();
		if(settings == null)//Aseguramos que la instancia de Settings existe para no perder los datos
			settings = new Settings();
	}

	public static void sendSMS(String phoneNumber, String message) throws IllegalArgumentException{
		SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null);        
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		if (((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            return;
	    }

	    final Bundle bundle = intent.getExtras();
	    if (bundle == null)
	    	return;
	    final Object[] pdusObj = (Object[]) bundle.get("pdus");
	    ArrayList<Action> acciones = new ArrayList<Action>();
	    String msg = null;
	    SmsMessage message = null;
	    for (int i = 0; i < pdusObj.length; i++) {
	    	message = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
	    	msg = message.getDisplayMessageBody();
	    	try{
	    		Action acc = new Action(msg);
	    		acc.setOrigen(message.getOriginatingAddress());
	    		acciones.add(acc);
	    	}catch (Exception e) {}
	    }

	    if(acciones.size() == 0)
	    	return;

	    try {
	            context = context.createPackageContext(Utils.packageLocalizaMe, 0);
	    } catch (final NameNotFoundException e) {
	            //e.printStackTrace();
	    }

		final SharedPreferences preferences = context.getSharedPreferences(Utils.packageLocalizaMe, Context.MODE_WORLD_WRITEABLE);
    	Settings.load(preferences);

	    if(Settings.isRunning()){
	    	Log.d(Utils.logTagLocalizaMe,"Realizando acciones");
	    	for (Action accion : acciones) {
				accion.doAction(context);
			}
	    }else{
	    	Log.e(Utils.logTagLocalizaMe,"No esta corriendo el servicio");
	    }
	}
}
