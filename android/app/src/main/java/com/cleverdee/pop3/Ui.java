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

/* The launcher screens' look, in the spirit of Warrior Within's own front end: a wall of cold dark stone with the
   sand wraith's blood-red sigil, thin steel frames with red diamonds, and classical serif small capitals in
   polished steel. Everything is drawn here; nothing is taken from the game's files. */
final class Ui {
    /* Warrior Within: cold stone and steel with blood-red accents (the GOLD names are the metal of the frames) */
    static final int WHITE = Color.rgb(236, 236, 240), SOFT = Color.rgb(196, 198, 206), DIM = Color.rgb(124, 128, 138);
    static final int GOLD = Color.rgb(176, 182, 194), GOLD_LIGHT = Color.rgb(238, 242, 248), GOLD_DARK = Color.rgb(64, 68, 78);
    static final int EMBER = Color.rgb(178, 26, 24);
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
        credit.setTextColor(Color.argb(160, 176, 182, 194));
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

    /* "PRINCE OF PERSIA" over "WARRIOR WITHIN" in gold serif small caps, with a small line under it */
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
            stone(c, w, h);
            sigil(c, w, h);
            dust(c, w, h);
            /* shadow closing in from the edges */
            mPaint.setShader(new RadialGradient(w * 0.5f, h * 0.42f, Math.max(w, h) * 0.75f,
                                                new int[] { Color.argb(0, 0, 0, 0), Color.argb(90, 0, 0, 0), Color.argb(235, 0, 0, 0) },
                                                new float[] { 0f, 0.55f, 1f }, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mPaint);
            mPaint.setShader(null);
            frame(c, w, h);
        }

