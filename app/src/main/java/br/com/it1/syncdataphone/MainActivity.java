package br.com.it1.syncdataphone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Button enviarBt;
    ListView listView;
    ArrayAdapter<String> adapter;
    ArrayList<String> listMsg = new ArrayList<>();
    protected Handler myHandler;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;
    EditText msgTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enviarBt = findViewById(R.id.enviarButton);
        msgTxt = findViewById(R.id.mensagem);
        listView = findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item,listMsg);
        listView.setAdapter(adapter);

        //Create a message handler//
        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                adapter.add(bundle.getString("message"));
                return true;
            }
        });

//Register to receive local broadcasts, which we'll be creating in the next step//

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle msg = intent.getExtras();
            adapter.add("Mensagem recebida n." + receivedMessageNumber++ + ": " + msg.get("message").toString());
        }
    }

    public void enviarMensagem(View v) {
        new NewThread(Commom.MESSAGE_PATH, msgTxt.getText().toString()).start();
    }

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("message", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);
    }

    class NewThread extends Thread {
        String path;
        String message;

        NewThread(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {

            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());
                    try {
                        Integer result = Tasks.await(sendMessageTask);
                        sendmessage("Mensagem enviada n." + sentMessageNumber++ + ": " + message);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }
    }
}