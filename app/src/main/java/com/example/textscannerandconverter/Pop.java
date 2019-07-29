package com.example.textscannerandconverter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

public class Pop extends Activity {

    String url;
    EditText ocrText;
    TextView selectedText, definitionText;
    Button btnPronounce;
    TextToSpeech tts;
    private boolean isInFront;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        setContentView(R.layout.popwindow);

        //set popup metrics
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = (int)(dm.widthPixels * 0.8);
        int height = (int)(dm.heightPixels * 0.6);

        getWindow().setLayout(width,height);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ocrText = (EditText) findViewById(R.id.ocr_text);
        selectedText = (TextView) findViewById(R.id.selected_text);
        definitionText = (TextView) findViewById(R.id.definition_text);
        btnPronounce = (Button) findViewById(R.id.btnPronounce);

        //set text for ocr
        Intent intent = getIntent();

        if(intent != null){
            String ocr = intent.getStringExtra("ocr");
            String translation = "";

            if(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) != null) {
                translation = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString();
            }

            if(ocr != null) {
                ocrText.setText(ocr);
            } else if(!translation.equals("")) {
                selectedText.setText(translation);
                ocrText.setVisibility(View.GONE);
                selectedText.setVisibility(View.VISIBLE);
                definitionText.setVisibility(View.VISIBLE);
                btnPronounce.setVisibility(View.VISIBLE);

                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR) {
                            tts.setLanguage(Locale.UK);
                        }
                    }
                });

                final String finalTranslation = translation;
                btnPronounce.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tts.speak(finalTranslation, TextToSpeech.QUEUE_FLUSH, null);
                    }
                });

                MyDictionaryRequest myDictionaryRequest = new MyDictionaryRequest(Pop.this, definitionText);
                url = dictionaryEntries(translation);
                myDictionaryRequest.execute(url);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(MotionEvent.ACTION_OUTSIDE == event.getAction() && isInFront) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("ocrResult", ocrText.getText().toString());
            setResult(RESULT_OK, returnIntent);
            finish();
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }

        super.onPause();
        isInFront = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInFront = true;
    }

    private String dictionaryEntries(String translation) {
        final String language = "en-gb";
        final String word = translation;
        final String fields = "definitions";
        final String strictMatch = "false";
        final String word_id = word.toLowerCase();
        return "https://od-api.oxforddictionaries.com:443/api/v2/entries/" + language + "/" + word_id + "?" + "fields=" + fields + "&strictMatch=" + strictMatch;
    }
}
