package com.hsalf.smilerating;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sujith on 11/10/16.
 */
public class SmileRating extends BaseRating {

    private static final String TAG = "RatingView";

    private int mPlaceHolderSmileColor = Color.WHITE;
    private int mAngryColor = Color.parseColor("#f29a68");
    private int mNormalColor = Color.parseColor("#f2dd68");
    private int mDrawingColor = Color.parseColor("#353431");
    private int mPlaceholderBackgroundColor = Color.parseColor("#e6e8ed");

    private Face[] mFaces = new Face[SMILES_LIST.length];
    private Map<Integer, Point> mTouchPoints = new HashMap<>();
    private float mSmileGap;
    private Paint mPathPaint = new Paint();
    private Paint mBackgroundPaint = new Paint();

    private Paint mPointPaint1 = new Paint();
    private Paint mPointPaint2 = new Paint();
    //    private Path mEyePathLeft = new Path();
//    private Path mEyePathRight = new Path();
    private Point mFaceCenter = new Point();
    private Path mSmilePath = new Path();
    private Paint mPlaceHolderFacePaint = new Paint();
    private Paint mPlaceholderLinePaint = new Paint();
    private Paint mPlaceHolderCirclePaint = new Paint();
    private float divisions;
    private ValueAnimator mValueAnimator = new ValueAnimator();
    private FloatEvaluator mFloatEvaluator = new FloatEvaluator();
    private ArgbEvaluator mColorEvaluator = new ArgbEvaluator();
    private ClickAnalyser mClickAnalyser;
    private Matrix mScaleMatrix = new Matrix();
    private RectF mScaleRect = new RectF();
    private RectF mTouchBounds = new RectF();
    private Path mDummyDrawPah = new Path();
    private Paint mTextPaint = new Paint();

    @Smiley
    private int mSelectedSmile = OKAY;
    @Smiley
    private int mPreviousSmile = -1;
    @Smiley
    private int mNearestSmile = OKAY;
    @Smiley
    private int mPendingActionSmile = OKAY;
    private Smileys mSmileys;
    // private float mTranslation = 0;
    private float mWidth;
    private float mHeight;
    private float mCenterY;
    private float mFromRange;
    private float mToRange;
    private float mPrevX;
    private boolean mFaceClickEngaged = false;
    private OnRatingSelectedListener mOnRatingSelectedListener = null;
    private OnSmileySelectionListener mOnSmileySelectionListener = null;
    private float mPlaceHolderScale = 1f;

    public SmileRating(Context context) {
        super(context);

    }

    public SmileRating(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(attrs);
    }

    public SmileRating(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttrs(attrs);
    }

