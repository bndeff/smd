package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Locale;

public class DataActivity extends AppCompatActivity {

    private PersistedProjectList pl = null;
    private TextView txtData;
    private String savedText;
    private int mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        txtData = findViewById(R.id.txtData);
        mode = getIntent().getIntExtra("mode", 0);
        pl = new PersistedProjectList(this);
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveChanges();
        if(pl != null) pl.persistState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pl != null) pl.loadState();
        refresh();
    }

    private void refresh() {
        saveChanges();
        if(mode == 1) {
            StateMachine sm = pl.getMachine();
            this.setTitle(sm.getName());
            savedText = sm.getProgram();
            txtData.setText(savedText);
        } else {
            this.setTitle(genProjectName());
            savedText = "";
            txtData.setText(savedText);
        }
    }

    private void saveChanges() {
        String newText = txtData.getText().toString();
        if(newText.equals(savedText)) return;
        if(mode == 1) {
            pl.importProject(pl.getMachine().getName() + "*", newText);
        } else {
            pl.importProject(genProjectName(), newText);
        }
        savedText = newText;
    }

    private String genProjectName() {
        return String.format(Locale.US, "Project #%d", pl.getProjectCount() + 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miPreviousProject) {
            saveChanges();
            if(mode == 1) {
                int sel = pl.getSelIndex() - 1;
                if (sel < 0) sel = pl.getProjectCount() - 1;
                pl.selectProject(sel);
            } else mode = 1;
            refresh();
        }
        else if(id == R.id.miNextProject) {
            saveChanges();
            int sel = pl.getSelIndex() + 1;
            if(sel >= pl.getProjectCount()) sel = 0;
            pl.selectProject(sel);
            mode = 1;
            refresh();
        }
        return true;
    }

}