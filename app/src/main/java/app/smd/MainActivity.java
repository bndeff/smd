package app.smd;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private String savedPattern;
    private int tintIndex;
    private static final int[] tintArray = {0xffff4040, 0xff5080ff, 0xffe0c000, 0xff00c000};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LedGridView v = findViewById(R.id.ledView);
        TextView t = findViewById(R.id.textView);
        savedPattern = v.getHexPattern();
        t.setText(v.getHexPattern());
        v.setOnChangeListener(() -> {
            t.setText(v.getHexPattern());
            findViewById(R.id.undoButton).setEnabled(v.hasUndo());
            findViewById(R.id.redoButton).setEnabled(v.hasRedo());
        });
        tintIndex = 0;

        findViewById(R.id.upButton).setOnClickListener(__ -> v.shiftUp());
        findViewById(R.id.downButton).setOnClickListener(__ -> v.shiftDown());
        findViewById(R.id.leftButton).setOnClickListener(__ -> v.shiftLeft());
        findViewById(R.id.rightButton).setOnClickListener(__ -> v.shiftRight());
        findViewById(R.id.invertButton).setOnClickListener(__ -> v.invert());
        findViewById(R.id.undoButton).setOnClickListener(__ -> v.undo());
        findViewById(R.id.redoButton).setOnClickListener(__ -> v.redo());
        findViewById(R.id.saveButton).setOnClickListener(__ -> savedPattern = v.getHexPattern());
        findViewById(R.id.loadButton).setOnClickListener(__ -> v.setHexPattern(savedPattern, true));
        findViewById(R.id.clearButton).setOnClickListener(__ -> v.clear());
        findViewById(R.id.lockButton).setOnClickListener(__ -> v.setEditable(!v.getEditable()));
        findViewById(R.id.tintButton).setOnClickListener(__ -> {
            tintIndex = (tintIndex + 1) % tintArray.length;
            v.setOnColor(tintArray[tintIndex]);
            t.setTextColor(tintArray[tintIndex]);
        });
    }

}