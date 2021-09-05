package app.smd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TransferButtons extends TableLayout {

    private final List<Button> btnTransfer = new ArrayList<>();
    private final List<LedGridView> ledTransfer = new ArrayList<>();
    private final List<TextView> txtTransfer = new ArrayList<>();
    private final List<ImageView> imgTransfer = new ArrayList<>();

    private final List<Integer> buttonType = new ArrayList<>();
    private final List<Integer> buttonValue = new ArrayList<>();

    private StateMachine sm = null;
    private int bgMode = 0;
    private int fgMode = 1;

    public static final int BT_NONE = 0;
    public static final int BT_TX = 1;
    public static final int BT_OP = 2;
    public static final int BT_RESET = 3;
    public static final int BT_DEF = 4;

    public static final int BG_HIDE = 0;
    public static final int BG_SHOW = 1;
    public static final int BG_DIFF = 2;
    public static final int BG_FIXED = 3;

    public static final int FG_HIDE = 0;
    public static final int FG_TX = 1;
    public static final int FG_RAW = 2;
    public static final int FG_OP = 3;
    public static final int FG_STATE = 4;

    public TransferButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        for(int j=0; j<3; ++j) {
            TableRow tr = new TableRow(context);
            tr.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for(int i=0; i<3; ++i) {
                inflater.inflate(R.layout.item_control, tr, true);
                FrameLayout fl = (FrameLayout) tr.getChildAt(i);
                Button btn = fl.findViewById(R.id.btnTransfer);
                int pos = btnTransfer.size();
                btnTransfer.add(btn);
                ledTransfer.add(fl.findViewById(R.id.ledTransfer));
                txtTransfer.add(fl.findViewById(R.id.txtTransfer));
                imgTransfer.add(fl.findViewById(R.id.imgTransfer));
                buttonType.add(BT_NONE);
                buttonValue.add(0);
            }
            this.addView(tr);
        }
    }

    public void setStateMachine(StateMachine sm) {
        this.sm = sm;
    }

    public void setupButton(int pos, int mode, int value, OnClickListener onClickListener) {
        buttonType.set(pos, mode);
        buttonValue.set(pos, value);
        btnTransfer.get(pos).setOnClickListener(onClickListener);
    }

    private int getTxIcon(int tx) {
        switch (tx) {
            case StateMachine.TX_UP: return R.drawable.ic_up;
            case StateMachine.TX_AUTO: return R.drawable.ic_auto;
            case StateMachine.TX_LEFT: return R.drawable.ic_left;
            case StateMachine.TX_CLICK: return R.drawable.ic_click;
            case StateMachine.TX_RIGHT: return R.drawable.ic_right;
            case StateMachine.TX_DOWN: return R.drawable.ic_down;
            default: return R.drawable.ic_debug;
        }
    }

    private int getOpIcon(int op) {
        switch (op) {
            case StateMachine.OP_INHERIT: return R.drawable.ic_default;
            case StateMachine.OP_NEXT: return R.drawable.ic_next;
            case StateMachine.OP_PREV: return R.drawable.ic_previous;
            case StateMachine.OP_PAUSE: return R.drawable.ic_pause;
            case StateMachine.OP_FASTER: return R.drawable.ic_faster;
            case StateMachine.OP_SLOWER: return R.drawable.ic_slower;
            case StateMachine.OP_NONE: return R.drawable.ic_none;
            case StateMachine.OP_ERROR: return R.drawable.ic_error;
            default: return R.drawable.ic_debug;
        }
    }

    public void setDisplayMode(int fg, int bg) {
        fgMode = fg;
        bgMode = bg;
    }

    @SuppressLint("SetTextI18n")
    public void refresh() {
        int currentState = 0;
        if(sm != null) currentState = sm.getCurrentState();
        for(int i=0; i<9; ++i) {
            Button btn = btnTransfer.get(i);
            LedGridView led = ledTransfer.get(i);
            TextView txt = txtTransfer.get(i);
            ImageView img = imgTransfer.get(i);
            int bt = buttonType.get(i);
            if(sm == null) bt = BT_NONE;
            if(bt == BT_NONE) {
                btn.setVisibility(INVISIBLE);
                led.setVisibility(INVISIBLE);
                txt.setVisibility(INVISIBLE);
                img.setVisibility(INVISIBLE);
                continue;
            }
            btn.setVisibility(VISIBLE);
            int bv = buttonValue.get(i);
            int opQuery;
            int opDisplay;
            if(bt == BT_TX) {
                opQuery = sm.getTransfer(bv);
                opDisplay = (fgMode == FG_RAW) ? sm.getRawTransfer(bv) : opQuery;
            }
            else if(bt == BT_DEF) {
                opQuery = sm.getDefaultTransfer(bv);
                opDisplay = opQuery;
            }
            else {
                opQuery = bv;
                opDisplay = bv;
            }
            boolean opRem;
            int state;
            String pattern;
            if(fgMode == FG_STATE || bgMode >= BG_SHOW) {
                StateMachine.OpResult res = sm.queryOp(opQuery);
                opRem = res.opRemains;
                state = res.newState;
                pattern = sm.getThumbnail(state, true);
            } else {
                opRem = true;
                state = currentState;
                pattern = "";
            }
            if(fgMode == FG_HIDE) {
                txt.setVisibility(INVISIBLE);
                img.setVisibility(INVISIBLE);
            }
            else if(fgMode == FG_TX && bt == BT_TX) {
                txt.setVisibility(INVISIBLE);
                img.setVisibility(VISIBLE);
                img.setImageResource(getTxIcon(bv));
            }
            else if(fgMode == FG_TX && bt == BT_RESET) {
                txt.setVisibility(INVISIBLE);
                img.setVisibility(VISIBLE);
                img.setImageResource(R.drawable.ic_reset);
            }
            else {
                if(fgMode == FG_STATE && !opRem) opDisplay = state;
                if(opDisplay < 0) {
                    txt.setVisibility(INVISIBLE);
                    img.setVisibility(VISIBLE);
                    img.setImageResource(getOpIcon(opDisplay));
                } else {
                    txt.setVisibility(VISIBLE);
                    img.setVisibility(INVISIBLE);
                    txt.setText(Integer.toString(opDisplay + 1));
                }
            }
            if(bgMode == BG_HIDE || (bgMode == BG_DIFF && state == currentState) ||
                    (bgMode == BG_FIXED && opDisplay < 0) || pattern.isEmpty()) {
                led.setVisibility(INVISIBLE);
            } else {
                led.setVisibility(VISIBLE);
                led.setHexPattern(pattern, false);
            }
        }
    }

}
