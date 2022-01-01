package com.worldscience.chatszone.Activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.worldscience.chatszone.Adapters.TopStatusAdapter;
import com.worldscience.chatszone.Adapters.UsersAdapter;
import com.worldscience.chatszone.Models.Status;
import com.worldscience.chatszone.Models.User;
import com.worldscience.chatszone.Models.UserStatus;
import com.worldscience.chatszone.R;
import com.worldscience.chatszone.databinding.ActivityMainBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdapter usersAdapter;
    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog dialog;

    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.fetchAndActivate().addOnSuccessListener(aBoolean -> {

            String backgroundImage = mFirebaseRemoteConfig.getString("backgroundImage");
            Glide.with(MainActivity.this)
                    .load(backgroundImage)
                    .into(binding.backgroundImage);

            /* Toolbar Color */
            String toolbarColor = mFirebaseRemoteConfig.getString("toolbarColor");
            String toolBarImage = mFirebaseRemoteConfig.getString("toolbarImage");
            boolean isToolBarImageEnabled = mFirebaseRemoteConfig.getBoolean("toolBarImageEnabled");



            if(isToolBarImageEnabled) {
                Glide.with(MainActivity.this)
                        .load(toolBarImage)
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull @NotNull Drawable resource, @Nullable @org.jetbrains.annotations.Nullable Transition<? super Drawable> transition) {
                                Objects.requireNonNull(getSupportActionBar())
                                        .setBackgroundDrawable(resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {

                            }
                        });
            } else {
                Objects.requireNonNull(getSupportActionBar())
                        .setBackgroundDrawable
                                (new ColorDrawable(Color.parseColor(toolbarColor)));
            }

        });

        database = FirebaseDatabase.getInstance();

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("token", token);
                    database.getReference()
                            .child("users")
                            .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                            .updateChildren(map);
                    //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                });


        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);


        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        database.getReference().child("users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        usersAdapter = new UsersAdapter(this, users);
        statusAdapter = new TopStatusAdapter(this, userStatuses);
//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);
        binding.statusList.setAdapter(statusAdapter);

        binding.recyclerView.setAdapter(usersAdapter);

        binding.recyclerView.showShimmerAdapter();
        binding.statusList.showShimmerAdapter();

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for(DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    if (user != null && !Objects.equals(user.getUid(), FirebaseAuth.getInstance().getUid()))
                        users.add(user);
                }
                binding.recyclerView.hideShimmerAdapter();
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    userStatuses.clear();
                    for(DataSnapshot storySnapshot : snapshot.getChildren()) {
                        UserStatus status = new UserStatus();
                        status.setName(storySnapshot.child("name").getValue(String.class));
                        status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                        status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));

                        ArrayList<Status> statuses = new ArrayList<>();

                        for(DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {
                            Status sampleStatus = statusSnapshot.getValue(Status.class);
                            statuses.add(sampleStatus);
                        }

                        status.setStatuses(statuses);
                        userStatuses.add(status);
                    }
                    binding.statusList.hideShimmerAdapter();
                    statusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        binding.bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.status) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 75);
            }
            return false;
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null) {
            if(data.getData() != null) {
                dialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child("status").child(date.getTime() + "");

                reference.putFile(data.getData()).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        reference.getDownloadUrl().addOnSuccessListener(uri -> {
                            UserStatus userStatus = new UserStatus();
                            userStatus.setName(user.getName());
                            userStatus.setProfileImage(user.getProfileImage());
                            userStatus.setLastUpdated(date.getTime());

                            HashMap<String, Object> obj = new HashMap<>();
                            obj.put("name", userStatus.getName());
                            obj.put("profileImage", userStatus.getProfileImage());
                            obj.put("lastUpdated", userStatus.getLastUpdated());

                            String imageUrl = uri.toString();
                            Status status = new Status(imageUrl, userStatus.getLastUpdated());

                            database.getReference()
                                    .child("stories")
                                    .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                                    .updateChildren(obj);

                            database.getReference().child("stories")
                                    .child(FirebaseAuth.getInstance().getUid())
                                    .child("statuses")
                                    .push()
                                    .setValue(status);

                            dialog.dismiss();
                        });
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        if (currentId != null) {
            database.getReference().child("presence").child(currentId).setValue("Online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(Objects.requireNonNull(currentId)).setValue("Offline");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.group:
                startActivity(new Intent(MainActivity.this, GroupChatActivity.class));
                break;
            case R.id.search:
                Toast.makeText(this, "Search clicked.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.settings:
                Toast.makeText(this, "Settings Clicked.", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}