package will.tesler.mousemover;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;

public class DialogUtils {

    private DialogUtils() {}

    public static AlertDialog.Builder createConnectionErrorDialog(Context context, SharedPreferences preferences) {
        String savedPairingCode = preferences.getString(PreferenceConstants.PAIRING_CODE, "");

        return new AlertDialog.Builder(context)
                .setTitle("Cannot connect to device")
                .setMessage("Ensure the other device is enabled and displays pairing code " + savedPairingCode)
                .setCancelable(false);
    }
}
