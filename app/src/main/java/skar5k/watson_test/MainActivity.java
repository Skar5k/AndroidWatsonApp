package skar5k.watson_test;

import android.Manifest;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private TextView t;                                                 //views for UI
    private EditText user_input;
    private Button b;
    private Button speak;

    private Map<String, Object> context;                                //converstion context
    private ConversationService wats;                                   //conversation service
    private TextToSpeech textService;                                   //text to speech service

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                         //find views and set listeners
        t = (TextView) findViewById(R.id.textv);
        b = (Button) findViewById(R.id.button);
        speak = (Button) findViewById(R.id.Speak_button);
        user_input = (EditText) findViewById(R.id.userEntry);
        user_input.setOnEditorActionListener(enter_handle);
        context = new HashMap<>();
        b.setOnClickListener(button_click);
        speak.setOnClickListener(speak_click);

        ActivityCompat.requestPermissions(this,                             //check wifi permissions
                new String[]{Manifest.permission.CHANGE_WIFI_STATE},
                3);
        textService = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {    //create services
                textService.setLanguage(Locale.ENGLISH);
            }
        });

        AskWatsonTask init = new AskWatsonTask();                                //get initial welcome message
        init.execute(new String[]{""});


    }

    View.OnClickListener speak_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {                                           //Speech to text button
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,                                  //prompt speech input
                    "Say something!");
            try {
                startActivityForResult(intent, 100);
            } catch (ActivityNotFoundException a) {
                Toast.makeText(getApplicationContext(),
                        "Not Supported",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    View.OnClickListener button_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {                                       //send text message to watson
            AskWatsonTask tas = new AskWatsonTask();
            tas.execute(new String[]{user_input.getText().toString()});
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {     //permission check
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 100: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    AskWatsonTask tas = new AskWatsonTask();
                    tas.execute(new String[]{result.get(0)});
                }
                break;
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {          //request permission protocol
        if(requestCode == 0 || requestCode == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

        }
        else{
            Toast t = Toast.makeText(getApplicationContext(), "Did not get permission!",Toast.LENGTH_SHORT);
            t.show();
        }
    }


    private class AskWatsonTask extends AsyncTask<String, Void, String> {                                       //allows a text message to be sent to Watson
        @Override
        protected String doInBackground(String... textsToAnalyse) {


            wats = new ConversationService(ConversationService.VERSION_DATE_2017_02_03);                    //set up service
            wats.setUsernameAndPassword("597cbd24-5f8d-4004-84f3-a9f581932f4b", "uuKVEU2r156D");

            MessageRequest m;
            if(context.size() > 0){                                                                         //set context if needed
                m = new MessageRequest.Builder().inputText(textsToAnalyse[0]).context(context).build();
            }
            else
                m = new MessageRequest.Builder().inputText(textsToAnalyse[0]).build();

            MessageResponse res = wats.message("e9974ceb-724f-4490-ae09-4eda4dc6802f",m).execute();         //send message and get response

            context = (Map) res.getContext();                                                               //update context

            return res.getText().toString();                                                                //return result for onPostExecute
        }

        //setting the value of UI outside of the thread
        @Override
        protected void onPostExecute(String result) {
            String speech = "";                                                             //stores message to show user


            if(result.contains("TURNONMUSIC")){                                             //execute command based on result
                AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                if(am.isMusicActive()){
                    am.setStreamVolume(AudioManager.STREAM_MUSIC,5,0);
                    speech = "Unmuting your music";
                }
                else {
                    playMusic();
                    speech = "Ok, opening your music player";
                }
            }

            else if(result.contains("SKIPFORWARD")){
                change_song("next");
                speech = "Ok, skipping to the next song";
            }

            else if(result.contains("SKIPBACK")){
                change_song("previous");
                change_song("previous");
                speech = "Ok, going back";
            }

            else if(result.contains("TURNOFFMUSIC")){
                change_song("stop");
                speech = "Ok, turning off your music";
            }

            else if(result.contains("MUTE")){
                muteMusic();
                speech = "Ok, muting your music";
            }

            else if(result.contains("OPENBROWSER")){
                openInternet(null);
                speech = "Ok, opening your browser";
            }

            else if(result.contains("VOLUP")){
                changeVolume(2);
                speech = "Turning up your volume!";
            }
            else if(result.contains("VOLDOWN")){
                changeVolume(-2);
                speech = "Turning down your volume!";
            }

            else if(result.contains("WIFION")){
                change_wifi_enabled(true);
                speech = "Turning on WiFi";
            }

            else if(result.contains("WIFIOFF")){
                change_wifi_enabled(false);
                speech = "Turning off WiFi";
            }

            else if(result.contains("FLASHON")){
                change_flash_state(true);
                speech = "Turning on flashlight";
            }

            else if(result.contains("FLASHOFF")){
                change_flash_state(false);
                speech = "Turning off flashlight";
            }

            t.setText(speech);                                          //send speech text to Text To Speech object
            TTS tts = new TTS();
            tts.execute(speech);
        }
    }

    private void change_flash_state(Boolean state){                                 //changes flashlight on/off
        CameraManager cam = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cam.getCameraIdList()[0];
            cam.setTorchMode(cameraId, state);
        }
        catch (Exception r){}
    }

    private void change_wifi_enabled(Boolean state){                                //changes wifi on/off
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wm.setWifiEnabled(state);
    }

    private void change_song(String s){                                             //change music based on command in String s
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if(am.isMusicActive()){
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", s);
            sendBroadcast(i);
        }
    }

    private class TTS extends AsyncTask<String, Void, String> {                         //takes a string and uses TTS to say it audibly
        @Override
        protected String doInBackground(String... textToSpeak) {
            String utteranceID = this.hashCode() + "";
            textService.speak(textToSpeak[0], TextToSpeech.QUEUE_FLUSH, null, utteranceID);
            return "text to speech done";
        }

        @Override
        protected void onPostExecute(String result) {

        }

    }


    private void openPhone(String number){                                                  //opens phone app
        Intent i = new Intent(Intent.ACTION_DIAL);
        if(number != null){
            i.setData(Uri.parse("tel:"+number));
        }

        startActivity(i);
    }

    private void muteMusic(){                                                           //mutes music
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
    }

    private void openInternet(String query){                                            //opens internet browser
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


    private void playMusic(){                                                       //starts music app
        String pkgname = "com.sec.android.app.music";
        PackageManager pkgmanager = getPackageManager();
        Intent intent = pkgmanager.getLaunchIntentForPackage(pkgname);
        startActivity(intent);
    }

    private void changeVolume(int change_val){                                      //changes volume up or down based on int parameter
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int curr = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, curr + change_val, 0);
    }

    TextView.OnEditorActionListener enter_handle = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {     //allows enter key on keyboard to be used to accept input
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                AskWatsonTask tas = new AskWatsonTask();
                tas.execute(new String[]{user_input.getText().toString()});
            }
            return true;
        }
    };

}
