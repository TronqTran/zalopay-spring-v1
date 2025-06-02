package com.vn.zalopay.util;

import com.vn.zalopay.crypto.HMACUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class ZaloPayUtil {
    public static String getCurrentTimeString(String format) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+7"));
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setCalendar(cal);
        return fmt.format(cal.getTimeInMillis());
    }
    public static String generateMac(String key1, String appid, String reqtime) throws Exception {
        String data = appid + "|" + reqtime;
        return HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, key1, data);
    }
}
