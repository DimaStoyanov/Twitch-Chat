package ru.ifmo.android_2016.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Stream;

public class ErrorActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textView = new TextView(this);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(textView);

        String threadName = getIntent().getStringExtra("Thread");
        Throwable throwable = (Throwable) getIntent().getSerializableExtra("Throwable");

        textView.append("Error on thread " + threadName + "\n\n");
        textView.append(throwable.getMessage());

        Stream.of(throwable.getStackTrace())
                .forEach(stackTraceElement -> textView.append(stackTraceElement.toString()));
    }
}
