package net.rollanwar.localizame.clases;

import java.text.ParseException;
import net.rollanwar.localizame.controler.SmsCall;
import net.rollanwar.localizame.controler.SmsGPS;
import android.content.Context;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class Action {
	public static final String gpsActions = "GPS";
	public static final String callActions = "CALL";
	//public static final String vcallActions = "VCALL";
	public static final String[] supportActions = {gpsActions, callActions};//, vcallActions};

	protected static final int maxValue = 3;//ACCION [PASS] [DESTINO]

	protected int type = -1;
	protected String to = null;
	protected String origen = null;
	protected String signal = null;//Usado para mandar un pass

	protected void parseString(final String msg){
		final String[] parametos = msg.split(" ");
		if(parametos.length >= 1 && parametos.length <= maxValue){//ACCION
			int i;
			for (i=0; i < Action.supportActions.length ; i++) {
				if(parametos[0].equalsIgnoreCase(Action.supportActions[i])){
					this.type = i;
					break;
				}
			}
			if(i < Action.supportActions.length){//Ha salido por encontar el TAG
				if(parametos.length >= 2){//PASSWORD
					this.signal = parametos[1];
					if(parametos.length == 3)//DESTINO
						this.to = parametos[2];
					else
						this.to = this.origen;
				}else{
					this.signal = "";
					this.to = this.origen;
				}
			}
		}
	}

	public static boolean isValidString(final String msg){
		final String[] parametos = msg.split(" ");
		if(parametos.length >= 1 && parametos.length <= maxValue){//ACCION
			int i;
			for (i=0; i < Action.supportActions.length ; i++) {
				if(parametos[0].equalsIgnoreCase(Action.supportActions[i])){
					break;
				}
			}
			if(i < Action.supportActions.length){//Ha salido por encontar el TAG
				if(parametos.length == maxValue)//DESTINO
					return PhoneNumberUtils.isGlobalPhoneNumber(parametos[2]);
				return true;
			}
		}
		return false;
	}

	public String toString(){
		String txt = getActionString();
		if(txt == null)
			return "";
		if( signal != null)
			txt = txt + " " + signal;
		else
			txt = txt + " ";
		if( to != null)
			txt = txt + " " + to;
		return txt;
	}

	public Action(final String msg) throws ParseException{
		if(!Action.isValidString(msg))
			throw new ParseException("This msg is not a accion.", 0);
		this.parseString(msg);
	}

	public String getActionString(){
		if(this.type < 0 || this.type >= supportActions.length)
			return null;
		return Action.supportActions[this.type];
	}

	public void doAction(Context context){
		if(this.to == null)
			this.to = this.origen;
		if(this.to == null){
			Log.e(Utils.logTagLocalizaMe, "Error, no existe origen.");
			return;
		}

		final String accion = this.getActionString();
		if(accion != null && (PhoneNumberUtils.isEmergencyNumber(this.to) || checkSettings(context) ) ){
			if(gpsActions.equalsIgnoreCase(accion)){
				Log.d(Utils.logTagLocalizaMe, "Obtener posicion GPS y mandar.");
				final SmsGPS gps = new SmsGPS(context, this.to);
				gps.start();
			}else if(callActions.equalsIgnoreCase(accion)){
				Log.d(Utils.logTagLocalizaMe, "Hacer llamada.");
				Uri dest =  null;
				if(PhoneNumberUtils.isGlobalPhoneNumber(this.to)){
					dest =  Uri.parse("tel:"+this.to);
				}else if(PhoneNumberUtils.isGlobalPhoneNumber(this.origen)){
					dest =  Uri.parse("tel:"+this.origen);
				}
				final SmsCall call = new SmsCall(context, dest);
				final Thread th = new Thread(call);
				th.start();
			}/*else if(vcallActions.equalsIgnoreCase(accion)){
				Log.d(Utils.logTagLocalizaMe, "Hacer Video llamada");
			}//*/
			else{
				Log.e(Utils.logTagLocalizaMe, "Accion no soportada.");
			}
		}
	}

	protected boolean checkSettings(Context context) {
		boolean stop = false;
		final String accion = this.getActionString();
		if( accion != null && Settings.isRunning() ){
			if(gpsActions.equalsIgnoreCase(accion) && !Settings.isSendCoordinates()){
				Log.d(Utils.logTagLocalizaMe, "No se mandan coordendas");
				stop = true;
			}else if(callActions.equalsIgnoreCase(accion) && !Settings.isDoCall()){
				Log.d(Utils.logTagLocalizaMe, "No se hacen llamadas.");
				stop = true;
			}/*else if(vcallActions.equals(accion) && !Settings.isDoVideoCall()){
				Log.e(Utils.logTagLocalizaMe, "No se permite hacer video llamadas");
				stop = true;
			}//*/

			if(!stop && Settings.isForeceSignal()){
				stop = !Utils.cifra(signal).equals(Settings.signal);
				if(stop)
					Log.d(Utils.logTagLocalizaMe, "Firma incorrecta.");
				else
					Log.d(Utils.logTagLocalizaMe, "Firma correcta.");
			}

			if(!stop && Settings.isOnlyContact()){
				if(this.origen != null){
					stop = !Utils.isContact(this.origen, context.getContentResolver());
					if(stop)
						Log.d(Utils.logTagLocalizaMe, "No es contacto.");
					else
						Log.d(Utils.logTagLocalizaMe, "Es un contacto.");
				}else{
					stop = true;
				}
			}
		}else{
			Log.e(Utils.logTagLocalizaMe, "Accion nula o no esta corriendo el servicio");
			stop = true;
		}
		return !stop;
	}

	public String getOrigen() {
		return origen;
	}

	public void setOrigen(String origen) {
		this.origen = origen;
	}
}
