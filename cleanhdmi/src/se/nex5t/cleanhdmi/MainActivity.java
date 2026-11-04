package se.nex5t.cleanhdmi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.sony.scalar.hardware.CameraEx;
import com.sony.scalar.hardware.avio.DisplayManager;
import com.sony.scalar.sysutil.ScalarInput;
import java.util.List;

/**
 * Full-screen live view on HDMI with the graphics (OSD) layer off.
 * LEFT/RIGHT: pick setting (Mode, F, Time, Focus, EV, ISO, WB). Wheel or UP/DOWN: change it.
 * Half-press: autofocus. ENTER: pin overlay on/off. DELETE: exit.
 * Any change shows a short overlay, then the picture is clean again.
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String[] ITEMS = {"Läge", "F", "Tid", "Fokus", "EV", "ISO", "WB"};
    private static final String[] MODES = {"program-auto", "aperture-priority", "shutter-speed", "manual-exposure"};
    private static final String[] MODE_NAMES = {"P", "A", "S", "M"};
    private static final int OVERLAY_MS = 2000;

    private SurfaceHolder holder;
    private TextView status, overlay;
    private CameraEx camera;
    private DisplayManager display;
    private final Handler handler = new Handler();
    private boolean pinned = false;
    private int item = 0;
    private String log = "";

    private final Runnable hideOverlay = new Runnable() {
        public void run() { if (!pinned) setGraphics(false); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        SurfaceView surface = new SurfaceView(this);
        root.addView(surface, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        status = new TextView(this);
        status.setTextColor(Color.YELLOW);
        status.setBackgroundColor(0x80000000);
        status.setVisibility(TextView.GONE);
        root.addView(status, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        overlay = new TextView(this);
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(28);
        overlay.setBackgroundColor(0xA0000000);
        overlay.setPadding(16, 8, 16, 8);
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        setContentView(root);
        holder = surface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifyAppInfo();
        try { display = new DisplayManager(); } catch (Throwable t) { log("DisplayManager: " + t); }
        try { camera = CameraEx.open(0, null); } catch (Throwable t) { log("CameraEx.open: " + t); }
        holder.addCallback(this);
        switchTo(DisplayManager.DEVICE_ID_HDMI);
        showOverlay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(hideOverlay);
        holder.removeCallback(this);
        setGraphics(true);
        if (camera != null) { camera.release(); camera = null; }
        if (display != null) { display.finish(); display = null; }
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        try {
            cam().setPreviewDisplay(h);
            cam().startPreview();
        } catch (Throwable t) { log("preview: " + t); }
    }

    @Override public void surfaceChanged(SurfaceHolder h, int format, int w, int hh) {}
    @Override public void surfaceDestroyed(SurfaceHolder h) {}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int scan = event.getScanCode();
        switch (scan) {
            case ScalarInput.ISV_KEY_LEFT: item = (item + ITEMS.length - 1) % ITEMS.length; showOverlay(); return true;
            case ScalarInput.ISV_KEY_RIGHT: item = (item + 1) % ITEMS.length; showOverlay(); return true;
            case ScalarInput.ISV_KEY_UP:
            case ScalarInput.ISV_DIAL_1_CLOCKWISE:
            case ScalarInput.ISV_DIAL_2_CLOCKWISE:
            case ScalarInput.ISV_DIAL_KURU_CLOCKWISE: change(+1); return true;
            case ScalarInput.ISV_KEY_DOWN:
            case ScalarInput.ISV_DIAL_1_COUNTERCW:
            case ScalarInput.ISV_DIAL_2_COUNTERCW:
            case ScalarInput.ISV_DIAL_KURU_COUNTERCW: change(-1); return true;
            case ScalarInput.ISV_KEY_ENTER:
                pinned = !pinned;
                status.setVisibility(pinned ? TextView.VISIBLE : TextView.GONE);
                showOverlay();
                return true;
            case ScalarInput.ISV_KEY_S1_1: if (event.getRepeatCount() == 0) autoFocus(); return true;
            case ScalarInput.ISV_KEY_DELETE:
            case ScalarInput.ISV_KEY_SK2: finish(); return true;
            default:
                // Shows codes of unhandled buttons, e.g. TV remote via HDMI-CEC.
                log("key scan=" + scan + " code=" + keyCode);
                flash("Knapp " + scan + " / " + keyCode);
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getScanCode() == ScalarInput.ISV_KEY_S1_1) {
            try { cam().cancelAutoFocus(); } catch (Throwable t) { log("cancelAF: " + t); }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private Camera cam() { return camera.getNormalCamera(); }

    private void change(int dir) {
        try {
            Camera.Parameters cur = cam().getParameters();
            Camera.Parameters p = camera.createEmptyParameters();
            switch (item) {
                case 0: p.setSceneMode(MODES[Math.max(0, Math.min(MODES.length - 1, indexOf(MODES, cur.getSceneMode()) + dir))]); break;
                case 1: if (dir > 0) camera.incrementAperture(); else camera.decrementAperture(); showOverlay(); return;
                case 2: if (dir > 0) camera.incrementShutterSpeed(); else camera.decrementShutterSpeed(); showOverlay(); return;
                case 3: p.setFocusMode(step(cur.getSupportedFocusModes(), cur.getFocusMode(), dir)); break;
                case 4: {
                    int ev = Math.max(cur.getMinExposureCompensation(), Math.min(cur.getMaxExposureCompensation(), cur.getExposureCompensation() + dir));
                    p.setExposureCompensation(ev);
                    break;
                }
                case 5: {
                    CameraEx.ParametersModifier m = camera.createParametersModifier(cur);
                    camera.createParametersModifier(p).setISOSensitivity(step(m.getSupportedISOSensitivities(), m.getISOSensitivity(), dir));
                    break;
                }
                case 6: {
                    List<String> wbs = cur.getSupportedWhiteBalance();
                    String wb = step(wbs, cur.getWhiteBalance(), dir);
                    log("WB " + cur.getWhiteBalance() + " -> " + wb + " of " + wbs);
                    // Empty parameters did not change WB; send it with the full set.
                    cur.setWhiteBalance(wb);
                    p = cur;
                    break;
                }
            }
            cam().setParameters(p);
        } catch (Throwable t) { log(ITEMS[item] + ": " + t); }
        showOverlay();
    }

    private static int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i;
        return -1;
    }

    private static <T> T step(List<T> list, T current, int dir) {
        int i = list.indexOf(current);
        return list.get(Math.max(0, Math.min(list.size() - 1, i + dir)));
    }

    private void autoFocus() {
        try {
            cam().autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera c) { flash(success ? "AF ✔" : "AF ✖"); }
            });
        } catch (Throwable t) { log("autoFocus: " + t); }
    }

    private String describe() {
        StringBuilder sb = new StringBuilder();
        try {
            Camera.Parameters p = cam().getParameters();
            CameraEx.ParametersModifier m = camera.createParametersModifier(p);
            int iso = m.getISOSensitivity();
            int mode = indexOf(MODES, p.getSceneMode());
            android.util.Pair<Integer, Integer> ss = m.getShutterSpeed();
            String[] values = {
                mode < 0 ? p.getSceneMode() : MODE_NAMES[mode],
                String.format("%.1f", m.getAperture() / 100f),
                ss == null ? "?" : ss.first == 1 && ss.second != 1 ? "1/" + ss.second : ss.second == 1 ? ss.first + "\"" : ss.first + "/" + ss.second,
                "auto".equals(p.getFocusMode()) ? "AF" : "manual".equals(p.getFocusMode()) ? "MF" : p.getFocusMode(),
                String.format("%+.1f", p.getExposureCompensation() * p.getExposureCompensationStep()),
                iso == 0 ? "Auto" : String.valueOf(iso),
                p.getWhiteBalance(),
            };
            for (int i = 0; i < ITEMS.length; i++) {
                if (i > 0) sb.append("  ");
                sb.append(i == item ? "[" + ITEMS[i] + " " + values[i] + "]" : ITEMS[i] + " " + values[i]);
            }
        } catch (Throwable t) { sb.append(ITEMS[item]); log("describe: " + t); }
        return sb.toString();
    }

    private void showOverlay() { flash(describe()); }

    private void flash(String text) {
        overlay.setText(text);
        setGraphics(true);
        handler.removeCallbacks(hideOverlay);
        handler.postDelayed(hideOverlay, OVERLAY_MS);
    }

    private void switchTo(String device) {
        if (display == null) return;
        try {
            display.switchDisplayOutputTo(device);
            log("output -> " + device + ", active=" + display.getActiveDevice());
        } catch (Throwable t) { log("switch " + device + ": " + t); }
    }

    private void setGraphics(boolean on) {
        if (display == null) return;
        try { display.controlGraphicsOutputAll(on); } catch (Throwable t) { log("graphics: " + t); }
    }

    private void log(String line) {
        log = line + "\n" + log;
        if (log.length() > 1500) log = log.substring(0, 1500);
        status.setText("←→ val  hjul/↑↓ ändra  halvtryck AF  mitt: pin  DEL: avsluta\n" + log);
    }

    private void notifyAppInfo() {
        Intent intent = new Intent("com.android.server.DAConnectionManagerService.AppInfoReceive");
        intent.putExtra("package_name", getComponentName().getPackageName());
        intent.putExtra("class_name", getComponentName().getClassName());
        sendBroadcast(intent);
    }
}
