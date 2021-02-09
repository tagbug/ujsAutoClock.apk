package com.example.ujsAutoClock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {

    private final String file_name = "Settings.txt";
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    // 通知相关
    private int id = 1111;
    private String channelId = "channelId1";//渠道id
    private NotificationManager mNotificationManager;

    @SuppressLint("WrongConstant")
    private void Notification(Context context, String message, int mode) {
        NotificationCompat.Builder mBuilder;
        if (mode == 1) {
            String[] msg=message.split("\n");
            mBuilder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)//小图标
                    .setContentTitle("ujs自动健康打卡")
                    .setContentText(msg[0])
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg[1]));
        } else {
            mBuilder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)//小图标
                    .setContentTitle("ujs自动健康打卡")
                    .setContentText(message);
        }
        mNotificationManager.notify(id, mBuilder.build());
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            try {
                mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                addNotificationChannel();
                String[] settings = loadSettings(context).split("\n");
                String setting_time = settings[2];
                int hour = Integer.parseInt(setting_time.split(":")[0]);
                int minute = Integer.parseInt(setting_time.split(":")[1]);
                alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent aintent = new Intent(context, AutoTimer.class);
                alarmIntent = PendingIntent.getBroadcast(context, 0, aintent, 0);
                alarmMgr.cancel(alarmIntent);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                if (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) >= hour * 60 + minute) {
                    calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
                }
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                java.util.Date date = calendar.getTime();
                Notification(context, String.format("恢复定时任务成功！\n下次运行时间：%tF %tT%n", date, date), 1);
            } catch (IOException e) {
                Notification(context, "错误：恢复定时任务时：设置文件不存在或读取失败！",0);
            } catch (Exception ee) {
                Notification(context, "错误：恢复定时任务失败！",0);
            }
        }
    }

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
}