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

package io.github.danielt3131.fsm.MMS;

import static com.klinker.android.send_message.Transaction.DEFAULT_EXPIRY_TIME;
import static com.klinker.android.send_message.Transaction.DEFAULT_PRIORITY;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MMSPart;
import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduBody;
import com.google.android.mms.pdu_alt.PduComposer;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPart;
import com.google.android.mms.pdu_alt.SendReq;
import com.google.android.mms.smil.SmilHelper;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;
import com.klinker.android.send_message.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MMSSender {

    public static void sendMmsSegment(byte[] buffer, String txtMessage, String filename, String phoneNumber, Context context) throws Exception {
        com.klinker.android.send_message.Settings sendSettings = new com.klinker.android.send_message.Settings();
        sendSettings.setMmsc(sendSettings.getMmsc());
        sendSettings.setProxy(sendSettings.getProxy());
        sendSettings.setPort(sendSettings.getPort());
        sendSettings.setUseSystemSending(true);

        Transaction transaction = new Transaction(context, sendSettings);
        Message message = new Message(txtMessage, phoneNumber);
        message.addMedia(buffer, "application/octet-stream", filename);
        message.setSave(true);
        Log.d("MMS", "Sent segment");
        // Sets the address from to the user's phone number based on their SIM Card
        message.setFromAddress(Utils.getMyPhoneNumber(context));
        sendMmsMessageNative(context, message, sendSettings);

    }


    /**
     * From KDE Connect https://invent.kde.org/network/kdeconnect-android
     * @param context
     * @param message The current message object from the android-smsmms library
     * @param klinkerSettings The settings object from the android-smsmms library
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    protected static void sendMmsMessageNative(Context context, Message message, Settings klinkerSettings) {
        ArrayList<MMSPart> data = new ArrayList<>();

        for (Message.Part p : message.getParts()) {
            MMSPart part = new MMSPart();
            if (p.getName() != null) {
                part.Name = p.getName();
            } else {
                part.Name = p.getContentType().split("/")[0];
            }
            part.MimeType = p.getContentType();
            part.Data = p.getMedia();
            data.add(part);
        }

        if (message.getText() != null && !message.getText().equals("")) {
            // add text to the end of the part and send
            MMSPart part = new MMSPart();
            part.Name = "text";
            part.MimeType = "text/plain";
            part.Data = message.getText().getBytes();
            data.add(part);
        }

        SendReq sendReq = buildPdu(context, message.getFromAddress(), message.getAddresses(), message.getSubject(), data, klinkerSettings);

        Bundle configOverrides = new Bundle();
        configOverrides.putBoolean(SmsManager.MMS_CONFIG_GROUP_MMS_ENABLED, klinkerSettings.getGroup());

        // Write the PDUs to disk so that we can pass them to the SmsManager
        final String fileName = "send." + Math.abs(new Random().nextLong()) + ".dat";
        File mSendFile = new File(context.getCacheDir(), fileName);

        Uri contentUri = (new Uri.Builder())
                .authority(context.getPackageName() + ".MmsFileProvider")
                .path(fileName)
                .scheme(ContentResolver.SCHEME_CONTENT)
                .build();

        try (FileOutputStream writer = new FileOutputStream(mSendFile)) {
            writer.write(new PduComposer(context, sendReq).make());
        } catch (final IOException e)
        {
            android.util.Log.e("SENDING_MESSAGE", "Error while writing temporary PDU file: ", e);
        }

        SmsManager mSmsManager;

        if (klinkerSettings.getSubscriptionId() < 0)
        {
            mSmsManager = SmsManager.getDefault();
        } else {
            mSmsManager = SmsManager.getSmsManagerForSubscriptionId(klinkerSettings.getSubscriptionId());
        }

        mSmsManager.sendMultimediaMessage(context, contentUri, null, null, null);
    }

    /**
     * From KDE Connect https://invent.kde.org/network/kdeconnect-android
     * @param pb object of the PduBody
     * @param p object of the MMSPart
     * @param id
     * @return
     */
    private static int addTextPart(PduBody pb, MMSPart p, int id) {
        String filename = p.Name;
        final PduPart part = new PduPart();
        // Set Charset if it's a text media.
        if (p.MimeType.startsWith("text")) {
            part.setCharset(CharacterSets.UTF_8);
        }
        // Set Content-Type.
        part.setContentType(p.MimeType.getBytes());
        // Set Content-Location.
        part.setContentLocation(filename.getBytes());
        int index = filename.lastIndexOf(".");
        String contentId = (index == -1) ? filename
                : filename.substring(0, index);
        part.setContentId(contentId.getBytes());
        part.setData(p.Data);
        pb.addPart(part);

        return part.getData().length;
    }


    /**
     * From KDE Connect https://invent.kde.org/network/kdeconnect-android
     * Builds PDU
     * @param context
     * @param fromAddress
     * @param recipients
     * @param subject
     * @param parts
     * @param settings
     * @return
     */
    private static SendReq buildPdu(Context context, String fromAddress, String[] recipients, String subject,
                                    List<MMSPart> parts, Settings settings) {
        final SendReq req = new SendReq();
        // From, per spec
        req.prepareFromAddress(context, fromAddress, settings.getSubscriptionId());
        // To
        for (String recipient : recipients) {
            req.addTo(new EncodedStringValue(recipient));
        }
        // Subject
        if (!TextUtils.isEmpty(subject)) {
            req.setSubject(new EncodedStringValue(subject));
        }
        // Date
        req.setDate(System.currentTimeMillis() / 1000);
        // Body
        PduBody body = new PduBody();
        // Add text part. Always add a smil part for compatibility, without it there
        // may be issues on some carriers/client apps
        int size = 0;
        for (int i = 0; i < parts.size(); i++) {
            MMSPart part = parts.get(i);
            size += addTextPart(body, part, i);
        }

        // add a SMIL document for compatibility
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(body), out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        body.addPart(0, smilPart);

        req.setBody(body);
        // Message size
        req.setMessageSize(size);
        // Message class
        req.setMessageClass(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes());
        // Expiry
        req.setExpiry(DEFAULT_EXPIRY_TIME);
        try {
            // Priority
            req.setPriority(DEFAULT_PRIORITY);
            // Delivery report
            req.setDeliveryReport(PduHeaders.VALUE_NO);
            // Read report
            req.setReadReport(PduHeaders.VALUE_NO);
        } catch (InvalidHeaderValueException e) {}

        return req;
    }

}
