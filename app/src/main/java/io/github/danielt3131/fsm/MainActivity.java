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


package io.github.danielt3131.fsm;


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
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.github.danielt3131.fsm.MMS.MMSSender;

public class MainActivity extends AppCompatActivity {

    Button fileSelectButton, startButton, textPreset, emailPreset, customSize;
    Switch toggleSwitch;
    final int SEGMENT_SIZE_EMAIL_PRESET = 20000000; // 20MB
    final int SEGMENT_SIZE_MMS_PRESET = 1000000;
    final int INPUT_FILE = 10;
    final String SAVE_LOCATION = "/storage/emulated/0/Documents/FSM/";
    int segmentSize = 0;
    int MAX_BUFFFERSIZE = 1024 * 1024 * 250;
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
            segmentSize = SEGMENT_SIZE_MMS_PRESET;
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
            segmentSize = SEGMENT_SIZE_EMAIL_PRESET;
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
            if (inputSegmentSize.toString().length() != 0) {
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
            File saveDir = new File(SAVE_LOCATION);
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
                    } catch (IllegalThreadStateException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Merge File Thread", e.getMessage());
                    }
                } else if (split && gotInputPath) {
                    //segmentSize = Integer.parseInt(inputSegmentSize.getText().toString());
                    // Sets the segment size if the user didn't press done when typing in a segment size
                    if (inputSegmentSize.getVisibility() == View.VISIBLE && segmentSize == 0) {
                        if (inputSegmentSize.getText().length() != 0) {
                            try {
                                segmentSize = Integer.parseInt(inputSegmentSize.getText().toString());
                            } catch (NumberFormatException e) {
                                // Print the exception as a toast.
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("Inputted Segment Size", e.getMessage());
                            }
                        }
                    } else if (segmentSize == 0) {
                        //toast.setText("Enter segment size in bytes");
                        //toast.show();
                        toast.setText("Choose a preset or custom");
                        toast.show();
                    } else {
                        Log.d("Segment Size", String.valueOf(segmentSize));
                        Log.d("Phone number", phoneNumber);
                        progressBar.setProgress(0); // Reset progress bar
                        try {
                            splitFileThread.start();
                        } catch (RuntimeException e) {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
     * Create threads for splitFile and mergeFile
     */

    Thread splitFileThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                splitFile();
            } catch (Exception e) {
                Log.e("Split File", e.getMessage());
            }
        }
    });


    Thread mergeFileThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                mergeFile();
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
        startActivityForResult(intent, INPUT_FILE);
    }

    String inputFilePath = "";
    String inputFileName = "";

    /**
     * Mode toggle switch listener
     */
    CompoundButton.OnCheckedChangeListener toggleSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                toggleSwitch.setText("Split mode");
                //inputSegmentSize.setVisibility(View.VISIBLE);
                //inputSegmentSize.setHint("Segment size in bytes");
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
        if (resultCode == Activity.RESULT_OK && requestCode == INPUT_FILE) {
            if (data != null) {
                Uri uri = data.getData();
                inputFileName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);    // Gets the input file name
                // Gets absolute file path from th URI assuming it's internal storage not external/sd card storage
                inputFilePath = "/storage/emulated/0/" + uri.getPath().substring(uri.getPath().lastIndexOf(":") + 1);
                gotInputPath = true;
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == 25) {
            Log.d("MMS", "Called share menu");
            //notify();
        } else {
            fileSelectButton.setText(FILE_SELECT);  // Resets the file select button text
        }
    }

    /**
     * Method to split a file into n segments
     * @throws IOException
     */
    public void splitFile() throws Exception {
        File inputFile = new File(inputFilePath);
        long numberOfSegments = inputFile.length() / segmentSize;
        long remainderSegmentSize = inputFile.length() % segmentSize;
        int numMessages = (int) numberOfSegments;
        if (remainderSegmentSize != 0) {
            numMessages++;
        }
        byte[] buffer;
        if (segmentSize <= MAX_BUFFFERSIZE) {
            buffer = new byte[segmentSize];
        } else {
            throw new IOException("Segment size exceeded 250MiB");
        }
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        long i;
        Log.d("Split File", "Started Split File");

        // Setting progress bar
        progressBar.setMax(numMessages);

        for (i = 1; i <= numberOfSegments; i++) {
            fileInputStream.read(buffer, 0, buffer.length);
            Log.d("FileRead", "Read File");
            String outputName = String.format("%s.fsm.%d", inputFileName, i);
            String outputFilePath = SAVE_LOCATION + outputName;
            progressBar.setProgress((int) i, true);
            if (sendMMS) {
                MMSSender.sendMmsSegment(buffer,"Segment " + i + " out of " + numMessages, outputName, phoneNumber, MainActivity.this);
                //wait();
            } else {
                FileOutputStream outputStream = new FileOutputStream(outputFilePath);
                outputStream.write(buffer, 0, buffer.length);
                Log.d("FileWrite", "Wrote segment");
                outputStream.close();
            }
        }

        if (remainderSegmentSize != 0) {
            fileInputStream.read(buffer, 0, (int) remainderSegmentSize);
            String outputName = String.format("%s.fsm.%d", inputFileName, i);
            String outputFilePath = SAVE_LOCATION + outputName;
            progressBar.setProgress((int) i, true);
            if (sendMMS) {
                MMSSender.sendMmsSegment(buffer, "Segment " + i + " out of " + numMessages, outputName, phoneNumber, MainActivity.this);
            } else {
                FileOutputStream outputStream = new FileOutputStream(outputFilePath);
                outputStream.write(buffer, 0, (int) remainderSegmentSize);
                outputStream.close();
            }
        }
        // Close the input stream
        fileInputStream.close();
    }



    /**
     * Method to merge a file
     * @throws IOException
     */
    public void mergeFile() throws IOException {
        String outputName = inputFileName.substring(0, inputFileName.lastIndexOf(".fsm"));
        String segmentDir = inputFilePath.substring(0, inputFilePath.lastIndexOf(outputName));
        //execButton.setText(segmentDir);
        FileOutputStream outputStream = new FileOutputStream(SAVE_LOCATION + outputName);

        // Calculate segment size for buffer allocation
        File fileSegmentSize = new File(segmentDir + outputName + ".fsm.1");
        long segmentSize = fileSegmentSize.length();
        byte[] buffer = new byte[(int) segmentSize];

        long i = 1;
        boolean hasCompleted = false;
        while (!hasCompleted) {
            String fileSegmentPath = String.format("%s%s.fsm.%d", segmentDir, outputName, i);
            File fileSegment =  new File(fileSegmentPath);
            if (fileSegment.exists()) {
                FileInputStream fileInputStream = new FileInputStream(fileSegment);
                int indivSegmentSize = (int) fileSegment.length();
                fileInputStream.read(buffer, 0, indivSegmentSize);
                outputStream.write(buffer, 0, indivSegmentSize);
                fileInputStream.close();
                progressBar.setProgress((int) i, true);
                i++;
            } else {
                hasCompleted = true;
            }
            fileSegment.delete();   // Remove segment from storage / auto cleanup
        }
        // Close the output stream
        outputStream.close();
    }
}