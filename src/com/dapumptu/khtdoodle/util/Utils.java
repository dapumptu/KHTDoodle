package com.dapumptu.khtdoodle.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class Utils {
    
    private static final String TAG = "Utils";
    private static final String SAVE_FOLDER_PATH = Environment.getExternalStorageDirectory() + "/test001/";
    private static final String DEFAULE_IMAGE_FORMAT = ".png";
    
    private Utils() {
        
    }
    
    // TODO: generate path for every image that has been created
    public static String getSavedImagePath() {
        // TODO: should I check this?
        if (!FileControlUtils.makeDirOfFullPath(SAVE_FOLDER_PATH)) {
            return null;
        }
        
        StringBuilder savedPath = new StringBuilder();
        savedPath.append(SAVE_FOLDER_PATH).append("test001")
                .append(DEFAULE_IMAGE_FORMAT);
        
        Log.d(TAG, "savedPath: " + savedPath);
        return savedPath.toString();
    }
    
    public static File createTempImageFile() throws IOException {
        // TODO: only create the temp image file once?
        
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageNamePrefix = "JPEG_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        String path = storageDir.getAbsolutePath() + "/KHTDoodle/";
        FileControlUtils.makeDirOfFullPath(path);
        File image = File.createTempFile(imageNamePrefix, ".jpg", new File(path));

        return image;
    }

}
