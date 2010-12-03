package com.corn3lius.android.HexWallpaper;

import java.util.Random;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;

import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class HexWallpaper extends WallpaperService {

	public static final String SHARED_PREFS_NAME = "hexsettings";
	public static final int xCount = 25;
	public static final int yCount = 20;

	public enum Direction {
		UP, UPRIGHT, DOWNRIGHT, DOWN, DOWNLEFT, UPLEFT, NONE
	}

	/**
	 * This class holds the status of the moving pieces on the screen
	 * 
	 * @author mark
	 */
	static class HexLight {
		public Point point;
		public Direction dir;
		public boolean onscreen;

		HexLight() {
			this.point = new Point();
			this.dir = Direction.NONE;
			this.onscreen = false;
		}

		public void setDir(int value) {
			if (value == 0) {
				this.dir = Direction.UP;
			} else if (value == 1) {
				this.dir = Direction.UPRIGHT;
			} else if (value == 2) {
				this.dir = Direction.DOWNRIGHT;
			} else if (value == 3) {
				this.dir = Direction.DOWN;
			} else if (value == 4) {
				this.dir = Direction.DOWNLEFT;
			} else if (value == 5) {
				this.dir = Direction.UPLEFT;
			} else {
				this.dir = Direction.NONE;
				Log.e("set dir", "Set direction to none");
			}
		}
	}

	private final Handler mHandler = new Handler();

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// / This is set up for the class HexEngine
	// / where all the code for the wall paper is.
	@Override
	public Engine onCreateEngine() {
		return new HexEngine();
	}

	class HexEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener {

		private final Paint mPaint = new Paint();
		private float mOffset;
		private float mTouchX = -1;
		private float mTouchY = -1;
		private long mStartTime;
		private long mTouchTime = 0;
		private float mCenterX;
		private float mCenterY;
		private Bitmap mHexTile;
		private Bitmap mHexHole;
		private int hexLightCount = 6;
		private HexLight[] hexLight = new HexLight[hexLightCount];
		private int[] aAlpha = new int[xCount * yCount];
		private int bg_color = Color.CYAN;
		private int bg_bright = 196;
		private final Runnable mDraw = new Runnable() {
			@Override
			public void run() {
				drawFrame();
			}
		};
		private boolean mVisible;
		private boolean mMoveHex = false;
		private SharedPreferences mPrefs;

		/**
		 * Everyone needs a little randomness in their life
		 */
		private final Random RNG = new Random();

		HexEngine() {
			final Paint paint = mPaint;
			paint.setColor(0xffffffff);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);

			mStartTime = SystemClock.elapsedRealtime();
			mPrefs = HexWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME,
					0);
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPrefs, null);
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {

			Log.i("onPref", " get color ");
			String color = prefs.getString("bg_colors", "Black");
			int colorId = getResources().getIdentifier(color, "color",
					getPackageName());
			String bright = prefs.getString("bg_bright", "90");
			bg_bright = Integer.parseInt(bright,16);
			// combine     A                RGB
			bg_color = (bg_bright << 24) + getResources().getColor(colorId);
			
			Log.i("onPref", "color " + bg_color);
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);

			// By default we don't get touch events, so enable them.
			setTouchEventsEnabled(true);
			Resources res = getResources();
			mHexTile = BitmapFactory.decodeResource(res, R.drawable.hex_tile);
			mHexHole = BitmapFactory.decodeResource(res, R.drawable.hex_hole);
			for (int i = 0; i < hexLightCount; i++) {
				hexLight[i] = startHexLight();
			}
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mDraw);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				drawFrame();
			} else {
				mHandler.removeCallbacks(mDraw);
			}

			Log.i("onVisibility", "visible " + mVisible);
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			mCenterX = width / 2.0f;
			mCenterY = height / 2.0f;
			Log.i("onSurfaceChange", "Center " + mCenterX + " " + mCenterY);
			drawFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDraw);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			mOffset = xPixels;

			Log.i("onOffset", xOffset + " - " + yOffset + " | " + xStep + " - "
					+ yStep + " | " + xPixels + " - " + yPixels);
			drawFrame();
		}

		/*
		 * Store the position of the touch event so we can use it for drawing
		 * later
		 */
		@Override
		public void onTouchEvent(MotionEvent event) {
			// if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mTouchX = event.getX();
				mTouchY = event.getY();
				mTouchTime = 16;
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mTouchX = event.getX();
				mTouchY = event.getY();
				mTouchTime = 16;
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				mTouchX = event.getX();
				mTouchY = event.getY();
				mTouchTime = 16;
			}
			super.onTouchEvent(event);
		}

		/*
		 * /* Draw one frame of the animation. This method gets called
		 * repeatedly by posting a delayed Runnable. You can do any drawing you
		 * want in here.
		 */
		void drawFrame() {
			/*
			 * add code to make lights and stuff for the background
			 */
			final SurfaceHolder holder = getSurfaceHolder();

			updateTime();

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					// draw something
					drawHex(c);
					updateHex();
					// drawTouchPoint(c);
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}
			// Reschedule the next redraw
			mHandler.removeCallbacks(mDraw);
			if (mVisible) {
				mHandler.postDelayed(mDraw, 1000 / 25);
			}
		}

		void drawHex(Canvas c) {
			c.save();
			c.drawColor(Color.BLACK);
			c.drawColor(bg_color);
			// mPaint.setColor(Color.WHITE);
			for (int y = 0; y < yCount; y++) {
				for (int x = 0; x < xCount; x++) {
					// 47 X 41 bitmap size
					int index = x + (y * xCount);
					float mX = (x * 38) - 250 - (mOffset / 2);
					float mY = (y * 44) + (22 * (x & 0x1));

					float distance = (float) Math
							.sqrt(((mX - (mTouchX - 24)) * (mX - (mTouchX - 24)))
									+ ((mY - (mTouchY - 22)) * (mY - (mTouchY - 22))));

					for (int i = 0; i < hexLightCount; i++) {
						if ((hexLight[i].onscreen)
								&& (hexLight[i].point.x == x)
								&& (hexLight[i].point.y == y)) {
							aAlpha[index] += 186;
						}
					}

					aAlpha[index] += (int) ((mTouchTime * 124) / distance);
					if (aAlpha[index] > 255)
						aAlpha[index] = 255;
					if (aAlpha[index] <= 16)
						aAlpha[index] = 0;
					else
						aAlpha[index] -= 16;

					mPaint.setAlpha(255 - aAlpha[index]);
					c.drawBitmap(mHexHole, mX, mY, mPaint);
					mPaint.setAlpha(aAlpha[index]);
					c.drawBitmap(mHexTile, mX, mY, mPaint);
				}
			}
			c.restore();
		}

		/*
		 * Draw a circle around the current touch point, if any.
		 */
		void drawTouchPoint(Canvas c) {

			if (mTouchX >= 0 && mTouchY >= 0) {
				c.drawCircle(mTouchX, mTouchY, 20, mPaint);
			}
		}

		/*
		 * 
		 */
		void updateTime() {
			if (mTouchTime > 0)
				mTouchTime--;

			if ((mStartTime + 5000) < SystemClock.elapsedRealtime()) {
				Log.i("Timer", "Five seconds tick");
				mStartTime = SystemClock.elapsedRealtime();
			}
		}

		void updateHex() {
			mMoveHex = !mMoveHex;
			if (!mMoveHex) {
				return;
			}
			for (int i = 0; i < hexLightCount; i++) {
				if (hexLight[i].dir == Direction.NONE) {
					if ((RNG.nextInt(19) % 3) == 0)
						hexLight[i] = startHexLight();
				} else {
					hexLight[i].point = getNextCell(hexLight[i].point,
							hexLight[i].dir);
					if ((hexLight[i].point.x >= xCount)
							|| (hexLight[i].point.y >= yCount)
							|| (hexLight[i].point.x < 0)
							|| (hexLight[i].point.y < 0)) {
						hexLight[i].onscreen = false;
						hexLight[i].dir = Direction.NONE;
						hexLight[i].point = new Point(0, 0);
					}
				}
			}
		}

		HexLight startHexLight() {
			HexLight hl = new HexLight();
			hl.setDir(RNG.nextInt(6));
			int newy = RNG.nextInt(yCount);
			int newx = RNG.nextInt(xCount);
			boolean notSide = (RNG.nextInt(11) % 10) == 0;
			switch (hl.dir) {
			case UP:
				newy = yCount - 1;
				break;
			case DOWN:
				newy = 0;
				break;
			case DOWNLEFT:
				newx = (notSide) ? newx : xCount - 1;
				newy = (notSide) ? 0 : newy;
				break;
			case UPLEFT:
				newx = (notSide) ? newx : xCount - 1;
				newy = (notSide) ? yCount - 1 : newy;
				break;
			case DOWNRIGHT:
				newx = (notSide) ? newx : 0;
				newy = (notSide) ? 0 : newy;
				break;
			case UPRIGHT:
				newx = (notSide) ? newx : 0;
				newy = (notSide) ? yCount - 1 : newy;
				break;

			}
			// Log.i("set point", String.format("notSide - %b  @ (%d,%d) - %s",
			// notSide, newx, newy, hl.dir.toString()));
			hl.point = new Point(newx, newy);
			hl.onscreen = true;
			return hl;

		}

		Point getNextCell(Point p, Direction dir) {
			Point lPoint = new Point(p);
			int yofx = (p.x % 2);
			switch (dir) {
			case UP:
				lPoint.offset(0, -1);
				break;
			case UPRIGHT:
				lPoint.offset(1, yofx - 1);
				break;
			case UPLEFT:
				lPoint.offset(-1, yofx - 1);
				break;
			case DOWN:
				lPoint.offset(0, 1);
				break;
			case DOWNRIGHT:
				lPoint.offset(1, yofx);
				break;
			case DOWNLEFT:
				lPoint.offset(-1, yofx);
				break;
			}
			// Log.i("updateHex", String.format("(%d,%d) to (%d,%d)", p.x, p.x,
			// lPoint.x, lPoint.y));
			return lPoint;
		}

	}
}