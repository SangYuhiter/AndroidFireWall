/*
 * Service for UserInfo
 */
package com.group.droidwall.Service;

import android.content.Context;
import android.content.Intent;

import com.group.droidwall.PhotoAlbum;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

public class UserInfoService {
    public static boolean deleteUserInfo(Context context){
        File file = new File(context.getFilesDir(),"info.txt");
        if(file.exists()&&file.isFile()){
            file.delete();
            return true;
        }
        return false;
    }

    public static boolean changeUserName(Context context,String newname){
        Map<String,String> map = LoginService.getSaveInfo(context);
        File file = new File(context.getFilesDir(),"info.txt");
        try{
            FileOutputStream fos = new FileOutputStream(file);
            if (map != null) {
                fos.write((newname+"###"+map.get("password")).getBytes());
            }
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean changePassword(Context context,String newpassword){
        Map<String,String> map = LoginService.getSaveInfo(context);
        File file = new File(context.getFilesDir(),"info.txt");
        try{
            FileOutputStream fos = new FileOutputStream(file);
            if (map != null) {
                fos.write((map.get("username")+"###"+newpassword).getBytes());
            }
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
