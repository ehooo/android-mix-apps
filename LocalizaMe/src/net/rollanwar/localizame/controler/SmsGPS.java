package net.rollanwar.localizame.controler;

import java.util.List;
import net.rollanwar.localizame.R;
import net.rollanwar.localizame.clases.Utils;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class SmsGPS extends Thread {

	private Context context = null;
	private String to = null;
	private int esperas = 0;
	private Location location = null;
	private GPSListener gpsListener = null;
	private static final Criteria basicCriteria = new Criteria();

	static {
		basicCriteria.setAltitudeRequired(false);
		basicCriteria.setBearingRequired(false);
		basicCriteria.setCostAllowed(true);
		basicCriteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		basicCriteria.setSpeedRequired(false);
		basicCriteria.setAccuracy(Criteria.ACCURACY_FINE);
	}

	public SmsGPS(Context context, String to){
		super();
		this.context = context;
		this.to = to;
		this.location = null;
	}

	private void sendSMS(String txt){
		sendSMS(txt,false);
	}

	private void sendSMS(String txt, boolean parse){
		String msg = new String(txt);
		if (parse)
			msg = parse(msg);
		try{
			Log.d(Utils.logTagLocalizaMe,"Enviando SMS a " + to);
			SmsListener.sendSMS(to, msg);
		}catch (final Exception e) {
			e.printStackTrace();
			Log.e(Utils.logTagLocalizaMe, "No se puede mandar el SMS con coodenadas a " + to);
		}
	}

	private String parse(final String txt){
		String ret = new String(txt);
		ret = ret.replace(Utils.tagXml, Utils.xml);
		if(location != null){
			ret = ret.replace(Utils.tagLatitude, String.valueOf(location.getLatitude()));
			ret = ret.replace(Utils.tagLogitude, String.valueOf(location.getLongitude()));
		}else
			Log.e(Utils.logTagLocalizaMe,"No hay localizacion.");
		return ret;
	}

	private String getGSMPosition(){
		GsmCellLocation.requestLocationUpdate();
		final TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
		CellLocation celda = telephonyManager.getCellLocation();
		if(celda instanceof GsmCellLocation){
			final GsmCellLocation gsm = (GsmCellLocation) celda;
			String msg = "GSM cell: Loc area: "+gsm.getLac()+" Cell ID: "+gsm.getCid();
			return msg;
		}
		Log.e(Utils.logTagLocalizaMe, "No es una instancia de GSMCell.");
		return "";
	}

	private String getLastPosition(LocationManager locationManager){
		final List<String> names = locationManager.getProviders(basicCriteria, false);
		StringBuffer sb = new StringBuffer();
		Location loc = null;
		for (String string : names) {
			loc = locationManager.getLastKnownLocation(string);
			sb.append(string+": "+String.valueOf(loc.getLatitude())+", "+String.valueOf(loc.getLatitude()) );
		}
		return sb.toString();
	}

	private boolean setPosition(final LocationManager locationManager){
		esperas = 0;
		Looper.prepare();
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, gpsListener);
			esperas++;
		}
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, gpsListener);
			esperas++;
		}
		Looper.loop();
		Looper.myLooper().quit();
		return esperas != 0;
	}

	@Override
	public void run() {
		final LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		if(locationManager != null){
			gpsListener = new GPSListener();
			if(!setPosition(locationManager)){
				String msg = context.getText(R.string.gpsNofount).toString();
				msg += getLastPosition(locationManager);
				msg += getGSMPosition();
				Log.d(Utils.logTagLocalizaMe,"Ultimas posiciones: "+msg);
				sendSMS(msg);
			}
		}
	}

	private Handler manejador = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			if(esperas <= 0){
	    		try {
	                context = context.createPackageContext(Utils.packageLocalizaMe, 0);
	    	    } catch (final NameNotFoundException e) {
	    	            //e.printStackTrace();
	    	    }
	    		sendSMS(context.getText(R.string.gpsSms).toString(), true);
	    		esperas = 0;
	    		locationManager.removeUpdates(gpsListener);
	    	}
		}
	};

	private class GPSListener implements LocationListener {
		@Override
		public void onLocationChanged(Location loc) {
			if(loc != null && esperas > 0){
				if(location == null)
					location = loc;
				if(location.getAccuracy() < loc.getAccuracy())
					location = loc;
				esperas--;
                manejador.sendEmptyMessage(0);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {}	
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		@Override
		public void onProviderDisabled(String provider) {}
	}
}