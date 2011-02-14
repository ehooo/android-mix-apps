package net.rollanwar.localizame.clases;

import android.content.SharedPreferences;

public class Settings{
    //Acciones
	private static boolean sendCoordinates=false;
	//private static boolean doVideoCall=false;
    private static boolean doCall=false;

    //Seguridad
    public static String signal=null;/* Clave cifrada */
    private static boolean onlyContact=false;
	private static boolean foreceSignal=false;

	//Estado
	private static boolean running=false;

    public static void load(final SharedPreferences preferences) {
    	if(signal == null)
    		reload(preferences);
    }

    public static void reload(final SharedPreferences preferences) {
	    setSendCoordinates(preferences.getBoolean(Utils.preferencesLocalizaMe+"sendcoordinates", true));
	    //setDoVideoCall(preferences.getBoolean(Utils.preferencesLocalizaMe+"dovideocall", true));
	    setDoCall(preferences.getBoolean(Utils.preferencesLocalizaMe+"docall", true));

	    signal = preferences.getString(Utils.preferencesLocalizaMe+"signal", "");
	    setOnlyContact(preferences.getBoolean(Utils.preferencesLocalizaMe+"onlycontact", true));
	    setForeceSignal(preferences.getBoolean(Utils.preferencesLocalizaMe+"forecesignal", true));

	    setRunning(preferences.getBoolean(Utils.preferencesLocalizaMe+"running", true));
    }

	public static void save(final SharedPreferences sharedPreferences) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(Utils.preferencesLocalizaMe+"sendcoordinates", isSendCoordinates());
		editor.putBoolean(Utils.preferencesLocalizaMe+"docall", isDoCall());
		//editor.putBoolean(Utils.preferencesLocalizaMe+"dovideocall", isDoVideoCall());
		editor.putBoolean(Utils.preferencesLocalizaMe+"onlycontact", isOnlyContact());
		editor.putBoolean(Utils.preferencesLocalizaMe+"forecesignal", isForeceSignal());
		editor.putString(Utils.preferencesLocalizaMe+"signal", signal );
		editor.putBoolean(Utils.preferencesLocalizaMe+"running", isRunning());
		editor.commit();
	}

	public static String getSignal() {/* Retorna la clave sin cifrar */
		return Utils.descifra(signal);
	}

	public static void setSignal(final String signal) {/* Almacena la clave cifrandola */
		Settings.signal = Utils.cifra(signal);
	}

	public static void setRunning(boolean running) {
		Settings.running = running;
	}

	public static boolean isRunning() {
		return running;
	}

	public static void setForeceSignal(boolean foreceSignal) {
		Settings.foreceSignal = foreceSignal;
	}

	public static boolean isForeceSignal() {
		return foreceSignal;
	}

	public static void setOnlyContact(boolean onlyContact) {
		Settings.onlyContact = onlyContact;
	}

	public static boolean isOnlyContact() {
		return onlyContact;
	}

	/*
	public static void setDoVideoCall(boolean doVideoCall) {
		Settings.doVideoCall = doVideoCall;
	}

	public static boolean isDoVideoCall() {
		return doVideoCall;
	}//*/

	public static void setDoCall(boolean doCall) {
		Settings.doCall = doCall;
	}

	public static boolean isDoCall() {
		return doCall;
	}

	public static void setSendCoordinates(boolean sendCoordinates) {
		Settings.sendCoordinates = sendCoordinates;
	}

	public static boolean isSendCoordinates() {
		return sendCoordinates;
	}

}
