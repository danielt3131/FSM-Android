package io.github.danielt3131.fsm;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    Button fileSelectButton, startButton;
    Switch toggleSwitch;
    final int READ_WRITE_PERM_REQ = 15;
    final int INPUT_FILE = 10;
    final String SAVE_LOCATION = "/storage/emulated/0/Documents/FSM/";
    int segmentSize = 0;
    int MAX_BUFFFERSIZE = 1024 * 1024 * 250;
    final String FILE_SELECT = "File Select";
    boolean hasRW = false;

    TextView inputSegmentSize;
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

        // Force portrat on phones
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL || screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        fileSelectButton = findViewById(R.id.fileSelector);
        toggleSwitch = findViewById(R.id.toggleSwitch);
        startButton = findViewById(R.id.startButton);
        inputSegmentSize = findViewById(R.id.segmentSize);
        fileSelectButton.setOnClickListener(fileSelectView);
        toggleSwitch.setOnCheckedChangeListener(toggleSwitchListener);
        inputSegmentSize.setOnClickListener(getSegmentSize);
        startButton.setOnClickListener(startButtonView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                hasRW = true;
            } else {
                Toast toast = new Toast(this);
                toast.setText("Need all files permission to continue");
                toast.show();
                // Pull up settings
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }

    }

    boolean gotInputPath = false;
    boolean merged = true;
    boolean split = false;

    /**
     * Segment size text entry listener
     */
    View.OnClickListener getSegmentSize = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            segmentSize = Integer.parseInt(inputSegmentSize.getText().toString());
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
            if (split && inputSegmentSize.getText().length() == 0) {
                fileSelectButton.setText("Enter segment size in bytes");
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
            if (merged && gotInputPath) {
                try {
                    mergeFile();
                    toast.setText("The operation of merge file completed");
                    toast.show();
                } catch (IOException e) {
                    toast.setText("There was an error " + e);
                    toast.show();
                }
            } else if (split && gotInputPath) {
                try {
                    if (inputSegmentSize.getText().length() == 0) {
                        toast.setText("Enter segment size in bytes");
                        toast.show();
                    } else {
                        //segmentSize = Integer.parseInt(inputSegmentSize.getText().toString());
                        if (segmentSize == 0) {
                            toast.setText("Enter segment size in bytes");
                            toast.show();
                        } else {
                            Log.d("Segment Size", String.valueOf(segmentSize));
                            splitFile();
                            toast.setText("The operation of split file completed");
                            toast.show();
                        }
                    }
                } catch (IOException e) {
                    toast.setText("There was an error " + e);
                    toast.show();
                }
            } else {
                toast.setText("No mode selected");
                toast.show();
            }
        }
    };

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
                inputSegmentSize.setVisibility(View.VISIBLE);
                inputSegmentSize.setHint("Segment size in bytes");
                split = true;
                merged = false;
                fileSelectButton.setText(FILE_SELECT);
            } else {
                toggleSwitch.setText("Merge mode");
                inputSegmentSize.setHint("");
                fileSelectButton.setText(FILE_SELECT);
                inputSegmentSize.setVisibility(View.INVISIBLE);
                split = false;
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
        } else {
            fileSelectButton.setText(FILE_SELECT);  // Resets the file select button text
        }
    }

    /**
     * Method to split a file into n segments
     * @throws IOException
     */
    public void splitFile() throws IOException {
        File inputFile = new File(inputFilePath);
        long numberOfSegments = inputFile.length() / segmentSize;
        long remainderSegmentSize = inputFile.length() % segmentSize;
        byte[] buffer;
        if (segmentSize <= MAX_BUFFFERSIZE) {
            buffer = new byte[segmentSize];
        } else {
            throw new IOException("Segment size exceeded 250MiB");
        }
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        long i;
        Log.d("Split File", "Started Split File");

        for (i = 1; i <= numberOfSegments; i++) {
            fileInputStream.read(buffer, 0, buffer.length);
            Log.d("FileRead", "Read File");
            String outputName = String.format("%s.fsm.%d", inputFileName, i);
            String outputFilePath = SAVE_LOCATION + outputName;
            FileOutputStream outputStream = new FileOutputStream(outputFilePath);
            outputStream.write(buffer, 0, buffer.length);
            Log.d("FileWrite", "Wrote segment");
            outputStream.close();
        }

        if (remainderSegmentSize != 0) {
            fileInputStream.read(buffer,0, (int) remainderSegmentSize);
            String outputName = String.format("%s.fsm.%d", inputFileName, i + 1);
            String outputFilePath = SAVE_LOCATION + outputName;
            FileOutputStream outputStream = new FileOutputStream(outputFilePath);
            outputStream.write(buffer, 0, (int) remainderSegmentSize);
            outputStream.close();
        }
        // Close the input stream
        fileInputStream.close();
        inputSegmentSize.setText("");   // Reset the input segment size
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