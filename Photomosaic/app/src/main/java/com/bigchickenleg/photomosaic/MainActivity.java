package com.bigchickenleg.photomosaic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bigchickenleg.photomosaic.adapter.ImageAdapter;
import com.bigchickenleg.imageselector.utils.ImageSelector;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 0x00000011;

    private boolean targetFlag = false;
    private boolean rvFlag = false;

    private ArrayList<String> targetPaths;
    private ArrayList<String> rvPaths;

    private RecyclerView rvImage;
    private ImageAdapter mAdapter;

    private RecyclerView targetImage;
    private ImageAdapter targetAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetImage = findViewById(R.id.target_image);
        targetImage.setLayoutManager(new GridLayoutManager(this, 1));
        targetAdapter = new ImageAdapter(this);
        targetImage.setAdapter(targetAdapter);

        rvImage = findViewById(R.id.rv_image);
        rvImage.setLayoutManager(new GridLayoutManager(this, 3));
        mAdapter = new ImageAdapter(this);
        rvImage.setAdapter(mAdapter);

        findViewById(R.id.btn_single).setOnClickListener(this);
        findViewById(R.id.btn_unlimited).setOnClickListener(this);
        findViewById(R.id.btn_generate).setOnClickListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("debug","MainActivity.onActivityResult()");

        if (requestCode == REQUEST_CODE && data != null) {
            ArrayList<String> images = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
            boolean isCameraImage = data.getBooleanExtra(ImageSelector.IS_CAMERA_IMAGE, false);
//            Log.d("ImageSelector", "是否是拍照图片：" + isCameraImage);
            if(targetFlag){
                targetAdapter.refresh(images);
                targetFlag = false;
                targetPaths = images;
            }else if(rvFlag) {
                mAdapter.refresh(images);
                targetFlag = false;
                rvPaths = images;
            }
        }
        Log.i("debug","MainActivity.onActivityResult()1");

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_single:
                //单选
//                ImageSelectorUtils.openPhoto(MainActivity.this, REQUEST_CODE, true, 0);
                targetFlag = true;
                ImageSelector.builder()
                        .useCamera(true) // 设置是否使用拍照
                        .setSingle(true)  //设置是否单选
                        .setViewImage(true) //是否点击放大图片查看,，默认为true
                        .start(this, REQUEST_CODE); // 打开相册
                break;


            case R.id.btn_unlimited:
                //多选(不限数量)
//                ImageSelectorUtils.openPhoto(MainActivity.this, REQUEST_CODE);
//                ImageSelectorUtils.openPhoto(MainActivity.this, REQUEST_CODE, mAdapter.getImages()); // 把已选的传入。
                //或者
//                ImageSelectorUtils.openPhoto(MainActivity.this, REQUEST_CODE, false, 0);
//                ImageSelectorUtils.openPhoto(MainActivity.this, REQUEST_CODE, false, 0, mAdapter.getImages()); // 把已选的传入。
                rvFlag=true;
                ImageSelector.builder()
                        .useCamera(true) // 设置是否使用拍照
                        .setSingle(false)  //设置是否单选
                        .setViewImage(true) //是否点击放大图片查看,，默认为true
                        .setMaxSelectCount(0) // 图片的最大选择数量，小于等于0时，不限数量。
                        .start(this, REQUEST_CODE); // 打开相册
                break;

            case R.id.btn_generate:
                generatePhotomosaic();
                break;

        }
    }


    private void generatePhotomosaic(){

        if(targetPaths==null||targetPaths.size()==0||rvPaths==null||rvPaths.size()==0){
            Toast.makeText(getApplicationContext(),"Please select the images",Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
        intent.putStringArrayListExtra("target",targetPaths);
        intent.putStringArrayListExtra("tiled",rvPaths);

//        Log.i("generate",targetPaths.get(0));
//        Log.i("generate",rvPaths.get(0));

        startActivity(intent);
    }

}
