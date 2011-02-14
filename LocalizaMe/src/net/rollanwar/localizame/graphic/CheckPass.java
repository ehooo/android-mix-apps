package net.rollanwar.localizame.graphic;

import net.rollanwar.localizame.LocalizaMe;
import net.rollanwar.localizame.R;
import net.rollanwar.localizame.clases.Settings;
import net.rollanwar.localizame.clases.Utils;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class CheckPass implements OnClickListener{

	private static LocalizaMe localizame; 

	public CheckPass(LocalizaMe loc) {
		localizame = loc;
		localizame.setContentView(R.layout.checkpass);
		final Button button = (Button) localizame.findViewById(R.id.ok);
		if(button != null){
	    	button.setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		final EditText pass = (EditText) localizame.findViewById(R.id.signal);
		if(pass != null){
			final Editable text = pass.getText();
			if( Utils.cifra(text.toString()).equals(Settings.signal) ){//Para comparar no se descifra
				localizame.setView();
			}else{
				Log.e(Utils.logTagLocalizaMe,"Error, clave erronea");
				localizame.finish();
			}
		}
	}
}
