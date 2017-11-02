package ru.ifmo.android_2016.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.Collections;
import java.util.List;

import ru.ifmo.android_2016.irc.utils.FileUtils;

public final class ErrorActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<String> files = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(recyclerView);
        recyclerView.setAdapter(new Adapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(null);

        files = Stream.of(getCacheDir().list()).filter(s -> s.startsWith("log")).collect(Collectors.toList());
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private String toString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(throwable.getMessage()).append('\n');

        Stream.of(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .forEach(s -> stringBuilder.append(s).append('\n'));

        return stringBuilder.toString();
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new TextView(ErrorActivity.this));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(files.get(position));
            holder.textView.setOnClickListener(v -> {
                TextView textView = (TextView) v;
                textView.setText(ErrorActivity.this.toString(FileUtils.readObjectFromFile(getCacheDir() + "/" + textView.getText())));
                v.setClickable(false);
            });
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(TextView itemView) {
                super(itemView);
                textView = itemView;
            }
        }
    }
}
