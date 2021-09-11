package app.smd;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.ViewHolder> {

    private final ProjectList pl;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final List<LedGridView> ledPreview = new ArrayList<>();
        private final int numPreviews;
        private final TextView tvProjectName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            if(itemView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                numPreviews = 7;
            } else {
                numPreviews = 3;
            }
            createLedPreviews();
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
        }

        private void createLedPreviews() {
            ConstraintLayout cl = (ConstraintLayout) itemView;
            Guideline gl = cl.findViewById(R.id.guideline);
            for(int i=0; i<numPreviews; ++i) {
                LedGridView lp = (LedGridView) LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.template_preview, cl, false);
                lp.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));
                cl.addView(lp);
                lp.setId(View.generateViewId());
                ledPreview.add(lp);
            }
            ConstraintSet cs = new ConstraintSet();
            cs.clone(cl);
            int m2dp = dpToPixels(cl, 2);
            int[] chainIds = new int[numPreviews];
            for(int i=0; i<numPreviews; ++i) {
                int lpId = ledPreview.get(i).getId();
                cs.connect(lpId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, m2dp);
                cs.connect(lpId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, m2dp);
                cs.setDimensionRatio(lpId, "1:1");
                chainIds[i] = lpId;
            }
            cs.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
                    gl.getId(), ConstraintSet.RIGHT,
                    chainIds, null, ConstraintSet.CHAIN_SPREAD);
            int m3dp = dpToPixels(cl, 3);
            for(int i=0; i<numPreviews; ++i) {
                int lpId = ledPreview.get(i).getId();
                cs.setMargin(lpId, ConstraintSet.LEFT, m3dp);
                cs.setMargin(lpId, ConstraintSet.RIGHT, m3dp);
            }
            cs.applyTo(cl);
        }

        @SuppressLint("ResourceAsColor")
        public void bindStateMachine(ProjectList.Preview p, boolean isSelected) {
            for(int i=0; i<numPreviews; ++i) {
                LedGridView lp = ledPreview.get(i);
                if(i < p.numStates) {
                    lp.setHexPattern(p.patterns.get(i), false);
                    lp.setOffColor(0xff404040);
                    lp.setBackgroundColor(0xff000000);
                } else {
                    lp.setHexPattern(StateMachine.nullPattern, false);
                    lp.setOffColor(0xff808080);
                    lp.setBackgroundColor(0xff606060);
                }
            }
            tvProjectName.setText(p.name);
            itemView.setBackgroundColor(isSelected ? 0x400000ff : 0x00000000);
        }

        private static int dpToPixels(View view, int value) {
            return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    value, view.getResources().getDisplayMetrics()) + .5f);
        }

    }

    public ProjectListAdapter(ProjectList projectList) {
        pl = projectList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindStateMachine(pl.getPreview(position), pl.getSelIndex() == position);
        holder.itemView.setOnClickListener(v -> {
            int oldIndex = pl.getSelIndex();
            pl.selectProject(position);
            notifyItemChanged(oldIndex);
            notifyItemChanged(pl.getSelIndex());
        });
    }

    @Override
    public int getItemCount() {
        return pl.getProjectCount();
    }

}
