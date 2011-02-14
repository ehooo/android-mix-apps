package net.rollanwar.localizame.controler;

import net.rollanwar.localizame.clases.Utils;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

public class SmsCall extends BroadcastReceiver implements Runnable {

	private static final String extraKey = Utils.preferencesLocalizaMe + "SmsCall";
	private Context context = null;
	private Uri to;
	private static boolean check = false;

	public SmsCall(){ super(); }
	public SmsCall(Context context, Uri to){
		this();
		this.context = context;
		this.to = to;
	}

	@Override
	public void run() {
		try{
			if(to != null && context != null){
				final Intent call = new Intent(Intent.ACTION_CALL, to);
				call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				call.putExtra(SmsCall.extraKey, true);
				context.startActivity(call);
				SmsCall.check = true;
			}else{
				Log.e(Utils.logTagLocalizaMe,"Error: El numero o el contexto no es correcto");
			}
		}catch (ActivityNotFoundException e) {
			Log.e(Utils.logTagLocalizaMe, e.getMessage());
		}catch (SecurityException e) {
			Log.e(Utils.logTagLocalizaMe, e.getMessage());
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		//final String newState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		//if(TelephonyManager.EXTRA_STATE_OFFHOOK.equals(newState)){//Realizando una llamada
		if(SmsCall.check){
			final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			//if( audioManager.isBluetoothA2dpOn() )
			//audioManager.setBluetoothScoOn(true);
			//if( !audioManager.isSpeakerphoneOn() )
			audioManager.setSpeakerphoneOn(true);

			int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
			audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,max,AudioManager.FLAG_SHOW_UI);
			SmsCall.check = false;
		}
		//}
	}
}