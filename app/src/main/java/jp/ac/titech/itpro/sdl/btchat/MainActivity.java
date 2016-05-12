package jp.ac.titech.itpro.sdl.btchat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    private TextView statusText;
    private ProgressBar connectionProgress;
    private EditText inputText;
    private Button sendButton;
    private ListView chatLogList;

    private ArrayList<ChatMessage> chatLog;
    private ArrayAdapter<ChatMessage> chatLogAdapter;
    private final static String KEY_CHATLOG = "MainActivity.chatLog";

    private BluetoothAdapter btAdapter;
    private final static int REQCODE_ENABLE_BT = 1111;
    private final static int REQCODE_GET_DEVICE = 2222;
    private final static int REQCODE_DISCOVERABLE = 3333;

    public enum State {
        Initializing,
        Disconnected,
        Connecting,
        Connected,
        Waiting
    }

    State state = State.Initializing;

    private final static String SPP_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB";
    private final static UUID SPP_UUID = UUID.fromString(SPP_UUID_STRING);

    private int message_seq = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
            chatLog = savedInstanceState.getParcelableArrayList(KEY_CHATLOG);
        if (chatLog == null)
            chatLog = new ArrayList<>();

        setupUI();
        setState(State.Initializing);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null)
            setupBT();
        else {
            Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onSteop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        MenuItem itemConnect = menu.findItem(R.id.menu_connect);
        MenuItem itemDisconnect = menu.findItem(R.id.menu_disconnect);
        MenuItem itemServerStart = menu.findItem(R.id.menu_server_start);
        MenuItem itemServerStop = menu.findItem(R.id.menu_server_stop);
        switch (state) {
        case Initializing:
        case Connecting:
            itemConnect.setVisible(false);
            itemDisconnect.setVisible(false);
            itemServerStart.setVisible(false);
            itemServerStop.setVisible(false);
            return true;
        case Disconnected:
            itemConnect.setVisible(true);
            itemDisconnect.setVisible(false);
            itemServerStart.setVisible(true);
            itemServerStop.setVisible(false);
            return true;
        case Connected:
            itemConnect.setVisible(false);
            itemDisconnect.setVisible(true);
            itemServerStart.setVisible(false);
            itemServerStop.setVisible(false);
            return true;
        case Waiting:
            itemConnect.setVisible(false);
            itemDisconnect.setVisible(false);
            itemServerStart.setVisible(false);
            itemServerStop.setVisible(true);
            return true;
        default:
            return super.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case R.id.menu_connect:
            connect();
            return true;
        case R.id.menu_disconnect:
            disconnect();
            return true;
        case R.id.menu_server_start:
            startServer();
            return true;
        case R.id.menu_server_stop:
            stopServer();
            return true;
        case R.id.menu_about:
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about_dialog_title)
                    .setMessage(R.string.about_dialog_content)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putParcelableArrayList(KEY_CHATLOG, chatLog);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        switch (reqCode) {
        case REQCODE_ENABLE_BT:
            if (resCode == Activity.RESULT_OK)
                setupBT1();
            else {
                Toast.makeText(this, R.string.toast_bluetooth_disabled, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        case REQCODE_GET_DEVICE:
            if (resCode == Activity.RESULT_OK)
                connect1((BluetoothDevice) data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
            else
                setState(State.Disconnected);
            break;
        case REQCODE_DISCOVERABLE:
            if (resCode != Activity.RESULT_CANCELED) {
                Log.d(TAG, "resCode=" + resCode);
                startServer1(resCode);
            }
            break;
        }
    }

    private void setupUI() {
        Log.d(TAG, "setupUI");
        statusText = (TextView) findViewById(R.id.status_text);
        connectionProgress = (ProgressBar) findViewById(R.id.connection_progress);
        inputText = (EditText) findViewById(R.id.input_text);
        sendButton = (Button) findViewById(R.id.send_button);
        chatLogList = (ListView) findViewById(R.id.chat_log_list);
        chatLogAdapter = new ArrayAdapter<ChatMessage>(this, 0, chatLog) {
            @Override
            public View getView(int pos, View view, ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                ChatMessage message = getItem(pos);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText(message.content);
                return view;
            }
        };
        chatLogList.setAdapter(chatLogAdapter);
        final DateFormat dateFormat = DateFormat.getDateTimeInstance();
        chatLogList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage message = (ChatMessage) parent.getItemAtPosition(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.message_title, message.seq, message.sender))
                        .setMessage(getString(R.string.message_content, message.content,
                                dateFormat.format(new Date(message.time))))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

    }

    private void setState(State state) {
        setState(state, null);
    }

    private void setState(State state, String arg) {
        this.state = state;
        switch (state) {
        case Initializing:
        case Disconnected:
            statusText.setText(R.string.status_text_disconnected);
            inputText.setEnabled(false);
            sendButton.setEnabled(false);
            break;
        case Connecting:
            statusText.setText(getString(R.string.status_text_connecting_to, arg));
            inputText.setEnabled(false);
            sendButton.setEnabled(false);
            break;
        case Connected:
            statusText.setText(getString(R.string.status_text_connected_to, arg));
            inputText.setEnabled(true);
            sendButton.setEnabled(true);
            break;
        case Waiting:
            statusText.setText(R.string.status_text_waiting_for_connection);
            inputText.setEnabled(false);
            sendButton.setEnabled(false);
            break;
        }
        invalidateOptionsMenu();
    }

    public void onClickSendButton(View v) {
        Log.d(TAG, "onClickSendButton");
        if (commThread != null) {
            String content = inputText.getText().toString().trim();
            String sender = btAdapter.getName();
            ChatMessage message =
                    new ChatMessage(++message_seq, System.currentTimeMillis(), content, sender);
            commThread.send(message);
            chatLogAdapter.add(message);
            chatLogAdapter.notifyDataSetChanged();
            chatLogList.smoothScrollToPosition(chatLog.size());
            inputText.getEditableText().clear();
        }
    }

    private void setupBT() {
        Log.d(TAG, "setupBT");
        if (!btAdapter.isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQCODE_ENABLE_BT);
        else
            setupBT1();
    }

    private void setupBT1() {
        Log.d(TAG, "setupBT1");
        setState(State.Disconnected);
    }

    private void connect() {
        Log.d(TAG, "connect");
        Intent intent = new Intent(this, BTScanActivity.class);
        startActivityForResult(intent, REQCODE_GET_DEVICE);
    }

    private void connect1(BluetoothDevice device) {
        Log.d(TAG, "connect1: device=" + device.getName());
        clientTask = new ClientTask();
        clientTask.execute(device);
        setState(State.Connecting, device.getName());
    }

    private void disconnect() {
        Log.d(TAG, "disconnect");
        if (commThread != null) {
            commThread.close();
            commThread = null;
        }
        if (peerSocket != null && peerSocket.isConnected()) {
            try {
                peerSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        setState(State.Disconnected);
    }

    private void startServer() {
        Log.d(TAG, "startServer");
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 90);
        startActivityForResult(intent, REQCODE_DISCOVERABLE);
    }

    private void startServer1(int timeout) {
        Log.d(TAG, "startServer1: timeout=" + timeout);
        serverTask = new ServerTask();
        serverTask.execute(timeout);
        setState(State.Waiting);
    }

    private void stopServer() {
        Log.d(TAG, "stopServer");
        if (serverTask != null)
            serverTask.stop();
    }

    private BluetoothSocket peerSocket;
    private ClientTask clientTask;
    private ServerTask serverTask;

    private class ClientTask extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {
        private final static String TAG = "ClientTask";

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "onPreExecute");
            connectionProgress.setIndeterminate(true);
        }

        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... params) {
            Log.d(TAG, "doInBackground");
            BluetoothSocket socket = null;
            try {
                socket = params[0].createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
            }
            catch (IOException e) {
                if (socket != null) {
                    try {
                        socket.close();
                    }
                    catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    socket = null;
                }
            }
            return socket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket socket) {
            Log.d(TAG, "onPostExecute");
            connectionProgress.setIndeterminate(false);
            if (socket == null) {
                Toast.makeText(MainActivity.this, R.string.toast_connection_failed,
                        Toast.LENGTH_SHORT).show();
                peerSocket = null;
                setState(State.Disconnected);
            }
            else {
                peerSocket = socket;
                try {
                    commThread = new CommThread(peerSocket);
                    commThread.start();
                }
                catch (IOException e) {
                    try {
                        peerSocket.close();
                    }
                    catch (IOException e1) { e1.printStackTrace(); }
                    peerSocket = null;
                    setState(State.Disconnected);
                }
            }
            clientTask = null;
        }
    }

    private class ServerTask extends AsyncTask<Integer, Void, BluetoothSocket> {
        private final static String TAG = "ServerTask";
        private BluetoothServerSocket serverSocket;

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "onPreExecute");
            connectionProgress.setIndeterminate(true);
        }

        @Override
        protected BluetoothSocket doInBackground(Integer... params) {
            Log.d(TAG, "doInBackground");
            BluetoothSocket socket = null;
            try {
                serverSocket =
                        btAdapter.listenUsingRfcommWithServiceRecord(btAdapter.getName(), SPP_UUID);
                socket = serverSocket.accept(params[0] * 1000);
            }
            catch (IOException e) {
                socket = null;
            }
            finally {
                try {
                    serverSocket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return socket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket socket) {
            Log.d(TAG, "onPostExecute: socket=" + socket);
            connectionProgress.setIndeterminate(false);
            if (socket == null) {
                Toast.makeText(MainActivity.this, R.string.toast_connection_failed,
                        Toast.LENGTH_SHORT).show();
                peerSocket = null;
                setState(State.Disconnected);
            }
            else {
                peerSocket = socket;
                try {
                    commThread = new CommThread(peerSocket);
                    commThread.start();
                }
                catch (IOException e) {
                    try {
                        peerSocket.close();
                    }
                    catch (IOException e1) { e1.printStackTrace(); }
                    peerSocket = null;
                    setState(State.Disconnected);
                }
            }
            serverTask = null;
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled: serverSocket=" + serverSocket);
            connectionProgress.setIndeterminate(false);
            setState(State.Disconnected);
            serverTask = null;
        }

        public void stop() {
            Log.d(TAG, "stop");
            try {
                serverSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            cancel(false);
        }
    }

    private final static int MESG_STARTED = 1111;
    private final static int MESG_RECEIVED = 2222;
    private final static int MESG_FINISHED = 3333;

    private CommThread commThread;

    private Handler commHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage");
            switch (msg.what) {
            case MESG_STARTED:
                BluetoothDevice device = (BluetoothDevice)msg.obj;
                setState(State.Connected, device.getName());
                break;
            case MESG_FINISHED:
                setState(State.Disconnected);
                break;
            case MESG_RECEIVED:
                chatLogAdapter.add((ChatMessage)msg.obj);
                chatLogAdapter.notifyDataSetChanged();
                chatLogList.smoothScrollToPosition(chatLogAdapter.getCount());
                break;
            default:
            }
            return false;
        }
    });

    private class CommThread extends Thread {
        private final static String TAG = "CommThread";
        private final BluetoothSocket socket;
        private ChatMessage.Reader reader;
        private ChatMessage.Writer writer;

        public CommThread(BluetoothSocket socket) throws IOException {
            this.socket = socket;
            reader = new ChatMessage.Reader(new JsonReader(new InputStreamReader(socket.getInputStream(), "UTF-8")));
            writer = new ChatMessage.Writer(new JsonWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")));
        }

        @Override
        public void run() {
            Log.d(TAG, "run");
            commHandler.sendMessage(commHandler.obtainMessage(MESG_STARTED, socket.getRemoteDevice()));
            try {
                writer.beginArray();
                reader.beginArray();
                while (socket.isConnected()) {
                    Log.d(TAG, "waiting for read");
                    ChatMessage message = reader.read();
                    commHandler.sendMessage(commHandler.obtainMessage(MESG_RECEIVED, message));
                }
            }
            catch (IOException e) {
                Log.d(TAG, "Reader gets exception");
            }
            finally {
                try {
                    writer.endArray();
                    reader.endArray();
                    reader.close();
                    writer.close();
                    socket.close();
                }
                catch (IOException e) { e.printStackTrace(); }
            }
            commHandler.sendMessage(commHandler.obtainMessage(MESG_FINISHED));
        }

        public void send(ChatMessage message) {
            try {
                writer.write(message);
                writer.flush();
            }
            catch (IOException e) {
                Log.d(TAG, "writer gets exceptions");
            }
        }

        public void close() {
            try {
                socket.close();
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }
}