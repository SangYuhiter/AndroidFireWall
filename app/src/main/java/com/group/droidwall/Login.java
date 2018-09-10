/*
 * Dialog displayed when the login the softrare
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
import android.widget.ImageView;
import android.widget.Toast;

import com.group.droidwall.Service.LoginService;
import com.group.droidwall.Service.UserInfoService;

import java.util.Map;

public class Login extends Activity implements View.OnClickListener {
    private EditText et_username;
    private EditText et_password;
    private CheckBox cb_save;
    private ImageView iv_loginimage;

    private static String path="/sdcard/myHead/";//sd路径

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);
        cb_save = findViewById(R.id.cb_save);
        iv_loginimage = findViewById(R.id.iv_loginimage);
        Button bt_login = findViewById(R.id.bt_login);
        Button bt_signup = findViewById(R.id.bt_signup);
        Button bt_fogetpass = findViewById(R.id.bt_forgetpass);

        bt_login.setOnClickListener(this);
        bt_signup.setOnClickListener(this);
        bt_fogetpass.setOnClickListener(this);
        Bitmap bt = BitmapFactory.decodeFile(path + "head.jpg");//从Sd中找头像，转换成Bitmap
        if(bt!=null){
            @SuppressWarnings("deprecation") Drawable drawable = new BitmapDrawable(bt);//转换成drawable
            iv_loginimage.setImageDrawable(drawable);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_login:
                login();
                break;
            case R.id.bt_signup:
                signup(this);
                break;
            case R.id.bt_forgetpass:
                fogetpass(this);
                break;
        }
    }

    private void login() {
        String username = et_username.getText().toString().trim();
        String password = et_password.getText().toString().trim();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            msg(this, "用户名或密码不能为空");
        } else {
            if (cb_save.isChecked()) {
                boolean result = LoginService.saveUserInfo(this, username, password);
                if (result) {
                    msg(this, "保存成功！");
                } else {
                    msg(this, "保存失败！");
                }
            }
            if (LoginService.checkUserInfo(this, username, password)) {
                msg(this, "登陆成功！");
                Intent intent = new Intent();
                intent.setClass(Login.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                msg(this, "用户名或密码输入出错！");
            }
        }
    }

    private void signup(Context context) {
        String username = et_username.getText().toString().trim();
        String password = et_password.getText().toString().trim();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            msg(this, "用户名或密码不能为空");
        } else {
            Map<String,String> map = LoginService.getSaveInfo(context);
            if(map==null){
                boolean result = LoginService.saveUserInfo(this, username, password);
                if (result) {
                    msg(this, "注册成功！");
                    Intent intent = new Intent();
                    intent.setClass(Login.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }else {
                    msg(this, "注册失败！");
                }
            }else{
                msg(this,"不可重复注册");
            }
        }
    }

    private void fogetpass(Context context) {
        if(UserInfoService.deleteUserInfo(context)){
            msg(context,"原用户信息已注销！");
        }else{
            msg(context,"用户信息注销失败！");
        }
    }

    private void msg(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}