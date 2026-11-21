package com.officeautomationsystemapi.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 简易农历转换，只覆盖 2000 到 2029 年
public class LunarUtil {

    // 每年农历数据的位编码，低位存闰月月份，0x10000 位表示闰月是否 30 天
    private static final int[] LUNAR_INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x0b250, 0x06a50, 0x06d40, 0x1af46, 0x0ab20, 0x09b50, 0x10955
    };

    private static int leapMonth(int year) {
        return LUNAR_INFO[year - 2000] & 0xf;
    }

    private static boolean leapMonthDays(int year) {
        return (LUNAR_INFO[year - 2000] & 0x10000) != 0;
    }

    private static int monthDays(int year, int month) {
        return (LUNAR_INFO[year - 2000] & (0x10000 >> month)) != 0 ? 30 : 29;
    }

    private static int yearDays(int year) {
        int sum = 348;
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            sum += (LUNAR_INFO[year - 2000] & i) != 0 ? 1 : 0;
        }
        return sum + (leapMonthDays(year) ? 30 : 29);
    }

    // 公历转农历，返回农历月日，闰月按本月算
    public static int[] toLunar(LocalDate solar) {
        int year = solar.getYear();
        if (year < 2000 || year > 2029) {
            return null;
        }
        int offset = (int) ChronoUnit.DAYS.between(LocalDate.of(year, 1, 1), solar);

        int leap = leapMonth(year);
        boolean isLeap = false;
        int lm = 1;

        while (lm < 13) {
            int days;
            if (leap > 0 && lm == leap + 1 && !isLeap) {
                days = leapMonthDays(year) ? 30 : 29;
                isLeap = true;
                lm--;
            } else {
                days = monthDays(year, lm);
            }
            if (offset < days) {
                break;
            }
            offset -= days;
            if (isLeap && lm == leap + 1) {
                isLeap = false;
            }
            lm++;
        }
        return new int[]{lm, offset + 1};
    }
}
