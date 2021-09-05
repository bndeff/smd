package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SimulationActivity extends AppCompatActivity {

    private StateMachine sm;
    private int initState;
    private boolean debugMode = false;
    private int viewMode = 0;
    private LedGridView ledPattern;
    private final List<Button> btnTransfer = new ArrayList<>();
    private final List<TextView> txtTransfer = new ArrayList<>();
    private final List<ImageView> imgTransfer = new ArrayList<>();
    private final List<LedGridView> ledTransfer = new ArrayList<>();

    private class MultiLabel {
        private final int type;
        private final int value;
        private final String pattern;

        MultiLabel(int type, int value) {
            this.type = type;
            this.value = value;
            this.pattern = "";
        }

        MultiLabel(int type, String pattern) {
            this.type = type;
            this.value = 0;
            this.pattern = pattern;
        }

        @SuppressLint("SetTextI18n")
        void applyTo(int pos) {
            Button btn = btnTransfer.get(pos);
            TextView txt = txtTransfer.get(pos);
            ImageView img = imgTransfer.get(pos);
            LedGridView led = ledTransfer.get(pos);
            btn.setVisibility(View.VISIBLE);
            txt.setVisibility(View.INVISIBLE);
            img.setVisibility(View.INVISIBLE);
            led.setVisibility(View.INVISIBLE);
            switch(type) {
                case 0:
                    btn.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    txt.setVisibility(View.VISIBLE);
                    txt.setText(Integer.toString(value));
                    break;
                case 2:
                    img.setVisibility(View.VISIBLE);
                    img.setImageResource(value);
                    break;
                case 3:
                    led.setVisibility(View.VISIBLE);
                    led.setHexPattern(pattern, false);
            }
        }
    }

    private boolean isPosAvailable(int pos) {
        if(debugMode) return true;
        return pos != 0 && pos != 6 && pos != 8;
    }

    private int resFromPos(int pos) {
        switch (pos) {
            case 0: return R.drawable.ic_reset;
            case 1: return R.drawable.ic_up;
            case 2: return R.drawable.ic_auto;
            case 3: return R.drawable.ic_left;
            case 4: return R.drawable.ic_click;
            case 5: return R.drawable.ic_right;
            case 6: return R.drawable.ic_previous;
            case 7: return R.drawable.ic_down;
            case 8: return R.drawable.ic_next;
            default: return R.drawable.ic_debug;
        }
    }

    private int txFromPos(int pos) {
        switch (pos) {
            case 1: return StateMachine.TX_UP;
            case 2: return StateMachine.TX_AUTO;
            case 3: return StateMachine.TX_LEFT;
            case 4: return StateMachine.TX_CLICK;
            case 5: return StateMachine.TX_RIGHT;
            case 7: return StateMachine.TX_DOWN;
            default: return -1;
        }
    }

    private int opFromPos(int pos, boolean raw) {
        switch (pos) {
            case 0: return initState;
            case 1: case 2: case 3: case 4: case 5: case 7:
                int tx = txFromPos(pos);
                return raw ? sm.getRawTransfer(tx) : sm.getTransfer(tx);
            case 6: return StateMachine.OP_PREV;
            case 8: return StateMachine.OP_NEXT;
            default: return StateMachine.OP_ERROR;
        }
    }

    private StateMachine smFromPos(int pos) {
        int op = opFromPos(pos, false);
        StateMachine nsm = new StateMachine(sm, true);
        nsm.processOp(op);
        return nsm;
    }

    private int stateFromPos(int pos) {
        return smFromPos(pos).getCurrentState();
    }

    private String patternFromPos(int pos) {
        return smFromPos(pos).getPattern();
    }

    private int resFromOp(int op) {
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

    private MultiLabel mlFromOp(int op) {
        if(op < 0) return new MultiLabel(2, resFromOp(op));
        return new MultiLabel(1, op);
    }

    private MultiLabel mlFromPos(int pos) {
        if(!isPosAvailable(pos)) return new MultiLabel(0, 0);
        switch(viewMode) {
            case 0: return new MultiLabel(2, resFromPos(pos));
            case 1: return mlFromOp(opFromPos(pos, true));
            case 2: return mlFromOp(opFromPos(pos, false));
            case 3: return new MultiLabel(1, stateFromPos(pos));
            case 4: return new MultiLabel(3, patternFromPos(pos));
            default: return new MultiLabel(2, R.drawable.ic_debug);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);
        ledPattern = findViewById(R.id.ledPattern);

        LayoutInflater inflater = LayoutInflater.from(this);
        TableLayout tlControls = (TableLayout) findViewById(R.id.tlControls);
        for(int j=0; j<3; ++j) {
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for(int i=0; i<3; ++i) {
                inflater.inflate(R.layout.item_control, tr, true);
                FrameLayout fl = (FrameLayout) tr.getChildAt(i);
                Button btn = fl.findViewById(R.id.btnTransfer);
                int pos = btnTransfer.size();
                btn.setOnClickListener(v -> processButton(pos));
                btnTransfer.add(btn);
                txtTransfer.add(fl.findViewById(R.id.txtTransfer));
                imgTransfer.add(fl.findViewById(R.id.imgTransfer));
                ledTransfer.add(fl.findViewById(R.id.ledTransfer));
            }
            tlControls.addView(tr);
        }
        debugMode = true;
        viewMode = 0;

        sm = new StateMachine((new PersistedProjectList(this)).getMachine(), true);
        initState = sm.getCurrentState();
        refreshDisplay();
    }

    private void refreshDisplay() {
        ledPattern.setHexPattern(sm.getPattern(), false);
        for(int i=0; i<9; ++i) {
            mlFromPos(i).applyTo(i);
        }
    }

    private void processButton(int pos) {
        sm.processOp(opFromPos(pos, false));
        refreshDisplay();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_simulation, menu);
        return true;
    }

    private void unimplemented() {
        Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miViewSim) {
            viewMode = (viewMode + 1) % 5;
            refreshDisplay();
        }
        else if(id == R.id.miPauseSim) {
            unimplemented();
        }
        else if(id == R.id.miFasterSim) {
            unimplemented();
        }
        else if(id == R.id.miSlowerSim) {
            unimplemented();
        }
       return true;
    }

}