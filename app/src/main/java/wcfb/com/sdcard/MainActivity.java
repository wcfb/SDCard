package wcfb.com.sdcard;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    ListView lv_photos;

    ImageLoader imageLoader;
    int screenWidth = 0;
    ListViewAdapter<SelectPhotoAdapter.SelectPhotoEntity> photoListViewAdapter;
    ArrayList<SelectPhotoAdapter.SelectPhotoEntity> selectedPhotos;
    PermissionHelper permissionHelper = new PermissionHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View bt_add_photo = findViewById(R.id.bt_add_photo);
        lv_photos = findViewById(R.id.lv_photos);
        bt_add_photo.setOnClickListener(this);
        imageLoader = new ImageLoader(this);
        screenWidth = SelectPhotoAdapter.getScreenWidth(this);
        photoListViewAdapter = new ListViewAdapter<SelectPhotoAdapter.SelectPhotoEntity>(this,R.layout.listview_item,null) {
            @Override
            public void convert(ViewHolder holder, int position, SelectPhotoAdapter.SelectPhotoEntity entity) {
                ImageView imageView = holder.getView(R.id.iv_selected_photo);
                imageLoader.setAsyncBitmapFromSD(entity.url,imageView,screenWidth,true,true,false);//这里因为图片太大，所以不要保存缩略图
            }
        };
        getImagePathFromSD();
    }

    /**
     * 从sd卡获取图片资源
     * @return
     */
    private void getImagePathFromSD() {
        //检查权限,获取权限之后将手机所有注册图片搜索出来，并按照相册进行分类
        permissionHelper.checkPermission(this, new PermissionHelper.AskPermissionCallBack() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "成功", Toast.LENGTH_SHORT).show();
                // 得到sd卡内image文件夹的路径   File.separator(/)
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/wcfb/camera";

                // 得到该路径文件夹下所有的文件
                File fileAll = new File(filePath);
                File[] files = fileAll.listFiles();
                // 将所有的文件存入ArrayList中,并过滤所有图片格式的文件
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (checkIsImageFile(file.getPath())) {
                        String albumUrl = null;
                        ContentResolver cr = MainActivity.this.getContentResolver();
                        //返回值为要发送图片的url
                        albumUrl = BitmapUtils.insertImage(MainActivity.this, cr, file, true);
                        // 提交数据，和选择图片用的同一个ArrayList
                        SelectPhotoAdapter.SelectPhotoEntity photoEntity = new SelectPhotoAdapter.SelectPhotoEntity();
                        //因为存储到相册之后exif全都没了，所以应该传源文件的路径
                        photoEntity.url = albumUrl;
                        selectedPhotos.add(photoEntity);
                    }
                }
                // 返回得到的图片列表
                ListView lv_photos = findViewById(R.id.lv_photos);
                if (photoListViewAdapter != null) {
                    photoListViewAdapter.setmDatas(selectedPhotos);
                    lv_photos.setAdapter(photoListViewAdapter);
                }
            }

            @Override
            public void onFailed() {
                Toast.makeText(MainActivity.this, "请允许权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 检查扩展名，得到图片格式的文件
     * @param fName  文件名
     * @return
     */
    private boolean checkIsImageFile(String fName) {
        boolean isImageFile = false;
        // 获取扩展名
        String FileEnd = fName.substring(fName.lastIndexOf(".") + 1,
                fName.length()).toLowerCase();
        if (FileEnd.equals("jpg") || FileEnd.equals("png") || FileEnd.equals("gif")
                || FileEnd.equals("jpeg")|| FileEnd.equals("bmp") ) {
            isImageFile = true;
        } else {
            isImageFile = false;
        }
        return isImageFile;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_add_photo:
                Intent intent = new Intent(this,SelectPhotoActivity.class);
                startActivityForResult(intent,10);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null || resultCode != SelectPhotoActivity.SELECT_PHOTO_OK)
            return;
        selectedPhotos = data.getParcelableArrayListExtra("selectPhotos");
        photoListViewAdapter.setmDatas(selectedPhotos);
        lv_photos.setAdapter(photoListViewAdapter);
    }
}
