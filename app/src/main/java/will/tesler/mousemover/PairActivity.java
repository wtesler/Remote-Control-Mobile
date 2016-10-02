package will.tesler.mousemover;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PairActivity extends Activity {

    @BindView(R.id.button_continue) Button mButtonContinue;
    @BindView(R.id.edittext_pairing) EditText mEditTextPairing;

    SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        ButterKnife.bind(this);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String savedPairingCode = mPreferences.getString(PreferenceConstants.PAIRING_CODE, "");
        mEditTextPairing.setText(savedPairingCode);
    }

    @OnClick(R.id.button_continue)
    void onContinueButtonClick() {
        String text = mEditTextPairing.getText().toString();
        if (!isValidIP(text)) {
            mEditTextPairing.setText("");
            Toast.makeText(this, "Not a valid pairing code.", Toast.LENGTH_LONG).show();
        } else {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(PreferenceConstants.PAIRING_CODE, text);
            editor.apply();
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    public static boolean isValidIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            return !ip.endsWith(".");

        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
