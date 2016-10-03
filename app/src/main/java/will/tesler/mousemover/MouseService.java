package will.tesler.mousemover;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
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

    private AsyncTask<String, Void, Exception> mConnectionTask;
    private DataOutputStream mDataOutputStream;
    private Handler mMainThreadHandler;
    HandlerThread mMessageThread;
    Handler mMessageThreadHandler;
    private MouseBinder mMouseBinder;
    private Socket mSocket;

    private String mServerIp, mPort;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MouseService", "Service Created.");
        mMainThreadHandler = new Handler();
        mMessageThread = new HandlerThread("MessageThread");
        mMessageThread.start();
        mMessageThreadHandler = new Handler(mMessageThread.getLooper());
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
        if (mMessageThread != null && mMessageThread.isAlive()) {
            mMessageThread.quit();
        }
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
                    Debug.D("Connected to Server.");
                } else if (exception instanceof ConnectException) {
                    Debug.D("ConnectException");
                    mMouseBinder.getListener().onConnectionError();
                } else if (exception instanceof UnknownHostException) {
                    Debug.D("UnknownHostException");
                    exception.printStackTrace();
                } else if (exception instanceof IOException) {
                    Debug.D("IOException");
                    exception.printStackTrace();
                }
            }
        };
    }

    public class MouseBinder extends Binder {

        private Listener mListener;

        public void sendCalibration() {
            sendValues(ProtocolConstants.CODE_CALIBRATE);
        }

        public void sendLeftClick() {
            sendValues(ProtocolConstants.CODE_LEFT_CLICK);
        }

        public void sendLeftDown() {
            sendValues(ProtocolConstants.CODE_LEFT_DOWN);
        }

        public void sendLeftUp() {
            sendValues(ProtocolConstants.CODE_LEFT_UP);
        }

        public void sendRightClick() {
            sendValues(ProtocolConstants.CODE_RIGHT_CLICK);
        }

        public void sendRightDown() {
            sendValues(ProtocolConstants.CODE_RIGHT_DOWN);
        }

        public void sendRightUp() {
            sendValues(ProtocolConstants.CODE_RIGHT_UP);
        }

        public void sendUnicodeChar(int keyCode) {
            sendValues(ProtocolConstants.CODE_KEYBOARD, keyCode);
        }

        public void sendMousePosition(final int x, final int y) {
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
                    }
                }
            };
            mMessageThreadHandler.post(runnable);
        }
    }

    public interface Listener {
        void onConnectionError();
    }
}
