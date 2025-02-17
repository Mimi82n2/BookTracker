/**
*NotificationRequestedTabFragment
* Fragment that contains a listview which displays the books a
* user have requested.
* Allows the user to scan and view pickup location of book
 */
package com.example.booktracker.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.booktracker.Book;
import com.example.booktracker.MainActivity;
import com.example.booktracker.NotificationMessage;
import com.example.booktracker.R;
import com.example.booktracker.ScanBarcodeActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class NotificationRequestedTabFragment extends Fragment {

    private FirebaseFirestore db;
    ArrayList<Book> bookList;
    private String title;
    private String requestStatus;
    private String author;
    private String isbn;
    private String status;
    private String bookImg;
    private String owner;
    private String pickupLat;
    private String pickupLon;
    RequestedListAdapter myAdapter;

    public NotificationRequestedTabFragment() {
        // Required empty public constructor
    }

    public static NotificationRequestedTabFragment newInstance() {
        NotificationRequestedTabFragment fragment = new NotificationRequestedTabFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Sets up the layout of the requested tab
     * where users can see the requests they've made
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return view
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        bookList = new ArrayList<>();
        View view = inflater.inflate(R.layout.fragment_notification_requested_tab, container, false);
        ListView listView = view.findViewById(R.id.notificationRequestedListView);
        myAdapter = new RequestedListAdapter(getActivity(), R.layout.requested_list_item, bookList);
        listView.setAdapter(myAdapter);
        getInfoFromDB();
        return view;
    }

    /**
     * RequestedListAdapter that helps to display the fields of a request inside a listview item
     */
    private class RequestedListAdapter extends ArrayAdapter<Book>{
        private int layout;
        public RequestedListAdapter(@NonNull Context context, int resource, @NonNull List<Book> objects) {
            super(context, resource, objects);
            layout = resource;
        }


        /**
         * Allows us to set the attributes of each list item independently
         * at different positions
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout, parent, false);
            final RequestedViewHolder viewHolder = new RequestedViewHolder();
            viewHolder.title = convertView.findViewById(R.id.requested_book_title);
            viewHolder.status = convertView.findViewById(R.id.requested_book_status);
            viewHolder.owner = convertView.findViewById(R.id.requested_book_owner);
            viewHolder.scan_button = convertView.findViewById(R.id.scan_received_button);
            viewHolder.book_pickup_button =  convertView.findViewById(R.id.requested_book_pickup_button);
            viewHolder.book_pickup_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Shows the location the owner has picked for pickup
                    final Intent pickup = new Intent(getContext(), PickupLocation.class);
                    db = FirebaseFirestore.getInstance();

                    DocumentReference docRef = db.collection("Users")
                            .document(MainActivity.current_user)
                            .collection("Requested Books")
                            .document(getItem(position).getIsbn());
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                    if (document.get("book.pickupLat") == null || document.get("book.pickupLon") == null){
                                        Toast.makeText(getActivity(), "Location Has Not Been Picked By Owner!", Toast.LENGTH_LONG).show();
                                    }else {
                                        pickupLat = document.get("book.pickupLat").toString();
                                        pickupLon = document.get("book.pickupLon").toString();
                                        pickup.putExtra("pickupLat", pickupLat);
                                        pickup.putExtra("pickupLon", pickupLon);
                                        Log.d(TAG, "onComplete: " + pickupLat + "," + pickupLon);
                                        startActivity(pickup);
                                    }
                                } else {
                                    Log.d(TAG, "No such document");
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                            }
                        }
                    });

                }
            });
            viewHolder.scan_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Scan that confirms the user has received the book
                    status = getItem(position).getStatus();
                    isbn = getItem(position).getIsbn();
                    owner = getItem(position).getOwner();
                    author = getItem(position).getAuthor();
                    title = getItem(position).getTitle();
                    requestStatus = getItem(position).getRequestStatus();
                    Intent intent = new Intent(getActivity(), ScanBarcodeActivity.class);
                    startActivityForResult(intent, 103);
                }
            });
            viewHolder.title.setText("Title: "+getItem(position).getTitle());
            viewHolder.owner.setText("Owner: " + getItem(position).getOwner());
            viewHolder.status.setText("Status: "+getItem(position).getRequestStatus());
            if(getItem(position).getRequestStatus().equals("Accepted")){
                viewHolder.book_pickup_button.setVisibility(View.VISIBLE);
                viewHolder.scan_button.setVisibility(View.VISIBLE);
            }
            convertView.setTag(viewHolder);

            return convertView;
        }
    }

    /**
     * RequestedViewHolder helps to hold and set the values of each list item
     */
    private class RequestedViewHolder{
        TextView title;
        TextView status;
        TextView owner;
        Button scan_button;
        Button book_pickup_button;
    }

    /**
     *Gets information to be displayed from the database and store it in the bookList
     */
    private void getInfoFromDB(){
        bookList.clear();
        db = FirebaseFirestore.getInstance();
        db.collection("Users").document(MainActivity.current_user)
                .collection("Requested Books")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData().get("book"));
                                Map<String, Object> book = (Map<String, Object>) document.getData().get("book");
                                isbn = document.getId();
                                title = (String) book.get("title");
                                author = (String) book.get("author");
                                status = (String)book.get("status");
                                bookImg = (String) book.get("image");
                                owner = (String) book.get("owner");
                                requestStatus = (String)book.get("requestStatus");
                                Book newBook = new Book(title, author, isbn, status, owner);
                                newBook.setRequestStatus(requestStatus);
                                newBook.setImage(bookImg);
                                if (requestStatus!=null && (requestStatus.equals("Accepted") || requestStatus.equals("Pending Request"))) {
                                    bookList.add(newBook);
                                }

                            }
                            myAdapter.notifyDataSetChanged(); 
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        Collections.sort(bookList, new Comparator<Book>() {
                            @Override
                            public int compare(Book o1, Book o2) {
                                return o1.getRequestStatus().compareTo(o2.getRequestStatus());
                            }
                        });
                    }
                });
    }

    /**
     * Handles what happens when a user selects to scan a book
     * Allows user to scan book to confirm receival and change status in database accordingly
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Handles the result of scanning
        if (requestCode == 103) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    final Barcode barcode = data.getParcelableExtra("barcode"); //Contains barcode
                    if (barcode.displayValue.equals(isbn)){
                        if(status.equals("Borrowed (Pending)")) {
                            Map<String, Book> book = new HashMap<>();
                            Book newBook = new Book(title, author, isbn, "Borrowed", owner);
                            newBook.setRequester(MainActivity.current_user);
                            book.put("book", newBook);
                            db = FirebaseFirestore.getInstance();
                            //Add the book to the current user's Borrowed Books collection
                            db.collection("Users")
                                    .document(MainActivity.current_user)
                                    .collection("Borrowed Books")
                                    .document(isbn)
                                    .set(book);
                            //Add the book to the owner's Book Lent Out collection
                            db.collection("Users")
                                    .document(owner)
                                    .collection("Books Lent Out")
                                    .document(isbn)
                                    .set(book);
                            //Remove the book from the current user's Requested Books collection
                            db.collection("Users")
                                    .document(MainActivity.current_user)
                                    .collection("Requested Books")
                                    .document(isbn)
                                    .delete();
                            //Set the status of the book to Borrowed in the owners collection
                            db.collection("Users")
                                    .document(owner)
                                    .collection("Books")
                                    .document(isbn)
                                    .update("book.status", "Borrowed");
                            Toast.makeText(getActivity(), "Successfully Borrowed!", Toast.LENGTH_LONG).show();

                            Map<String, NotificationMessage> notification = new HashMap<>();
                            Date newDate = new Date();
                            NotificationMessage newNotificationMessage = new NotificationMessage("Book Successfully Lent!",
                                    MainActivity.current_user + " has Received and Scanned the Following Book: \n" + title + "\n" + "isbn: " + isbn,
                                    newDate.toString());
                            notification.put("notification", newNotificationMessage);
                            //Adds a new notification to the owner's Notifications collection to indicate book has been borrowed
                            db.collection("Users")
                                    .document(owner)
                                    .collection("Notifications")
                                    .document(newDate.toString()).set(notification);
                            myAdapter.notifyDataSetChanged();
                        }
                        else{
                            Toast.makeText(getActivity(),"Owner have not scanned this book!!",Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(getActivity(),"ISBN does not match!!\n"+isbn+"\n"+barcode.displayValue,Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }
}
