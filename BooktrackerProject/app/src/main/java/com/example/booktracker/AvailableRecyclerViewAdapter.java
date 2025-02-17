/**
*AvailableRecyclerViewAdapter
*Implements a RecyclerViewAdapter that is used for displaying
*available books in the home page
*/
package com.example.booktracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import static android.content.ContentValues.TAG;

public class AvailableRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private Context myContext;
    private List<Book> myData;

    public AvailableRecyclerViewAdapter(Context myContext, List<Book> myData){
        //Constructor
        this.myContext = myContext;
        this.myData = myData;
    }

    /**
     * Links the view to the desired xml file for the item to be listed
     * @param parent
     * @param viewType
     * @return ViewHolder for adapter
     */
    @NonNull
    @Override
    public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater myInflater = LayoutInflater.from(myContext);
        view = myInflater.inflate(R.layout.cardview_book_item, parent, false);
        return new RecyclerViewAdapter.MyViewHolder(view);
    }

    /**
     * Sets the image for each available book as well as the title
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.MyViewHolder holder, final int position) {

        holder.tv_book_title.setText(myData.get(position).getTitle());
        String encodedString = myData.get(position).getImage();

        if(encodedString != null){
            Log.d(TAG, "onBindViewHolder: pic exists");
            byte [] encodeByte= Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            holder.img_book_thumbnail.setImageBitmap(bitmap);
        }
        else{
            Log.d(TAG, "onBindViewHolder: pic doesnt exists");
        }
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            /*
            * When a user clicks on an available book in the home page,
            * transition to the requestPageFragment where they can request a book
             */
            @Override
            public void onClick(View view) {
                Navigation.createNavigateOnClickListener(R.id.navigation_home_to_requestPageFragment);
                Bundle args = new Bundle();
                args.putString("title", myData.get(position).getTitle());
                args.putString("author", myData.get(position).getAuthor());
                args.putString("status", myData.get(position).getStatus());
                args.putString("isbn", myData.get(position).getIsbn());
                args.putString("img",myData.get(position).getImage());
                args.putString("owner",myData.get(position).getOwner());
                Navigation.findNavController(view).navigate(R.id.requestPageFragment, args);
            }
        });
    }

    @Override
    public int getItemCount() {
        return myData.size();
    }
    public static class MyViewHolder extends RecyclerView.ViewHolder{
        /**
        * MyViewHolder is used to hold attributes to be shown/used
        * when displaying books in the home page
        */
        TextView tv_book_title;
        ImageView img_book_thumbnail;
        CardView cardView;

        public MyViewHolder(@NonNull View itemView) {
            //Constructor
            super(itemView);
            tv_book_title = (TextView) itemView.findViewById(R.id.book_title);
            img_book_thumbnail = (ImageView) itemView.findViewById(R.id.book_img);
            cardView = (CardView) itemView.findViewById(R.id.cardview_id);
        }
    }
}
