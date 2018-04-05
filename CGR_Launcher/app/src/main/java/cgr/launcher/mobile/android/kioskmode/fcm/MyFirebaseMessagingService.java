/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cgr.launcher.mobile.android.kioskmode.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import cgr.launcher.mobile.android.kioskmode.App;
import cgr.launcher.mobile.android.kioskmode.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "CGRFirebaseMsgService";

    public static final String INTENT_FILTER = "INTENT_FILTER";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        //Neura Magic
       /* if(remoteMessage!=null && remoteMessage.getData()!=null) {
            Map data = remoteMessage.getData();
            if (NeuraPushCommandFactory.getInstance().isNeuraEvent(data)) {
                NeuraEvent event = NeuraPushCommandFactory.getInstance().getEvent(data);
                Log.i(getClass().getSimpleName(), "received Neura event - " + event.toString());
                //Toast.makeText(getApplicationContext(), "received Neura event - " + event.toString(), Toast.LENGTH_LONG);

                testsendNotification(event);
            }
        }*/

        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        if (remoteMessage != null && remoteMessage.getNotification() != null && remoteMessage.getNotification().getBody() != null) {
            Log.d(TAG, "From: " + remoteMessage.getFrom());
            Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());
           // if (App.getUser() != null)
                //sendNotification(remoteMessage.getNotification());
            Intent intent = new Intent(INTENT_FILTER);
            Bundle mBundle = new Bundle();
            mBundle.putString("Body" , remoteMessage.getNotification().getBody());
            intent.putExtras(mBundle);
            sendBroadcast(intent);
        }


    }


    /**
     * Create and show a simple notification containing the received FCM message.
     */
   /* private void sendNotification(RemoteMessage.Notification notification) {
        try {
            String activityOpen = null;
            String activityContext = null;
            Intent intent = null;
            if (notification.getClickAction() != null) {
                activityOpen = notification.getClickAction();
                activityContext = notification.getTag();
            }
            if (activityOpen != null && !activityOpen.trim().equals("")) {
                if (activityOpen.contains("http")) {
                    try {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(activityOpen));
                        intent.setPackage("com.android.chrome");
                    } catch (ActivityNotFoundException e) {
                        // Chrome is not installed
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(activityOpen));
                    }
                } else if (activityOpen.equals("Market Overview")) {
                    intent = new Intent(App.getContext(), MarketOverviewActivity.class);
                    intent.putExtra("CurrentPage", 4);
                } else {
                    Class clazz = Class.forName(activityOpen);
                    intent = new Intent(App.getContext(), clazz);
                    intent.putExtra("CurrentPage", activityContext);
                }
            } else {
                intent = new Intent(this, MarketOverviewActivity.class);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 *//* Request code *//*, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_cgr_logo)
                    .setContentTitle(notification.getTitle())
                    .setContentText(notification.getBody())
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            NotificationCompat.BigTextStyle notifStyle = new NotificationCompat.BigTextStyle();
            notifStyle.setBigContentTitle(notification.getTitle());
            notifStyle.bigText(notification.getBody());
            notifStyle.setSummaryText(notification.getBody());
            notificationBuilder.setStyle(notifStyle);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0 *//* ID of notification *//*, notificationBuilder.build());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