    private void parseAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SmileRating);
            mAngryColor = a.getColor(R.styleable.SmileRating_angryColor, mAngryColor);
            mNormalColor = a.getColor(R.styleable.SmileRating_normalColor, mNormalColor);
            mDrawingColor = a.getColor(R.styleable.SmileRating_drawingColor, mDrawingColor);
            mPlaceHolderSmileColor = a.getColor(R.styleable.SmileRating_placeHolderSmileColor,
                    mPlaceHolderSmileColor);
            mPlaceholderBackgroundColor = a.getColor(R.styleable.SmileRating_placeHolderBackgroundColor,
                    mPlaceholderBackgroundColor);
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        mClickAnalyser = ClickAnalyser.newInstance(getResources().getDisplayMetrics().density);

        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        mPathPaint.setAntiAlias(true);
        mPathPaint.setStrokeWidth(3);
        mPathPaint.setColor(mDrawingColor);
        mPathPaint.setStyle(Paint.Style.FILL);

        mPointPaint1.setColor(Color.RED);
        mPointPaint1.setStyle(Paint.Style.FILL);

        mPointPaint2.setColor(Color.BLUE);
        mPointPaint2.setStyle(Paint.Style.STROKE);

        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mPlaceHolderFacePaint.setColor(mPlaceHolderSmileColor);
        mPlaceHolderFacePaint.setStyle(Paint.Style.FILL);

        mPlaceHolderCirclePaint.setColor(mPlaceholderBackgroundColor);
        mPlaceHolderCirclePaint.setStyle(Paint.Style.FILL);

        mPlaceholderLinePaint.setColor(mPlaceholderBackgroundColor);
        mPlaceholderLinePaint.setStyle(Paint.Style.STROKE);

        mValueAnimator.setDuration(250);
        mValueAnimator.addListener(mAnimatorListener);
        mValueAnimator.addUpdateListener(mAnimatorUpdateListener);
        mValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float anim = (float) animation.getAnimatedValue();
            moveSmile(anim);
        }
    };

    private Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            notifyListener();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    private void notifyListener() {
        Log.i(TAG, mPreviousSmile + " " + getSelectedSmile());
        if (mOnSmileySelectionListener != null && mPreviousSmile != getSelectedSmile()) {
            mPreviousSmile = mSelectedSmile;
            mOnSmileySelectionListener.onSmileySelected(mSelectedSmile);
            if (mOnRatingSelectedListener != null) {
                mOnRatingSelectedListener.onRatingSelected(getRating());
            }
        }
    }

    public void setOnSmileySelectionListener(OnSmileySelectionListener l) {
        mOnSmileySelectionListener = l;
    }


    public void setOnRatingSelectedListener(OnRatingSelectedListener l) {
        mOnRatingSelectedListener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        // mHeight = mWidth / 5.3f;
        mHeight = mWidth / (5.3f * 1.3f);
        mCenterY = mHeight / 2f;
        mFaceCenter.y = mCenterY;
        divisions = (mHeight / 32f);
        mTextPaint.setTextSize(mHeight / 4.5f);
        mSmileys = Smileys.newInstance(Math.round(mWidth), Math.round(mHeight));
        setMeasuredDimension(Math.round(mWidth), (int) Math.round(mHeight + (mHeight * 0.48)));
        createTouchPoints();
        mPlaceholderLinePaint.setStrokeWidth(mHeight * 0.05f);
        /*getSmiley(mSmileys, 0.5f, divisions, mFromRange, mToRange,
                mFaceCenter, mSmilePath, mCenterY);*/
        setSelectedSmile(mPendingActionSmile, mTouchPoints.get(mPendingActionSmile), false, false);
        Log.i(TAG, "Selected smile:" + getSmileName(mPendingActionSmile));
    }

    private void createTouchPoints() {
        mTouchPoints.clear();
        float divisions = mWidth / 5f;
        float divCenter = divisions / 2f;
        mSmileGap = (divisions - mHeight) / 2f;
        mFromRange = mSmileGap + (mHeight / 2);
        mToRange = mWidth - (mHeight / 2) - mSmileGap;
        int count = SMILES_LIST.length;
        for (int i = 0; i < count; i++) {
            mFaces[i] = createFace(i, mCenterY);
            mTouchPoints.put(SMILES_LIST[i], new Point((divisions * i) + divCenter, mCenterY));
        }
    }

    private Face createFace(int index, float centerY) {
        Face face = new Face();
        face.smileType = index;
        getSmiley(mSmileys, index * 0.25f, divisions, mFromRange, mToRange, face.place,
                face.smile, centerY);
        face.place.y = centerY;
        return face;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Point start = mFaces[0].place;
        Point end = mFaces[mFaces.length - 1].place;
        canvas.drawLine(start.x, start.y, end.x, end.y, mPlaceholderLinePaint);
        for (Face face : mFaces) {
            if (!mSmilePath.isEmpty()) {
                float scale = getScale(face.smileType);
                canvas.drawCircle(face.place.x, face.place.y,
                        scale * (mHeight / 2), mPlaceHolderCirclePaint);
                mScaleMatrix.reset();
                face.smile.computeBounds(mScaleRect, true);
                mScaleMatrix.setScale(scale, scale,
                        mScaleRect.centerX(), mScaleRect.centerY());
                mDummyDrawPah.reset();
                mDummyDrawPah.addPath(face.smile, mScaleMatrix);
                canvas.drawPath(mDummyDrawPah, mPlaceHolderFacePaint);
                float transY = 0.15f - (scale * 0.15f);
                mTextPaint.setColor((int) mColorEvaluator.evaluate((transY / 0.15f),
                        mPlaceholderBackgroundColor, Color.BLACK));
                drawTextCentered(getSmileName(face.smileType), face.place.x,
                        face.place.y + (mHeight * (0.70f + transY)), mTextPaint, canvas);
            }
        }
        canvas.drawCircle(mFaceCenter.x, mFaceCenter.y, mHeight / 2f, mBackgroundPaint);
        if (!mSmilePath.isEmpty()) {
            canvas.drawPath(mSmilePath, mPathPaint);
        }
    }

    private void drawTextCentered(String text, float x, float y, Paint paint, Canvas canvas) {
        float xPos = x - (paint.measureText(text) / 2);
        float yPos = (y - ((paint.descent() + paint.ascent()) / 2));

        canvas.drawText(text, xPos, yPos, paint);
    }

    private float getScale(@Smiley int smile) {
        if (smile == mNearestSmile) {
            return mPlaceHolderScale;
        }
        return 0.80f;
    }

    public static String getSmileName(int smile) {
        switch (smile) {
            case BaseRating.BAD:
                return "Bad";
            case BaseRating.GOOD:
                return "Good";
            case BaseRating.GREAT:
                return "Great";
            case BaseRating.OKAY:
                return "Okay";
            case BaseRating.TERRIBLE:
                return "Terrible";
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mClickAnalyser.start(x, y);
                mFaceClickEngaged = isSmileyBounds(mFaceCenter.x, mFaceCenter.y,
                        x, y, mCenterY);
                mPrevX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                mClickAnalyser.move(x, y);
                if (mClickAnalyser.isMoved() && mFaceClickEngaged) {
                    moveSmile(mFaceCenter.x - (mPrevX - x));
                }
                mPrevX = x;
                break;
            case MotionEvent.ACTION_UP:
                mFaceClickEngaged = false;
                mClickAnalyser.stop(x, y);
                if (!mClickAnalyser.isMoved()) {
                    onClickView(x, y);
                } else {
                    positionSmile();
                }
                break;
        }
        return true;
    }

    private void positionSmile() {
        float currentPosition = mFaceCenter.x;
        float distance = Integer.MAX_VALUE;
        Point point = null;
        @Smiley
        int smile = GREAT;
        for (Integer s : mTouchPoints.keySet()) {
            Point p = mTouchPoints.get(s);
            float d = Math.abs(p.x - currentPosition);
            if (distance > d) {
                point = p;
                smile = s;
                distance = d;
            }
        }
        setSelectedSmile(smile, point, false, true);
    }

    private void moveSmile(float position) {
        float fraction = (position - mFromRange) / (mToRange - mFromRange);
        moveSmileByFraction(fraction);
    }

    private void moveSmileByFraction(float fraction) {
        if (fraction >= 0f && fraction <= 1f) {
            getSmiley(mSmileys, fraction, divisions, mFromRange, mToRange,
                    mFaceCenter, mSmilePath, mCenterY);
            invalidate();
        }
    }

    private void onClickView(float x, float y) {
        for (Integer smile : mTouchPoints.keySet()) {
            Point point = mTouchPoints.get(smile);
            boolean touched = isSmileyBounds(point.x, point.y, x, y, mCenterY);
            if (touched) {
                setSelectedSmile(smile, point, true, true);
            }
        }
    }

    private boolean isSmileyBounds(float cx, float cy, float tx, float ty, float width) {
        mTouchBounds.set(cx - width, 0, cx + width, getMeasuredHeight());
        return mTouchBounds.contains(tx, ty);
    }

    public void setSelectedSmile(@Smiley int smile) {
        setSelectedSmile(smile, false);
    }

    public void setSelectedSmile(@Smiley int smile, boolean animate) {
        mPendingActionSmile = smile;
        setSelectedSmile(smile, mTouchPoints.get(smile), true, animate);
    }

    private void setSelectedSmile(@Smiley int smile, Point point, boolean check, boolean animate) {
        if (mSelectedSmile == smile && check) {
            return;
        }
        if (mFaceCenter == null || point == null) {
            mSelectedSmile = smile;
            return;
        }
        mSelectedSmile = smile;
        mValueAnimator.setFloatValues(mFaceCenter.x, point.x);
        if (animate) {
            mValueAnimator.start();
        } else {
            moveSmile(point.x);
            // notifyListener();
        }
    }

    @Smiley
    public int getSelectedSmile() {
        return mSelectedSmile;
    }

    public int getRating() {
        return getSelectedSmile() + 1;
    }

    /**
     * Evaluates click actions using touch events
     */
    protected static class ClickAnalyser {

        private static final int MAX_CLICK_DISTANCE = 20;
        private static final int MAX_CLICK_DURATION = 200;

        private float mPressX;
        private float mPressY;
        private final float mDensity;
        private long mPressStartTime;
        private boolean mMoved = false;
        private boolean mClickEventOccured = true;

        public ClickAnalyser(float density) {
            mDensity = density;
        }

        public static ClickAnalyser newInstance(float density) {
            return new ClickAnalyser(density);
        }

        public void start(float x, float y) {
            mPressX = x;
            mPressY = y;
            mMoved = false;
            mClickEventOccured = true;
            mPressStartTime = System.currentTimeMillis();
        }

        /**
         * returns long press
         *
         * @param x
         * @param y
         * @return
         */
        public void move(float x, float y) {
            float dist = distance(mPressX, mPressY, x, y);
            long time = System.currentTimeMillis() - mPressStartTime;
            if (!mMoved && dist > MAX_CLICK_DISTANCE) {
                mMoved = true;
            }
            if ((time) > MAX_CLICK_DURATION || mMoved) {
                mClickEventOccured = false;
            }
        }

        public boolean stop(float x, float y) {
            move(x, y);
            return mClickEventOccured;
        }

        private float distance(float x1, float y1, float x2, float y2) {
            float dx = x1 - x2;
            float dy = y1 - y2;
            float distanceInPx = (float) Math.sqrt(dx * dx + dy * dy);
            return pxToDp(distanceInPx);
        }

        public boolean isMoved() {
            return mMoved;
        }

        private float pxToDp(float px) {
            return px / mDensity;
        }
    }

    private void getSmiley(Smileys smileys, float fraction, float divisions, float fromRange,
                           float toRange, Point point, Path smilePath,
                           float centerY) {
        if (smileys == null) {
            return;
        }
        float actualTranslation = mFloatEvaluator.evaluate(fraction, fromRange, toRange);
        point.x = actualTranslation;
        float trans = actualTranslation - centerY;
        if (fraction > 0.75f) {
            fraction -= 0.75f;
            fraction *= 4;
            findNearestSmile(fraction, GOOD, GREAT);
            mBackgroundPaint.setColor(mNormalColor);
            transformSmile(trans, fraction, smilePath,
                    smileys.getSmile(GOOD), smileys.getSmile(GREAT), mFloatEvaluator);
            createEyeLocation(smileys, divisions, fraction, actualTranslation, GREAT, smilePath, smilePath, centerY);
        } else if (fraction > 0.50f) {
            fraction -= 0.50f;
            fraction *= 4;
            findNearestSmile(fraction, OKAY, GOOD);
            mBackgroundPaint.setColor(mNormalColor);
            transformSmile(trans, fraction, smilePath,
                    smileys.getSmile(OKAY), smileys.getSmile(GOOD), mFloatEvaluator);
            createEyeLocation(smileys, divisions, fraction, actualTranslation, GOOD, smilePath, smilePath, centerY);
        } else if (fraction > 0.25f) {
            fraction -= 0.25f;
            fraction *= 4;
            findNearestSmile(fraction, BAD, OKAY);
            mBackgroundPaint.setColor(mNormalColor);
            transformSmile(trans, fraction, smilePath,
                    smileys.getSmile(BAD), smileys.getSmile(OKAY), mFloatEvaluator);
            createEyeLocation(smileys, divisions, fraction, actualTranslation, BAD, smilePath, smilePath, centerY);
        } else {
            fraction *= 4;
            findNearestSmile(fraction, TERRIBLE, BAD);
            mBackgroundPaint.setColor((Integer) mColorEvaluator.evaluate(fraction, mAngryColor, mNormalColor));
            transformSmile(trans, fraction, smilePath,
                    smileys.getSmile(TERRIBLE), smileys.getSmile(BAD), mFloatEvaluator);
            createEyeLocation(smileys, divisions, fraction, actualTranslation, TERRIBLE, smilePath, smilePath, centerY);
        }
    }

    private void findNearestSmile(float fraction, @Smiley int leftSmile, @Smiley int rightSmile) {
        if (fraction < 0.5f) {
            mPlaceHolderScale = limitNumberInRange(fraction * 2);
            mNearestSmile = leftSmile;
        } else {
            mPlaceHolderScale = limitNumberInRange(1f - (fraction - 0.5f) * 2);
            mNearestSmile = rightSmile;
        }
    }

    private float limitNumberInRange(float num) {
        // The range is going to be in between 0 to 0.80
        num *= 0.80f;
        return num;
    }

    private void createEyeLocation(Smileys smileys, float divisions, float fraction, float actualTranslation, @Smiley int smile, Path leftEye, Path rightEye, float centerY) {
        Eye eyeLeft = EyeEmotion.prepareEye(smileys.getEye(Eye.LEFT), mFloatEvaluator, fraction, smile);
        Eye eyeRight = EyeEmotion.prepareEye(smileys.getEye(Eye.RIGHT), mFloatEvaluator, fraction, smile);
        eyeLeft.radius = divisions * 2.5f;
        eyeRight.radius = divisions * 2.5f;
        eyeLeft.center.x = (divisions * 11f) + actualTranslation - centerY;
        eyeLeft.center.y = centerY * 0.70f;
        eyeRight.center.x = (divisions * 21f) + actualTranslation - centerY;
        eyeRight.center.y = centerY * 0.70f;
        eyeLeft.fillPath(leftEye);
        eyeRight.fillPath(rightEye);
    }

    private static class Face {
        Point place = new Point();
        Path smile = new Path();
        @Smiley
        int smileType;
        /*Path leftEye = new Path();
        Path rightEye = new Path();*/
    }

    public interface OnSmileySelectionListener {
        void onSmileySelected(@Smiley int smiley);
    }

    public interface OnRatingSelectedListener {
        void onRatingSelected(int level);
    }
}
