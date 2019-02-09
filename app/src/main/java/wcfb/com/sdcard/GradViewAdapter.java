package wcfb.com.sdcard;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class GradViewAdapter extends ArrayAdapter<GradViewAdapter.PhotoEntity>
        implements View.OnClickListener{

    //文件夹名
    public String folderName;
    //文件夹中的图片数
    public int imageCounts;
    //这是存储相片的路径
    public String topImagePath = "/wcfb/camera";
    public String cameraPhotoUrl;
    private Activity mActivity;
    public ArrayList<GradViewAdapter.PhotoEntity> allPhotoList;
    int maxSelectedPhotoCount = 9;

    public static final int REQ_CAMARA = 1000;
    private File mfile1;
    private ImageLoader imageLoader;
    private int destWidth, destHeight;
    int screenWidth;

    @Override
    public String toString() {
        return "ImageBean{" +
                "folderName='" + folderName + '\'' +
                ", topImagePath='" + topImagePath + '\'' +
                ", imageCounts=" + imageCounts +
                '}';
    }

    public GradViewAdapter(Activity activity, ArrayList<GradViewAdapter.PhotoEntity> array) {
        super(activity, R.layout.adapter_grad_item, array);
        this.mActivity = activity;
        this.allPhotoList = array;
        this.imageLoader = new ImageLoader(activity);
        screenWidth = getScreenWidth(activity);
        this.destWidth = (screenWidth - 20) / 3;
        this.destHeight = (screenWidth - 20) / 3;
    }

    @Override
    public int getCount() {
        return allPhotoList.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        GradViewAdapter.ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new GradViewAdapter.ViewHolder();

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_select_photo,
                    parent, false);
            viewHolder.rlPhoto = convertView.findViewById(R.id.rlPhotos);
            viewHolder.iv_photo = convertView.findViewById(R.id.iv_photos);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GradViewAdapter.ViewHolder) convertView.getTag();
        }

        if (viewHolder.iv_photo.getLayoutParams() != null) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) viewHolder.iv_photo.getLayoutParams();
            lp.width = destWidth;
            lp.height = destHeight;
            viewHolder.iv_photo.setLayoutParams(lp);
        }

        viewHolder.rlPhoto.setOnClickListener(null);

        if((allPhotoList != null) && (position >= 0) &&
                (allPhotoList.size() >= position) && (allPhotoList.get(position-1) != null)){
            final GradViewAdapter.PhotoEntity photoEntity = allPhotoList.get(position-1);
            final String filePath = photoEntity.url;
            imageLoader.setAsyncBitmapFromSD(filePath,viewHolder.iv_photo,
                    screenWidth/3,false,
                    true,true);
            viewHolder.rlPhoto.setTag(R.id.rlPhoto,photoEntity);
            viewHolder.rlPhoto.setOnClickListener(this);

        }
        return convertView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.rlPhoto:
                Log.i("Alex","点击了rl photo");
                SelectPhotoAdapter.SelectPhotoEntity entity = (SelectPhotoAdapter.SelectPhotoEntity) v.getTag(R.id.rlPhoto);
                ImageView ivSelect = v.findViewById(R.id.iv_select);
                if (mActivity == null)
                    return;
                if (mActivity instanceof SelectPhotoAdapter.CallBackActivity)
                    ((SelectPhotoAdapter.CallBackActivity)mActivity).updateSelectActivityViewUI();
                break;
        }
    }


    public interface CallBackActivity{
        void updateSelectActivityViewUI();
    }

    class ViewHolder {
        public RelativeLayout rlPhoto;
        public ImageView iv_photo;
    }

    public static class PhotoEntity implements Serializable, Parcelable {

        public String url;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
        }

        public PhotoEntity() {
        }

        protected PhotoEntity(Parcel in) {
            this.url = in.readString();
        }

        public static final Creator<SelectPhotoAdapter.SelectPhotoEntity> CREATOR = new Creator<SelectPhotoAdapter.SelectPhotoEntity>() {
            @Override
            public SelectPhotoAdapter.SelectPhotoEntity createFromParcel(Parcel source) {
                return new SelectPhotoAdapter.SelectPhotoEntity(source);
            }

            @Override
            public SelectPhotoAdapter.SelectPhotoEntity[] newArray(int size) {
                return new SelectPhotoAdapter.SelectPhotoEntity[size];
            }
        };

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("SelectPhotoEntity{");
            sb.append("url='").append(url).append('\'');
            sb.append('}');
            return sb.toString();
        }

        @Override
        public int hashCode() {
            //使用hashcode和equals方法防止重复
            if(url != null)return url.hashCode();
            return super.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof SelectPhotoAdapter.SelectPhotoEntity){
                return o.hashCode() == this.hashCode();
            }
            return super.equals(o);

        }
    }

    public static int getScreenWidth(Activity activity) {
        DisplayMetrics metric = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    public static Drawable getDrawable(Context context, int id) {
        if ((context == null) || (id < 0)) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(id, null);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    /**
     * 从系统相册里面取出图片的uri
     */
    public static void getPhotoFromLocalStorage(final Context context,
                         final SelectPhotoAdapter.LookUpPhotosCallback completeCallback) {
        new MultiTask<Void,Void,ArrayList<SelectPhotoAdapter.SelectPhotoEntity>>(){

            @Override
            protected ArrayList<SelectPhotoAdapter.SelectPhotoEntity> doInBackground(Void... params) {
                ArrayList<SelectPhotoAdapter.SelectPhotoEntity> allPhotoArrayList = new ArrayList<>();

                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                //得到内容处理者实例
                ContentResolver mContentResolver = context.getContentResolver();

                //设置拍摄日期为倒序
                String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " desc";
                Log.i("Alex","准备查找图片");
                // 只查询jpeg和png的图片
                Cursor mCursor = mContentResolver.query(mImageUri,
                        new String[]{MediaStore.Images.Media.DATA},
                        MediaStore.Images.Media.MIME_TYPE + "=? or "
                                + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[]{"image/jpeg", "image/png"},
                        sortOrder+" limit 500");
                if (mCursor == null) return allPhotoArrayList;
                int size = mCursor.getCount();
                Log.i("Alex","查到的size是"+size);
                if (size == 0) return allPhotoArrayList;
                //遍历全部图片
                for (int i = 0; i < size; i++) {
                    mCursor.moveToPosition(i);
                    // 获取图片的路径
                    String path = mCursor.getString(0);
                    SelectPhotoAdapter.SelectPhotoEntity entity = new SelectPhotoAdapter.SelectPhotoEntity();
                    //将图片的uri放到对象里去
                    entity.url = path;
                    allPhotoArrayList.add(entity);
                }
                mCursor.close();
                return allPhotoArrayList;
            }

            @Override
            protected void onPostExecute(ArrayList<SelectPhotoAdapter.SelectPhotoEntity> photoArrayList) {
                super.onPostExecute(photoArrayList);
                if(photoArrayList == null)return;
                if(completeCallback != null)completeCallback.onSuccess(photoArrayList);
            }
        }.executeDependSDK();
    }

    /**
     * 查询照片成功的回调函数
     */
    public interface LookUpPhotosCallback {
        void onSuccess(ArrayList<SelectPhotoAdapter.SelectPhotoEntity> photoArrayList);
    }

    /**
     * 判断某张照片是否已经被选择过
     * @param entity
     * @return
     */
    HashSet<SelectPhotoAdapter.SelectPhotoEntity> selectedPhotosSet = new HashSet<>(9);
    public boolean checkIsExistedInSelectedPhotoArrayList(SelectPhotoAdapter.SelectPhotoEntity photo) {

        if (selectedPhotosSet == null || selectedPhotosSet.size() == 0)
            return false;
        if(selectedPhotosSet.contains(photo))
            return true;

        return false;
    }

    public void removeSelectedPhoto(SelectPhotoAdapter.SelectPhotoEntity photo) {
        selectedPhotosSet.remove(photo);
    }
    public boolean isFullInSelectedPhotoArrayList() {
        if (maxSelectedPhotoCount > 0 &&
                selectedPhotosSet.size() < maxSelectedPhotoCount)
            return false;
        return true;
    }
    public void addSelectedPhoto(SelectPhotoAdapter.SelectPhotoEntity photo) {
        selectedPhotosSet.add(photo);
    }
}