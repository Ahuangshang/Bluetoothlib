package com.baidu.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.baidu.tts.BTClient;
import com.baidu.tts.service.BluetoothManagerService;
import com.baidu.tts.ui.BluetoothDeviceListActivity;
import com.baidu.tts.util.RFIDReaderUtil;
import com.baidu.tts.util.SoundPlayUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rv;
    private ArrayList<UserCardModel> discernSuccessUserIdList = new ArrayList<>();
    private Timer timer;
    private TimerTask timerTask;
    private UserCardAdapter userCardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rv = findViewById(R.id.rv);
        //设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(layoutManager);

        //开启蓝牙数据读取功能
        startReadBlueCard();

        //设置适配器
        userCardAdapter = new UserCardAdapter(R.layout.item_user_id_list, discernSuccessUserIdList);
        rv.setAdapter(userCardAdapter);

        //设置分割线
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));


    }

    public void test(View view) {
        Intent intent = new Intent(this, BluetoothDeviceListActivity.class);
        startActivity(intent);
    }

    private void startReadBlueCard() {
        new Thread(new Runnable() {


            @Override
            public void run() {
                timer = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        read();
                    }
                };
                timer.schedule(timerTask, 0, 200);
            }
        }).start();
    }

    private void read() {
        ArrayList<String> rfidCardNumList = RFIDReaderUtil.getRFIDCardNum();
        if (rfidCardNumList == null) return;
        for (String anUid : rfidCardNumList) {
//             因为卡号被读取的频率是5次/秒（默认频率，可调），但不能一张卡的卡号被读取了五次就记录五次卡号，这样不符合逻辑。
//             正确的逻辑是：在当前页面的生命周期内，同一张卡的卡号只能被记录一次。
            boolean isHaveSameDiscernSuccessUserId = false;
            if (discernSuccessUserIdList.size() > 0) {
                for (int i = 0; i < discernSuccessUserIdList.size(); i++) {
                    if (anUid.equals(discernSuccessUserIdList.get(i).getUserId())) {
                        isHaveSameDiscernSuccessUserId = true;
                    }
                }
            }
            if (!isHaveSameDiscernSuccessUserId) {
                UserCardModel userCardModel = new UserCardModel();
                userCardModel.setUserId(anUid);
                discernSuccessUserIdList.add(userCardModel);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userCardAdapter.notifyDataSetChanged();
                        SoundPlayUtils.play(SoundPlayUtils.getSoundID());
                        //PromptVoiceUtil.playBee(IntoActivity.this);
                    }
                });
            }

        }
    }
}
