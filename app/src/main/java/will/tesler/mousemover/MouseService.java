package will.tesler.mousemover;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MouseService extends Service {

    public static final String EXTRA_SERVER_IP = "will.tesler.mousemover.SERVER_IP";
    public static final String EXTRA_PORT = "will.tesler.mousemover.PORT";

    public static final int CODE_CALIBRATE = Integer.MIN_VALUE;
    public static final int CODE_LEFT_CLICK_DOWN = Integer.MIN_VALUE + 1;
    public static final int CODE_LEFT_CLICK_UP = Integer.MIN_VALUE + 2;
    public static final int CODE_RIGHT_CLICK_DOWN = Integer.MIN_VALUE + 3;
    public static final int CODE_RIGHT_CLICK_UP = Integer.MIN_VALUE + 4;
    public static final int CODE_KEYBOARD = Integer.MIN_VALUE + 5;

    private AsyncTask<String, Void, Exception> mConnectionTask;
    private DataOutputStream mDataOutputStream;
    private Handler mMainThreadHandler;
    private MouseBinder mMouseBinder;
    private Socket mSocket;

    private String mServerIp, mPort;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MouseService", "Service Created.");
        mMainThreadHandler = new Handler();
        mMouseBinder = new MouseBinder();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        Log.d("MouseService", "Service Bound.");

        mServerIp = intent.getStringExtra(EXTRA_SERVER_IP);
        mPort = Integer.toString(intent.getIntExtra(EXTRA_PORT, -1));

        return mMouseBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d("MouseService", "Service Destroyed.");
    }

    private void createConnectionTask() {
        mConnectionTask = new AsyncTask<String, Void, Exception>() {
            @Override
            protected Exception doInBackground(String... params) {
                String serverIp = params[0];
                int port = Integer.parseInt(params[1]);
                try {
                    mSocket = new Socket(serverIp, port);
                    mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Exception exception) {
                if (exception == null) {
                    return;
                } else if (exception instanceof ConnectException) {
                    mMouseBinder.getListener().onConnectionError();
                } else if (exception instanceof UnknownHostException) {
                    exception.printStackTrace();
                } else if (exception instanceof IOException) {
                    exception.printStackTrace();
                }
            }
        };
    }

    public class MouseBinder extends Binder {

        private Listener mListener;
        private boolean mIsConnectedToServer;

        public void sendCalibration() {
            sendValues(CODE_CALIBRATE);
        }

        public void sendLeftClickDown() {
            sendValues(CODE_LEFT_CLICK_DOWN);
        }

        public void sendLeftClickUp() {
            sendValues(CODE_LEFT_CLICK_UP);
        }

        public void sendRightClickDown() {
            sendValues(CODE_RIGHT_CLICK_DOWN);
        }

        public void sendRightClickUp() {
            sendValues(CODE_RIGHT_CLICK_UP);
        }

        public void sendUnicodeChar(int keyCode) {
            sendValues(CODE_KEYBOARD, keyCode);
        }

        public void sendMouseEvent(final int x, final int y) {
            sendValues(x, y);
        }

        public Listener getListener() {
            return mListener;
        }

        public void setListener(Listener listener) {
            mListener = listener;
        }

        public void contactServer() {
            createConnectionTask();
            mConnectionTask.execute(mServerIp, mPort);
        }

        private void sendValues(final int... values) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mDataOutputStream != null) {
                        try {
                            for (int value : values) {
                                mDataOutputStream.writeInt(value);
                            }
                            mDataOutputStream.flush();
                        } catch (SocketException e) {
                            mMainThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mMouseBinder.getListener().onConnectionError();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.w("MouseService", "No output stream available.");
                    }
                }
            };
            new Thread(runnable).start();
        }
    }

    public interface Listener {
        void onConnectionError();
    }
}
