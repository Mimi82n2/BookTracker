/**
*AddFragment
* Fragment that allows the user to adds a new book to their collection
* and store the book in the database
 */
package com.example.booktracker.ui.add;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.example.booktracker.Book;
import com.example.booktracker.FetchBook;
import com.example.booktracker.MainActivity;
import com.example.booktracker.NotificationMessage;
import com.example.booktracker.R;
import com.example.booktracker.ScanBarcodeActivity;
import com.example.booktracker.ui.home.HomeFragment;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static com.example.booktracker.App.CHANNEL_1_ID;
import static com.example.booktracker.ui.home.HomeFragment.newLength;
import static com.example.booktracker.ui.notifications.NotificationMessagesTabFragment.newNotification;

public class AddFragment extends Fragment {
    private NotificationManagerCompat notificationManager;
    ArrayList<NotificationMessage> notificationList;
    private String notificationTitle;
    private FirebaseFirestore db2;
    private String message;
    private String date;

    private static final int GET_FROM_GALLERY = 1;
    private AddViewModel addViewModel;
    private Button addBook;
    private EditText author;
    private EditText title;
    private EditText isbn;
    private FirebaseFirestore db;
    private ImageView scanButton;
    private byte[] imageInfo;
    private ImageView imgV;

    /**
     * This sets up the layout for the fragment
     * Sets up click listeners for buttons
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     *      Return the view root
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        addViewModel =
                ViewModelProviders.of(this).get(AddViewModel.class);
       View root = inflater.inflate(R.layout.fragment_add, container,false);
        addViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
            }
        });
        author = root.findViewById(R.id.authorText);
        title = root.findViewById(R.id.titleText);
        isbn = root.findViewById(R.id.isbnText);
        addBook = root.findViewById(R.id.add_button);
        scanButton = root.findViewById(R.id.imageViewScan);

        ImageButton image = root.findViewById(R.id.addImage);
        imgV = root.findViewById(R.id.imageView);

        image.setOnClickListener(new View.OnClickListener() {
            //starts activity to get an image from gallery
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_FROM_GALLERY);
            }
        });
        scanButton.setOnClickListener(new View.OnClickListener() {
            //starts activity to scan for isbn
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ScanBarcodeActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        addBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewBook();
                author.setText("");
                title.setText("");
                isbn.setText("");
                Drawable resImg = ResourcesCompat.getDrawable(getResources(), R.drawable.image_needed, null);
                imgV.setImageDrawable(resImg);
                Bundle args = new Bundle();
                args.putString("key", title.getText().toString());
                Navigation.findNavController(view).navigate(R.id.navigation_dashboard, args);
            }
        });
        notificationList = new ArrayList<>();
        db2 = FirebaseFirestore.getInstance();
        db2.collection("Users")
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
                                notificationTitle = (String) notification.get("title");
                                message = (String) notification.get("message");
                                date = (String) notification.get("receiveDate");
                                NotificationMessage newMessage = new NotificationMessage(notificationTitle, message, date);
                                notificationList.add(newMessage);

                            }
                        }
                        else{
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        newLength = notificationList.size();
                        if (newLength > HomeFragment.size){
                            newNotification = true;
                            notificationManager = NotificationManagerCompat.from(getActivity());
                            Notification notification = new NotificationCompat.Builder(getActivity(), CHANNEL_1_ID)
                                    .setSmallIcon(R.drawable.ic_baseline_new)
                                    .setContentTitle(notificationList.get(newLength-1).getTitle())
                                    .setContentText(notificationList.get(newLength-1).getMessage())
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                    .build();

                            notificationManager.notify(1, notification);
                        }
                        HomeFragment.size = newLength;
                        Log.i(TAG, "SIZEed: "+ newLength + "length "+ HomeFragment.size);
                    }
                });
        newNotification = false;

        return root;
    }

    /**
     * This adds a new book into the database
     * Makes checks to ensure all details of the book are given
     * Stores the new book with status set to available
     */
    public void addNewBook(){
        //Attempts to add a new book using information entered
        String authorS = author.getText().toString();
        String titleS = title.getText().toString();
        String isbnS = isbn.getText().toString();
        String owner = MainActivity.current_user;
        String status = "available";
        if(authorS.isEmpty() || titleS.isEmpty() || isbnS.isEmpty()){
            //If not all information is entered
            Toast toast = Toast.makeText(getContext(), "Please enter missing information", Toast.LENGTH_SHORT);
            toast.show();
        }
        else{
            //Add new book to the user's database if book is valid
            Map<String, Book> book = new HashMap<>();
            Book bookObj = new Book(titleS, authorS, isbnS, status, owner);
            String imgString = null;
            if(imageInfo != null  && imageInfo.length > 0){
                Log.d(TAG, "addNewBook: book image is empty");
                imgString = Base64.encodeToString(imageInfo, Base64.DEFAULT);
            }
            else{
                imgString = "";
            }
            bookObj.setImage(imgString);
            book.put("book", bookObj);
            db = FirebaseFirestore.getInstance();
            db.collection("Users").document(MainActivity.current_user)
                    .collection("Books")
                    .document(isbnS).set(book);
            Toast toast = Toast.makeText(getContext(), "Book Successfully Added", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    /**
     * This brings the user to the scan barcode or gallery page
     * depending on the requestCode.
     * Allows users to add a image to their book or
     * scan the isbn for book details.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Implements the result of scanning a barcode as well as selecting a book image from gallery
        if (requestCode==0) {
            //Scanning barcode: attempts to get an barcode and if successful, sets the fields for the new book
            if (resultCode== CommonStatusCodes.SUCCESS) {
                if(data!=null) {
                    Barcode barcode = data.getParcelableExtra("barcode");
                    String queryString = barcode.displayValue;
                    isbn.setText(barcode.displayValue);
                    new FetchBook(title, author, isbn).execute(queryString);
                }
                else {
                    isbn.setText("Barcode not found");
                }
            }
        }
        else if(requestCode==GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            //Attempting to get an image from gallery
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getActivity().getContentResolver(), selectedImage);
                imgV.setImageBitmap(bitmap);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream);
                imageInfo = stream.toByteArray();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
