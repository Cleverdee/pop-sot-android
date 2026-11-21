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

/* The launcher screens' look, in the spirit of The Sands of Time's own front end: a moonlit night over the palace
   of Azad (a skyline of domes, minarets and arches), a faint hourglass in the sky, drifting sand, thin sand-gold
   frames with turquoise diamonds, and classical serif small capitals. Everything is drawn here; nothing is taken
   from the game's files. */
final class Ui {
    /* Sands of Time: sand gold metal on night blue, the turquoise glow of the Sands as the accent (EMBER) */
    static final int WHITE = Color.rgb(248, 240, 222), SOFT = Color.rgb(226, 212, 184), DIM = Color.rgb(150, 140, 120);
    static final int GOLD = Color.rgb(222, 178, 96), GOLD_LIGHT = Color.rgb(255, 234, 178), GOLD_DARK = Color.rgb(108, 72, 28);
    static final int EMBER = Color.rgb(62, 200, 206);
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
        credit.setTextColor(Color.argb(170, 222, 178, 96));
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

    /* "PRINCE OF PERSIA" in turquoise over "THE SANDS OF TIME" in gold, serif small caps, a small rule between */
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

    /* a menu item: a dark panel in a thin double gold frame with gold capitals; it glows like the Sands while pressed */
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
            sky(c, w, h);
            hourglass(c, w, h);
            palace(c, w, h);
            sand(c, w, h);
            /* shadow closing in from the edges */
            mPaint.setShader(new RadialGradient(w * 0.5f, h * 0.42f, Math.max(w, h) * 0.75f,
                                                new int[] { Color.argb(0, 0, 0, 0), Color.argb(90, 0, 0, 0), Color.argb(235, 0, 0, 0) },
                                                new float[] { 0f, 0.55f, 1f }, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mPaint);
            mPaint.setShader(null);
            frame(c, w, h);
        }

        /* a moonlit night: deep blue sky with a turquoise haze low down, a large pale moon and thin cloud */
        private void sky(Canvas c, int w, int h) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShader(new LinearGradient(0, 0, 0, h, new int[] { Color.rgb(5, 9, 22), Color.rgb(14, 28, 52), Color.rgb(26, 52, 72), Color.rgb(10, 18, 30) },
                                                new float[] { 0f, 0.45f, 0.78f, 1f }, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mPaint);
            float mx = w * 0.29f, my = h * 0.17f, mr = h * 0.095f;
            mPaint.setShader(new RadialGradient(mx, my, mr * 4.5f, new int[] { Color.argb(90, 170, 210, 220), Color.argb(0, 170, 210, 220) },
                                                null, Shader.TileMode.CLAMP));
            c.drawCircle(mx, my, mr * 4.5f, mPaint);
            mPaint.setShader(new RadialGradient(mx - mr * 0.3f, my - mr * 0.3f, mr * 1.3f, Color.rgb(236, 240, 236), Color.rgb(176, 196, 204),
                                                Shader.TileMode.CLAMP));
            c.drawCircle(mx, my, mr, mPaint);
            mPaint.setShader(null);
            java.util.Random r = new java.util.Random(3);
            for (int i = 0; i < 9; i++) {   /* long thin clouds drifting across the moon's light */
                float cy = h * (0.08f + r.nextFloat() * 0.45f), cx = r.nextFloat() * w, cw = w * (0.2f + r.nextFloat() * 0.35f);
                mPaint.setColor(Color.argb(18 + r.nextInt(22), 120, 150, 176));
                c.drawOval(cx - cw / 2, cy - h * 0.012f, cx + cw / 2, cy + h * 0.012f, mPaint);
            }
        }

