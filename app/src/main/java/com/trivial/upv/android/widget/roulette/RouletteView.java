package com.trivial.upv.android.widget.roulette;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.trivial.upv.android.R;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;
import com.trivial.upv.android.model.Theme;

import java.util.Random;

import static java.lang.Thread.sleep;

public class RouletteView extends View implements GestureDetector.OnGestureListener {

    private int SIZE_BORDER = 10;
    private static final String TAB = "RoluetteView";
    private static final int K_ADJUST_ROULETTE_EDGE = 2;
    private static final int K_ADJUST_ROULETTE_DEGREES_EDGE = 5;
    private String images[] = null;
    private RectF oval;
    private Theme[] themes;
    private int backgroundColor;

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
        this.invalidate();
    }

    private String line1;
    private String line2;
    private boolean isRotating;
    private RotateEventLisstener rotationEventListener;
    private boolean playable = false;

    public RouletteView(Context context) {
        super(context);
        setupView();
    }

    private float mDensity;

    public void setupView() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        new Thread(new Runnable() {
            public void run() {
                soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
                idSoundRoulette = soundPool.load(getContext(), R.raw.tick, 0);
                idSoundYourTurn = soundPool.load(getContext(), R.raw.alleluia, 0);

            }
        }).start();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDensity = displayMetrics.density;
        mDetector = new GestureDetectorCompat(getContext(), this);


        SWIPE_MIN_DISTANCE = (int) (75.0f * mDensity / 2.0f);
        SWIPE_MAX_OFF_PATH = (int) (75.0f * mDensity / 2.0f);
        SWIPE_THRESHOLD_VELOCITY = (int) (75.0f * mDensity / 2.0f);

        paint = new Paint();
        path = new Path();

        //background color
        TypedArray array = getContext().getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.background,
        });
        backgroundColor = array.getColor(0, 0xFF00FF);
        array.recycle();
    }

    Paint paint;
    Path path;
    private GestureDetectorCompat mDetector = null;


    private Bitmap mBitmaps[];

    public RouletteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }


    public int getNumSectors() {
        return numSectors;
    }

    public synchronized void setNumSectors(final boolean minified, int sectors, String[] images, Theme[] mThemes) {
        this.numSectors = sectors;
        this.images = images;
        this.themes = mThemes;

        if (minified)
            SIZE_BORDER = 5;
        // Recycle
        if (mBitmaps != null) {
            for (int i = 0; i < mBitmaps.length; i++) {
                if (mBitmaps[i] != null) {
                    mBitmaps[i].recycle();
                    mBitmaps[i] = null;
                }
            }
        }
        mBitmaps = new Bitmap[numSectors];

        ImageLoader imageLoader = VolleySingleton.getInstance(getContext()).getImageLoader();

        for (int i = 0; i < numSectors; i++) {

            final int finalI = i;
            if (i < images.length && images[i]!=null) {

                imageLoader.get(images[i], new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                        if (response != null) {
                            if (response.getBitmap() != null)
                                synchronized (this) {
                                    mBitmaps[finalI] = response.getBitmap();
//                                    int maxWidth = (int)(Math.tan(((double)angulo)*(Math.PI/180.0d)*(float)r/2.0d *2.0d));
//
//                                    float ((float)maxWidth/(float)mBitmaps[finalI].getWidth());
//                                    float sizeW = (float)getWidth()/((float)numSectors/1.1f);
//                                    float scala = sizeW/(float)mBitmaps[finalI].getWidth();
//                                    int x= (int)((float)mBitmaps[finalI].getWidth()*scala);
//                                    int y = (int)((float)mBitmaps[finalI].getHeight()*scala);
////
                                    if (!minified)
                                        mBitmaps[finalI] = getResizedBitmap(mBitmaps[finalI], (int) ((float) mBitmaps[finalI].getWidth() * 1.5f), (int) ((float) mBitmaps[finalI].getHeight() * 1.5f));
                                    else
                                        mBitmaps[finalI] = getResizedBitmap(mBitmaps[finalI], (int) convertDpToPixel(20, getContext()), (int) convertDpToPixel(20, getContext()));
                                    RouletteView.this.invalidate();
                                }
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("error_volley", error.getMessage());
                    }
                });
                this.invalidate();
            }
        }
    }

    private int numSectors = 0;


    private float angulo;
    private int r;

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
//        bm.recycle();
        return resizedBitmap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        oval = new RectF(SIZE_BORDER / 2, SIZE_BORDER / 2, w - SIZE_BORDER / 2 - 1, w - SIZE_BORDER / 2 - 1);
        r = w / 2;      // Calculated from canvas width
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (numSectors != 0) {
            angulo = 360.0f / (float) numSectors;
            boolean userCenter = true;
            int x, y;
            for (int i = 0; i < numSectors; i++) {
                if (i < themes.length && themes[i] != null)
                    paint.setColor(ContextCompat.getColor(getContext(), themes[i].getWindowBackgroundColor()));
                else
                    paint.setColor(backgroundColor);

                paint.setStyle(Paint.Style.FILL);
                path.reset();
                if (numSectors > 0) {
                    if (numSectors > 1) {
                        canvas.drawArc(oval, (((float) i) * angulo - 90.0f), angulo, true, paint);
                        path.arcTo(oval, (((float) i) * angulo - 90.0f), angulo);
                        path.lineTo(getWidth() / 2, getWidth() / 2);
                        path.close();

                    }
                    else { canvas.drawCircle(r,r, r-SIZE_BORDER/2, paint);
                        path.addCircle(r,r, r-SIZE_BORDER/2, Path.Direction.CCW);
                    }
                    if (numSectors > 1) {

                    }
                }

                synchronized (this) {
                    Bitmap mBitmap = mBitmaps[i];
                    if (mBitmap != null) {
                        canvas.save();
                        canvas.clipPath(path, Region.Op.INTERSECT);

                        double trad = ((((float) i) * angulo - 90.0f) + angulo / 2) * (Math.PI / 180d); // = 5.1051
                        x = (int) (r * Math.cos(trad));
                        x += r;
                        x += (r - x) / 3;

                        if (numSectors > 1) {
                            y = (int) (r * Math.sin(trad));
                            y += r;
                            y += (r - y) / 3;
                        } else y = r;

                        x -= mBitmap.getWidth() / 2;
                        y -= mBitmap.getHeight() / 2;

//                    Log.d("LOG", ((((float) i) * angulo - 90.0f) + angulo / 2) + "");
                        if (numSectors > 1)
                            canvas.rotate((((float) i) * angulo - 90.0f) + angulo / 2 + 90.0f, x + mBitmap.getWidth() / 2, y + mBitmap.getHeight() / 2);
                        canvas.drawBitmap(mBitmap, x, y, null);

                        try {
                            canvas.restore();
                        } catch (Exception e) {

                        }
                    }
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(SIZE_BORDER);


                if (numSectors == 1) userCenter = false;
                else userCenter = true;
                canvas.drawArc(oval, (((float) i) * angulo - 90.0f), angulo, userCenter, paint);
                // Draw
//            Paint paintIndicador = new Paint();
//            paintIndicador.setColor(Color.BLACK);
//            paintIndicador.setStyle(Paint.Style.FILL_AND_STROKE);
//            Path pathIndicador = new Path();
//            pathIndicador.moveTo(getWidth()/2, 0);
//            pathIndicador.lineTo(getWidth()/2 + getWidth()/2/11, 0);
//            pathIndicador.lineTo(getWidth()/2, getWidth()/2/6);
//            pathIndicador.lineTo(getWidth()/2 - getWidth()/2/11, 0);
//            pathIndicador.close();
//            canvas.drawPath(pathIndicador, paintIndicador);
                synchronized (RouletteView.this) {
                    if (line1 != null) {
                        Paint mpaint = new Paint();
                        Paint paint2 = new Paint();
                        paint2.setColor(Color.BLACK);
                        paint2.setTextSize(convertDpToPixel(20, getContext()));  //set text size
//                paint2.setTextAlign(Paint.Align.CENTER);
                        float maxW = 0;
                        String[] splitedLines = line1.split("\n");
                        for (String line : splitedLines) {
                            float w = paint2.measureText(line) / 2;
                            if (w > maxW) {
                                maxW = w;
                            }
                        }
                        float totalYSize = (splitedLines.length - 1) * (-paint2.ascent() + paint2.descent());
                        float yy = r - totalYSize / 2;

                        float textSize = paint2.getTextSize();
                        Path path = roundedRect(r - maxW - textSize / 2, yy - textSize, r + maxW + textSize / 2, r + totalYSize / 2 + textSize / 3, convertDpToPixel(5, getContext()), convertDpToPixel(5, getContext()), true, true, true, true);
                        mpaint.setColor(Color.WHITE);
                        mpaint.setStyle(Paint.Style.FILL);
                        canvas.drawPath(path, mpaint);
                        mpaint.setColor(Color.BLACK);
                        mpaint.setStyle(Paint.Style.STROKE);
                        canvas.drawPath(path, mpaint);

                        for (String line : line1.split("\n")) {
                            canvas.drawText(line, r - maxW, yy, paint2);
                            yy += -paint2.ascent() + paint2.descent();
                        }
                    }
                }
            }
        } else {
            // Draw a Circle
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(SIZE_BORDER);
            canvas.drawArc(oval, 0, 360, false, paint);
        }
    }

    private static Path roundedRect(
            float left, float top, float right, float bottom, float rx, float ry,
            boolean tl, boolean tr, boolean br, boolean bl
    ) {
        Path path = new Path();
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;
        float width = right - left;
        float height = bottom - top;
        if (rx > width / 2) rx = width / 2;
        if (ry > height / 2) ry = height / 2;
        float widthMinusCorners = (width - (2 * rx));
        float heightMinusCorners = (height - (2 * ry));

        path.moveTo(right, top + ry);
        if (tr)
            path.rQuadTo(0, -ry, -rx, -ry);//top-right corner
        else {
            path.rLineTo(0, -ry);
            path.rLineTo(-rx, 0);
        }
        path.rLineTo(-widthMinusCorners, 0);
        if (tl)
            path.rQuadTo(-rx, 0, -rx, ry); //top-left corner
        else {
            path.rLineTo(-rx, 0);
            path.rLineTo(0, ry);
        }
        path.rLineTo(0, heightMinusCorners);

        if (bl)
            path.rQuadTo(0, ry, rx, ry);//bottom-left corner
        else {
            path.rLineTo(0, ry);
            path.rLineTo(rx, 0);
        }

        path.rLineTo(widthMinusCorners, 0);
        if (br)
            path.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
        else {
            path.rLineTo(rx, 0);
            path.rLineTo(0, -ry);
        }

        path.rLineTo(0, -heightMinusCorners);

        path.close();//Given close, last lineto can be removed.

        return path;
    }

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public static float convertPixelsToDp(float px, Context context) {
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private long lngDegrees = 0;

    // SWIPE LEFT TO RIGHT Y SWIPE BOTTOM TO UP DETECTOR
    private int SWIPE_MIN_DISTANCE = 75;
    private int SWIPE_MAX_OFF_PATH = 75;
    private int SWIPE_THRESHOLD_VELOCITY = 75;

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        Log.d(TAB, "onFling: " + e1.toString() + e2.toString());
        if (isRotating) return false;
        float max = Math.abs(Math.max(velocityX * mDensity / 2.0f, velocityY * mDensity / 2.0f));
        if (Math.abs(e1.getY() - e2.getY()) * mDensity / 2.0f > SWIPE_MAX_OFF_PATH) {
            if (Math.abs(velocityY) < SWIPE_THRESHOLD_VELOCITY)
                return false;
            if ((e1.getY() - e2.getY()) * mDensity / 2.0f > SWIPE_MIN_DISTANCE) {
//                Log.d("FLING", "bottomToTop: " + max);
                rotate(max / 360.0f);
            } else if ((e2.getY() - e1.getY()) * mDensity / 2.0f > SWIPE_MIN_DISTANCE)
//                Log.d("FLING", "topToBottom  ");
                rotate(max / 360.0f);
        } else {
            if (Math.abs(velocityX) < SWIPE_THRESHOLD_VELOCITY)
                return false;
            if ((e1.getX() - e2.getX()) * mDensity / 2.0f > SWIPE_MIN_DISTANCE) {
                rotate(max / 360.0f);
            } else if ((e2.getX() - e1.getX()) * mDensity / 2.0f > SWIPE_MIN_DISTANCE) {
//                Log.d("FLING", "swipe LeftToright," + max);
                rotate(max / 360.0f);
            }
        }
        return true;
    }

    public void setRotationEventListener(RotateEventLisstener rotationEventListener,
                                         boolean playable) {
        this.rotationEventListener = rotationEventListener;
        this.playable = playable;
    }


    public interface RotateEventLisstener {
        void rotateStart();

        void rotateEnd(int category);

    }

    private float speed;

    private RotateAnimation rotateAnimation = null;

    public void rotate(float speed) {
        if (playable) {
            if (!isRotating && speed > 0.5f) {
                synchronized (RouletteView.this) {
                    line1 = null;
                }
                isRotating = true;
                this.speed = speed;

                parar = false;

//            int ran = new Random().nextInt(360);
                if (speed>20.0f)
                    speed=20.0f;
                long ran = (long) (360.0f * speed) + new Random().nextInt(360);
                long ajuste = ((lngDegrees + ran) % 360) % ((360 / numSectors));
//                Log.d("ROTATE", ran +"");
//                Log.d("ROTATE",((int) (((double) numSectors)
//                        - Math.floor(((double) lngDegrees) / (360.0d / ((double) numSectors)))) - 1) + "");
                if (ajuste <= K_ADJUST_ROULETTE_EDGE) {
                    ran = ran + K_ADJUST_ROULETTE_DEGREES_EDGE;
//                    Log.d("RAN" , "RAN AJUSTADO");
                }

//                Log.d("RAN", "grados: " + lngDegrees + " ran=" +ran +  " SPEED=" + speed + " ajuste =" + ajuste + " grados ajustados:" +(lngDegrees + ran) % 360);
                rotateAnimation = new RotateAnimation((float) this.lngDegrees, (float)
                        (this.lngDegrees + ((long) ran)), this.getWidth() / 2, this.getWidth() / 2);
//                    Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);

                lngDegrees = (lngDegrees + ran) % 360;

                rotateAnimation.setDuration(ran);
                rotateAnimation.setFillAfter(true);
                rotateAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        playSoundRoulette();
                        rotationEventListener.rotateStart();
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
//                        isRotating = false;
                        rotationEventListener.rotateEnd((int) (((double) numSectors)
                                - Math.floor(((double) lngDegrees) / (360.0d / ((double) numSectors)))) - 1);
                        parar = true;
                        destroySound();

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                setAnimation(rotateAnimation);
                startAnimation(rotateAnimation);
            }
        }

    }

    private SoundPool soundPool = null;
    private int idSoundRoulette = 0;
    private int idSoundYourTurn = 0;
    long lastTime = 0;
    boolean parar = false;

    public void playSoundRoulette() {
        final ValueAnimator animacion = ValueAnimator.ofFloat(0, 10);
        animacion.setDuration((long) (360 * speed + lngDegrees));
        animacion.setInterpolator(new AccelerateDecelerateInterpolator());
        animacion.setRepeatCount(0);
        lastTime = System.currentTimeMillis();
        parar = true;
        animacion.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
//                Log.d("SONIDO", Float.parseFloat(valueAnimator.getAnimatedValue().toString()) + " " + (System.currentTimeMillis() - start -auxTime));
//                auxTime = (System.currentTimeMillis() - start);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (parar) {
                            parar = false;
                            soundPool.play(idSoundRoulette, 0.5f, 0.5f, 1, 0, 1f);
                        } else if (System.currentTimeMillis() - lastTime >= (long) (15.0f + 10f * (Float.parseFloat(valueAnimator.getAnimatedValue().toString())))) {
                            lastTime = System.currentTimeMillis();
                            soundPool.play(idSoundRoulette, 0.5f, 0.5f, 1, 0, 1f);
                        }
                    }
                }).start();
            }
        });
        animacion.start();
    }

    public void playSoundIsYourTurn() {
        if (soundPool != null)
            soundPool.play(idSoundYourTurn, 0.5f, 0.5f, 1, 0, 1f);
    }

    public void resumeSound() {
        if (soundPool != null) {
            soundPool.resume(idSoundRoulette);
            soundPool.resume(idSoundYourTurn);
        }

    }

    public void destroySound() {
        if (soundPool != null) {
            soundPool.stop(idSoundRoulette);
            soundPool.stop(idSoundYourTurn);
            soundPool.release();
        }
    }

    public void pauseSound() {
        if (soundPool != null) {
            soundPool.pause(idSoundRoulette);
            soundPool.pause(idSoundYourTurn);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (playable) {
            if (isRotating) return false;
            boolean salida = mDetector.onTouchEvent(event);
            if (salida)
                return salida;
            else
                return super.onTouchEvent(event);
        } else
            return super.onTouchEvent(event);
    }
}