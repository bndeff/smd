package app.smd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

@SuppressWarnings("unused")
public class LedGridView extends View {
    private Paint drawPaint;
    private final int w = 8, h = 8;  // be careful: functions using gridData assume w = h = 8
    private final byte[] gridData = new byte[8];
    private int offColor = Color.GRAY, onColor = Color.RED;
    private float margin = 0;
    private boolean editable = true;
    private final HashMap<Integer, Boolean> pointerMode = new HashMap<>();
    private final Deque<byte[]> undoBuffer = new ArrayDeque<>(), redoBuffer = new ArrayDeque<>();
    private int undoCap = 256;
    private OnChangeListener onChangeListener = null;

    public LedGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LedGridView);
        try {
            offColor = a.getColor(R.styleable.LedGridView_offColor, offColor);
            onColor = a.getColor(R.styleable.LedGridView_onColor, onColor);
            margin = a.getDimension(R.styleable.LedGridView_margin, margin);
            editable = a.getBoolean(R.styleable.LedGridView_editable, editable);
            undoCap = a.getInt(R.styleable.LedGridView_undoCap, undoCap);
            setHexPatternInternal(a.getString(R.styleable.LedGridView_pattern));
        } finally {
            a.recycle();
        }
        clearUndo();
    }

    public LedGridView(Context context) {
        super(context);
        setupPaint();
        clearUndo();
    }

    private void setupPaint() {
        drawPaint = new Paint();
        drawPaint.setStyle(Paint.Style.FILL);
        drawPaint.setColor(Color.BLACK);
        drawPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cw = getWidth() - 2 * margin;
        float ch = getHeight() - 2 * margin;
        float r = Math.min(cw / (2*w), ch / (2*h));
        for(int j=0; j<h; ++j) {
            float y = margin + ch * (2*j+1) / (2*h);
            for(int i=0; i<w; ++i) {
                float x = margin + cw * (2*i+1) / (2*w);
                drawPaint.setColor(getPixel(i, j) ? onColor : offColor);
                canvas.drawCircle(x, y, r, drawPaint);
            }
        }
    }

    private int x2i(float x) {
        float cw = getWidth() - 2 * margin;
        return Math.min(Math.max((int)(((x - margin) / cw) * w), 0), w-1);
    }

    private int y2j(float y) {
        float ch = getHeight() - 2 * margin;
        return Math.min(Math.max((int)(((y - margin) / ch) * h), 0), h-1);
    }

    private boolean getPixel(int i, int j) {
        return (gridData[j] & 0x80>>i) != 0;
    }

    private void setPixel(int i, int j, boolean value) {
        if(value) {
            gridData[j] |= 0x80>>i;
        } else {
            gridData[j] &= ~(0x80>>i);
        }
    }

    public String getHexPattern() {
        return String.format("%016x", new BigInteger(1, gridData));
    }

    private void setHexPatternInternal(String pattern) {
        if(pattern == null) return;
        byte[] newGrid = new BigInteger(pattern, 16).toByteArray();
        int n = newGrid.length;
        Arrays.fill(gridData, (byte) 0);
        if(n <= h) {
            System.arraycopy(newGrid, 0, gridData, h-n, n);
        } else {
            System.arraycopy(newGrid, n-h, gridData, 0, h);
        }
    }

    public void setHexPattern(String pattern, boolean addUndo) {
        setHexPatternInternal(pattern);
        markUpdated(addUndo);
    }

    public boolean getEditable() {
        return editable;
    }

    public void setEditable(boolean value) {
        editable = value;
    }

    public int getOnColor() {
        return onColor;
    }

    public int getOffColor() {
        return offColor;
    }

    public void setOnColor(int value) {
        onColor = value;
        postInvalidate();
    }

    public void setOffColor(int value) {
        offColor = value;
        postInvalidate();
    }

    public void setColors(int on, int off) {
        onColor = on;
        offColor = off;
        postInvalidate();
    }

    public float getMargin() {
        return margin;
    }

    public void setMargin(float value) {
        margin = value;
        postInvalidate();
    }

    public void setOnChangeListener(OnChangeListener listener) {
        onChangeListener = listener;
    }

    private void markUpdated(boolean addUndo) {
        if(addUndo) {
            addUndoFrame();
            redoBuffer.clear();
        }
        postInvalidate();
        if(onChangeListener != null) onChangeListener.onChange();
    }

    private void addUndoFrame() {
        if(!undoBuffer.isEmpty() && Arrays.equals(gridData, undoBuffer.peekLast())) return;
        if(undoBuffer.size() >= undoCap) undoBuffer.removeFirst();
        undoBuffer.addLast(gridData.clone());
    }

    public void clearUndo() {
        undoBuffer.clear();
        undoBuffer.addLast(gridData.clone());
        redoBuffer.clear();
    }

    private void restoreUndoFrame() {
        byte[] last = undoBuffer.peekLast();
        if(last == null) return;
        System.arraycopy(last, 0, gridData, 0, h);
        markUpdated(false);
    }

    public void undo() {
        if(!editable) return;
        addUndoFrame();
        if(undoBuffer.size() >= 2) {
            redoBuffer.addLast(undoBuffer.removeLast());
        }
        restoreUndoFrame();
    }

    public boolean hasUndo() {
        if(undoBuffer.isEmpty()) return false;
        return undoBuffer.size() > 1 || !Arrays.equals(gridData, undoBuffer.peekLast());
    }

    public boolean hasRedo() {
        return !redoBuffer.isEmpty();
    }

    public void redo() {
        if(!editable) return;
        if(redoBuffer.isEmpty()) return;
        undoBuffer.addLast(redoBuffer.removeLast());
        restoreUndoFrame();
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public void shiftUp() {
        if(!editable) return;
        System.arraycopy(gridData, 1, gridData, 0, h - 1);
        gridData[h-1] = 0;
        markUpdated(true);
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public void shiftDown() {
        if(!editable) return;
        System.arraycopy(gridData, 0, gridData, 1, h - 1);
        gridData[0] = 0;
        markUpdated(true);
    }

    public void shiftLeft() {
        if(!editable) return;
        for(int j=0; j<h; ++j) {
            gridData[j] <<= 1;
            gridData[j] &= 0xfe;
        }
        markUpdated(true);
    }

    public void shiftRight() {
        if(!editable) return;
        for(int j=0; j<h; ++j) {
            gridData[j] >>= 1;
            gridData[j] &= 0x7f;
        }
        markUpdated(true);
    }

    public void invert() {
        if(!editable) return;
        for(int j=0; j<h; ++j) gridData[j] ^= 0xff;
        markUpdated(true);
    }

    public void clear() {
        if(!editable) return;
        Arrays.fill(gridData, (byte) 0);
        markUpdated(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    // TODO https://developer.android.com/guide/topics/ui/accessibility/custom-views#virtual-hierarchy
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int a, k, p, c; float x, y; Boolean m;
        if(!editable) return false;
        a = event.getActionMasked();
        switch(a) {
            case ACTION_DOWN:
            case ACTION_POINTER_DOWN:
                k = (a == ACTION_DOWN) ? 0 : event.getActionIndex();
                p = event.getPointerId(k);
                x = event.getX(k);
                y = event.getY(k);
                pointerMode.put(p, !getPixel(x2i(x), y2j(y)));
                break;
            case ACTION_POINTER_UP:
                k = event.getActionIndex();
                p = event.getPointerId(k);
                pointerMode.remove(p);
                break;
            case ACTION_UP:
            case ACTION_CANCEL:
                pointerMode.clear();
                break;
        }
        c = event.getPointerCount();
        for(k=0; k<c; ++k) {
            p = event.getPointerId(k);
            m = pointerMode.get(p);
            if(m == null) continue;
            x = event.getX(k);
            y = event.getY(k);
            setPixel(x2i(x), y2j(y), m);
        }
        markUpdated(a == ACTION_UP || a == ACTION_CANCEL);
        return true;
    }

}
