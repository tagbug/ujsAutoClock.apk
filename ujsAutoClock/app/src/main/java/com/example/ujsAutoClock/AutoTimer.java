package com.example.ujsAutoClock;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AutoTimer extends BroadcastReceiver {

    private final String file_name = "Settings.txt";
    // 通知相关
    private int id = 1111;
    private String channelId = "channelId1";//渠道id
    private NotificationManager mNotificationManager;

    private String loadSettings(Context context) throws IOException {
        FileInputStream fis = null;
        fis = context.openFileInput(file_name);
        byte[] buff = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int len = 0;
        while ((len = fis.read(buff)) > 0) {
            sb.append(new String(buff, 0, len));
        }
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    @SuppressLint("WrongConstant")
    private void Notification(Context context, String message) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)//小图标
                .setContentTitle("ujs自动健康打卡")
                .setContentText(message);
        mNotificationManager.notify(id, mBuilder.build());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            addNotificationChannel();
            String[] settings = loadSettings(context).split("\n");
            Daka daka=new Daka(settings[0],settings[1],context,mNotificationManager);
        } catch (IOException e) {
            Notification(context,"错误：设置文件不存在或读取失败！");
        }
    }

    public void addNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //创建通知渠道
            CharSequence name = "AutoTimer";
            String description = "自动健康打卡器";
            int importance = NotificationManager.IMPORTANCE_HIGH;//重要性级别
            NotificationChannel mChannel = new NotificationChannel(channelId, name, importance);

            mChannel.setDescription(description);//渠道描述
            mChannel.enableLights(true);//是否显示通知指示灯
            mChannel.enableVibration(true);//是否振动

            mNotificationManager.createNotificationChannel(mChannel);//创建通知渠道
        }
    }
}