        /* a wall of rough-cut blocks in cold grey-blue, courses of uneven length */
        private void stone(Canvas c, int w, int h) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShader(new LinearGradient(0, 0, 0, h, new int[] { Color.rgb(20, 22, 26), Color.rgb(34, 37, 43), Color.rgb(16, 17, 20) },
                                                new float[] { 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mPaint);
            mPaint.setShader(null);
            java.util.Random r = new java.util.Random(11);
            float course = Math.max(mDp * 34, h / 9f), joint = Math.max(1f, mDp * 1.6f);
            mLine.setStyle(Paint.Style.STROKE);
            for (float y = -course * 0.3f; y < h; y += course) {
                float x = -r.nextFloat() * course * 1.5f;
                while (x < w) {
                    float bw = course * (1.4f + r.nextFloat() * 1.6f);
                    int g = 30 + r.nextInt(16), tone = r.nextInt(6);
                    mPaint.setShader(new LinearGradient(0, y, 0, y + course, Color.rgb(g + 8, g + 10 + tone, g + 15 + tone),
                                                        Color.rgb(g - 6, g - 5, g - 2), Shader.TileMode.CLAMP));
                    c.drawRect(x + joint, y + joint, x + bw - joint, y + course - joint, mPaint);
                    mPaint.setShader(null);
                    /* worn edges: a faint light line on top, a dark one below */
                    mLine.setStrokeWidth(Math.max(1f, mDp * 0.8f));
                    mLine.setColor(Color.argb(40, 200, 210, 225));
                    c.drawLine(x + joint * 2, y + joint * 1.5f, x + bw - joint * 2, y + joint * 1.5f, mLine);
                    mLine.setColor(Color.argb(120, 0, 0, 0));
                    c.drawLine(x + joint * 2, y + course - joint * 1.5f, x + bw - joint * 2, y + course - joint * 1.5f, mLine);
                    if (r.nextInt(4) == 0) {   /* the odd crack */
                        mPath.reset();
                        float cx = x + bw * (0.2f + r.nextFloat() * 0.6f), cy = y + joint * 2;
                        mPath.moveTo(cx, cy);
                        for (int k = 0; k < 4; k++) { cx += (r.nextFloat() - 0.5f) * course * 0.5f; cy += course * 0.22f; mPath.lineTo(cx, cy); }
                        mLine.setColor(Color.argb(90, 0, 0, 0));
                        c.drawPath(mPath, mLine);
                    }
                    x += bw;
                }
            }
        }

        /* the sand wraith's sigil in old blood on the wall: rings of runes, six seals, a three-armed spiral */
        private void sigil(Canvas c, int w, int h) {
            float cx = w * 0.5f, cy = h * 0.5f, R = Math.min(w, h) * 0.46f;
            mLine.setStyle(Paint.Style.STROKE);
            mLine.setColor(Color.argb(80, 150, 18, 16));
            mLine.setStrokeWidth(Math.max(1f, R * 0.012f));
            c.drawCircle(cx, cy, R, mLine);
            c.drawCircle(cx, cy, R * 0.86f, mLine);
            c.drawCircle(cx, cy, R * 0.42f, mLine);
            java.util.Random r = new java.util.Random(5);
            mLine.setStrokeWidth(Math.max(1f, R * 0.009f));
            for (int k = 0; k < 64; k++) {   /* runes: short angular strokes between the outer rings */
                double a = k * Math.PI * 2 / 64;
                float r0 = R * 0.89f, r1 = R * (0.94f + r.nextFloat() * 0.04f);
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a), tx = -sa * R * 0.02f, ty = ca * R * 0.02f;
                boolean flip = r.nextBoolean();
                c.drawLine(cx + ca * r0, cy + sa * r0, cx + ca * r1 + (flip ? tx : -tx), cy + sa * r1 + (flip ? ty : -ty), mLine);
            }
            mPaint.setStyle(Paint.Style.FILL);
            for (int k = 0; k < 6; k++) {   /* six seals on the middle ring */
                double a = -Math.PI / 2 + k * Math.PI / 3;
                float sx = cx + (float) Math.cos(a) * R * 0.64f, sy = cy + (float) Math.sin(a) * R * 0.64f;
                mPaint.setColor(Color.argb(45, 150, 18, 16));
                c.drawCircle(sx, sy, R * 0.13f, mPaint);
                c.drawCircle(sx, sy, R * 0.13f, mLine);
            }
            mLine.setStrokeWidth(Math.max(1f, R * 0.03f));
            mLine.setStrokeCap(Paint.Cap.ROUND);
            for (int k = 0; k < 3; k++) {   /* three spiral arms */
                mPath.reset();
                double a0 = k * Math.PI * 2 / 3;
                for (int t = 0; t <= 40; t++) {
                    double a = a0 + t * 0.07, rr = R * 0.36f * (1 - t / 46.0);
                    float px = cx + (float) (Math.cos(a) * rr), py = cy + (float) (Math.sin(a) * rr);
                    if (t == 0) mPath.moveTo(px, py); else mPath.lineTo(px, py);
                }
                c.drawPath(mPath, mLine);
            }
            mLine.setStrokeCap(Paint.Cap.BUTT);
        }

