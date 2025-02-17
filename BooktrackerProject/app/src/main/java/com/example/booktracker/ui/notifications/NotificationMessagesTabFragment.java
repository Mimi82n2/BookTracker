/**
*NotificationMessagesTabFragment
* Fragment that is used to display the notifications that the current user
* have received
 */
package com.example.booktracker.ui.notifications;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.booktracker.MainActivity;
import com.example.booktracker.NotificationMessage;
import com.example.booktracker.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class NotificationMessagesTabFragment extends Fragment {
    public static Boolean newNotification = false;

    private FirebaseFirestore db;
    private String title;
    private String message;
    private String date;
    private NotificationListAdapter myAdapter;
    private ArrayList<NotificationMessage> notificationMessageList;

    public NotificationMessagesTabFragment() {
        // Required empty public constructor
    }

    public static NotificationMessagesTabFragment newInstance() {
        NotificationMessagesTabFragment fragment = new NotificationMessagesTabFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    /**
     * Sets up the layout for the Alerts tab
     * set up the delete button for each message
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //Set up adapter and list
        notificationMessageList = new ArrayList<>();
        View view = inflater.inflate(R.layout.fragment_notification_messages, container, false);
        myAdapter = new NotificationListAdapter(getActivity(),R.layout.notification_list_item,notificationMessageList);
        ListView listView = view.findViewById(R.id.notificationMessagesListView);
        listView.setAdapter(myAdapter);
        getInfoFromDB();
        myAdapter.notifyDataSetChanged();
        return view;
    }

    /**
    *NotificationListAdapter
    * Helps to display the notification objects inside a listview
     */
    private class NotificationListAdapter extends ArrayAdapter<NotificationMessage>{
        private int layout;
        public NotificationListAdapter(@NonNull Context context, int resource, @NonNull List<NotificationMessage> objects) {
            super(context, resource, objects);
            layout = resource;
        }

        /**
         * Allows us to set the attributes of each list item at different positions
         * in the list
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout, parent,false);
            final NotificationViewHolder viewHolder = new NotificationViewHolder();
            viewHolder.title = convertView.findViewById(R.id.notification_item_title);
            viewHolder.message = convertView.findViewById(R.id.notification_item_message);
            viewHolder.date = convertView.findViewById(R.id.notification_item_date);
            viewHolder.deletebtn = convertView.findViewById(R.id.notification_item_delete_btn);
            viewHolder.deletebtn.setOnClickListener(new View.OnClickListener() {
                //Deletes the notification clicked
                @Override
                public void onClick(View v) {
                    db.collection("Users")
                            .document(MainActivity.current_user)
                            .collection("Notifications")
                            .document(getItem(position).getReceiveDate().toString())
                            .delete();
                    getInfoFromDB();
                    myAdapter.notifyDataSetChanged();
                }
            });
            viewHolder.title.setText(getItem(position).getTitle());
            viewHolder.message.setText(getItem(position).getMessage());
            viewHolder.date.setText(getItem(position).getReceiveDate().toString());
            convertView.setTag(viewHolder);

            return convertView;
        }
    }

    /**
     * ViewHolder that helps to contain the attributes of each list item
     */
    private class NotificationViewHolder{
        TextView title;
        TextView message;
        TextView date;
        Button deletebtn;
    }

    /**
     *Clears the notificationMessageList to be shown
     *re-adds new notifications from the database of the user
     * sort the list by date, descending
     */
    private void getInfoFromDB(){
        notificationMessageList.clear();
        db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .document(MainActivity.current_user)
                .collection("Notifications")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for (QueryDocumentSnapshot document:task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData().get("notification"));
                                Map<String, Object> notification = (Map<String, Object>) document.getData().get("notification");
                                title = (String) notification.get("title");
                                message = (String) notification.get("message");
                                date = (String) notification.get("receiveDate");
                                NotificationMessage newMessage = new NotificationMessage(title, message, date);
                                notificationMessageList.add(newMessage);
                            }
                        }
                        else{
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        Collections.sort(notificationMessageList, new Comparator<NotificationMessage>() {
                            @Override
                            public int compare(NotificationMessage o1, NotificationMessage o2) {
                                return o2.getReceiveDate().compareTo(o1.getReceiveDate());
                            }
                        });
                        myAdapter.notifyDataSetChanged();

                    }
                });
    }



}
