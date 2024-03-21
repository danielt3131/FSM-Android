/*
 * Copyright (c) 2024 Daniel J. Thompson.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


package io.github.danielt3131.fsm.ui;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.sql.Ref;

import io.github.danielt3131.fsm.MergeFile;
import io.github.danielt3131.fsm.Permissions;
import io.github.danielt3131.fsm.R;
import io.github.danielt3131.fsm.Reference;
import io.github.danielt3131.fsm.SplitFile;

public class MainActivity extends AppCompatActivity {

    Button fileSelectButton, startButton, textPreset, emailPreset, customSize;
    Switch toggleSwitch;
    int segmentSize = 0;
    final String FILE_SELECT = "File Select";
    boolean hasRW = false;
    androidx.appcompat.widget.Toolbar toolbar;

    TextView inputSegmentSize, inputPhoneNumber;
    Permissions permissionList;
    ProgressBar progressBar;
    /**
     * The creation method
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Checking and getting permissions on startup
        permissionList = new Permissions(MainActivity.this, MainActivity.this);
        permissionList.getReadWritePermissions();

        // Force portrait mode on phones
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL || screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        fileSelectButton = findViewById(R.id.fileSelector);
        toggleSwitch = findViewById(R.id.toggleSwitch);
        startButton = findViewById(R.id.startButton);
        inputSegmentSize = findViewById(R.id.segmentSize);
        textPreset = findViewById(R.id.mmsPreset);
        emailPreset = findViewById(R.id.emailPreset);
        customSize = findViewById(R.id.customMode);
        inputPhoneNumber = findViewById(R.id.inputPhoneNumber);
        toolbar = findViewById(R.id.materialToolbar);
        setSupportActionBar(toolbar);
        Log.d("Toolbar", toolbar.getTitle().toString());
        toolbar.setTitle("File Split Merge");
        progressBar = findViewById(R.id.progressbar);

        // Set listeners
        fileSelectButton.setOnClickListener(fileSelectView);
        toggleSwitch.setOnCheckedChangeListener(toggleSwitchListener);
        inputSegmentSize.setOnClickListener(getSegmentSize);
        startButton.setOnClickListener(startButtonView);
        textPreset.setOnClickListener(textMsgPresetListener);
        emailPreset.setOnClickListener(emailPresetListener);
        customSize.setOnClickListener(customSizeListener);
        inputPhoneNumber.setOnClickListener(getPhoneNumber);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.credits) {
            startActivity(new Intent(MainActivity.this, CreditsActivity.class));
            return true;
        }
        return false;
    }

    boolean gotInputPath = false;
    boolean merged = true;
    boolean split = false;
    boolean sendMMS = false;


    /**
     * Preset size listeners
     */
    View.OnClickListener textMsgPresetListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            segmentSize = Reference.SEGMENT_SIZE_MMS_PRESET;
            inputSegmentSize.setVisibility(View.INVISIBLE);
            Toast.makeText(MainActivity.this, "Using text message preset | 1MB", Toast.LENGTH_SHORT).show();
            if (gotInputPath) {
                fileSelectButton.setText("Press the start button to begin");
            }
            inputPhoneNumber.setVisibility(View.VISIBLE);
            inputPhoneNumber.setHint("Phone Number");
        }
    };
    String phoneNumber = "";
    View.OnClickListener getPhoneNumber = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!inputPhoneNumber.toString().isEmpty() || !phoneNumber.isEmpty()) {
                phoneNumber = inputPhoneNumber.getText().toString();
                permissionList.getMMSPermissions();
                sendMMS = true;
            }
        }
    };

    View.OnClickListener emailPresetListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            segmentSize = Reference.SEGMENT_SIZE_EMAIL_PRESET;
            inputSegmentSize.setVisibility(View.INVISIBLE);
            if (gotInputPath) {
                fileSelectButton.setText("Press the start button to begin");
            }
            Toast.makeText(MainActivity.this, "Using email preset | 20MB", Toast.LENGTH_SHORT).show();
            sendMMS = false;
            inputPhoneNumber.setVisibility(View.INVISIBLE);
        }
    };

    View.OnClickListener customSizeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            inputSegmentSize.setVisibility(View.VISIBLE);
            inputSegmentSize.setHint("Segment size in bytes");
            sendMMS = false;
            segmentSize = 0;
            inputPhoneNumber.setVisibility(View.INVISIBLE);
        }
    };



    /**
     * Segment size text entry listener
     */
    View.OnClickListener getSegmentSize = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!inputSegmentSize.toString().isEmpty()) {
                try {
                    segmentSize = Integer.parseInt(inputSegmentSize.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Inputted Segment Size", e.getMessage());
                }
            }
            if (split && gotInputPath && (inputSegmentSize.getText().length() > 0)) {
                fileSelectButton.setText("Press the start button to begin");
            }
        }
    };

    /**
     * File select button listener
     */
    View.OnClickListener fileSelectView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Create an input stream
            getInputFileURI();
            File saveDir = new File(Reference.SAVE_LOCATION);
            if (!saveDir.exists()) {
                saveDir.mkdir();
            }
            if (split && segmentSize == 0) {
                fileSelectButton.setText("Select a preset or use custom mode");
            } else {
                fileSelectButton.setText("Press the start button to begin");
            }
        }
    };


    /**
     * Start button listener
     */
    View.OnClickListener startButtonView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast toast = new Toast(MainActivity.this);
            if (permissionList.checkReadWritePermissions()) {
                if (merged && gotInputPath) {
                    progressBar.setProgress(0); // Reset progress bar
                    try {
                        mergeFileThread.start();
                        gotInputPath = false;   //Resets the condition to require file selection on next run
                    } catch (IllegalThreadStateException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Merge File Thread", e.getMessage());
                    }
                } else if (split && gotInputPath) {
                    // Sets the segment size if the user didn't press done when typing in a segment size
                    if (inputSegmentSize.getVisibility() == View.VISIBLE && segmentSize == 0) {
                        if (inputSegmentSize.getText().length() != 0) {
                            try {
                                segmentSize = Integer.parseInt(inputSegmentSize.getText().toString());
                            } catch (NumberFormatException e) {
                                // Print the exception as a toast.
                                Log.e("Inputted Segment Size", e.getMessage());
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else if (segmentSize == 0) {
                        toast.setText("Choose a preset or custom");
                        toast.show();
                    } else {
                        Log.d("Segment Size", String.valueOf(segmentSize));
                        Log.d("Phone number", phoneNumber);
                        progressBar.setProgress(0); // Reset progress bar
                        try {
                            splitFileThread.start();
                            gotInputPath = false;   //Resets the condition to require file selection on next run
                        } catch (RuntimeException e) {
                            Log.e("Split File Thread", e.getMessage());
                        }
                        if (sendMMS) {
                            toast.setText("Sent the segments to " + phoneNumber);
                            toast.show();
                        } else {
                            toast.setText("The operation of split file completed. The output is at Documents/FSM");
                            toast.show();
                        }
                    }
                } else if (gotInputPath) {
                    toast.setText("No mode selected");
                    toast.show();
                } else {
                    toast.setText("No file selected");
                    toast.show();
                }
            } else{
                permissionList.getReadWritePermissions();
            }
        }
    };

    /**
     * Create threads for SplitFile and MergeFile
     */

    Thread splitFileThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                SplitFile splitFile = new SplitFile(fileUri, segmentSize, MainActivity.this, progressBar, phoneNumber);
                splitFile.run();
            } catch (Exception e) {
                Log.e("Split File", e.getMessage());
            }
        }
    });


    Thread mergeFileThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                MergeFile mergeFile = new MergeFile(fileUri, MainActivity.this, MainActivity.this, progressBar);
                mergeFile.run();
            } catch (Exception e) {
                Log.e("Merge File", e.getMessage());
            }
        }
    });


    /**
     * Method to create intent to summon the system file picker to get an input file name and path
     */
    public void getInputFileURI() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        startActivityForResult(intent, Reference.INPUT_FILE);
    }

    /**
     * Mode toggle switch listener
     */
    CompoundButton.OnCheckedChangeListener toggleSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                toggleSwitch.setText("Split mode");
                textPreset.setVisibility(View.VISIBLE);
                emailPreset.setVisibility(View.VISIBLE);
                customSize.setVisibility(View.VISIBLE);
                split = true;
                inputPhoneNumber.setVisibility(View.INVISIBLE);
                merged = false;
                fileSelectButton.setText(FILE_SELECT);
            } else {
                toggleSwitch.setText("Merge mode");
                //inputSegmentSize.setHint("");
                fileSelectButton.setText(FILE_SELECT);
                inputSegmentSize.setVisibility(View.INVISIBLE);
                textPreset.setVisibility(View.INVISIBLE);
                emailPreset.setVisibility(View.INVISIBLE);
                customSize.setVisibility(View.INVISIBLE);
                split = false;
                sendMMS = false;
                merged = true;
            }
        }
    };

    Uri fileUri;

    /**
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == Reference.INPUT_FILE) {
            if (data != null) {
                fileUri = data.getData();
                gotInputPath = true;
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == 25) {
            Log.d("MMS", "Called share menu");
            //notify();
        } else {
            fileSelectButton.setText(FILE_SELECT);  // Resets the file select button text
        }
    }
}