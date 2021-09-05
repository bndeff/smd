package app.smd;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.ViewHolder> {

    private final ProjectList pl;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LedGridView ledPreview1;
        private final LedGridView ledPreview2;
        private final LedGridView ledPreview3;
        private final TextView tvProjectName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ledPreview1 = (LedGridView) itemView.findViewById(R.id.ledPreview1);
            ledPreview2 = (LedGridView) itemView.findViewById(R.id.ledPreview2);
            ledPreview3 = (LedGridView) itemView.findViewById(R.id.ledPreview3);
            tvProjectName = (TextView) itemView.findViewById(R.id.tvProjectName);
        }

        @SuppressLint("ResourceAsColor")
        public void bindStateMachine(String preview, boolean isSelected) {
            ledPreview1.setHexPattern(preview.substring(0, 16), false);
            ledPreview2.setHexPattern(preview.substring(16, 32), false);
            ledPreview3.setHexPattern(preview.substring(32, 48), false);
            tvProjectName.setText(preview.substring(48));
            itemView.setBackgroundColor(isSelected ? 0x400000ff : 0x00000000);
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
            pl.selectProject(position);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return pl.getProjectCount();
    }

}
