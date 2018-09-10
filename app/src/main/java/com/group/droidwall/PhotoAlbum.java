package com.group.droidwall;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PhotoAlbum extends Activity implements View.OnClickListener{
    private ImageView ivHead;//头像显示
    private Bitmap head;//头像Bitmap
    private static String path="/sdcard/myHead/";//sd路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photoalbum);
        initView();
    }

    private void initView() {
        //初始化控件
        Button bt_Photos = (Button) findViewById(R.id.bt_photos);
        Button bt_Takephoto = (Button) findViewById(R.id.bt_takephoto);
        Button bt_Exitphotoalbum = (Button)findViewById(R.id.bt_exitphotoalbum);
        bt_Photos.setOnClickListener(this);
        bt_Takephoto.setOnClickListener(this);
        bt_Exitphotoalbum.setOnClickListener(this);
        ivHead = (ImageView) findViewById(R.id.iv_head);
        Bitmap bt = BitmapFactory.decodeFile(path + "head.jpg");//从Sd中找头像，转换成Bitmap
        if(bt!=null){
            @SuppressWarnings("deprecation") Drawable drawable = new BitmapDrawable(bt);//转换成drawable
            ivHead.setImageDrawable(drawable);
        }else{
            Bitmap defaulthead = BitmapFactory.decodeResource(getResources(),R.drawable.defaulthead);
            @SuppressWarnings("deprecation") Drawable drawable = new BitmapDrawable(defaulthead);//转换成drawable
            ivHead.setImageDrawable(drawable);
        }



    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_photos://从相册里面取照片
                Intent intent1 = new Intent(Intent.ACTION_PICK, null);
                intent1.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent1, 1);
                break;
            case R.id.bt_takephoto://调用相机拍照
                Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent2.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                        "head.jpg")));
                startActivityForResult(intent2, 2);//采用ForResult打开
                break;
            case R.id.bt_exitphotoalbum:
                Intent intent_changeuserimage = new Intent();
                intent_changeuserimage.setClass(PhotoAlbum.this, UserInfo.class);
                startActivity(intent_changeuserimage);
                finish();
            default:
                break;
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    cropPhoto(data.getData());//裁剪图片
                }

                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    File temp = new File(Environment.getExternalStorageDirectory()
                            + "/head.jpg");
                    cropPhoto(Uri.fromFile(temp));//裁剪图片
                }

                break;
            case 3:
                if (data != null) {
                    Bundle extras = data.getExtras();
                    head = extras.getParcelable("data");
                    if(head!=null){
                        /**
                         * 上传服务器代码
                         */
                        setPicToView(head);//保存在SD卡中
                        ivHead.setImageBitmap(head);//用ImageView显示出来
                    }
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    };
    /**
     * 调用系统的裁剪
     * @param uri
     */
    public void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, 3);
    }

    private void setPicToView(Bitmap mBitmap) {
        String sdStatus = Environment.getExternalStorageState();
        if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用
            return;
        }
        FileOutputStream b = null;
        File file = new File(path);
        file.mkdirs();// 创建文件夹
        String fileName =path + "head.jpg";//图片名字
        try {
            b = new FileOutputStream(fileName);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);// 把数据写入文件

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭流
                b.flush();
                b.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
