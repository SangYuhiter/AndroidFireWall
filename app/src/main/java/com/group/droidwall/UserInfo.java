/*
 * Dialog displayed when the "Set UserInfo" menu option is selected
 */
package com.group.droidwall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.group.droidwall.Service.LoginService;
import com.group.droidwall.Service.UserInfoService;

import java.util.Map;

public class UserInfo extends Activity implements View.OnClickListener{
    private EditText et_oldusername;
    private EditText et_newusername;
    private EditText et_oldpassword;
    private EditText et_newpassword;

    private CheckBox cb_newusername;
    private CheckBox cb_newpassword;

    private ImageView iv_userimage;

    private static String path="/sdcard/myHead/";//sd路径

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userinfo);
        et_oldusername = findViewById(R.id.et_oldusername);
        et_oldpassword = findViewById(R.id.et_oldpassword);
        et_newusername = findViewById(R.id.et_newusername);
        et_newpassword = findViewById(R.id.et_newpassword);

        cb_newusername = findViewById(R.id.cb_newusername);
        cb_newpassword = findViewById(R.id.cb_newpassword);
        iv_userimage = findViewById(R.id.iv_userimage);

        Button bt_changeusername = findViewById(R.id.bt_changeusername);
        Button bt_changepassword = findViewById(R.id.bt_changepassword);
        Button bt_exituserinfo = findViewById(R.id.bt_exituserinfo);
        Button bt_changeuserimage = findViewById(R.id.bt_changeuserimage);

        bt_changeusername.setOnClickListener(this);
        bt_changepassword.setOnClickListener(this);
        bt_exituserinfo.setOnClickListener(this);
        bt_changeuserimage.setOnClickListener(this);
        Map<String,String> map = LoginService.getSaveInfo(this);
        if (map != null) {
            et_oldusername.setText(map.get("username"));
        }
        Bitmap bt = BitmapFactory.decodeFile(path + "head.jpg");//从Sd中找头像，转换成Bitmap
        if(bt!=null) {
            @SuppressWarnings("deprecation") Drawable drawable = new BitmapDrawable(bt);//转换成drawable
            iv_userimage.setImageDrawable(drawable);
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.bt_changeusername:
                if(cb_newusername.isChecked()){
                    String username = et_oldusername.getText().toString().trim();
                    String password = et_oldpassword.getText().toString().trim();
                    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                        msg(this, "用户名或密码不能为空");
                    }else{
                        if(LoginService.checkUserInfo(this,username,password)){
                            String newusername = et_newusername.getText().toString().trim();
                            if(UserInfoService.changeUserName(this,newusername)){
                                msg(this,"username changed!");
                            }else{
                                msg(this,"change username error!");
                            }
                        }else{
                            msg(this, "用户名或密码输入出错！");
                        }
                    }
                }
                break;
            case R.id.bt_changepassword:
                if(cb_newpassword.isChecked()){
                    String username = et_oldusername.getText().toString().trim();
                    String password = et_oldpassword.getText().toString().trim();
                    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                        msg(this, "用户名或密码不能为空");
                    }else{
                        if(LoginService.checkUserInfo(this,username,password)){
                            String newpassword = et_newpassword.getText().toString().trim();
                            if(UserInfoService.changePassword(this,newpassword)){
                                msg(this,"password changed!");
                            }else{
                                msg(this,"change password error!");
                            }
                        }else{
                            msg(this, "用户名或密码输入出错！");
                        }
                    }
                }
                break;
            case R.id.bt_exituserinfo:
                Intent intent_exit = new Intent();
                intent_exit.setClass(UserInfo.this, MainActivity.class);
                startActivity(intent_exit);
                finish();
                break;
            case R.id.bt_changeuserimage:
                Intent intent_changeuserimage = new Intent();
                intent_changeuserimage.setClass(UserInfo.this, PhotoAlbum.class);
                startActivity(intent_changeuserimage);
                finish();
            default:
                break;
        }
    }
    private void msg(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
