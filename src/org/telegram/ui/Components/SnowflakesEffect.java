/*
 * Enhanced permanent snow animation for Telegram Android chats.
 * Based on original Telegram code, modified to always run.
 */
package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class SnowflakesEffect {
    private BatchParticlesDrawHelper.BatchParticlesBuffer batchParticlesBuffer;
    private Paint batchParticlesPaint;

    private final Paint particlePaint;
    private final Paint particleThinPaint;
    private final Paint bitmapPaint = new Paint();
    private int colorKey = Theme.key_actionBarDefaultTitle;
    private int forcedColor;
    private final int viewType;
    private final int maxCount;

    private Bitmap particleBitmap;
    private long lastAnimationTime;

    private class Particle {
        float x, y, vx, vy, velocity, alpha, lifeTime, currentTime, scale;
        int type;

        public void draw(Canvas canvas) {
            switch (type) {
                case 0:
                    particlePaint.setAlpha((int) (255 * alpha));
                    canvas.drawPoint(x, y, particlePaint);
                    break;
                default:
                    if (particleBitmap == null) {
                        particleBitmap = createParticlesBitmap(false);
                    }
                    bitmapPaint.setAlpha((int) (255 * alpha));
                    canvas.save();
                    canvas.scale(scale, scale, x, y);
                    canvas.drawBitmap(particleBitmap, x - particleBitmap.getWidth() / 2f, y - particleBitmap.getHeight() / 2f, bitmapPaint);
                    canvas.restore();
                    break;
            }
        }
    }

    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<Particle> freeParticles = new ArrayList<>();
    private int color;

    /**
     * @param viewType 0 = лёгкий, 1 = плотный (для чатов)
     */
    public SnowflakesEffect(int viewType) {
        this.viewType = viewType;
        this.maxCount = viewType == 0 ? 80 : 250;

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStrokeWidth(dp(1.5f));
        particlePaint.setStrokeCap(Paint.Cap.ROUND);
        particlePaint.setStyle(Paint.Style.STROKE);

        particleThinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleThinPaint.setStrokeWidth(dp(0.5f));
        particleThinPaint.setStrokeCap(Paint.Cap.ROUND);
        particleThinPaint.setStyle(Paint.Style.STROKE);

        try {
            if (BatchParticlesDrawHelper.isAvailable()) {
                batchParticlesBuffer = new BatchParticlesDrawHelper.BatchParticlesBuffer(maxCount);
                batchParticlesPaint = BatchParticlesDrawHelper.createBatchParticlesPaint(createParticlesBitmap(true));
            }
        } catch (Throwable t) {
            batchParticlesBuffer = null;
            batchParticlesPaint = null;
        }

        updateColors();
        for (int a = 0; a < 30; a++) {
            freeParticles.add(new Particle());
        }
    }

    public void setForcedColor(int forcedColor) {
        this.forcedColor = forcedColor;
        updateColors();
    }

    public void setColorKey(int key) {
        colorKey = key;
        updateColors();
    }

    public void updateColors() {
        final int c = forcedColor != 0 ? forcedColor : (Theme.getColor(colorKey) & 0xffe6e6e6);
        if (this.color != c) {
            this.color = c;
            particlePaint.setColor(c);
            particleThinPaint.setColor(c);
        }
    }

    private void updateParticles(long dt) {
        int count = particles.size();
        for (int a = 0; a < count; a++) {
            Particle p = particles.get(a);
            if (p.currentTime >= p.lifeTime) {
                if (freeParticles.size() < 50) freeParticles.add(p);
                particles.remove(a);
                a--;
                count--;
                continue;
            }
            if (p.currentTime < 200f) {
                p.alpha = AndroidUtilities.accelerateInterpolator.getInterpolation(p.currentTime / 200f);
            } else if (p.lifeTime - p.currentTime < 2000) {
                p.alpha = AndroidUtilities.decelerateInterpolator.getInterpolation((p.lifeTime - p.currentTime) / 2000f);
            } else {
                p.alpha = 1f;
            }
            p.x += p.vx * p.velocity * dt / 500f;
            p.y += p.vy * p.velocity * dt / 500f;
            p.currentTime += dt;
        }
    }

    public void onDraw(View parent, Canvas canvas) {
        if (parent == null || canvas == null) return;
        if (!org.telegram.messenger.Mods.ModsConfig.snowEnabled) return;

        if (batchParticlesBuffer != null) {
            final int count = Math.min(maxCount, particles.size());
            final int texSize = dp(TEXTURE_SIZE_DP);
            for (int a = 0; a < count; a++) {
                Particle p = particles.get(a);
                float h = p.type == 0 ? texSize / 2f : texSize / 2f * p.scale;
                float tx = p.type == 0 ? texSize : 0;
                batchParticlesBuffer.setParticleColor(a, ColorUtils.setAlphaComponent(color, (int) (255 * p.alpha)));
                batchParticlesBuffer.setParticleVertexCords(a, p.x - h, p.y - h, p.x + h, p.y + h);
                batchParticlesBuffer.setParticleTextureCords(a, tx, 0, tx + texSize, texSize);
            }
            BatchParticlesDrawHelper.draw(canvas, batchParticlesBuffer, count, batchParticlesPaint);
        } else {
            for (int a = 0; a < particles.size(); a++) {
                particles.get(a).draw(canvas);
            }
        }

        int createPerFrame = viewType == 0 ? 1 : 8;
        if (particles.size() < maxCount) {
            for (int i = 0; i < createPerFrame; i++) {
                if (particles.size() >= maxCount) break;
                if (Utilities.random.nextFloat() > 0.65f) {
                    float cx = Utilities.random.nextFloat() * parent.getMeasuredWidth();
                    float cy = viewType == 0
                            ? AndroidUtilities.statusBarHeight + Utilities.random.nextFloat() * (parent.getMeasuredHeight() - dp(20) - AndroidUtilities.statusBarHeight)
                            : Utilities.random.nextFloat() * parent.getMeasuredHeight();

                    int angle = Utilities.random.nextInt(50) - 25 + 90;
                    float vx = (float) Math.cos(Math.PI / 180.0 * angle);
                    float vy = (float) Math.sin(Math.PI / 180.0 * angle);

                    Particle np = freeParticles.isEmpty() ? new Particle() : freeParticles.remove(0);
                    np.x = cx;
                    np.y = cy;
                    np.vx = vx;
                    np.vy = vy;
                    np.alpha = 0f;
                    np.currentTime = 0;
                    np.scale = 0.6f + Utilities.random.nextFloat() * 1.0f;
                    np.type = Utilities.random.nextInt(2);
                    np.lifeTime = viewType == 0 ? 1800 + Utilities.random.nextInt(800) : 2800 + Utilities.random.nextInt(2200);
                    np.velocity = 18f + Utilities.random.nextFloat() * 6f;
                    particles.add(np);
                }
            }
        }

        long now = System.currentTimeMillis();
        long dt = Math.min(17, now - lastAnimationTime);
        updateParticles(dt);
        lastAnimationTime = now;
        parent.invalidate();
    }

    private static final int TEXTURE_SIZE_DP = 10;

    private static Bitmap createParticlesBitmap(boolean useFull) {
        Paint thin = new Paint(Paint.ANTI_ALIAS_FLAG);
        thin.setStrokeWidth(dp(0.5f));
        thin.setStrokeCap(Paint.Cap.ROUND);
        thin.setStyle(Paint.Style.STROKE);
        thin.setColor(0xFFFFFFFF);

        Bitmap bmp = Bitmap.createBitmap(useFull ? dp(TEXTURE_SIZE_DP * 2) : dp(TEXTURE_SIZE_DP),
                dp(TEXTURE_SIZE_DP), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        float px = dpf2(2f) * 2;
        float px1 = -dpf2(0.57f) * 2;
        float py1 = dpf2(1.55f) * 2;
        float x = dp(TEXTURE_SIZE_DP / 2f);
        float y = dp(TEXTURE_SIZE_DP / 2f);
        float angleDiff = (float) (Math.PI / 180 * 60);

        float angle = (float) -Math.PI / 2;
        for (int a = 0; a < 6; a++) {
            float x1 = (float) Math.cos(angle) * px;
            float y1 = (float) Math.sin(angle) * px;
            float cx = x1 * 0.66f;
            float cy = y1 * 0.66f;
            c.drawLine(x, y, x + x1, y + y1, thin);

            float angle2 = (float) (angle - Math.PI / 2);
            x1 = (float) (Math.cos(angle2) * px1 - Math.sin(angle2) * py1);
            y1 = (float) (Math.sin(angle2) * px1 + Math.cos(angle2) * py1);
            c.drawLine(x + cx, y + cy, x + x1, y + y1, thin);

            x1 = (float) (-Math.cos(angle2) * px1 - Math.sin(angle2) * py1);
            y1 = (float) (-Math.sin(angle2) * px1 + Math.cos(angle2) * py1);
            c.drawLine(x + cx, y + cy, x + x1, y + y1, thin);

            angle += angleDiff;
        }

        if (useFull) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStrokeWidth(dp(1.5f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0xFFFFFFFF);
            c.drawPoint(dp(TEXTURE_SIZE_DP * 1.5f), dp(TEXTURE_SIZE_DP / 2f), p);
        }
        return bmp;
    }
}
