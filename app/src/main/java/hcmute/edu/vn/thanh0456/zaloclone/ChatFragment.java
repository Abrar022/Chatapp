package hcmute.edu.vn.thanh0456.zaloclone;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import hcmute.edu.vn.thanh0456.zaloclone.activities.ChatActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.SearchActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.SignInActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.UsersActivity;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.RecentConversationAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.TopStoryAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.FragmentChatBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.ConversationListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.Story;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.models.UserStory;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment implements ConversationListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FragmentChatBinding binding;
    private PreferenceManager preferenceManager;
    private ArrayList<ChatMessage> conversations;
    private ArrayList<UserStory> userStories;
    private RecentConversationAdaptor recentConversationAdaptor;
    private TopStoryAdaptor topStoryAdaptor;
    private FirebaseFirestore database;
    private String imageURL = null;
    private StorageTask uploadTask;
    private Uri imageUri;
    ArrayList<Task> tasks = new ArrayList<>();
    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ExploreFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }


    private void setListeners() {
//
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getActivity().getApplicationContext(), UsersActivity.class)));

        binding.createStory.setOnClickListener(v -> createStory());

        binding.inputSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity().getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        });
    }

    private void createStory() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent.createChooser(intent, "Select Image"), 438);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {

            imageUri = data.getData();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Story Files");
            Date date = new Date();
            StorageReference imagePath = storageReference.child(date.getTime() + "." + "jpg");
            uploadTask = imagePath.putFile(imageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    showToast(task.getException().getMessage());
                    throw task.getException();
                }
                return imagePath.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadURL = (Uri) task.getResult();

                    imageURL = downloadURL.toString();


                    UserStory userStory = new UserStory();
                    userStory.name = preferenceManager.getString(Constants.KEY_NAME);
                    userStory.image = preferenceManager.getString(Constants.KEY_IMAGE);
                    userStory.lastUpdated = date.getTime();

                    Story story = new Story(imageURL, userStory.lastUpdated);


                    HashMap<String, Object> userStoryObj = new HashMap<>();
                    userStoryObj.put(Constants.KEY_NAME, userStory.name);
                    userStoryObj.put(Constants.KEY_IMAGE, userStory.image);
                    userStoryObj.put(Constants.KEY_LAST_UPDATED, userStory.lastUpdated);


                    database.collection(Constants.KEY_COLLECTION_USER_STORY)
                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .set(userStoryObj);
                    (database.collection(Constants.KEY_COLLECTION_USER_STORY)
                            .document(preferenceManager.getString(Constants.KEY_USER_ID)))
                            .collection(Constants.KEY_COLLECTION_STORY)
                            .add(story);
                }
            });
        }
    }


    private void listenStories() {
        database.collection(Constants.KEY_COLLECTION_USER_STORY)
                .addSnapshotListener(eventStoryListener);
    }
    private final EventListener<QuerySnapshot> eventStoryListener = ((value, error) -> {
        if (error != null) {
            return;
        }

        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    UserStory userStory = new UserStory();
                    userStory.name = documentChange.getDocument().getString(Constants.KEY_NAME);
                    userStory.image = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                    userStory.lastUpdated = documentChange.getDocument().getLong(Constants.KEY_LAST_UPDATED);
                    userStories.add(userStory);

//                    ArrayList<Story> stories = new ArrayList<>();

                    Task task = documentChange.getDocument().getReference().collection(Constants.KEY_COLLECTION_STORY)
                            .get();
                    tasks.add(task);
//                    documentChange.getDocument().getReference().collection(Constants.KEY_COLLECTION_STORY)
//                            .get()
//                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                                @Override
//                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                                        Story story = documentSnapshot.toObject(Story.class);
//                                        stories.add(story);
//
//                                    }
//                                    userStory.stories = stories;
//                                    userStories.add(userStory);
//                                    topStoryAdaptor.notifyDataSetChanged();
//                                }
//                            });
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {

                    UserStory userStory = new UserStory();
                    userStory.name = documentChange.getDocument().getString(Constants.KEY_NAME);
                    userStory.image = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                    userStory.lastUpdated = documentChange.getDocument().getLong(Constants.KEY_LAST_UPDATED);

                    documentChange.getDocument().getReference().collection(Constants.KEY_COLLECTION_STORY)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    ArrayList<Story> stories = new ArrayList<>();
                                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                        Story story = documentSnapshot.toObject(Story.class);
                                        stories.add(story);
                                    }
                                    for (UserStory userStory1 : userStories) {
                                        if (userStory1.name.equals(userStory.name)) {
                                            userStory1.stories = stories;

                                            Collections.sort(userStories, new Comparator<UserStory>() {
                                                @Override
                                                public int compare(UserStory o1, UserStory o2) {
                                                    return (o1.lastUpdated < o2.lastUpdated) ? -1 : ((o1.lastUpdated == o2.lastUpdated) ? 0 : 1);
                                                }
                                            });

                                            topStoryAdaptor.notifyDataSetChanged();
                                            break;
                                        }
                                    }
                                }
                            });

                }
            }
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                @Override
                public void onSuccess(List<Object> objects) {
                    int i = 0;
                    for (Object object : objects) {
                        ArrayList<Story> stories = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : ((QuerySnapshot) object)) {
                            Story story = documentSnapshot.toObject(Story.class);
                            stories.add(story);
                            userStories.get(i).stories = stories;
                        }
                        i += 1;
                    }
                    topStoryAdaptor.notifyItemRangeChanged(0, userStories.size());
                    binding.storiesRecyclerView.smoothScrollToPosition(0);
                    binding.storiesRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        }
    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        init();
