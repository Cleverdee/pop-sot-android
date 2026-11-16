package com.cleverdee.pop3;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/* The launcher screens' look, in the spirit of the game's own front end: a charcoal night over a burning city
   (an ember glow and a skyline of domes and towers), thin ornate gold frames, and classical serif small capitals
   in burnished gold. Everything is drawn here; nothing is taken from the game's files. */
final class Ui {
    static final int WHITE = Color.rgb(250, 240, 220), SOFT = Color.rgb(226, 210, 180), DIM = Color.rgb(160, 138, 104);
    static final int GOLD = Color.rgb(214, 168, 88), GOLD_LIGHT = Color.rgb(252, 228, 164), GOLD_DARK = Color.rgb(122, 80, 32);
    static final int EMBER = Color.rgb(232, 112, 36);
    static final Typeface SERIF = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
    static final Typeface SERIF_BOLD = Typeface.create(Typeface.SERIF, Typeface.BOLD);

    private Ui() {}

    /* A page: the backdrop, the title and text column on the left (returned), the menu column on the right
       (side(): buttons go there by themselves), and the credit along the bottom left. Both columns scroll. */
    static LinearLayout page(Activity a) {
        FrameLayout frame = new FrameLayout(a);
        frame.setBackground(new Backdrop(a.getResources().getDisplayMetrics().density));
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout left = column(a), right = column(a);
        left.setTag(right);
        row.addView(scroller(a, left), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.1f));
        row.addView(scroller(a, right), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        frame.addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView credit = new TextView(a);
        credit.setText("Made by Cleverdee");
        credit.setTextSize(11);
        credit.setTextColor(Color.argb(160, 214, 168, 88));
        credit.setTypeface(SERIF);
        credit.setLetterSpacing(0.12f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                   ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        lp.bottomMargin = dp(a, 22);
        lp.leftMargin = dp(a, 30);
        frame.addView(credit, lp);
        a.setContentView(frame);
        return left;
    }

    /* empties both columns of a page */
    static void clear(LinearLayout col) {
        side(col).removeAllViews();
        col.removeAllViews();
    }

    /* the menu column of a page */
    static LinearLayout side(LinearLayout col) {
        return col.getTag() instanceof LinearLayout ? (LinearLayout) col.getTag() : col;
    }

    private static LinearLayout column(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(dp(c, 24), dp(c, 18), dp(c, 24), dp(c, 30));
        return col;
    }

    private static ScrollView scroller(Context c, View content) {
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        if (android.os.Build.VERSION.SDK_INT >= 26) sv.setDefaultFocusHighlightEnabled(false);
        sv.addView(content);
        return sv;
    }

    /* "PRINCE OF PERSIA" over "THE TWO THRONES" in gold serif small caps, with a small line under it */
    static void title(LinearLayout col, String sub) {
        Context c = col.getContext();
        col.addView(new Logo(c), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 96)));
        if (sub != null) {
            TextView t = text(col, sub.toUpperCase(java.util.Locale.US), 11, DIM);
            t.setTypeface(SERIF);
            t.setLetterSpacing(0.45f);
        }
    }

    /* a heading for a sub-page */
    static TextView heading(LinearLayout col, String s) {
        TextView t = text(col, s.toUpperCase(java.util.Locale.US), 20, GOLD);
        t.setTypeface(SERIF_BOLD);
        t.setLetterSpacing(0.1f);
        return t;
    }

    static TextView text(LinearLayout col, String s, int sp, int color) {
        Context c = col.getContext();
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(SERIF);
        t.setShadowLayer(dp(c, 3), 0, dp(c, 1), Color.argb(220, 0, 0, 0));
        t.setPadding(0, dp(c, 4), 0, dp(c, 4));
        t.setMaxWidth(dp(c, 600));
        col.addView(t);
        return t;
    }

    /* a menu item: a dark panel in a thin double gold frame with gold capitals; it glows like embers while pressed */
    static MenuButton button(LinearLayout col, String s, View.OnClickListener l) {
        return button(col, s, false, l);
    }

    /* small: a secondary action, narrower and quieter */
    static MenuButton button(LinearLayout col, String s, boolean small, View.OnClickListener l) {
        Context c = col.getContext();
        col = side(col);
        MenuButton b = new MenuButton(c, s, small);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, small ? 250 : 300), dp(c, small ? 40 : 48));
        lp.topMargin = dp(c, small ? 8 : 10);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(b, lp);
        return b;
    }

    static int dp(Context c, float v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    /* Small capitals: each word's first letter full size, the rest at 78%. Returns the width; draws when c != null. */
    static float smallCaps(Canvas c, String s, float x, float base, float size, Paint p) {
        float x0 = x;
        boolean first = true;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            String one = String.valueOf(ch);
            boolean minor = first && s.startsWith("OF ", i);   /* "of" stays small */
            p.setTextSize(first && !minor ? size : size * 0.78f);
            if (c != null) c.drawText(one, x, base, p);
            x += p.measureText(one);
            first = ch == ' ';
        }
        p.setTextSize(size);
        return x - x0;
    }

    /* a small diamond, the ornament used on the frames and rules */
    static void diamond(Canvas c, float cx, float cy, float r, Paint p) {
        Path d = new Path();
        d.moveTo(cx, cy - r);
        d.lineTo(cx + r * 0.7f, cy);
        d.lineTo(cx, cy + r);
        d.lineTo(cx - r * 0.7f, cy);
        d.close();
        c.drawPath(d, p);
    }

    /* ------------------------------------------------------------------ the backdrop */

    static final class Backdrop extends Drawable {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG), mLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mPath = new Path();
        private final float mDp;

        Backdrop(float density) { mDp = density; }

        @Override public void draw(Canvas c) {
            int w = getBounds().width(), h = getBounds().height();
            /* charcoal night, warming towards the burning city below */
            mPaint.setShader(new LinearGradient(0, 0, 0, h, new int[] { Color.rgb(8, 7, 7), Color.rgb(16, 11, 9), Color.rgb(40, 18, 8),
                                                                         Color.rgb(92, 36, 10) },
                                                new float[] { 0f, 0.45f, 0.8f, 1f }, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mPaint);
            /* fire glow low on the horizon */
            mPaint.setShader(new RadialGradient(w * 0.38f, h * 1.08f, Math.max(w, h) * 0.55f,
                                                Color.argb(150, 240, 110, 30), Color.argb(0, 240, 110, 30), Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mPaint);
            mPaint.setShader(new RadialGradient(w * 0.85f, h * 1.02f, Math.max(w, h) * 0.35f,
                                                Color.argb(90, 255, 150, 50), Color.argb(0, 255, 150, 50), Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mPaint);
            mPaint.setShader(null);
            /* drifting smoke */
            mLine.setStyle(Paint.Style.STROKE);
            mLine.setStrokeWidth(Math.max(1f, h / 300f));
            for (int i = 0; i < 5; i++) {
                float y0 = h * (0.5f + i * 0.08f), lift = h * (0.12f + i * 0.02f);
                mPath.reset();
                mPath.moveTo(-w * 0.1f, y0);
                mPath.cubicTo(w * 0.3f, y0 - lift, w * 0.6f, y0 + lift * 0.5f, w * 1.1f, y0 - lift * 0.4f);
                mLine.setColor(Color.argb(10 + i * 3, 255, 170, 90));
                c.drawPath(mPath, mLine);
            }
            skyline(c, w, h);
            embers(c, w, h);
            frame(c, w, h);
        }

        /* the city: domes, towers and walls in silhouette along the bottom, rimmed by the fire behind */
        private void skyline(Canvas c, int w, int h) {
            float base = h, u = h / 100f;
            mPath.reset();
            mPath.moveTo(0, base);
            float[][] parts = {   /* x (fraction of width), kind, width (u), height (u) */
                { 0.00f, 0, 9, 7 }, { 0.05f, 2, 3, 20 }, { 0.09f, 1, 7, 12 }, { 0.17f, 0, 10, 9 }, { 0.26f, 2, 2.5f, 26 },
                { 0.31f, 1, 11, 17 }, { 0.42f, 0, 14, 8 }, { 0.53f, 1, 8, 13 }, { 0.61f, 2, 3, 22 }, { 0.66f, 0, 9, 10 },
                { 0.74f, 1, 13, 19 }, { 0.86f, 2, 2.5f, 16 }, { 0.90f, 0, 12, 8 }, { 1.00f, 0, 1, 6 } };
            for (float[] p : parts) {
                float x = p[0] * w, bw = p[2] * u, bh = p[3] * u;
                mPath.lineTo(x, base - bh * 0.45f);
                if (p[1] == 1) {          /* a domed hall */
                    mPath.lineTo(x, base - bh * 0.6f);
                    mPath.cubicTo(x, base - bh * 1.05f, x + bw * 0.5f, base - bh * 1.05f, x + bw * 0.5f, base - bh * 1.25f);
                    mPath.cubicTo(x + bw * 0.5f, base - bh * 1.05f, x + bw, base - bh * 1.05f, x + bw, base - bh * 0.6f);
                    mPath.lineTo(x + bw, base - bh * 0.45f);
                } else if (p[1] == 2) {   /* a minaret */
                    mPath.lineTo(x, base - bh * 0.85f);
                    mPath.lineTo(x - bw * 0.25f, base - bh * 0.88f);
                    mPath.lineTo(x + bw * 0.5f, base - bh * 1.05f);
                    mPath.lineTo(x + bw * 1.25f, base - bh * 0.88f);
                    mPath.lineTo(x + bw, base - bh * 0.85f);
                    mPath.lineTo(x + bw, base - bh * 0.45f);
                } else {                  /* a wall with crenels */
                    float y = base - bh, step = bw / 5f;
                    mPath.lineTo(x, y);
                    for (int k = 0; k < 5; k++) {
                        mPath.lineTo(x + step * k + step * 0.5f, y);
                        mPath.lineTo(x + step * k + step * 0.5f, y + u * 0.8f);
                        mPath.lineTo(x + step * (k + 1), y + u * 0.8f);
                        mPath.lineTo(x + step * (k + 1), y);
                    }
                    mPath.lineTo(x + bw, base - bh * 0.45f);
                }
            }
            mPath.lineTo(w, base);
            mPath.close();
            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.rgb(12, 7, 5));
            c.drawPath(mPath, mPaint);
            mLine.setStrokeWidth(Math.max(1f, mDp * 0.8f));
            mLine.setColor(Color.argb(90, 255, 140, 50));
            c.drawPath(mPath, mLine);
        }

        /* sparks rising from the fire */
        private void embers(Canvas c, int w, int h) {
            java.util.Random r = new java.util.Random(7);
            mPaint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 46; i++) {
                float up = r.nextFloat();
                float x = r.nextFloat() * w, y = h - h * 0.7f * up * up;   /* most of them low, near the fire */
                float rad = mDp * (0.6f + r.nextFloat() * 1.4f);
                int a = (int) ((60 + r.nextInt(150)) * (1f - up * 0.6f));
                mPaint.setColor(Color.argb(a, 255, 150 + r.nextInt(80), 60));
                c.drawCircle(x, y, rad, mPaint);
            }
        }

        /* a thin double gold frame around the screen with diamonds at the corners */
        private void frame(Canvas c, int w, int h) {
            float m = mDp * 10, m2 = mDp * 14, cr = mDp * 4;
            mLine.setStrokeWidth(Math.max(1f, mDp * 1f));
            mLine.setColor(Color.argb(150, 214, 168, 88));
            c.drawRect(m, m, w - m, h - m, mLine);
            mLine.setStrokeWidth(Math.max(1f, mDp * 0.6f));
            mLine.setColor(Color.argb(80, 214, 168, 88));
            c.drawRect(m2, m2, w - m2, h - m2, mLine);
            mPaint.setColor(Color.argb(200, 230, 186, 104));
            diamond(c, m, m, cr, mPaint);
            diamond(c, w - m, m, cr, mPaint);
            diamond(c, m, h - m, cr, mPaint);
            diamond(c, w - m, h - m, cr, mPaint);
            diamond(c, w / 2f, m, cr * 0.8f, mPaint);
            diamond(c, w / 2f, h - m, cr * 0.8f, mPaint);
        }

        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter f) {}
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    /* ------------------------------------------------------------------ the title */

    static final class Logo extends View {
        private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG), mRule = new Paint(Paint.ANTI_ALIAS_FLAG);

        Logo(Context c) {
            super(c);
            mText.setTypeface(SERIF_BOLD);
            mText.setShadowLayer(dp(c, 5), 0, dp(c, 2), Color.argb(230, 0, 0, 0));
            setLayerType(LAYER_TYPE_SOFTWARE, null);   /* text shadows */
        }

        private Shader gold(float top, float bottom) {
            return new LinearGradient(0, top, 0, bottom, new int[] { GOLD_LIGHT, Color.rgb(232, 186, 100), GOLD_DARK, Color.rgb(200, 150, 70) },
                                      new float[] { 0f, 0.45f, 0.62f, 1f }, Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            /* PRINCE OF PERSIA, as wide as fits */
            String a = "PRINCE OF PERSIA", b = "THE TWO THRONES";
            float size = h * 0.4f;
            mText.setLetterSpacing(0.08f);
            mText.setShader(null);
            float wa = smallCaps(null, a, 0, 0, size, mText);
            if (wa > w * 0.96f) { size *= w * 0.96f / wa; wa = smallCaps(null, a, 0, 0, size, mText); }
            float base = h * 0.44f;
            mText.setShader(gold(base - size * 0.75f, base));
            smallCaps(c, a, (w - wa) / 2f, base, size, mText);
            /* the ornament: two fading gold lines meeting at a diamond */
            float ry = h * 0.58f, half = wa * 0.42f, cx = w / 2f;
            mRule.setShader(new LinearGradient(cx - half, 0, cx, 0, Color.argb(0, 214, 168, 88), GOLD, Shader.TileMode.CLAMP));
            c.drawRect(cx - half, ry - 0.6f, cx - size * 0.25f, ry + 0.6f + h * 0.006f, mRule);
            mRule.setShader(new LinearGradient(cx, 0, cx + half, 0, GOLD, Color.argb(0, 214, 168, 88), Shader.TileMode.CLAMP));
            c.drawRect(cx + size * 0.25f, ry - 0.6f, cx + half, ry + 0.6f + h * 0.006f, mRule);
            mRule.setShader(null);
            mRule.setColor(GOLD_LIGHT);
            diamond(c, cx, ry, size * 0.14f, mRule);
            mRule.setColor(EMBER);
            diamond(c, cx, ry, size * 0.06f, mRule);
            /* THE TWO THRONES, smaller and wider spaced */
            float sb = size * 0.48f;
            mText.setLetterSpacing(0.38f);
            mText.setShader(null);
            float wb = smallCaps(null, b, 0, 0, sb, mText);
            float bb = h * 0.84f;
            mText.setShader(gold(bb - sb * 0.75f, bb));
            smallCaps(c, b, (w - wb) / 2f + sb * 0.19f, bb, sb, mText);
            mText.setShader(null);
        }
    }

    /* ------------------------------------------------------------------ menu buttons */

    static final class MenuButton extends View {
        private final String mLabel;
        private final boolean mSmall;
        private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG), mText = new Paint(Paint.ANTI_ALIAS_FLAG),
                            mEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mPath = new Path(), mInner = new Path();
        private boolean mDown;
        private String mNote;   /* optional right-hand detail, e.g. ON / OFF */

        MenuButton(Context c, String label, boolean small) {
            super(c);
            mLabel = label.toUpperCase(java.util.Locale.US);
            mSmall = small;
            mText.setTypeface(SERIF_BOLD);
            mEdge.setStyle(Paint.Style.STROKE);
            setClickable(true);
            setFocusable(true);
        }

        void setNote(String note) { mNote = note; invalidate(); }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: mDown = true; invalidate(); break;
            case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL: mDown = false; invalidate(); break;
            }
            return super.onTouchEvent(e);
        }

        @Override public void setEnabled(boolean on) { super.setEnabled(on); invalidate(); }

        /* a long panel with clipped corners, pointed at both ends */
        private static void panel(Path p, float l, float t, float r, float b) {
            float m = (b - t) / 2f, cut = (b - t) * 0.32f;
            p.reset();
            p.moveTo(l, t + m);
            p.lineTo(l + cut, t);
            p.lineTo(r - cut, t);
            p.lineTo(r, t + m);
            p.lineTo(r - cut, b);
            p.lineTo(l + cut, b);
            p.close();
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density, inset = d * 2;
            panel(mPath, inset, inset, w - inset, h - inset);
            float in2 = inset + d * 3.5f;
            panel(mInner, in2, in2, w - in2, h - in2);
            boolean lit = mDown || isFocused();
            if (lit) mFill.setShader(new LinearGradient(0, 0, 0, h, new int[] { Color.rgb(255, 214, 130), Color.rgb(226, 132, 40), Color.rgb(150, 60, 14) },
                                                         null, Shader.TileMode.CLAMP));
            else mFill.setShader(new LinearGradient(0, 0, 0, h, Color.argb(225, 30, 22, 17), Color.argb(235, 10, 7, 6), Shader.TileMode.CLAMP));
            c.drawPath(mPath, mFill);
            mFill.setShader(null);
            /* the gold frame: a stronger outer line, a fine inner one */
            mEdge.setStrokeWidth(Math.max(1f, d * 1.3f));
            mEdge.setShader(new LinearGradient(0, 0, 0, h, GOLD_LIGHT, GOLD_DARK, Shader.TileMode.CLAMP));
            c.drawPath(mPath, mEdge);
            mEdge.setShader(null);
            mEdge.setStrokeWidth(Math.max(1f, d * 0.6f));
            mEdge.setColor(lit ? Color.argb(160, 90, 40, 10) : Color.argb(110, 214, 168, 88));
            c.drawPath(mInner, mEdge);
            /* diamonds at the points */
            mFill.setColor(lit ? Color.rgb(90, 36, 8) : GOLD);
            diamond(c, in2 + h * 0.2f, h / 2f, h * 0.1f, mFill);
            diamond(c, w - in2 - h * 0.2f, h / 2f, h * 0.1f, mFill);
            int alpha = isEnabled() ? 255 : 110;
            float size = h * (mSmall ? 0.36f : 0.38f);
            mText.setLetterSpacing(0.14f);
            mText.setTextSize(size);
            int fg = lit ? Color.argb(alpha, 40, 16, 4) : Color.argb(alpha, 246, 222, 160);
            mText.setColor(fg);
            float ty = h / 2f - (mText.descent() + mText.ascent()) / 2f + h * 0.02f;
            mText.setTextAlign(Paint.Align.LEFT);
            float lw = smallCaps(null, mLabel, 0, 0, size, mText);
            if (mNote == null) {
                smallCaps(c, mLabel, (w - lw) / 2f, ty, size, mText);
            } else {
                smallCaps(c, mLabel, h * 0.75f, ty, size, mText);
                mText.setTextAlign(Paint.Align.RIGHT);
                mText.setColor(lit ? fg : Color.argb(alpha, 232, 112, 36));
                c.drawText(mNote, w - h * 0.75f, ty, mText);
                mText.setTextAlign(Paint.Align.LEFT);
            }
        }
    }
}
