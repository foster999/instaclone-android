package com.example.instaclone;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import jp.wasabeef.glide.transformations.gpu.InvertFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SepiaFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SketchFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.ToonFilterTransformation;

public class MainActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private ImageView imageView;
    private Bitmap image;
    private String lastFilter = "plain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image_view);
    }

    public void choosePhoto(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    /*
     * Apply Glide filter to loaded image.
     */
    public void apply(Transformation<Bitmap> filter) {
        if (image == null) {
            return;
        }
        Glide
                .with(this)
                .load(image)
                .apply(RequestOptions
                        .bitmapTransform(filter))
                .into(imageView);
    }

    public void applySepia(View view) {
        apply(new SepiaFilterTransformation());
        lastFilter = "sepia";
    }

    public void applyToon(View view) {
        apply(new ToonFilterTransformation());
        lastFilter = "toon";
    }

    public void applySketch(View view) {
        apply(new SketchFilterTransformation());
        lastFilter = "sketch";
    }

    public void applyInvert(View view) {
        apply(new InvertFilterTransformation());
        lastFilter = "invert";
    }

    public void saveImage(View view) {
        if (image == null) {
            return;
        }
        Bitmap image = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        String root = Environment.getExternalStorageDirectory().toString();
        File outDir = new File(root + "/filtered_images");
        outDir.mkdirs();
        int fileNumber = 0;
        File outFile = new File(outDir, String.format(
                Locale.getDefault(),
                "%d_",
                fileNumber) + lastFilter + ".jpg");
        while (outFile.exists()) {
            fileNumber++;
            outFile = new File(outDir, String.format(
                    Locale.getDefault(),
                    "%d_",
                    fileNumber) + lastFilter + ".jpg");
        }
        Log.d("Instaclone", "Outfile: " + outFile.getAbsolutePath());
        try {
            FileOutputStream out = new FileOutputStream(outFile);
            image.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, outFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // or image/png
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (IOException e) {
            Log.e("Instaclone", "Image writing error", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().
                        openFileDescriptor(uri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                imageView.setImageBitmap(image);
            } catch (IOException e) {
                Log.e("Instaclone", "Image not found", e);
            }
        }
    }
}