        /* sand hanging in the air */
        private void dust(Canvas c, int w, int h) {
            java.util.Random r = new java.util.Random(7);
            mPaint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 70; i++) {
                float x = r.nextFloat() * w, y = r.nextFloat() * h, rad = mDp * (0.5f + r.nextFloat() * 1.1f);
                mPaint.setColor(Color.argb(30 + r.nextInt(70), 214, 204, 186));
                c.drawCircle(x, y, rad, mPaint);
            }
        }

        /* a thin double steel frame around the screen with red diamonds at the corners */
        private void frame(Canvas c, int w, int h) {
            float m = mDp * 10, m2 = mDp * 14, cr = mDp * 4;
            mLine.setStrokeWidth(Math.max(1f, mDp * 1f));
            mLine.setColor(Color.argb(150, 176, 182, 194));
            c.drawRect(m, m, w - m, h - m, mLine);
            mLine.setStrokeWidth(Math.max(1f, mDp * 0.6f));
            mLine.setColor(Color.argb(80, 176, 182, 194));
            c.drawRect(m2, m2, w - m2, h - m2, mLine);
            mPaint.setColor(Color.argb(220, 178, 26, 24));
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
            return new LinearGradient(0, top, 0, bottom, new int[] { Color.rgb(250, 252, 255), Color.rgb(196, 202, 212), Color.rgb(70, 74, 84), Color.rgb(170, 176, 188) },
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
            mText.setShader(gold(base - size * 0.75f, base));
            smallCaps(c, a, (w - wa) / 2f, base, size, mText);
            /* the ornament: two fading gold lines meeting at a diamond */
            float ry = h * 0.58f, half = wa * 0.42f, cx = w / 2f;
            mRule.setShader(new LinearGradient(cx - half, 0, cx, 0, Color.argb(0, 176, 182, 194), GOLD, Shader.TileMode.CLAMP));
            c.drawRect(cx - half, ry - 0.6f, cx - size * 0.25f, ry + 0.6f + h * 0.006f, mRule);
            mRule.setShader(new LinearGradient(cx, 0, cx + half, 0, GOLD, Color.argb(0, 176, 182, 194), Shader.TileMode.CLAMP));
            c.drawRect(cx + size * 0.25f, ry - 0.6f, cx + half, ry + 0.6f + h * 0.006f, mRule);
            mRule.setShader(null);
            mRule.setColor(GOLD_LIGHT);
            diamond(c, cx, ry, size * 0.14f, mRule);
            mRule.setColor(EMBER);
            diamond(c, cx, ry, size * 0.06f, mRule);
            /* WARRIOR WITHIN, smaller and wider spaced */
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
            if (lit) mFill.setShader(new LinearGradient(0, 0, 0, h, new int[] { Color.rgb(214, 60, 52), Color.rgb(150, 20, 18), Color.rgb(78, 8, 8) },
                                                         null, Shader.TileMode.CLAMP));
            else mFill.setShader(new LinearGradient(0, 0, 0, h, Color.argb(230, 34, 37, 43), Color.argb(240, 10, 11, 13), Shader.TileMode.CLAMP));
            c.drawPath(mPath, mFill);
            mFill.setShader(null);
            /* the gold frame: a stronger outer line, a fine inner one */
            mEdge.setStrokeWidth(Math.max(1f, d * 1.3f));
            mEdge.setShader(new LinearGradient(0, 0, 0, h, GOLD_LIGHT, GOLD_DARK, Shader.TileMode.CLAMP));
            c.drawPath(mPath, mEdge);
            mEdge.setShader(null);
            mEdge.setStrokeWidth(Math.max(1f, d * 0.6f));
            mEdge.setColor(lit ? Color.argb(170, 255, 190, 180) : Color.argb(110, 176, 182, 194));
            c.drawPath(mInner, mEdge);
            /* diamonds at the points */
            mFill.setColor(lit ? Color.rgb(250, 236, 232) : EMBER);
            diamond(c, in2 + h * 0.2f, h / 2f, h * 0.1f, mFill);
            diamond(c, w - in2 - h * 0.2f, h / 2f, h * 0.1f, mFill);
            int alpha = isEnabled() ? 255 : 110;
            float size = h * (mSmall ? 0.36f : 0.38f);
            mText.setLetterSpacing(0.14f);
            mText.setTextSize(size);
            int fg = lit ? Color.argb(alpha, 255, 246, 244) : Color.argb(alpha, 226, 230, 238);
            mText.setColor(fg);
            float ty = h / 2f - (mText.descent() + mText.ascent()) / 2f + h * 0.02f;
            mText.setTextAlign(Paint.Align.LEFT);
            float lw = smallCaps(null, mLabel, 0, 0, size, mText);
            if (mNote == null) {
                smallCaps(c, mLabel, (w - lw) / 2f, ty, size, mText);
            } else {
                smallCaps(c, mLabel, h * 0.75f, ty, size, mText);
                mText.setTextAlign(Paint.Align.RIGHT);
                mText.setColor(lit ? fg : Color.argb(alpha, 210, 52, 44));
                c.drawText(mNote, w - h * 0.75f, ty, mText);
                mText.setTextAlign(Paint.Align.LEFT);
            }
        }
    }
}
