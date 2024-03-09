package io.github.danielt3131.fsm;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    Button execButton;
    Switch toggleSwitch;
    final int READ_WRITE_PERM_REQ = 15;
    final int MERGE_FILE_SRC = 10;
    final int MERGE_FILE_DEST = 11;
    final int SPLIT_FILE_SRC = 12;
    final int SPLIT_FILE_DEST = 13;
    int segmentSize = 0;

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
        execButton = findViewById(R.id.execButton);
        toggleSwitch = findViewById(R.id.toggleSwitch);
        inputSegmentSize = findViewById(R.id.segmentSize);
        execButton.setOnClickListener(execButtonView);
        toggleSwitch.setOnCheckedChangeListener(toggleSwitchListener);
        inputSegmentSize.setOnClickListener(getSegmentSize);


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



    boolean merged = false;
    boolean split = false;

    View.OnClickListener getSegmentSize = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            segmentSize = Integer.parseInt(inputSegmentSize.getText().toString());
        }
    };

    View.OnClickListener execButtonView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            execButton.setText(String.valueOf(segmentSize));
            if (split && segmentSize > 0) {
                splitFileSrc(v);
                splitFileDestDir(v);
            }
            if (merged) {
                if (mergeFileSrcDir(v)) {
                    mergeFileDest(v);
                }

            }
        }
    };
    InputStream inputFileStream;
    CompoundButton.OnCheckedChangeListener toggleSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                toggleSwitch.setText("Split mode");
                split = true;
                merged = false;
            } else {
                toggleSwitch.setText("Merge mode");
                split = false;
                merged = true;
            }
        }
    };

    public boolean mergeFileSrcDir (View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, MERGE_FILE_SRC);
        return true;
    }

    public void mergeFileDest (View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(intent, MERGE_FILE_DEST);
    }

    public void splitFileSrc (View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(intent, SPLIT_FILE_SRC);
    }

    public void splitFileDestDir (View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, SPLIT_FILE_DEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == MERGE_FILE_SRC) {
            if (data != null) {
                Uri uri = data.getData();
                inputFile = new File(data.getData().getPath());
                System.out.println(inputFile.getPath());
                if (uri != null) {
                    try {
                        inputStream = getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if (resultCode == Activity.RESULT_OK && requestCode == MERGE_FILE_DEST) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        outputStream = getContentResolver().openOutputStream(uri);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if (resultCode == Activity.RESULT_OK && requestCode == SPLIT_FILE_SRC) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        inputStream = getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if (resultCode == Activity.RESULT_OK && requestCode == SPLIT_FILE_DEST) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        outputStream = getContentResolver().openOutputStream(uri);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}