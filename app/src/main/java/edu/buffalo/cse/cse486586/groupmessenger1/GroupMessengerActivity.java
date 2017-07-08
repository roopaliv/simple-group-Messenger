package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentResolver;
import android.net.Uri;
import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.view.View;
import java.net.*;
import java.io.*;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String Tag = "groupMessengerActivity";
    static final String[] RemotePorts = {"11108", "11112", "11116", "11120", "11124"};
    static int keySequence = 0;
    static final int SERVER_PORT = 10000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /* code taken from  PA1*/
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        final EditText editText = (EditText) findViewById(R.id.editText1);
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(Tag, "Can't create a ServerSocket");
            return;
        }

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /* code taken from  PA1*/
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    /* code taken from  PA1*/
    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                for(String remotePort : RemotePorts) {
                        /*sending msg*/
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    String msgToSend = msgs[0];
                    try {
                        OutputStream outputStreamObj = socket.getOutputStream();
                        DataOutputStream dataMsgStream = new DataOutputStream(outputStreamObj);
                        dataMsgStream.writeUTF(msgToSend);
                    } catch (IOException e) {
                        Log.e(Tag, e.getMessage());
                    } finally {
                        socket.close();
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(Tag, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(Tag, e.getMessage());//"ClientTask socket IOException");
            }

            return null;
        }
    }

    /* code taken from  PA1*/
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while(true) {
                try {
                    /*Listening for msgs*/
                    Socket listeningServer = serverSocket.accept();
                    DataInputStream inputStreamObj = new DataInputStream(listeningServer.getInputStream());
                    publishProgress(inputStreamObj.readUTF());
                    listeningServer.close();
                } catch(IOException e) {
                    Log.e(Tag, e.getMessage());
                    break;
                }
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            /*Displaying msgs*/
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strReceived + "\t\n");
            /*Adding values to sqlLite - code taken from OnPTestClickListener*/
            ContentValues cv = new ContentValues();
            ContentResolver contentResolver = getContentResolver();
            Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
            cv.put("key", Integer.toString(keySequence++));
            cv.put("value", strReceived);
            try {
                    contentResolver.insert(uri, cv);
            } catch (Exception e) {
                Log.e(Tag, e.getMessage());
            }
            return;
        }
    }
    /* code taken form OnPTestClickListener */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
