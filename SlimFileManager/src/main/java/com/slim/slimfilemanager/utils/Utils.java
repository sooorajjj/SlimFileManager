package com.slim.slimfilemanager.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.slim.slimfilemanager.R;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;

public class Utils {

    public static final long ONE_KB = 1024;
    public static final BigInteger ONE_KB_BI = BigInteger.valueOf(ONE_KB);
    public static final BigInteger ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI);
    public static final BigInteger ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI);
    public static final BigInteger ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI);
    public static final BigInteger ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI);
    public static final BigInteger ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI);

    public static ArrayList<String> listFiles(String path) {
        ArrayList<String> mDirContent = new ArrayList<>();

        if (!mDirContent.isEmpty())
            mDirContent.clear();

        final File file = new File(path);

        if (file.exists() && file.canRead()) {
            String[] list = file.list();

            if (list == null) return null;

            for (String aList : list) {
                mDirContent.add(path + "/" + aList);
            }
        } else {
            mDirContent = RootUtils.listFiles(file.getAbsolutePath(), true);
        }

        return mDirContent;
    }

    public static void onClickFile(Context context, String fileString) {
        File file = new File(fileString);
        final String mime = getMimeType(file);
        final Intent i = new Intent(Intent.ACTION_VIEW);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (mime != null) {
            i.setDataAndType(Uri.fromFile(file), mime);
        } else {
            i.setDataAndType(Uri.fromFile(file), "*/*");
        }

        if (context.getPackageManager().queryIntentActivities(i, 0).isEmpty()) {
            Toast.makeText(context, R.string.cant_open_file, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            context.startActivity(i);
        } catch (Exception e) {
            //Toast.makeText(context,
            //      context.getString(R.string.cantopenfile) + e.getMessage(),
            //    Toast.LENGTH_SHORT).show();
        }
    }

    public static String getMimeType(File file) {
        if (file.isDirectory()) {
            return null;
        }
        return MimeUtils.getMimeType(file.getName());
    }

    public static String displaySize(long bytes) {
        String displaySize;

        BigInteger size = BigInteger.valueOf(bytes);

        if (size.divide(ONE_EB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(ONE_EB_BI)) + " EB";
        } else if (size.divide(ONE_PB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(ONE_PB_BI)) + " PB";
        } else if (size.divide(ONE_TB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(ONE_TB_BI)) + " TB";
        } else if (size.divide(ONE_GB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(ONE_GB_BI)) + " GB";
        } else if (size.divide(ONE_MB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(ONE_MB_BI)) + " MB";
        } else if (size.divide(ONE_KB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(ONE_KB_BI)) + " KB";
        } else {
            displaySize = String.valueOf(size) + " bytes";
        }
        return displaySize;
    }

    public static int darkenColor(int color) {
        int r = Color.red(color);
        int b = Color.blue(color);
        int g = Color.green(color);

        return Color.rgb((int) (r * .9), (int) (g * .9), (int) (b * .9));
    }

    public static String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }
        if (red.length() == 1) {
            red = "0" + red;
        }
        if (green.length() == 1) {
            green = "0" + green;
        }
        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + alpha + red + blue + green;
    }

    public static String getCacheDir() {
        File cache = new File(Environment.getExternalStorageDirectory() + "/.file_manager_cache");
        if (!cache.exists()) {
            if (!cache.mkdirs()) return null;
        }
        return cache.getAbsolutePath();
    }
}
