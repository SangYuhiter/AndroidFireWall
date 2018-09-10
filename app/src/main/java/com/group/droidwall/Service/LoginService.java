/*
 * Service for Login
 */
package com.group.droidwall.Service;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LoginService {
    public static boolean saveUserInfo(Context context,String username, String password){
        File file = new File(context.getFilesDir(),"info.txt");
        try{
            FileOutputStream fos = new FileOutputStream(file);
            fos.write((username+"###"+password).getBytes());
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean checkUserInfo(Context context, String username, String password){
        Map<String,String> map = LoginService.getSaveInfo(context);
        if(map != null){
            if(map.get("username").equals(username)&&map.get("password").equals(password)){
                return true;
            }
        }
        return false;
    }

    public static Map<String,String> getSaveInfo(Context context){
        File file = new File(context.getFilesDir(),"info.txt");
        try{
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String str = br.readLine();
            String[] infos = str.split("###");
            Map<String,String> map = new HashMap<>();
            map.put("username",infos[0]);
            map.put("password",infos[1]);
            fis.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
