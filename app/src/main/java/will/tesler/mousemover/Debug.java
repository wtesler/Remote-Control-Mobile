package will.tesler.mousemover;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by admin on 10/2/16.
 */

public class Debug {

    private Debug() { }

    public static void D(String text) {

        String tag = "Default";

        try {
            StackTraceElement[] stackTraceElements = Thread.currentThread()
                    .getStackTrace();

            tag = stackTraceElements[stackTraceElements.length - 1]
                    .getClassName();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.DEBUG) {
            Log.d(tag, text);
        }
    }

    public static void I(String text) {

        String tag = "Default";

        try {
            StackTraceElement[] stackTraceElements = Thread.currentThread()
                    .getStackTrace();

            tag = stackTraceElements[stackTraceElements.length - 1]
                    .getClassName();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.DEBUG) {
            Log.i(tag, text);
        }
    }

    public static void W(String text) {

        String tag = "Default";

        try {
            StackTraceElement[] stackTraceElements = Thread.currentThread()
                    .getStackTrace();

            tag = stackTraceElements[stackTraceElements.length - 1]
                    .getClassName();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.DEBUG) {
            Log.w(tag, text);
        }
    }


    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