        /* the hourglass of the Sands of Time, faint in gold across the middle of the sky, inside two rings */
        private void hourglass(Canvas c, int w, int h) {
            float cx = w * 0.5f, cy = h * 0.47f, R = Math.min(w, h) * 0.44f;
            mLine.setStyle(Paint.Style.STROKE);
            mLine.setColor(Color.argb(46, 222, 178, 96));
            mLine.setStrokeWidth(Math.max(1f, R * 0.01f));
            c.drawCircle(cx, cy, R, mLine);
            c.drawCircle(cx, cy, R * 0.9f, mLine);
            for (int k = 0; k < 48; k++) {   /* hour marks between the rings */
                double a = k * Math.PI * 2 / 48;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a), r1 = R * (k % 4 == 0 ? 0.91f : 0.94f);
                c.drawLine(cx + ca * r1, cy + sa * r1, cx + ca * R * 0.985f, cy + sa * R * 0.985f, mLine);
            }
            float gw = R * 0.36f, gh = R * 0.62f, neck = R * 0.035f;
            mPath.reset();   /* the glass: two bulbs meeting at a narrow neck */
            mPath.moveTo(cx - gw, cy - gh);
            mPath.lineTo(cx + gw, cy - gh);
            mPath.cubicTo(cx + gw, cy - gh * 0.35f, cx + neck, cy - gh * 0.12f, cx + neck, cy);
            mPath.cubicTo(cx + neck, cy + gh * 0.12f, cx + gw, cy + gh * 0.35f, cx + gw, cy + gh);
            mPath.lineTo(cx - gw, cy + gh);
            mPath.cubicTo(cx - gw, cy + gh * 0.35f, cx - neck, cy + gh * 0.12f, cx - neck, cy);
            mPath.cubicTo(cx - neck, cy - gh * 0.12f, cx - gw, cy - gh * 0.35f, cx - gw, cy - gh);
            mPath.close();
            mLine.setStrokeWidth(Math.max(1f, R * 0.012f));
            mLine.setColor(Color.argb(58, 222, 178, 96));
            c.drawPath(mPath, mLine);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.argb(26, 62, 200, 206));   /* sand: a heap below, a thin falling stream */
            mPath.reset();
            mPath.moveTo(cx - gw * 0.92f, cy + gh * 0.97f);
            mPath.quadTo(cx, cy + gh * 0.3f, cx + gw * 0.92f, cy + gh * 0.97f);
            mPath.close();
            c.drawPath(mPath, mPaint);
            c.drawRect(cx - neck * 0.4f, cy, cx + neck * 0.4f, cy + gh * 0.62f, mPaint);
            mPaint.setColor(Color.argb(28, 222, 178, 96));   /* the frame's caps */
            c.drawRect(cx - gw * 1.18f, cy - gh - R * 0.05f, cx + gw * 1.18f, cy - gh, mPaint);
            c.drawRect(cx - gw * 1.18f, cy + gh, cx + gw * 1.18f, cy + gh + R * 0.05f, mPaint);
        }

        /* the palace of Azad against the sky along the bottom: walls, onion domes, minarets and arched windows */
        private void palace(Canvas c, int w, int h) {
            java.util.Random r = new java.util.Random(21);
            float base = h * 0.97f;
            mPaint.setStyle(Paint.Style.FILL);
            mPath.reset();
            mPath.moveTo(0, h);
            mPath.lineTo(0, base - h * 0.08f);
            float x = 0;
            while (x < w) {
                float bw = h * (0.12f + r.nextFloat() * 0.2f), top = base - h * (0.1f + r.nextFloat() * 0.16f);
                mPath.lineTo(x, top);
                int kind = r.nextInt(4);
                if (kind == 0) {   /* an onion dome on a drum */
                    float dc = x + bw / 2, dr = bw * 0.36f, drum = top - h * 0.03f;
                    mPath.lineTo(dc - dr, top);
                    mPath.lineTo(dc - dr, drum);
                    mPath.cubicTo(dc - dr * 1.35f, drum - dr * 1.1f, dc - dr * 0.15f, drum - dr * 1.5f, dc, drum - dr * 2.1f);
                    mPath.lineTo(dc, drum - dr * 2.5f);
                    mPath.lineTo(dc, drum - dr * 2.1f);
                    mPath.cubicTo(dc + dr * 0.15f, drum - dr * 1.5f, dc + dr * 1.35f, drum - dr * 1.1f, dc + dr, drum);
                    mPath.lineTo(dc + dr, top);
                } else if (kind == 1) {   /* a minaret */
                    float mc = x + bw * (0.3f + r.nextFloat() * 0.4f), mw = h * 0.012f, mt = top - h * (0.16f + r.nextFloat() * 0.12f);
                    mPath.lineTo(mc - mw, top);
                    mPath.lineTo(mc - mw, mt + h * 0.02f);
                    mPath.lineTo(mc - mw * 1.8f, mt + h * 0.016f);
                    mPath.lineTo(mc - mw * 1.8f, mt + h * 0.01f);
                    mPath.lineTo(mc - mw, mt + h * 0.008f);
                    mPath.lineTo(mc, mt - h * 0.025f);
                    mPath.lineTo(mc + mw, mt + h * 0.008f);
                    mPath.lineTo(mc + mw * 1.8f, mt + h * 0.01f);
                    mPath.lineTo(mc + mw * 1.8f, mt + h * 0.016f);
                    mPath.lineTo(mc + mw, mt + h * 0.02f);
                    mPath.lineTo(mc + mw, top);
                } else if (kind == 2) {   /* crenellated wall */
                    float step = h * 0.018f;
                    for (float q = x; q < x + bw - step; q += step * 2) {
                        mPath.lineTo(q, top - step);
                        mPath.lineTo(q + step, top - step);
                        mPath.lineTo(q + step, top);
                        mPath.lineTo(q + step * 2, top);
                    }
                }
                mPath.lineTo(x + bw, top);
                x += bw;
            }
            mPath.lineTo(w, h);
            mPath.close();
            mPaint.setShader(new LinearGradient(0, base - h * 0.45f, 0, h, Color.rgb(4, 8, 16), Color.rgb(1, 2, 5), Shader.TileMode.CLAMP));
            c.drawPath(mPath, mPaint);
            mPaint.setShader(null);
            r = new java.util.Random(8);   /* lamplit arched windows */
            for (int i = 0; i < 14; i++) {
                float wx = r.nextFloat() * w, wy = base - h * (0.025f + r.nextFloat() * 0.045f), ww = h * 0.0035f, wh = h * 0.009f;
                mPaint.setColor(Color.argb(80 + r.nextInt(80), 255, 190, 100));
                c.drawRect(wx - ww, wy - wh * 0.5f, wx + ww, wy + wh, mPaint);
                c.drawCircle(wx, wy - wh * 0.5f, ww, mPaint);
            }
        }

        /* grains of sand drifting on the wind: gold specks and a few turquoise glints */
        private void sand(Canvas c, int w, int h) {
            java.util.Random r = new java.util.Random(7);
            mPaint.setStyle(Paint.Style.FILL);
            mLine.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 90; i++) {
                float x = r.nextFloat() * w, y = r.nextFloat() * h, rad = mDp * (0.4f + r.nextFloat() * 1.0f);
                boolean glint = r.nextInt(6) == 0;
                int col = glint ? Color.argb(70 + r.nextInt(90), 120, 236, 236) : Color.argb(40 + r.nextInt(80), 236, 200, 130);
                if (r.nextInt(3) == 0) {   /* a streak blown sideways */
                    mLine.setColor(col);
                    mLine.setStrokeWidth(rad * 1.2f);
                    c.drawLine(x, y, x - mDp * (6 + r.nextFloat() * 10), y + mDp * 2, mLine);
                } else {
                    mPaint.setColor(col);
                    c.drawCircle(x, y, rad, mPaint);
                }
            }
            mLine.setStrokeCap(Paint.Cap.BUTT);
        }

        /* a thin double sand-gold frame around the screen with turquoise diamonds at the corners */
        private void frame(Canvas c, int w, int h) {
            float m = mDp * 10, m2 = mDp * 14, cr = mDp * 4;
            mLine.setStyle(Paint.Style.STROKE);
            mLine.setStrokeWidth(Math.max(1f, mDp * 1f));
            mLine.setColor(Color.argb(160, 222, 178, 96));
            c.drawRect(m, m, w - m, h - m, mLine);
            mLine.setStrokeWidth(Math.max(1f, mDp * 0.6f));
            mLine.setColor(Color.argb(80, 222, 178, 96));
            c.drawRect(m2, m2, w - m2, h - m2, mLine);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.argb(230, 62, 200, 206));
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

        /* burnished turquoise, like the game's own logo */
        private Shader teal(float top, float bottom) {
            return new LinearGradient(0, top, 0, bottom, new int[] { Color.rgb(214, 252, 250), Color.rgb(96, 206, 210), Color.rgb(18, 84, 96), Color.rgb(110, 200, 204) },
                                      new float[] { 0f, 0.45f, 0.62f, 1f }, Shader.TileMode.CLAMP);
        }

        private Shader gold(float top, float bottom) {
            return new LinearGradient(0, top, 0, bottom, new int[] { Color.rgb(255, 244, 200), Color.rgb(230, 186, 100), Color.rgb(112, 70, 24), Color.rgb(214, 168, 86) },
                                      new float[] { 0f, 0.45f, 0.62f, 1f }, Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            /* PRINCE OF PERSIA, as wide as fits */
            String a = "PRINCE OF PERSIA", b = "THE SANDS OF TIME";
            float size = h * 0.4f;
            mText.setLetterSpacing(0.08f);
            mText.setShader(null);
            float wa = smallCaps(null, a, 0, 0, size, mText);
            if (wa > w * 0.96f) { size *= w * 0.96f / wa; wa = smallCaps(null, a, 0, 0, size, mText); }
            float base = h * 0.44f;
            mText.setShader(teal(base - size * 0.75f, base));
            smallCaps(c, a, (w - wa) / 2f, base, size, mText);
            /* the ornament: two fading gold lines meeting at a diamond */
            float ry = h * 0.58f, half = wa * 0.42f, cx = w / 2f;
            mRule.setShader(new LinearGradient(cx - half, 0, cx, 0, Color.argb(0, 222, 178, 96), GOLD, Shader.TileMode.CLAMP));
            c.drawRect(cx - half, ry - 0.6f, cx - size * 0.25f, ry + 0.6f + h * 0.006f, mRule);
            mRule.setShader(new LinearGradient(cx, 0, cx + half, 0, GOLD, Color.argb(0, 222, 178, 96), Shader.TileMode.CLAMP));
            c.drawRect(cx + size * 0.25f, ry - 0.6f, cx + half, ry + 0.6f + h * 0.006f, mRule);
            mRule.setShader(null);
            mRule.setColor(GOLD_LIGHT);
            diamond(c, cx, ry, size * 0.14f, mRule);
            mRule.setColor(EMBER);
            diamond(c, cx, ry, size * 0.06f, mRule);
            /* THE SANDS OF TIME, smaller and wider spaced, in sand gold */
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
            if (lit) mFill.setShader(new LinearGradient(0, 0, 0, h, new int[] { Color.rgb(150, 246, 242), Color.rgb(52, 190, 198), Color.rgb(12, 92, 104) },
                                                         null, Shader.TileMode.CLAMP));
            else mFill.setShader(new LinearGradient(0, 0, 0, h, Color.argb(230, 18, 32, 54), Color.argb(240, 5, 9, 18), Shader.TileMode.CLAMP));
            c.drawPath(mPath, mFill);
            mFill.setShader(null);
            /* the gold frame: a stronger outer line, a fine inner one */
            mEdge.setStrokeWidth(Math.max(1f, d * 1.3f));
            mEdge.setShader(new LinearGradient(0, 0, 0, h, GOLD_LIGHT, GOLD_DARK, Shader.TileMode.CLAMP));
            c.drawPath(mPath, mEdge);
            mEdge.setShader(null);
            mEdge.setStrokeWidth(Math.max(1f, d * 0.6f));
            mEdge.setColor(lit ? Color.argb(180, 230, 255, 252) : Color.argb(110, 222, 178, 96));
            c.drawPath(mInner, mEdge);
            /* diamonds at the points */
            mFill.setColor(lit ? Color.rgb(6, 40, 48) : EMBER);
            diamond(c, in2 + h * 0.2f, h / 2f, h * 0.1f, mFill);
            diamond(c, w - in2 - h * 0.2f, h / 2f, h * 0.1f, mFill);
            int alpha = isEnabled() ? 255 : 110;
            float size = h * (mSmall ? 0.36f : 0.38f);
            mText.setLetterSpacing(0.14f);
            mText.setTextSize(size);
            int fg = lit ? Color.argb(alpha, 4, 30, 38) : Color.argb(alpha, 250, 232, 190);
            mText.setColor(fg);
            float ty = h / 2f - (mText.descent() + mText.ascent()) / 2f + h * 0.02f;
            mText.setTextAlign(Paint.Align.LEFT);
            float lw = smallCaps(null, mLabel, 0, 0, size, mText);
            if (mNote == null) {
                smallCaps(c, mLabel, (w - lw) / 2f, ty, size, mText);
            } else {
                smallCaps(c, mLabel, h * 0.75f, ty, size, mText);
                mText.setTextAlign(Paint.Align.RIGHT);
                mText.setColor(lit ? fg : Color.argb(alpha, 92, 216, 220));
                c.drawText(mNote, w - h * 0.75f, ty, mText);
                mText.setTextAlign(Paint.Align.LEFT);
            }
        }
    }
}
