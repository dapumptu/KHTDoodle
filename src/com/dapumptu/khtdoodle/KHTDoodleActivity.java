
package com.dapumptu.khtdoodle;

import java.io.File;
import java.io.IOException;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.dapumptu.khtdoodle.util.FileControlUtils;
import com.dapumptu.khtdoodle.util.KHTImageUtils;
import com.dapumptu.khtdoodle.util.Utils;

public class KHTDoodleActivity extends Activity implements
        ColorPickerDialog.OnColorChangedListener, KHTSlowDoodleView.TouchEventListener {

    private static final String TAG = "KHTDoodleActivity";
    
    private static final int GALLERY_REQUEST = 100;
    private static final int CAMERA_REQUEST = 1001;
    
    private static final int SHOW_DIALOG = 1;
    private static final int HIDE_DIALOG = 1 << 1;
    private static final int START_CAMERA_ACTIVITY = 1 << 2;
    
    private File mSavedFile;
    private ShareActionProvider mShareActionProvider;
    private KHTSlowDoodleView mDoodleView;
    private ProgressDialog mDialog;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_DIALOG:
                    mDialog = ProgressDialog.show(KHTDoodleActivity.this, null, "Please wait...");
                    break;
                case HIDE_DIALOG:
                    mDialog.dismiss();
                    break;
                case START_CAMERA_ACTIVITY:
                    final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Must specified MediaStore.EXTRA_OUTPUT
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mSavedFile));
                    startActivityForResult(intent, CAMERA_REQUEST);
                    break;
                default:
                    break;
            }
        }
    };
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        
        mDoodleView = new KHTSlowDoodleView(this);
        mDoodleView.setTouchListener(this);
        setContentView(mDoodleView);

        // setContentView(R.layout.activity_khtdoodle);
        // mDoodleView = (KHTSlowDoodleView)
        // findViewById(R.id.kHTSlowDoodleView1);
        // mDoodleView.setTouchListener(this);
        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //Debug.stopMethodTracing();
        
        Log.d(TAG, "onActivityResult: " + requestCode + ' ' + resultCode);

        // TODO: refactor image loading code into a common method
        if (requestCode == GALLERY_REQUEST) {
            
            if (resultCode == RESULT_OK) {
                Uri imageUri = intent.getData();

                Log.d(TAG, "Loading image from Gallery...Starting");
                mDoodleView.setBackgroundBitmap(loadImageFromPath(getPath(imageUri), false));
                Log.d(TAG, "Loading image from Gallery...done");
            } else if (resultCode == RESULT_CANCELED) {
                // TODO: handle the cancel result
            } else {
                // TODO: handle other result
            }
           
        } else if (requestCode == CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {

                if (intent != null) {
                    Uri imageUri = intent.getData();
                    Log.d(TAG, "imageUri: " + imageUri.toString());
                    Log.d(TAG, "Loading image from Camera...Starting");
                    mDoodleView.setBackgroundBitmap(loadImageFromPath(getPath(imageUri), true));
                    Log.d(TAG, "Loading image from Camera...done");
                } else {
                    Log.d(TAG, "intent: NULL");
                }
                
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Image capture canceled", Toast.LENGTH_LONG)
                .show();
            } else {
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_LONG)
                .show();
            }
        } else {
//            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
            Log.d(TAG, "WTF");
        }


    }
    
    // The default behavior of pressing the Back button 
    // will call Activity's finish().
    // We want to override this behavior to make it act like 
    // pressing the Home button.
    //
    // Ref:
    // http://stackoverflow.com/questions/2000102/android-override-back-button-to-act-like-home-button
    //
    @Override
    public void onBackPressed() {
        // Programmatically return to the home screen
        //
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);

        MenuItem item = menu.findItem(R.id.item_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        
        if (mShareActionProvider != null) {
            Intent shareIntent = getShareIntent();
            mShareActionProvider.setShareIntent(shareIntent);

            // Callback for handling stuff before starting the sharing Activity
            ShareActionProvider.OnShareTargetSelectedListener listener = new ShareActionProvider.OnShareTargetSelectedListener() {
                public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                    saveImage();
                    Log.d(TAG, "onShareTargetSelected: ");
                    return false;
                }
            };
            
            mShareActionProvider.setOnShareTargetSelectedListener(listener);
        }
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mDoodleView.reset();

        switch (item.getItemId()) {
            case R.id.item_color:
                new ColorPickerDialog(this, this, mDoodleView.getColor()).show();
                return true;
            case R.id.item_save:
                saveImage();
                return true;
            case R.id.item_gallery:
                chooseImageFromGallery();
                return true;
            case R.id.item_camera:
                captureImage();
                return true;
            case R.id.item_brush:
                mDoodleView.brush();
                item.setTitle(mDoodleView.isUseBrush() ? "Pen" : "Brush");
                return true;
            case R.id.item_clear:
                mDoodleView.clear();
                return true;
            case R.id.item_eraser:
                mDoodleView.erase();
                return true;
            case R.id.item_remove_bg:
                mDoodleView.enableBackground();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /* (non-Javadoc)
     * @see com.dapumptu.khtdoodle.ColorPickerDialog.OnColorChangedListener#colorChanged(int)
     */
    @Override
    public void colorChanged(int color) {
        mDoodleView.setColor(color);
    }
    
    /* (non-Javadoc)
     * @see com.dapumptu.khtdoodle.KHTSlowDoodleView.TouchEventListener#onDoubleTap()
     */
    @Override
    public void onDoubleTap() {
//        final ActionBar bar = getActionBar();
//        if (bar.isShowing())
//            bar.hide();
//        else
//            bar.show();
    }
    
    private void saveImage() {
        String path = Utils.getSavedImagePath();
        if (path != null) {
            FileControlUtils.saveBitmapImage(getApplicationContext(), 
                    mDoodleView.getBitmap(), path);
        }
    }

    // TODO: refactor this
    private String getPath(Uri uri) {
        // URI is a file path
        if (uri.getScheme().compareTo("file") == 0) {
            Log.d(TAG, "getPath: " + uri.getPath());
            return uri.getPath();

        } else if (uri.getScheme().compareTo("content") == 0) {
            // URI is a content URI

            String[] projection = {
                MediaStore.Images.Media.DATA
            };
            // Cursor cursor = managedQuery(uri, projection, null, null, null);
            Cursor cursor = getContentResolver().query(uri, projection, null, null,
                    "date_added ASC");
            if (cursor != null) {
                // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
                // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE
                // MEDIA
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                Log.d(TAG, "getPath: " + cursor.getString(column_index));
                return cursor.getString(column_index);
            }
        }
        
        return null;
        
//        if(cursor != null && cursor.moveToLast()){
//            Uri fileURI = Uri.parse(cursor.getString(cursor.getColumnIndex(Media.DATA)));
//            String fileSrc = fileURI.toString();
//            cursor.close();
//        }
    }
    
    private void chooseImageFromGallery() {
        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType("image/*");
        
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent = Intent.createChooser(intent, "Select an image from gallery");
        startActivityForResult(intent, GALLERY_REQUEST);
    }
    
    private void captureImage() {
        // start tracing to "/sdcard/calc.trace"
        //Debug.startMethodTracing("captureImage-01");

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            mHandler.sendEmptyMessage(SHOW_DIALOG);
            
            // Create the File where the photo should go
            File photoFile = null;
            
            try {
                photoFile = Utils.createTempImageFile();
                Log.d(TAG, "photoFile: " + photoFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
            if (photoFile != null) {
                mSavedFile = photoFile;
                // TODO: better handling of the progress dialog
                mHandler.sendEmptyMessageDelayed(HIDE_DIALOG, 800);
                mHandler.sendEmptyMessageDelayed(START_CAMERA_ACTIVITY, 1000);
            }
        }
    }
    
    private Intent getShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");

        // Specify the location of the image file for sharing
        String path = Utils.getSavedImagePath();
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        return intent;
    }
    
    private Bitmap loadImageFromPath(String path, boolean fromCamera) {
        Point srcSize = KHTImageUtils.getBitmapSizeFromFile(path);
        int srcWidth = srcSize.x;
        int srcHeight = srcSize.y;
        int screenWidth = mDoodleView.getWidth();
        int screenHeight = mDoodleView.getHeight();
        Point dstSize = KHTImageUtils.calculateAspectFitSize(srcWidth, srcHeight, screenWidth,
                screenHeight);
        int rotationInDegrees = 90;
        
        if (!fromCamera) {
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            rotationInDegrees = KHTImageUtils.exifToDegrees(rotation);

            Log.d(TAG, "DoodleView: " + mDoodleView.getWidth() + " " + mDoodleView.getHeight());

            srcWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, mDoodleView.getWidth());
            srcHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,
                    mDoodleView.getHeight());

            // if (rotationInDegrees == 0 || rotationInDegrees == 180)
            dstSize = KHTImageUtils.calculateAspectFitSize(srcWidth, srcHeight, screenWidth,
                    screenHeight);
            // else
            // dstSize = KHTImageUtil.calculateAspectFitSize(srcWidth,
            // srcHeight, screenHeight,
            // screenWidth);
        }
            
        Bitmap resultBitmap = KHTImageUtils.decodeSampledBitmapFromFile(path, dstSize.x, dstSize.y);
        resultBitmap = KHTImageUtils.rotateBitmap(resultBitmap, rotationInDegrees, true);
        return resultBitmap;
    }

}
