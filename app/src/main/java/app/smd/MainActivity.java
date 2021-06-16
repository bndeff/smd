package app.smd;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private StateMachine sm;
    private String savedProgram;
    private int txIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sm = new StateMachine();
        savedProgram = sm.getProgramString();
        updateDisplay();

        LedGridView v = findViewById(R.id.ledView);
        v.setOnChangeListener(() -> {
            String hp = v.getHexPattern();
            sm.setPattern(hp);
            if(!sm.getPattern().equals(hp)) updateDisplay();
        });

        findViewById(R.id.prevButton).setOnClickListener(__ -> {
            sm.gotoPrevState(true);
            updateDisplay();
        });
        findViewById(R.id.nextButton).setOnClickListener(__ -> {
            sm.gotoNextState(true);
            updateDisplay();
        });
        findViewById(R.id.upButton).setOnClickListener(__ -> {
            sm.moveStateUp();
            updateDisplay();
        });
        findViewById(R.id.downButton).setOnClickListener(__ -> {
            sm.moveStateDown();
            updateDisplay();
        });
        findViewById(R.id.delButton).setOnClickListener(__ -> {
            sm.removeState();
            updateDisplay();
        });
        findViewById(R.id.cutButton).setOnClickListener(__ -> {
            sm.cutState();
            updateDisplay();
        });
        findViewById(R.id.copyButton).setOnClickListener(__ -> {
            sm.copyState();
            updateDisplay();
        });
        findViewById(R.id.pasteButton).setOnClickListener(__ -> {
            sm.pasteState();
            updateDisplay();
        });
        findViewById(R.id.addButton).setOnClickListener(__ -> {
            sm.addState();
            updateDisplay();
        });
        findViewById(R.id.cloneButton).setOnClickListener(__ -> {
            sm.cloneState();
            updateDisplay();
        });
        findViewById(R.id.saveButton).setOnClickListener(__ -> {
            savedProgram = sm.getProgramString();
            updateDisplay();
        });
        findViewById(R.id.loadButton).setOnClickListener(__ -> {
            sm = new StateMachine(savedProgram);
            updateDisplay();
        });
        findViewById(R.id.cycleButton).setOnClickListener(__ -> {
            txIndex = (txIndex + 1) % 7;
            updateDisplay();
        });
        findViewById(R.id.incButton).setOnClickListener(__ -> {
            if(txIndex == 6) {
                sm.setSpeed((sm.getSpeed() + 1) % 4);
            } else {
                int rt = sm.getRawTransfer(txIndex) + 1;
                if(rt >= sm.getStateCount()) rt = -StateMachine.NUM_OP;
                sm.setRawTransfer(txIndex, rt);
            }
            updateDisplay();
        });
        findViewById(R.id.decButton).setOnClickListener(__ -> {
            if(txIndex == 6) {
                sm.setSpeed((sm.getSpeed() - 1 + 4) % 4);
            } else {
                int rt = sm.getRawTransfer(txIndex) - 1;
                if(rt < -StateMachine.NUM_OP) rt = sm.getStateCount() - 1;
                sm.setRawTransfer(txIndex, rt);
            }
            updateDisplay();
        });
        findViewById(R.id.stepButton).setOnClickListener(__ -> {
            if(txIndex != 6) sm.processOp(sm.getTransfer(txIndex));
            updateDisplay();
        });
    }

    void updateDisplay() {
        LedGridView v = findViewById(R.id.ledView);
        TextView t = findViewById(R.id.textView);

        v.setHexPattern(sm.getPattern(), true);

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(sm.getCurrentState());
        sb.append("/");
        sb.append(sm.getStateCount());
        sb.append("]    ");
        for(int i=0; i<6; ++i) {
            if(txIndex == i) sb.append("*");
            sb.append(sm.getRawTransfer(i));
            sb.append(":");
            sb.append(sm.getTransfer(i));
            sb.append("    ");
        }
        if(txIndex == 6) sb.append("*");
        sb.append(sm.getSpeed());
        sb.append(" => ");
        sb.append(sm.getDelay());
        if(sm.isPaused()) sb.append("P");
        t.setText(sb.toString());


        Log.d("prog", sm.getProgramString());

        findViewById(R.id.upButton).setEnabled(!sm.isFirstState());
        findViewById(R.id.downButton).setEnabled(!sm.isLastState());
        findViewById(R.id.pasteButton).setEnabled(sm.isClipboardValid());
        findViewById(R.id.stepButton).setEnabled(txIndex != 6);
    }

}