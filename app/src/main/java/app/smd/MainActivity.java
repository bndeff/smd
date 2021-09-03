package app.smd;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private String savedProgram;
    private String savedFullRepr;
    Random rand = new Random();

    private String genProjectName() {
        return String.format(Locale.US, "pr%03d", rand.nextInt(1000));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pl = new PersistedProjectList(this);

        savedProgram = pl.getMachine().getProgram();
        savedFullRepr = pl.exportAll();
        updateDisplay();

        LedGridView v = findViewById(R.id.ledView);
        v.setOnChangeListener(() -> {
            String hp = v.getHexPattern();
            StateMachine sm = pl.getMachine();
            sm.setPattern(hp);
            if(!sm.getPattern().equals(hp)) updateDisplay();
        });

        findViewById(R.id.prevButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            sm.gotoPrevState(true);
            updateDisplay();
        });
        findViewById(R.id.nextButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            sm.gotoNextState(true);
            updateDisplay();
        });
        findViewById(R.id.addButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            sm.addState();
            updateDisplay();
        });
        findViewById(R.id.delButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            sm.removeState();
            updateDisplay();
        });
        findViewById(R.id.copyButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            sm.copyState();
            updateDisplay();
        });
        findViewById(R.id.pasteButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            sm.pasteState();
            updateDisplay();
        });
        findViewById(R.id.saveButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            savedProgram = sm.getProgram();
            updateDisplay();
        });
        findViewById(R.id.loadButton).setOnClickListener(__ -> {
            StateMachine sm = pl.getMachine();
            sm.loadProgram(savedProgram);
            updateDisplay();
        });
        findViewById(R.id.prevProjButton).setOnClickListener(__ -> {
            pl.selectProject(pl.getSelIndex() - 1);
            updateDisplay();
        });
        findViewById(R.id.nextProjButton).setOnClickListener(__ -> {
            pl.selectProject(pl.getSelIndex() + 1);
            updateDisplay();
        });
        findViewById(R.id.addProjButton).setOnClickListener(__ -> {
            pl.addProject(genProjectName());
            updateDisplay();
        });
        findViewById(R.id.delProjButton).setOnClickListener(__ -> {
            pl.deleteProject();
            if(pl.getProjectCount() == 0) pl.addProject(genProjectName());
            updateDisplay();
        });
        findViewById(R.id.cloneProjButton).setOnClickListener(__ -> {
            pl.cloneProject(genProjectName());
            updateDisplay();
        });
        findViewById(R.id.importProjButton).setOnClickListener(__ -> {
            pl.importProject(genProjectName(), savedProgram);
            updateDisplay();
        });
        findViewById(R.id.saveProjButton).setOnClickListener(__ -> {
            savedFullRepr = pl.exportAll();
            updateDisplay();
        });
        findViewById(R.id.loadProjButton).setOnClickListener(__ -> {
            pl.clear();
            pl.importAll(savedFullRepr);
            updateDisplay();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        pl.persistState();
    }

    void updateDisplay() {
        LedGridView v = findViewById(R.id.ledView);
        TextView t = findViewById(R.id.textView);

        StateMachine sm = pl.getMachine();
        v.setHexPattern(sm.getPattern(), true);

        StringBuilder sb = new StringBuilder();
        sb.append(sm.getName());
        sb.append(" [");
        sb.append(sm.getCurrentState());
        sb.append("/");
        sb.append(sm.getStateCount());
        sb.append("]    ");
        int txIndex = 0;
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


        Log.d("prog", sm.getProgram());

        findViewById(R.id.pasteButton).setEnabled(sm.isClipboardValid());

    }

}