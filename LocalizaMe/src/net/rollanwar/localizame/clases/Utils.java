package net.rollanwar.localizame.clases;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Contacts;
import android.util.Log;


public class Utils {

	public static final String packageLocalizaMe = "net.rollawar.localizame";
	public static final String logTagLocalizaMe = "LocalizaMe";
	public static final String preferencesLocalizaMe = "LocalizaMe-";
	public static final String tagXml = "$xml$";
	public static final String tagLatitude = "$lat$";
	public static final String tagLogitude = "$log$";

	public static final String xml = "<?xml version='1.0' encoding='utf-8'?>"+
									 "<latitude>"+Utils.tagLatitude+"</latitude>"+
									 "<longitude>"+Utils.tagLogitude+"</longitude>";

	public static String cifra(final String signal){
		if(signal != null && signal.length() > 2){
			String enc = Base64Coder.encodeString(signal);
			Log.d(Utils.logTagLocalizaMe, "Cadena cifrada: "+enc);
			return enc;
		}else{
			return "";
		}
    }

	public static String descifra(final String signal){
		if(signal != null && signal.length() > 2){
			return Base64Coder.decodeString(signal);
		}else{
			return "";
		}
    }

	@SuppressWarnings("finally")
	public static boolean isContact(final String num, final ContentResolver contentResolver) {
		boolean ret = false;
		try{
			final Cursor cur = contentResolver.query(Contacts.Phones.CONTENT_URI, null, null, null, null);
			final int index = cur.getColumnIndex(Contacts.Phones.NUMBER);
			while (!ret && cur.moveToNext())
				if (num.equals(cur.getString(index)))
					ret = true;
			cur.close();
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			return ret;
		}
	}
}

