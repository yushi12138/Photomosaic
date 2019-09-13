package com.bigchickenleg.photomosaic;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bigchickenleg.imageselector.utils.ImageSelector;
import com.bigchickenleg.photomosaic.utils.ImgUtils;
import com.bumptech.glide.Glide;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class ResultActivity extends AppCompatActivity {

    private final static double SCALOR = 0.35;

    private ArrayList<String> targetPaths;
    private ArrayList<String> tiledPaths;

    private Bitmap targetBitmap;

    private Mat targetMatrix ;
    private ArrayList<Mat> mats = new ArrayList<>();

    private int resizeHeight;
    private int resizeWidth;
    private int maxProgress=0;
    private int progress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        OpenCVLoader.initDebug();

        Intent intent = getIntent();

        targetPaths =  intent.getStringArrayListExtra("target");
        tiledPaths = intent.getStringArrayListExtra("tiled");

//        ImageView imageView = (ImageView) findViewById(R.id.imageView);
//        Glide.with(this).load(targetPaths.get(0)).into(imageView);

        //load target and tiled images
        try {
            //load the target image
            targetBitmap = loadImageFromUri(targetPaths.get(0));
            targetMatrix = new Mat();
            Utils.bitmapToMat(targetBitmap,targetMatrix);

            resizeHeight = Math.min(targetBitmap.getHeight(),targetBitmap.getWidth())/15;
            resizeWidth = resizeHeight;
            //add all the tiled downsampled images into arraylist
            for(int i=0;i<tiledPaths.size();i++){
                Mat temp = new Mat();
                Utils.bitmapToMat(getResizedBitmap(loadImageFromUri(tiledPaths.get(i)),resizeHeight,resizeWidth),temp);
                mats.add(temp);
            }

        }catch(IOException e){
            e.printStackTrace();
        }

        //add all the tiled images to the target images
        new Thread(new Runnable() {
            @Override
            public void run() {
                addAll(mats);
                Utils.matToBitmap(targetMatrix,targetBitmap);
                ImageView mosaic = (ImageView ) findViewById (R.id.imageView);
                mosaic.setImageBitmap(targetBitmap);
            }
        }
        ).start();

        //show the image in the imageshow
        Utils.matToBitmap(targetMatrix,targetBitmap);
        ImageView my_img_view = (ImageView ) findViewById (R.id.imageView);
        my_img_view.setImageBitmap(targetBitmap);
        //click button to save
        Button mButton = findViewById(R.id.btn_save);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //only wehn progress is done
                if(progress==maxProgress){
                    ImgUtils.saveImageToGallery(getApplicationContext(),targetBitmap);
                    Toast.makeText(getApplicationContext(),"Moasic saved",Toast.LENGTH_LONG).show();
                }
            }
        });

//            Date date = new Date();
//            String filename = "photomosaic"+ date.toString()+".jpg";
//            write(filename,result);

    }

    private void addAll(ArrayList<Mat> mats){

        Random rand = new Random();
        int heightNum =(int) Math.ceil(targetMatrix.height()*1.0/resizeHeight);
        int widthNum = (int) Math.ceil(targetMatrix.width()*1.0/resizeWidth);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        maxProgress = heightNum*widthNum;
        progressBar.setMax(maxProgress);
        for(int i=0;i<heightNum;i++){
            for(int j=0;j<widthNum;j++){
                //add each resized tiled image to target image
                int randomNum = rand.nextInt(mats.size());
                Log.i("photo",String.valueOf(randomNum));
                addImages(targetMatrix,mats.get(randomNum),i,j);
                progressBar.setProgress(progress++);
            }
        }

    }

    private void addImages(Mat targetMatrix,Mat tiledMatrix, int i, int j){
        int resizeHeight=tiledMatrix.height();
        int resizeWidth=tiledMatrix.width();


        for(int m=0;m<resizeHeight;m++){
            for(int n=0;n<resizeWidth;n++){
                int targetY = i*resizeHeight+m;
                int targetX = j*resizeWidth+n;
                //check whether index is out of the taretMatrix's boundary
                if(targetY>=targetMatrix.height()||targetX>=targetMatrix.width())
                    break;

                double[] targetPixel = targetMatrix.get(targetY,targetX);
                double[] tiledPixel = tiledMatrix.get(m,n);

                for(int k=0;k<3;k++){
                    targetPixel[k] = targetPixel[k]*(1-SCALOR)+tiledPixel[k]*SCALOR;
                }
                targetMatrix.put(targetY,targetX,targetPixel);
            }
        }
    }
    private Bitmap loadImageFromUri(String path)throws IOException{
        Uri targetUri = Uri.fromFile(new File(path));
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), targetUri);
        return bitmap;
    }


    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {

        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
// CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
// RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
// RECREATE THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);

        return resizedBitmap;
    }


    public void write(String fileName, Bitmap bitmap) {
        FileOutputStream outputStream;
        try {
            outputStream = getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
        } catch (Exception error) {
            error.printStackTrace();
        }
    }
}
