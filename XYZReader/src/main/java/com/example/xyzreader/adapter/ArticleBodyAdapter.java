package com.example.xyzreader.adapter;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class ArticleBodyAdapter extends RecyclerView.Adapter<ArticleBodyAdapter.ViewHolder> {

    private final BufferedReader streamReader;
    private List<String> lines = new ArrayList<>();

    public ArticleBodyAdapter(byte[] data) {
        BufferedInputStream buffer = new BufferedInputStream(new ByteArrayInputStream(data));
        streamReader = new BufferedReader(new InputStreamReader(buffer));
        try {
            while (true){
                String line = streamReader.readLine();
                if(line != null){
                    lines.add(line.trim().replace("\n\r", "<br />"));
                }else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(R.layout.article_body_line, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(lines.get(position));
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView lineTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            lineTextView = (TextView) itemView.findViewById(R.id.textViewLine);
        }

        void bind(String line){
            lineTextView.setText(Html.fromHtml(line));
        }
    }
}
