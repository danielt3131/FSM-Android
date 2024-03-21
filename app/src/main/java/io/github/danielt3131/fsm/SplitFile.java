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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.github.danielt3131.fsm.MMS.MMSSender;

public class SplitFile {
    private Uri fileUri;
    private String filename;
    private int segmentSize;
    private String phoneNumber;
    private Context context;
    private ProgressBar progressBar;
    private long fileSize;



    public SplitFile(Uri fileUri, int segmentSize, Context context, ProgressBar progressBar, String phoneNumber) {
        this.fileUri = fileUri;
        this.segmentSize = segmentSize;
        this.context = context;
        this.progressBar = progressBar;
        this.phoneNumber = phoneNumber;
    }

    private void getFileInfo(){
        Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        filename = cursor.getString(nameIndex);
        fileSize = cursor.getLong(sizeIndex);
        Log.d("File Name", filename);
        Log.d("File Size", String.valueOf(fileSize));
    }

    private InputStream getInputFileStream() throws FileNotFoundException {
        return context.getContentResolver().openInputStream(fileUri);
    }


    public void run() throws Exception {
        getFileInfo();

        long numberOfSegments = fileSize/ segmentSize;
        long remainderSegmentSize = fileSize % segmentSize;
        int numMessages = (int) numberOfSegments;
        if (remainderSegmentSize != 0) {
            numMessages++;
        }

        byte[] buffer;
        if (segmentSize <= Reference.MAX_BUFFFERSIZE) {
            buffer = new byte[segmentSize];
        } else {
            throw new IOException("Segment size exceeded 250MiB");
        }
        //FileInputStream fileInputStream = new FileInputStream(inputFile);
        long i;
        Log.d("Split File", "Started Split File");

        // Setting progress bar
        progressBar.setMax(numMessages);
        InputStream fileInputStream = getInputFileStream();
        for (i = 1; i <= numberOfSegments; i++) {
            fileInputStream.read(buffer, 0, buffer.length);
            Log.d("FileRead", "Read File");
            String outputName = String.format("%s.fsm.%d", filename, i);
            String outputFilePath = Reference.SAVE_LOCATION + outputName;
            progressBar.setProgress((int) i, true);
            if (!phoneNumber.isEmpty()) {
                MMSSender.sendMmsSegment(buffer,"Segment " + i + " out of " + numMessages, outputName, phoneNumber, context);
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
            String outputName = String.format("%s.fsm.%d", filename, i);
            String outputFilePath = Reference.SAVE_LOCATION + outputName;
            progressBar.setProgress((int) i, true);
            if (!phoneNumber.isEmpty()) {
                MMSSender.sendMmsSegment(buffer, "Segment " + i + " out of " + numMessages, outputName, phoneNumber, context);
            } else {
                FileOutputStream outputStream = new FileOutputStream(outputFilePath);
                outputStream.write(buffer, 0, (int) remainderSegmentSize);
                outputStream.close();
            }
        }
        // Close the input stream
        fileInputStream.close();
    }


}
