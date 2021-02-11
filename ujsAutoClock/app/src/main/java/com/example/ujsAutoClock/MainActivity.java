package com.example.ujsAutoClock;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.ujsAutoClock.Utils.MobileInfoUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    // 通知相关
    private int id = 1111;
    private String channelId = "channelId1";//渠道id
    private NotificationManager mNotificationManager;

    private final EditText[] inputs = new EditText[2];
    private Button button_test, button_save, button_selectTime, button_cancle;
    private final String file_name = "Settings.txt";
    private String[] settings;
    private String setting_time;
    private TextView text_log;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MobileInfoUtils miu = new MobileInfoUtils();
        switch (item.getItemId()) {
            case R.id.menu_DetailInterface:
                miu.jumpDetailInterface(MainActivity.this);
                return true;
            case R.id.menu_StartInterface:
                miu.jumpStartInterface(MainActivity.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputs[0] = findViewById(R.id.input_username);
        inputs[1] = findViewById(R.id.input_password);
        button_selectTime = findViewById(R.id.button_selectTime);
        button_test = findViewById(R.id.button_test);
        button_save = findViewById(R.id.button_save);
        button_cancle = findViewById(R.id.button_cancle);
        text_log = findViewById(R.id.text_log);
        text_log.setMovementMethod(ScrollingMovementMethod.getInstance());
        try {
            settings = loadSettings().split("\n");
            for (int i = 0; i < settings.length; i++) {
                if (settings[i].equals("null")) {
                    settings[i] = null;
                }
                if (settings[i] != null) {
                    if (i == 2) {
                        button_selectTime.setText(setting_time = settings[2]);
                    } else {
                        inputs[i].setText(settings[i]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log("->提示：为了保证定时任务效果，建议给予自启动权限，点击右上角有三个点导航到设置页");
        Log("->提示：通知推送效果可以手动调节，可以手动测试");
        Log("->提示：一天请不要测试太多次，否则OCR服务器可能会拒绝你的访问，第二天恢复");
        Log("");
        if (setting_time != null) {
            button_cancle.setVisibility(View.VISIBLE);
            Log("定时任务已设置！");
            int hour = Integer.parseInt(setting_time.split(":")[0]);
            int minute = Integer.parseInt(setting_time.split(":")[1]);
            Date date = new Date();
            if (date.getHours() * 60 + date.getMinutes() >= hour * 60 + minute) {
                date.setDate(date.getDate() + 1);
            }
            date.setHours(hour);
            date.setMinutes(minute);
            date.setSeconds(0);
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Log("下次运行时间：" + ft.format(date));
        }
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        addNotificationChannel();
        button_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Daka daka = new Daka(inputs[0].getText().toString(), inputs[1].getText().toString(), getApplicationContext(), text_log, mNotificationManager);
            }
        });
        button_save.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if (saveSettings(1)) {
                    int hour = Integer.parseInt(setting_time.split(":")[0]);
                    int minute = Integer.parseInt(setting_time.split(":")[1]);
                    alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(MainActivity.this, AutoTimer.class);
                    intent.setPackage(MainActivity.this.getPackageName());
                    alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
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
                    button_cancle.setVisibility(View.VISIBLE);
                    //重启恢复
                    ComponentName receiver = new ComponentName(MainActivity.this, BootReceiver.class);
                    PackageManager pm = getPackageManager();
                    pm.setComponentEnabledSetting(receiver,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    Log("定时任务已设置！");
                    Log("i: 重启监听已开启，将自动恢复定时任务");
                    Log("i: 强烈建议给予APP自启动权限，否则在部分系统上可能会失效");
                    java.util.Date date = calendar.getTime();
                    Log(String.format("下次运行时间：%tF %tT%n", date, date));
                }
            }
        });
        button_selectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        setting_time = String.format("%02d:%02d", hourOfDay, minute);
                        button_selectTime.setText(setting_time);
                    }
                }, setting_time == null ? 0 : Integer.parseInt(setting_time.split(":")[0]), setting_time == null ? 0 : Integer.parseInt(setting_time.split(":")[1]), true).show();
            }
        });
        button_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(MainActivity.this, AutoTimer.class);
                alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
                alarmMgr.cancel(alarmIntent);
                setting_time = null;
                button_selectTime.setText("未设置");
                button_cancle.setVisibility(View.INVISIBLE);
                //取消重启监听
                ComponentName receiver = new ComponentName(MainActivity.this, BootReceiver.class);
                PackageManager pm = getPackageManager();
                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                saveSettings(0);
                Log("i: 重启监听已关闭");
                Toast.makeText(MainActivity.this, "取消成功！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void Log(String message) {
        try {
            text_log.setText(text_log.getText().toString() + message + "\n");
            int scrollAmount = text_log.getLayout().getLineTop(text_log.getLineCount()) - text_log.getHeight();
            if (scrollAmount > 0) {
                text_log.scrollTo(0, scrollAmount);
            } else {
                text_log.scrollTo(0, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean saveSettings(int mode) {
        StringBuilder sb = new StringBuilder();
        String text;
        for (int i = 0; i < 3; i++) {
            if (i == 2) {
                text = setting_time;
            } else {
                text = inputs[i].getText().toString();
            }
            if (mode == 1) {
                if (text == null || text.isEmpty()) {
                    Toast.makeText(MainActivity.this, "只有设置好了才能保存...", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            sb.append(text);
            sb.append("\n");
        }
        String settings = sb.toString();
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(file_name, MODE_PRIVATE);
            fos.write(settings.getBytes());
            Toast.makeText(MainActivity.this, "保存成功！", Toast.LENGTH_SHORT).show();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "I/O写入异常！请确保内置存储未满！", Toast.LENGTH_LONG).show();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
            }
            return false;
        }
    }

    private String loadSettings() {
        FileInputStream fis = null;
        try {
            fis = openFileInput(file_name);
            byte[] buff = new byte[1024];
            StringBuilder sb = new StringBuilder();
            int len = 0;
            while ((len = fis.read(buff)) > 0) {
                sb.append(new String(buff, 0, len));
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "I/O读取异常！", Toast.LENGTH_LONG).show();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
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