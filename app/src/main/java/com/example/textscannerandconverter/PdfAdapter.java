package com.example.textscannerandconverter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder{
        public Button nameTextView;

        public ViewHolder(View itemView){
            super(itemView);

            nameTextView = (Button) itemView.findViewById(R.id.pdf_name);
        }
    }

    private List<PDF> pdfs;

    public PdfAdapter(List<PDF> pdfs){
        this.pdfs = pdfs;
    }

    @NonNull
    @Override
    public PdfAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contextView = inflater.inflate(R.layout.item_pdf, viewGroup, false);

        ViewHolder viewHolder = new ViewHolder(contextView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PdfAdapter.ViewHolder viewHolder, int position) {
        final PDF pdf = pdfs.get(position);
        final String fileName = pdf.getName();

        TextView textView = viewHolder.nameTextView;
        textView.setText(fileName);

        viewHolder.nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fileName.contains(".pdf")) {
                    Intent intent = new Intent(v.getContext(), PdfViewingActivity.class);
                    intent.putExtra("Name", fileName);
                    v.getContext().startActivity(intent);
                } else if(fileName.contains(".txt")) {
                    Intent intent = new Intent(v.getContext(), TextViewingActivity.class);
                    intent.putExtra("Name", fileName);
                    v.getContext().startActivity(intent);
                }
            }
        });

        viewHolder.nameTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                removeItem(v, pdf);

                return true;
            }
        });
    }

    private void removeItem(View v, PDF pdf){
        int position = pdfs.indexOf(pdf);

        File file = new File(pdf.getPath());
        boolean deleted = file.delete();
        Toast.makeText(v.getContext(), "The result for deleting is: " + deleted, Toast.LENGTH_SHORT).show();

        pdfs.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return pdfs.size();
    }
}
