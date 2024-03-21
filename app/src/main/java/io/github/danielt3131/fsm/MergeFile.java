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
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MergeFile implements PickiTCallbacks {
    private Uri fileUri;
    private Context context;
    private PickiT pickiT;
    private String filepath;
    private Activity activity;
    private String inputFileName;
    ProgressBar progressBar;

    public MergeFile(Uri fileUri, Context context, Activity activity, ProgressBar progressBar) {
        this.fileUri = fileUri;
        this.context = context;
        this.activity = activity;
        this.progressBar = progressBar;
        pickiT = new PickiT(this.context, this, this.activity);
    }

    private void getFilePath(){
        pickiT.getPath(fileUri, Build.VERSION.SDK_INT);
    }


    @Override
    public void PickiTonUriReturned() {

    }

    @Override
    public void PickiTonStartListener() {

    }

    @Override
    public void PickiTonProgressUpdate(int i) {

    }

    /**
     * The method of the PickiT library that returns the file path
     * @param filepath
     * @param b
     * @param b1
     * @param wasSucessful
     * @param s1
     */
    @Override
    public void PickiTonCompleteListener(String filepath, boolean b, boolean b1, boolean wasSucessful, String s1) {
        if (wasSucessful) {
            this.filepath = filepath;
            inputFileName = this.filepath.substring(filepath.lastIndexOf("/"));
        }
    }

    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> arrayList, boolean b, String s) {

    }

    public void run() throws IOException {
        getFilePath();
        if (TextUtils.isEmpty(inputFileName)) {
            throw new IOException("Invalid filename");    // Don't continue inputFileName will be null
        }
        if (!inputFileName.contains(".fsm")) {
            throw new IOException("Invalid file");
        }
        String outputName = inputFileName.substring(0, inputFileName.lastIndexOf(".fsm"));
        String segmentDir = filepath.substring(0, filepath.lastIndexOf(outputName));
        //execButton.setText(segmentDir);
        FileOutputStream outputStream = new FileOutputStream(Reference.SAVE_LOCATION + outputName);

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
