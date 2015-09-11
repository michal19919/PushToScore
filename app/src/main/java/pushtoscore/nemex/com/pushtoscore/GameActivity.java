package pushtoscore.nemex.com.pushtoscore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class GameActivity extends Activity implements View.OnClickListener, View.OnKeyListener {

    /* Views on game.xml */
    TextView scoreTextView;
    TextView timeTextView;
    ImageButton pushButton;
    RelativeLayout gameZoneLayout;

    /* Game time and updater variables */
    Handler uiHandler; //Allows us to dispatch messages to the UI
    Timer gameTimer; //Our main game timer used to schedule button placement
    Timer stopperTimer; //Our stopper update timer
    int updatePeriod = 500; //The time button will appear

    /* Game Variables */
    boolean isPlaying = false;
    int score = 0; //Current game score
    long startTime; //Game start time (in milliseconds)

    MediaPlayer mediaPlayer; //Used to play push button sounds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        /* Setting variables pointing to their XML views */
        scoreTextView = (TextView) findViewById(R.id.scoreTextView);
        timeTextView = (TextView) findViewById(R.id.timeTextView);
        pushButton = (ImageButton) findViewById(R.id.pushButton);
        gameZoneLayout = (RelativeLayout) findViewById(R.id.gameZoneLayout);

        uiHandler = new Handler();
        gameTimer = new Timer();
        stopperTimer = new Timer();

        //Allows us to handle push button
        pushButton.setOnClickListener(this);
        pushButton.setOnKeyListener(this);

        //Preparing our push button media player to be ready
        mediaPlayer = MediaPlayer.create(this, R.raw.pushbutton_sound);
        try {
            mediaPlayer.prepare();
        } catch (Exception ignored) {
        }

        //Allows us to control the media volume using volume controls
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            startGame();
        else
            stopGame(false);
    }

    @Override
    public void onClick(View v) {
        if (isPlaying) {
            score += 1;
            updateScore();
            mediaPlayer.start();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        //Letting the user use the BACK button normally while avoiding other user input (preventing user from click on the "PUSH" button using keys)
        return keyCode != KeyEvent.KEYCODE_BACK;
    }

    void updateScore() {
        scoreTextView.setText("Score: " + score);
    }

    void startGame() {
        score = 0;
        updateScore();
        startTime = System.currentTimeMillis();
        gameTimer.scheduleAtFixedRate(new GameTimerTask(), 0, updatePeriod);
        stopperTimer.scheduleAtFixedRate(new StopperTimerTask(), 0, 50);
        isPlaying = true;
    }

    class GameTimerTask extends TimerTask {
        @Override
        public void run() {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    placePushButtonRandomly();
                }
            });
        }
    }

    void placePushButtonRandomly() {
        Random rnd = new Random();
        int left = rnd.nextInt(gameZoneLayout.getWidth() - pushButton.getWidth());
        int top = rnd.nextInt(gameZoneLayout.getHeight() - pushButton.getHeight());

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) pushButton.getLayoutParams();
        layoutParams.setMargins(left, top, 0, 0);
        pushButton.setLayoutParams(layoutParams);
    }

    class StopperTimerTask extends TimerTask {
        @Override
        public void run() {
            uiHandler.post(new Runnable() {

                @Override
                public void run() {
                    long timePassed = System.currentTimeMillis() - startTime; //Calculating time passed from the game start
                    timeTextView.setText(getTimeString(System.currentTimeMillis() - startTime)); //Updating the stopper

                    //If the game time passed 30 seconds
                    if ((timePassed / 1000) >= 30)
                        stopGame(true);
                }
            });

        }
    }

    String getTimeString(long milliTime) {
        int seconds = (int) milliTime / 1000;
        int milli = (int) (milliTime % 1000) / 10;
        return getClockString(seconds) + ":" + getClockString(milli);
    }

    String getClockString(int digit) {
        if (digit < 10)
            return "0" + digit;
        return String.valueOf(digit);
    }

    void stopGame(boolean showGameOver) {
        isPlaying = false;
        gameTimer.cancel();
        gameTimer = new Timer();

        stopperTimer.cancel();
        stopperTimer = new Timer();

        if (showGameOver)
            // ToDo update this function to it's new version
            showDialog(Dialogs.DIALOG_GAMEOVER);
    }

    class Dialogs {
        public final static int DIALOG_GAMEOVER = 0;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog diag = null;
        switch (id) {
            case Dialogs.DIALOG_GAMEOVER:
                return new AlertDialog.Builder(this)
                        .setTitle("Game Over!")
                        .setMessage("Your score is: " + score + "!")
                        .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .create();
        }

        return diag;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Final clean up
        mediaPlayer.release();
        mediaPlayer = null;
    }
}
