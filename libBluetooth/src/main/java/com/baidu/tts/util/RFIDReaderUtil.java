package com.baidu.tts.util;

import android.text.TextUtils;
import android.util.Log;

import com.baidu.tts.BTClient;
import com.baidu.tts.service.BluetoothManagerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RFIDReaderUtil {
    private static ArrayList<String> RFIDCardNumList = new ArrayList<>();
    private static BluetoothManagerService bluetoothManagerService;
    private static ExecutorService executorService;
    public static int READ_CARD_INSTANCE_TYPE_LOW = 1;//0.1m
    public static int READ_CARD_INSTANCE_TYPE_MIDDLE = 2;//0.3m
    public static int READ_CARD_INSTANCE_TYPE_HIGH = 3;//0.5m
    public static int READ_CARD_INSTANCE_TYPE_VERY_HIGH = 4;//1m


    public static void init() {
        bluetoothManagerService = new BluetoothManagerService();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static void connectRFIDReader(String bluetoothDeviceMac) {
        bluetoothManagerService.ConnectBT(bluetoothDeviceMac);
    }

    public static ArrayList<String> getRFIDCardNum() {
        String[] cardNumList = readRFIDCardNum();
        if (cardNumList == null) return null;
        RFIDCardNumList.clear();
        RFIDCardNumList.addAll(Arrays.asList(cardNumList));
        return RFIDCardNumList;
    }


    private static String[] readRFIDCardNum() {
        byte[] EPCList = new byte[5000];
        int[] CardNum = new int[2];
        int[] EPCLength = new int[2];
        int result = BTClient.Inventory_G2((byte) 4, (byte) 0, (byte) 0, (byte) 0, (byte) 0, CardNum, EPCList, EPCLength);
        if (((CardNum[0] & 255) > 0) && (result != 0x30)) {
            int Scan6CNum = CardNum[0] & 255;
            String[] lable = new String[Scan6CNum];
            StringBuffer bf;
            int j = 0, k;
            String str;
            byte[] epc;
            Log.i("zdy", "num = " + Scan6CNum + ">>>>>>" + "len = " + EPCLength[0]);
            for (int i = 0; i < Scan6CNum; i++) {
                bf = new StringBuffer("");
                Log.i("yl", "length = " + EPCList[j]);
                epc = new byte[EPCList[j] & 0xff];
                for (k = 0; k < (EPCList[j] & 0xff); k++) {
                    str = Integer.toHexString(EPCList[j + k + 1] & 0xff);
                    if (str.length() == 1) {
                        bf.append("0");
                    }
                    bf.append(str);
                    epc[k] = EPCList[j + k + 1];
                }

                String res = bf.toString().toUpperCase();
                if (!TextUtils.isEmpty(res) && res.length() >= 16) {
                    res = res.substring(0, 16);
                    String end = res.substring(12);
                    if (end.equals("0000")) {
                        res = res.substring(0, 12);
                    }
                }
                lable[i] = res;
                //epcBytes.put(lable[i], epc);
                j = j + k + 2;
            }
            return lable;
        }
        return null;
    }


    public static void setReadCardInstance(int type) {
        if (type == READ_CARD_INSTANCE_TYPE_LOW) {
            setIdentifiableDistance(6);
        } else if (type == READ_CARD_INSTANCE_TYPE_MIDDLE) {
            setIdentifiableDistance(12);
        } else if (type == READ_CARD_INSTANCE_TYPE_HIGH) {
            setIdentifiableDistance(18);
        } else if (type == READ_CARD_INSTANCE_TYPE_VERY_HIGH) {
            setIdentifiableDistance(26);
        }
    }

    //设置RFID阅读器和射频卡之间的最大可识别距离
    private static void setIdentifiableDistance(final int i) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                BTClient.SetPower((byte) i);
            }
        });
    }
}
