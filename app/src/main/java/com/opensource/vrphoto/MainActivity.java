package com.opensource.vrphoto;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.opensource.vrphoto.openglRenderer.GLToolBox;
import com.opensource.vrphoto.openglRenderer.TextureRenderer;
import com.opensource.vrphoto.utils.Utils;



import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements SensorEventListener ,GLSurfaceView.Renderer{

    private final static int BASE_RACE = 80;
    private final static int FASTEST_RACE = 20;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private long num;
    //private ImageView img;

    /**
     * 标识图像转动的方向
     */
    private boolean isRightRotate = false;
    /**
     * 默认显示第一张图片
     */
    private int curFrame = 0;
    /**
     * 间距，每滑动这个间距就换一张图片
     */
    private final static int interval = 30;
    /**
     * 按下时显示的图片
     */
    private int acton_down_page =  0;
    private Object object = new Object();
    private float[] lastQ = new float[4];
    float down_x = 0;
    long down_time = 0;
    float up_x = 0;
    long up_time = 0;
    private int curRate = 80;
    private float tempValue = 0;


    private int[] imgArray = {
            R.mipmap.img1, R.mipmap.img2, R.mipmap.img3, R.mipmap.img4, R.mipmap.img5, R.mipmap.img6, R.mipmap.img7,R.mipmap.img8,R.mipmap.img9,R.mipmap.img10
            ,R.mipmap.img11, R.mipmap.img12, R.mipmap.img13, R.mipmap.img14, R.mipmap.img15, R.mipmap.img16, R.mipmap.img17,R.mipmap.img18,R.mipmap.img19,R.mipmap.img20
            ,R.mipmap.img21, R.mipmap.img22, R.mipmap.img22, R.mipmap.img24, R.mipmap.img25, R.mipmap.img26, R.mipmap.img27,R.mipmap.img28,R.mipmap.img29,R.mipmap.img30
            ,R.mipmap.img31, R.mipmap.img32, R.mipmap.img33, R.mipmap.img34, R.mipmap.img35, R.mipmap.img36, R.mipmap.img37,R.mipmap.img38,R.mipmap.img39,R.mipmap.img40
            ,R.mipmap.img41, R.mipmap.img42, R.mipmap.img43, R.mipmap.img44, R.mipmap.img45, R.mipmap.img46, R.mipmap.img47,R.mipmap.img48,R.mipmap.img49,R.mipmap.img50
            ,R.mipmap.img51, R.mipmap.img52, R.mipmap.img53, R.mipmap.img54, R.mipmap.img55, R.mipmap.img56, R.mipmap.img57,R.mipmap.img58,R.mipmap.img59,R.mipmap.img60
            ,R.mipmap.img61, R.mipmap.img62, R.mipmap.img63, R.mipmap.img64, R.mipmap.img65, R.mipmap.img66, R.mipmap.img67,R.mipmap.img68,R.mipmap.img69,R.mipmap.img70
            ,R.mipmap.img71, R.mipmap.img72, R.mipmap.img73, R.mipmap.img74, R.mipmap.img75, R.mipmap.img76, R.mipmap.img77,R.mipmap.img78,R.mipmap.img79,R.mipmap.img80

    };



    private GLSurfaceView mGLView;
    private int[] mTextures =  new int[1];
    private TextureRenderer mTexRenderer = new TextureRenderer();
    private int mImageWidth;
    private int mImageHeight;
    private boolean mInitialized = false;
    //记录上一帧index，以保证不重复渲染
    private int lastFrame = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    private void initView(){
        getSupportActionBar().hide();

        //初始化GLview
        mGLView = (GLSurfaceView) findViewById(R.id.glview);
        mGLView.setEGLContextClientVersion(2);
        mGLView.setRenderer(this);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        //展示最中间的一张
        int lenght = imgArray.length;
        int i = lenght%2==0 ? lenght/2-1 : lenght/2;
        curFrame = i;
        //请求渲染
        mGLView.requestRender();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.i("Sensor", " x=" + event.values[0] + " y=" + event.values[1] + " z=" + event.values[2]+ " w=" + event.values[3]);

        //Android 已经提供 Sensor.TYPE_POSE_6DOF 传感器监听，直接给出四元数的值，详细请查看：
        //https://developer.android.google.cn/reference/android/hardware/SensorEvent.html#values
        //但是有些版本过低，还获取不到，所以我还是采用 Sensor.TYPE_ROTATION_VECTOR 监听，然后再转换成四元数

        float[] QAC = new float[4];
        Utils.getQuaternionFromVector(QAC, event.values);

        if(num >= 3){
            float[] QBA = new float[4];
            QBA[0] = lastQ[0];
            QBA[1] = -lastQ[1];
            QBA[2] = -lastQ[2];
            QBA[3] = -lastQ[3];
            // QBA = QBA * QAC
            float[] QBC = new float[4];
            QBC[0] = QAC[0]*QBA[0] - QAC[1]*QBA[1] - QAC[2]*QBA[2] -QAC[3]*QBA[3];
            QBC[1] = QAC[0]*QBA[1] + QAC[1]*QBA[0] + QAC[2]*QBA[3] -QAC[3]*QBA[2];
            QBC[2] = QAC[0]*QBA[2] - QAC[1]*QBA[3] + QAC[2]*QBA[0] +QAC[3]*QBA[1];
            QBC[3] = QAC[0]*QBA[3] + QAC[1]*QBA[2] - QAC[2]*QBA[1] +QAC[3]*QBA[0];

            //偏向Z轴的位移
            double z = Math.atan2(2*QBC[1]*QBC[2] - 2*QBC[0]*QBC[3]
                    , 2*QBC[0]*QBC[0] + 2*QBC[1]*QBC[1]-1);
            //偏向X轴的位移
            double x = -Math.asin(2*QBC[1]*QBC[3] + 2*QBC[0]*QBC[2]);
            //偏向Y轴的位移
            double y = Math.atan2(2*QBC[2]*QBC[3] - 2*QBC[0]*QBC[1]
                    , 2*QBC[0]*QBC[0] + 2*QBC[3]*QBC[3]-1);

            java.text.DecimalFormat   df   =new   java.text.DecimalFormat("#0.000");
            Log.i("Sensor","z=" +  df.format(z) + " x=" + df.format(x)  + " y=" +df.format(y) );

            int distance = (int)Math.round(y / 0.02);
            curFrame = curFrame - distance;
            if(curFrame <= 0){
                curFrame = 0;
            }else if(curFrame >= imgArray.length-1){
                curFrame = imgArray.length - 1;
            }

            if(distance != 0){
                mHandler.sendEmptyMessage(curFrame);
                lastQ = QAC;
            }

        } else {
            num++;
            lastQ = QAC;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("Sensor","accuracy="+accuracy);
    }

    public void leftRotate(){
        this.isRightRotate = false;
    }

    public void rightRotate(){
        this.isRightRotate = true;
    }

    /**
     * 处理滑动时的X坐标改变
     * @param offset_X
     */
    private void slidingHander(float offset_X){

        if((offset_X- down_x)/interval > 1){
            curFrame = acton_down_page - (int)(offset_X- down_x)/interval;
            if(curFrame == lastFrame){
                return;
            }
            turnRight();
            lastFrame = curFrame;

        }else if((offset_X - down_x )/interval < -1){
            curFrame = acton_down_page + Math.abs((int)(offset_X- down_x)/interval);
            if(curFrame == lastFrame){
                return;
            }
            turnLeft();
            lastFrame = curFrame;
        }
    }


    private void turnRight(){
        if(curFrame <= 0 ){
            curFrame = 0;
            return;
        }

        if(curFrame < imgArray.length){
            mGLView.requestRender();
        }
    }

    private void turnLeft(){
        if(curFrame >= imgArray.length - 1){
            curFrame = imgArray.length -1;
            return;
        }
        if(curFrame >= 0){
            mGLView.requestRender();
        }
    }

    /**
     * 处理用户离开屏幕动作
     * @param offset_X
     */
    private void actionUpHander(float offset_X){

        acton_down_page = curFrame;
        up_x = offset_X;
        up_time = System.currentTimeMillis();
        float rate = (up_x - down_x)/(up_time - down_time);

        if(rate > 0){
            rightRotate();
        }else {
            leftRotate();
        }

        if(rate < 0.8 && rate > -0.8){
        }else if(rate > 3 || rate < -3){
            curRate = FASTEST_RACE;

        }else if(rate >= 0.8 && rate <= 3){
            curRate = (int) (BASE_RACE/rate);

        }else if(rate >= -3 && rate <= -0.8){
            curRate = (int) Math.abs(BASE_RACE/rate);

        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                down_time = System.currentTimeMillis();
                down_x = event.getX();
                acton_down_page = curFrame;
                break;

            case MotionEvent.ACTION_MOVE:
                slidingHander(event.getX());
                break;

            case MotionEvent.ACTION_UP:
                actionUpHander(event.getX());
                break;
        }
        return super.onTouchEvent(event);
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            Log.i("ACTION","curFrame="+ msg.what);

            if(msg.what >= 0 && msg.what < imgArray.length){
                mGLView.requestRender();

            }
        }
    };

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        if(mTexRenderer !=null)
        {
            mTexRenderer.updateViewSize(width,height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        //懒加载
        if(!mInitialized){
            mTexRenderer.init();
            mInitialized = true;
        }

        //渲染当前帧
        renderFrameInResource(imgArray[curFrame]);  //通过资源文件显示

//        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/scene/" + (curFrame +1) + ".jpg";
//        renderFrameInFilePath(file_path); //通过文件路径显示
    }


    /**
     * 通过资源文件显示
     * @param resource_id
     */
    private void renderFrameInResource(int resource_id)
    {
        GLES30.glDeleteTextures(1,mTextures,0);

        // Generate textures
        GLES30.glGenTextures(1, mTextures, 0);

        // Load input bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),resource_id);
        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mTexRenderer.updateTextureSize(mImageWidth, mImageHeight);

        // Upload to texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextures[0]);
        GLToolBox.checkGlError("glBindTexture");
        try {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        }catch (RuntimeException e)
        {
            Log.e("GLError", "loadTexture: " );
        }

        // Set texture parameters
        GLToolBox.initTexParams();

        mTexRenderer.renderTexture(mTextures[0]);
    }

    /**
     * 通过文件路径显示
     * @param file_path
     */
    private void renderFrameInFilePath(String file_path) {

        //delete last texture first
        GLES30.glDeleteTextures(1,mTextures,0);

        // Generate textures
        GLES30.glGenTextures(1, mTextures, 0);

        // Load input bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(file_path);
        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mTexRenderer.updateTextureSize(mImageWidth, mImageHeight);

        // Upload to texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextures[0]);
        GLToolBox.checkGlError("glBindTexture");
        try {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        }catch (RuntimeException e)
        {
            Log.e("GLError", "loadTexture: " );
        }

        // Set texture parameters
        GLToolBox.initTexParams();

        mTexRenderer.renderTexture(mTextures[0]);
    }



}
