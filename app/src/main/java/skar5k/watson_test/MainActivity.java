package skar5k.watson_test;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private TextView t;
    private EditText user_input;
    private Button b;
    private Map<String, Object> context;
    private ConversationService wats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t = (TextView) findViewById(R.id.textv);
        b = (Button) findViewById(R.id.button);
        user_input = (EditText) findViewById(R.id.userEntry);
        user_input.setOnEditorActionListener(enter_handle);
        context = new HashMap<String, Object>();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET},0);
        AskWatsonTask init = new AskWatsonTask();
        init.execute(new String[]{""});
        b.setOnClickListener(button_click);

    }


    View.OnClickListener button_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        AskWatsonTask tas = new AskWatsonTask();
        tas.execute(new String[]{user_input.getText().toString()});
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        if(requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

        }
        else{
            Toast t = Toast.makeText(getApplicationContext(), "no internet",Toast.LENGTH_SHORT);
            t.show();
        }
    }


    private class AskWatsonTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... textsToAnalyse) {


            wats = new ConversationService(ConversationService.VERSION_DATE_2017_02_03);
            wats.setUsernameAndPassword("edbccd33-e2c5-4091-b52d-3512ebdf095d","3Aqppd5XtV4N");

            MessageRequest m;
            if(context.size() > 0){
                m = new MessageRequest.Builder().inputText(textsToAnalyse[0]).context(context).build();
            }
            else
                m = new MessageRequest.Builder().inputText(textsToAnalyse[0]).build();

            MessageResponse res = wats.message("2e15faf4-ce7c-41f6-b1ad-a926ad71d3f1",m).execute();

            context = (Map) res.getContext();


            return res.getText().toString();
        }

        //setting the value of UI outside of the thread
        @Override
        protected void onPostExecute(String result) {
            if(result.contains("phone app")){
                if(result.contains("Ok")){
                    String[] tokens = result.split(" ");
                    openPhone(tokens[4]);
                }
                else{
                    openPhone(null);
                }
            }
            if(result.contains("searching")){
                String parsed = result.replaceAll("\\[","").replaceAll("\\]","");
                String[] tokens = parsed.split(" ");

                openInternet(tokens[3]);
            }

            if(result.contains("music player")){
                playMusic();
            }
            if(result.contains("mute")){
                muteMusic();
            }

            if(result.contains("internet")){
                openInternet(null);
            }

            t.setText(result);
        }
    }

    private void openPhone(String number){
        Intent i = new Intent(Intent.ACTION_DIAL);
        if(number != null){
            i.setData(Uri.parse("tel:"+number));
        }

        startActivity(i);
    }

    private void muteMusic(){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
    }

    private void openInternet(String query){
        if(query == null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.google.com/"));
            startActivity(intent);
        }
        else {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY,query);
            startActivity(intent);
        }

    }


    private void playMusic(){
        String pkgname = "com.sec.android.app.music";
        PackageManager pkgmanager = getPackageManager();
        Intent intent = pkgmanager.getLaunchIntentForPackage(pkgname);
        startActivity(intent);



    }

    TextView.OnEditorActionListener enter_handle = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                AskWatsonTask tas = new AskWatsonTask();
                tas.execute(new String[]{user_input.getText().toString()});
            }
            return true;
        }
    };

}