//        loadUserDetails();
        setListeners();
        listenConversations();
        listenStories();
        return binding.getRoot();
    }


    private void init() {
        binding.imageProfile.setImageBitmap(getConversationImage(preferenceManager.getString(Constants.KEY_IMAGE)));
        conversations = new ArrayList<>();
        userStories = new ArrayList<>();
        recentConversationAdaptor = new RecentConversationAdaptor(conversations, preferenceManager.getString(Constants.KEY_USER_ID), this, getActivity());
        topStoryAdaptor = new TopStoryAdaptor(getActivity(), userStories);
        binding.conversationsRecyclerView.setAdapter(recentConversationAdaptor);
        binding.storiesRecyclerView.setAdapter(topStoryAdaptor);
        database = FirebaseFirestore.getInstance();
    }
//    private void loadUserDetails() {
//        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
//        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//        binding.imageProfile.setImageBitmap(bitmap);
//    }


    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }


    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        chatMessage.conversationId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                        for (ChatMessage conversation : conversations) {
                            if (conversation.conversationId.equals(chatMessage.receiverId)) {
                                conversations.remove(conversation);
                                break;
                            }
                        }
                    } else {
                        chatMessage.conversationId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                        for (ChatMessage conversation : conversations) {
                            if (conversation.conversationId.equals(chatMessage.senderId)) {
                                conversations.remove(conversation);
                                break;
                            }
                        }
                    }

                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }

            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            recentConversationAdaptor.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    });

//    private void signOut() {
//        showToast("Signing out...");
//        FirebaseFirestore database = FirebaseFirestore.getInstance();
//        DocumentReference documentReference =
//                database.collection(Constants.KEY_COLLECTION_USERS).document(
//                        preferenceManager.getString(Constants.KEY_USER_ID)
//                );
//        HashMap<String, Object> updates = new HashMap<>();
//        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
//        documentReference.update(updates)
//                .addOnSuccessListener(unused -> {
//                    FirebaseAuth.getInstance().signOut();
//                    preferenceManager.clear();
//                    startActivity(new Intent(getActivity().getApplicationContext(), SignInActivity.class));
//                    getActivity().finish();
//                })
//                .addOnFailureListener(e -> showToast("Unable to sign out"));
//    }


    @Override
    public void onConversationClicked(User user) {
        Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }


    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        recentConversationAdaptor.notifyDataSetChanged();
    }
}