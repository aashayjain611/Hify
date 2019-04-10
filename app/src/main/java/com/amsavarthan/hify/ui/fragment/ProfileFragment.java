package com.amsavarthan.hify.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amsavarthan.hify.R;
import com.amsavarthan.hify.adapters.PostsAdapter;
import com.amsavarthan.hify.adapters.PostsAdapter_v19;
import com.amsavarthan.hify.models.Post;
import com.amsavarthan.hify.ui.activities.MainActivity;
import com.amsavarthan.hify.ui.activities.notification.ImagePreview;
import com.amsavarthan.hify.utils.AnimationUtil;
import com.amsavarthan.hify.utils.database.UserHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tylersuehr.esr.EmptyStateRecyclerView;
import com.tylersuehr.esr.TextStateDisplay;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private View mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_profile_view, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadFragment(new ProfileFragment.AboutFragment());

        BottomNavigationView bottomNavigationView=mView.findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_profile:
                        loadFragment(new ProfileFragment.AboutFragment());
                        break;
                    case R.id.action_posts:
                        loadFragment(new ProfileFragment.PostsFragment());
                        break;
                    case R.id.action_saved:
                        loadFragment(new ProfileFragment.SavedFragment());
                        break;
                    case R.id.action_edit:
                        loadFragment(new ProfileFragment.EditFragment());
                        break;
                    default:
                        loadFragment(new ProfileFragment.AboutFragment());

                }
                return true;
            }
        });

        bottomNavigationView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_profile:
                        break;
                    case R.id.action_posts:
                        break;
                    case R.id.action_saved:
                        break;
                    case R.id.action_edit:
                        break;

                }
            }
        });


    }

    private void loadFragment(Fragment fragment) {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit();
    }

    public static class PostsFragment extends Fragment {

        List<Post> postList;
        PostsAdapter mAdapter;
        private EmptyStateRecyclerView mRecyclerView;
        private View statsheetView;
        private BottomSheetDialog mmBottomSheetDialog;
        private ProgressBar pbar;
        private PostsAdapter_v19 mAdapter_v19;

        public PostsFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            statsheetView = ((AppCompatActivity)getActivity()).getLayoutInflater().inflate(R.layout.stat_bottom_sheet_dialog, null);
            mmBottomSheetDialog = new BottomSheetDialog(rootView.getContext());
            mmBottomSheetDialog.setContentView(statsheetView);
            mmBottomSheetDialog.setCanceledOnTouchOutside(true);

            pbar=rootView.findViewById(R.id.pbar);

            postList=new ArrayList<>();

            mRecyclerView=rootView.findViewById(R.id.recyclerView);

            mRecyclerView.setStateDisplay(EmptyStateRecyclerView.STATE_EMPTY,
                    new TextStateDisplay(rootView.getContext(),"No posts found","Add some posts to see them here."));

            mRecyclerView.setStateDisplay(EmptyStateRecyclerView.STATE_ERROR,
                    new TextStateDisplay(rootView.getContext(),"Sorry for inconvenience","Something went wrong :("));

            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext(), LinearLayoutManager.VERTICAL, false));
            mRecyclerView.setHasFixedSize(true);
            if(Build.VERSION.SDK_INT>19) {
                mAdapter = new PostsAdapter(postList, rootView.getContext(), getActivity(), mmBottomSheetDialog, statsheetView, false);
                mRecyclerView.setAdapter(mAdapter);
            }else{
                mAdapter_v19 = new PostsAdapter_v19(postList, rootView.getContext(), getActivity(), mmBottomSheetDialog, statsheetView, false);
                mRecyclerView.addItemDecoration(new DividerItemDecoration(rootView.getContext(),DividerItemDecoration.VERTICAL));
                mRecyclerView.setAdapter(mAdapter_v19);
            }
            pbar.setVisibility(View.VISIBLE);
           getPosts();

            return rootView;
        }

        private void getPosts() {

            FirebaseFirestore.getInstance().collection("Posts")
                    .whereEqualTo("userId", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot querySnapshot) {

                            if(!querySnapshot.isEmpty()){

                                for(DocumentChange doc:querySnapshot.getDocumentChanges()){
                                    Post post = doc.getDocument().toObject(Post.class).withId(doc.getDocument().getId());
                                    postList.add(post);
                                    if(Build.VERSION.SDK_INT>19) {
                                        mAdapter.notifyDataSetChanged();
                                    }else{
                                        mAdapter_v19.notifyDataSetChanged();
                                    }
                                    pbar.setVisibility(View.GONE);
                                }


                            }else{
                                pbar.setVisibility(View.GONE);
                                mRecyclerView.invokeState(EmptyStateRecyclerView.STATE_EMPTY);
                            }

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pbar.setVisibility(View.GONE);
                            mRecyclerView.invokeState(EmptyStateRecyclerView.STATE_ERROR);
                            Log.e("Error",e.getMessage());
                        }
                    });

        }

    }

    public static class SavedFragment extends Fragment {

        List<Post> postList;
        PostsAdapter mAdapter;
        private EmptyStateRecyclerView mRecyclerView;
        private View statsheetView;
        private BottomSheetDialog mmBottomSheetDialog;
        private ProgressBar pbar;
        private PostsAdapter_v19 mAdapter_v19;

        public SavedFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            postList=new ArrayList<>();

            statsheetView = getActivity().getLayoutInflater().inflate(R.layout.stat_bottom_sheet_dialog, null);
            mmBottomSheetDialog = new BottomSheetDialog(rootView.getContext());
            mmBottomSheetDialog.setContentView(statsheetView);
            mmBottomSheetDialog.setCanceledOnTouchOutside(true);
            pbar=rootView.findViewById(R.id.pbar);

            mRecyclerView=rootView.findViewById(R.id.recyclerView);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext(), LinearLayoutManager.VERTICAL, false));
            mRecyclerView.setHasFixedSize(true);

            if(Build.VERSION.SDK_INT>19) {
                mAdapter = new PostsAdapter(postList, rootView.getContext(), getActivity(), mmBottomSheetDialog, statsheetView, false);
                mRecyclerView.setAdapter(mAdapter);
            }else{
                mAdapter_v19 = new PostsAdapter_v19(postList, rootView.getContext(), getActivity(), mmBottomSheetDialog, statsheetView, false);
                mRecyclerView.addItemDecoration(new DividerItemDecoration(rootView.getContext(),DividerItemDecoration.VERTICAL));
                mRecyclerView.setAdapter(mAdapter_v19);
            }

            mRecyclerView.setStateDisplay(EmptyStateRecyclerView.STATE_EMPTY,
                    new TextStateDisplay(rootView.getContext(),"No saved posts found","All your saved posts appear here."));

            mRecyclerView.setStateDisplay(EmptyStateRecyclerView.STATE_ERROR,
                    new TextStateDisplay(rootView.getContext(),"Sorry for inconvenience","Something went wrong :("));

            pbar.setVisibility(View.VISIBLE);
            getPosts();

            return rootView;
        }

        private void getPosts() {

            FirebaseFirestore.getInstance().collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                    .collection("Saved_Posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot querySnapshot) {

                            if(!querySnapshot.isEmpty()){
                                for(final DocumentChange doc:querySnapshot.getDocumentChanges()){

                                    FirebaseFirestore.getInstance().collection("Posts")
                                            .document(doc.getDocument().getId())
                                            .get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    if(documentSnapshot.exists()){
                                                        Post post = doc.getDocument().toObject(Post.class).withId(doc.getDocument().getId());
                                                        postList.add(post);
                                                        if(Build.VERSION.SDK_INT>19) {
                                                            mAdapter.notifyDataSetChanged();
                                                        }else{
                                                            mAdapter_v19.notifyDataSetChanged();
                                                        }
                                                        pbar.setVisibility(View.GONE);
                                                    }else{
                                                        FirebaseFirestore.getInstance().collection("Users")
                                                                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                .collection("Saved_Posts")
                                                                .document(doc.getDocument().getId())
                                                                .delete()
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        Log.e("Saved_users","Post not available");
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.e("Error",e.getMessage());
                                                                    }
                                                                });
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    pbar.setVisibility(View.GONE);
                                                    mRecyclerView.invokeState(EmptyStateRecyclerView.STATE_ERROR);
                                                    Log.e("Error",e.getMessage());
                                                }
                                            });

                                }
                            }else{
                                pbar.setVisibility(View.GONE);
                                mRecyclerView.invokeState(EmptyStateRecyclerView.STATE_EMPTY);
                            }

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pbar.setVisibility(View.GONE);
                            mRecyclerView.invokeState(EmptyStateRecyclerView.STATE_ERROR);
                            Log.e("Error",e.getMessage());
                        }
                    });

        }



    }

    public static class AboutFragment extends Fragment {

        private FirebaseAuth mAuth;
        private FirebaseFirestore mFirestore;
        private UserHelper userHelper;

        private TextView name,username,email,location,post,friend,bio;
        private CircleImageView profile_pic;

        public AboutFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.frag_about_profile, container, false);


            mAuth = FirebaseAuth.getInstance();
            mFirestore = FirebaseFirestore.getInstance();

            profile_pic=rootView.findViewById(R.id.profile_pic);
            name=rootView.findViewById(R.id.name);
            username=rootView.findViewById(R.id.username);
            email=rootView.findViewById(R.id.email);
            location=rootView.findViewById(R.id.location);
            post=rootView.findViewById(R.id.posts);
            friend=rootView.findViewById(R.id.friends);
            bio=rootView.findViewById(R.id.bio);

            mFirestore.collection("Users")
                    .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                    .collection("Friends")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot documentSnapshots) {
                            //Total Friends
                            friend.setText(String.format(Locale.ENGLISH,"Total Friends : %d",documentSnapshots.size()));
                        }
                    });

            userHelper = new UserHelper(rootView.getContext());

            Cursor rs = userHelper.getData(1);
            rs.moveToFirst();

            String usernam=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_USERNAME));
            String nam = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_NAME));
            String emai = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_EMAIL));
            final String imag = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_IMAGE));
            String loc=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_LOCATION));
            String bi=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_BIO));

            if (!rs.isClosed()) {
                rs.close();
            }
            username.setText(String.format(Locale.ENGLISH,"@%s", usernam));
            name.setText(nam);
            email.setText(emai);
            location.setText(loc);
            bio.setText(bi);

            Glide.with(rootView.getContext())
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                    .load(imag)
                    .into(profile_pic);

            profile_pic.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    rootView.getContext().startActivity(new Intent(rootView.getContext(),ImagePreview.class)
                            .putExtra("url",imag));
                    return false;
                }
            });

            FirebaseFirestore.getInstance().collection("Posts")
                    .whereEqualTo("userId",mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot querySnapshot) {

                            post.setText(String.format(Locale.ENGLISH,"Total Posts : %d",querySnapshot.size()));

                        }
                    });


            return rootView;
        }



    }

    public static class EditFragment extends Fragment {

        private FirebaseAuth mAuth;
        private FirebaseFirestore mFirestore;
        private UserHelper userHelper;

        private EditText name,username,email,bio,location;
        private CircleImageView profile_pic;
        private TextView updatebtn,updatepicture,updatepassbtn;
        private AuthCredential credential;
        private static final int PICK_IMAGE =100 ;

        private String usernam;
        private String nam;
        private String emai;
        private String imag;
        private String bi;
        private String loc;
        private String pass;

        private Uri imageUri=null;
        private View rootView;

        public EditFragment() {
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.frag_edit_profile, container, false);

            mAuth = FirebaseAuth.getInstance();
            mFirestore = FirebaseFirestore.getInstance();

            name=rootView.findViewById(R.id.name);
            username=rootView.findViewById(R.id.username);
            email=rootView.findViewById(R.id.email);
            bio=rootView.findViewById(R.id.bio);
            location=rootView.findViewById(R.id.location);
            profile_pic=rootView.findViewById(R.id.profile_pic);
            updatebtn=rootView.findViewById(R.id.update);
            updatepassbtn=rootView.findViewById(R.id.change_password);
            updatepicture=rootView.findViewById(R.id.picture);

            userHelper = new UserHelper(rootView.getContext());

            Cursor rs = userHelper.getData(1);
            rs.moveToFirst();

            while(rs.moveToNext())
            {
                usernam=rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_USERNAME));
                nam = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_NAME));
                emai = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_EMAIL));
                imag = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_IMAGE));
                bi = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_BIO));
                loc = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_LOCATION));
                pass = rs.getString(rs.getColumnIndex(UserHelper.CONTACTS_COLUMN_PASS));
            }

            if (!rs.isClosed()) {
                rs.close();
            }


            updatepicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(isOnline()){

                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE);

                    }else{
                        Toast.makeText(rootView.getContext(), "Go online to change profile picture", Toast.LENGTH_SHORT).show();
                    }

                }
            });

            updatepassbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(isOnline()) {

                        new MaterialDialog.Builder(rootView.getContext())
                                .title("Change Password")
                                .content("Enter your old password.")
                                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                .input("Old password", "", new MaterialDialog.InputCallback() {
                                            @Override
                                            public void onInput(@NonNull MaterialDialog mdialog, CharSequence input) {

                                                if (!input.toString().equals(pass)) {
                                                    mdialog.dismiss();
                                                    Toast.makeText(rootView.getContext(), "Invalid old password", Toast.LENGTH_SHORT).show();
                                                } else {

                                                    new MaterialDialog.Builder(rootView.getContext())
                                                            .title("Change Password")
                                                            .content("Enter new password.")
                                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                                            .input("New password", "", new MaterialDialog.InputCallback() {
                                                                @Override
                                                                public void onInput(@NonNull final MaterialDialog mdialog, final CharSequence input) {

                                                                    if(TextUtils.isEmpty(input.toString())){
                                                                        mdialog.dismiss();
                                                                        Toast.makeText(rootView.getContext(), "Invalid new password", Toast.LENGTH_SHORT).show();
                                                                    }else if(input.toString().length()<6){
                                                                        Toast.makeText(rootView.getContext(), "Password should contain at least 6 characters", Toast.LENGTH_SHORT).show();
                                                                    }else{

                                                                        final ProgressDialog dialog=new ProgressDialog(rootView.getContext());
                                                                        dialog.setMessage("Please wait...");
                                                                        dialog.setIndeterminate(true);
                                                                        dialog.setCancelable(false);
                                                                        dialog.setCanceledOnTouchOutside(false);
                                                                        dialog.show();

                                                                        Objects.requireNonNull(mAuth.getCurrentUser()).updatePassword(input.toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {
                                                                                dialog.dismiss();
                                                                                mdialog.dismiss();
                                                                                userHelper.updateContactPassword(1,input.toString());
                                                                                Toast.makeText(rootView.getContext(), "Password updated", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        }).addOnFailureListener(new OnFailureListener() {
                                                                            @Override
                                                                            public void onFailure(@NonNull Exception e) {
                                                                                dialog.dismiss();
                                                                                mdialog.dismiss();
                                                                                Toast.makeText(rootView.getContext(), "Error updating password: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                Log.e("password error",e.getLocalizedMessage());
                                                                            }
                                                                        });
                                                                    }

                                                                }
                                                            }).show();


                                                }
                                            }
                                        }
                                )
                                .show();

                    }else{
                        Toast.makeText(rootView.getContext(), "Go online to change password", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            if(!isOnline()){
                rootView.findViewById(R.id.h_username).animate()
                        .alpha(0.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                rootView.findViewById(R.id.h_username).setVisibility(View.GONE);
                            }
                        }).start();

                rootView.findViewById(R.id.h_email).animate()
                        .alpha(0.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                rootView.findViewById(R.id.h_email).setVisibility(View.GONE);
                            }
                        }).start();

            }

            username.setText(usernam);
            name.setText(nam);
            email.setText(emai);
            bio.setText(bi);
            location.setText(loc);

            Glide.with(rootView.getContext())
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                    .load(imag)
                    .into(profile_pic);

            profile_pic.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    rootView.getContext().startActivity(new Intent(rootView.getContext(),ImagePreview.class)
                            .putExtra("url",imag));
                    return false;
                }
            });

            updatebtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final ProgressDialog dialog=new ProgressDialog(getActivity());
                    dialog.setIndeterminate(true);
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);

                    if(isOnline()){

                        final DocumentReference userDocument=mFirestore.collection("Users").document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

                        if(imageUri!=null){

                            dialog.setMessage("Updating Details....");
                            dialog.show();

                            final String userUid = mAuth.getCurrentUser().getUid();
                            final StorageReference user_profile = FirebaseStorage.getInstance().getReference().child("images").child(userUid + ".png");
                            user_profile.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {

                                    if(task.isSuccessful()){

                                        user_profile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(final Uri uri) {
                                                Map<String,Object> map=new HashMap<>();
                                                map.put("image",uri.toString());

                                                userDocument.update(map)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                profile_pic.setImageURI(imageUri);
                                                                MainActivity.imageView.setImageURI(imageUri);
                                                                userHelper.updateContactImage(1,uri.toString());
                                                                dialog.dismiss();
                                                                Log.i("Update","success");
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.i("Update","failed: "+e.getMessage());
                                                                dialog.dismiss();
                                                            }
                                                        });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e("Error","listen",e);

                                            }
                                        });

                                    }else{
                                        Log.e("Error","listen",task.getException());
                                    }

                                }
                            });

                        }

                        if(!email.getText().toString().equals(emai)){
                            dialog.setMessage("Updating Details....");

                            new MaterialDialog.Builder(rootView.getContext())
                                    .title("Email changed")
                                    .content("It seems that you have changed your email, re-enter your password to change.")
                                    .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                    .input("Password", "", new MaterialDialog.InputCallback() {
                                        @Override
                                        public void onInput(@NonNull MaterialDialog mdialog, CharSequence input) {
                                            if(!input.toString().equals(pass)){
                                                dialog.dismiss();
                                                mdialog.show();
                                                Toast.makeText(rootView.getContext(), "Invalid password", Toast.LENGTH_SHORT).show();
                                            }else{

                                                mdialog.dismiss();
                                                final FirebaseUser currentuser=mAuth.getCurrentUser();

                                                credential = EmailAuthProvider
                                                        .getCredential(Objects.requireNonNull(currentuser.getEmail()), input.toString());

                                                currentuser.reauthenticate(credential)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                currentuser.updateEmail(email.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {

                                                                        if (task.isSuccessful()) {

                                                                            currentuser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {

                                                                                    Map<String, Object> userMap = new HashMap<>();
                                                                                    userMap.put("email", email.getText().toString());

                                                                                    FirebaseFirestore.getInstance().collection("Users")
                                                                                            .document(mAuth.getCurrentUser().getUid())
                                                                                            .update(userMap)
                                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                                @Override
                                                                                                public void onSuccess(Void aVoid) {
                                                                                                    dialog.dismiss();
                                                                                                    userHelper.updateContactEmail(1,  email.getText().toString());
                                                                                                    Toast.makeText(rootView.getContext(),"Verification email sent.",Toast.LENGTH_SHORT).show();
                                                                                                    dialog.dismiss();
                                                                                                }
                                                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                                        @Override
                                                                                        public void onFailure(@NonNull Exception e) {
                                                                                            dialog.dismiss();
                                                                                            Log.e("Update","failed: "+e.getLocalizedMessage());
                                                                                        }
                                                                                    });

                                                                                }
                                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                                @Override
                                                                                public void onFailure(@NonNull Exception e) {
                                                                                    dialog.dismiss();
                                                                                    Log.e("Error",e.getLocalizedMessage());
                                                                                    dialog.dismiss();
                                                                                }
                                                                            });

                                                                        } else {

                                                                            Log.e("Update email error", Objects.requireNonNull(task.getException()).getMessage() + "..");
                                                                            dialog.dismiss();

                                                                        }

                                                                    }
                                                                });

                                                            }
                                                        });

                                            }
                                        }
                                    })
                                    .positiveText("Done")
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog mdialog, @NonNull DialogAction which) {
                                            dialog.show();
                                            mdialog.dismiss();
                                        }
                                    })
                                    .negativeText("Don't change my email")
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog mdialog, @NonNull DialogAction which) {
                                            dialog.dismiss();
                                            mdialog.dismiss();
                                        }
                                    })
                                    .cancelable(false)
                                    .canceledOnTouchOutside(false)
                                    .show();



                        }

                        if(!name.getText().toString().equals(nam)){

                            dialog.setMessage("Updating Details....");
                            dialog.show();

                            Map<String,Object> map=new HashMap<>();
                            map.put("name",name.getText().toString());

                            userDocument.update(map)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            MainActivity.username.setText(nam);
                                            userHelper.updateContactName(1,name.getText().toString());
                                            dialog.dismiss();
                                            Log.i("Update","success");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.i("Update","failed: "+e.getMessage());
                                            dialog.dismiss();
                                        }
                                    });

                        }

                        if(!username.getText().toString().equals(usernam)){

                            dialog.setMessage("Updating Details....");
                            dialog.show();

                            mFirestore.collection("Usernames")
                                    .document(username.getText().toString())
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(!documentSnapshot.exists()){

                                                mFirestore.collection("Usernames")
                                                        .document(usernam)
                                                        .delete()
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {

                                                                Map<String,Object> map=new HashMap<>();
                                                                map.put("username",username.getText().toString());

                                                                mFirestore.collection("Usernames")
                                                                        .document(username.getText().toString())
                                                                        .set(map)
                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {

                                                                                Map<String,Object> map=new HashMap<>();
                                                                                map.put("username",username.getText().toString());

                                                                                userDocument.update(map)
                                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                            @Override
                                                                                            public void onSuccess(Void aVoid) {
                                                                                                dialog.dismiss();
                                                                                                userHelper.updateContactUserName(1,username.getText().toString());
                                                                                                Log.i("Update","success");
                                                                                            }
                                                                                        })
                                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                                            @Override
                                                                                            public void onFailure(@NonNull Exception e) {
                                                                                                dialog.dismiss();
                                                                                                Log.i("Update","failed: "+e.getMessage());

                                                                                            }
                                                                                        });

                                                                            }
                                                                        })
                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                            @Override
                                                                            public void onFailure(@NonNull Exception e) {
                                                                                dialog.dismiss();
                                                                                Log.i("error","failed: "+e.getMessage());
                                                                            }
                                                                        });

                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                dialog.dismiss();
                                                                Log.i("error","failed: "+e.getMessage());

                                                            }
                                                        });



                                            }else{

                                                dialog.dismiss();
                                                Toast.makeText(rootView.getContext(), "Username already exists", Toast.LENGTH_SHORT).show();
                                                AnimationUtil.shakeView(username,rootView.getContext());

                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            dialog.dismiss();
                                            Log.e("error",""+e.getLocalizedMessage());
                                        }
                                    });

                        }

                        if(!bio.getText().toString().equals(bi)){

                            dialog.setMessage("Updating Details....");
                            dialog.show();

                            Map<String,Object> map=new HashMap<>();
                            map.put("bio",bio.getText().toString());

                            userDocument.update(map)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            dialog.dismiss();
                                            userHelper.updateContactBio(1,bio.getText().toString());
                                            Log.i("Update","success");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            dialog.dismiss();
                                            Log.i("Update","failed: "+e.getMessage());

                                        }
                                    });

                        }

                        if(!location.getText().toString().equals(loc)){

                            dialog.setMessage("Updating Details....");
                            dialog.show();

                            Map<String,Object> map=new HashMap<>();
                            map.put("location",location.getText().toString());

                            userDocument.update(map)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            dialog.dismiss();
                                            userHelper.updateContactLocation(1,location.getText().toString());
                                            Log.i("Update","success");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            dialog.dismiss();
                                            Log.i("Update","failed: "+e.getMessage());

                                        }
                                    });

                        }


                    }else{


                        if(!name.getText().toString().equals(nam)){

                            userHelper.updateContactName(1,name.getText().toString());

                        }

                        if(!bio.getText().toString().equals(bi)){

                            userHelper.updateContactBio(1,bio.getText().toString());

                        }

                        if(!location.getText().toString().equals(loc)){

                            userHelper.updateContactLocation(1,location.getText().toString());

                        }

                        Toast.makeText(rootView.getContext(),"Only your name,bio and location has been scheduled for update, when internet connection is available it will be updated.",Toast.LENGTH_LONG).show();


                    }

                }
            });

            return rootView;
        }

        public boolean isOnline() {
            ConnectivityManager cm =
                    (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert cm != null;
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if(requestCode==PICK_IMAGE){
                if(resultCode==RESULT_OK){
                    imageUri=data.getData();
                    // start crop activity
                    UCrop.Options options = new UCrop.Options();
                    options.setCompressionFormat(Bitmap.CompressFormat.PNG);
                    options.setCompressionQuality(100);
                    options.setShowCropGrid(true);

                    UCrop.of(imageUri, Uri.fromFile(new File(rootView.getContext().getCacheDir(), "hify_user_profile_picture.png")))
                            .withAspectRatio(1, 1)
                            .withOptions(options)
                            .start(getActivity());

                }
            }
            if (requestCode == UCrop.REQUEST_CROP) {
                if (resultCode == RESULT_OK) {
                    try {
                        File compressedFile= new Compressor(rootView.getContext()).setCompressFormat(Bitmap.CompressFormat.PNG).setQuality(50).setMaxHeight(96).setMaxWidth(96).compressToFile(new File(Objects.requireNonNull(Objects.requireNonNull(UCrop.getOutput(data)).getPath())));
                        profile_pic.setImageURI(Uri.fromFile(compressedFile));
						imageUri=Uri.fromFile(compressedFile);
                        Toast.makeText(rootView.getContext(), "Profile picture uploaded, click Save details button to apply changes", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(rootView.getContext(), "Profile photo updated click Save details to apply but unable to compress: "+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        profile_pic.setImageURI(imageUri);
                        imageUri = UCrop.getOutput(data);
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    Log.e("Error", "Crop error:" + Objects.requireNonNull(UCrop.getError(data)).getMessage());
                }
            }

        }
    }

}
