package io.github.cmw025.nifty;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class ListFragmentPersonal extends Fragment {


    // Assumming calling from inside project
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);

        // Setup D&D feature and RecyclerView
        RecyclerViewDragDropManager dragMgr = new RecyclerViewDragDropManager();

        dragMgr.setInitiateOnMove(false);
        dragMgr.setInitiateOnLongPress(true);

        RecyclerView recyclerView =  (RecyclerView) v.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager mgr = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mgr);

        // Divider decoration
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), mgr.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        // Set Adapter
        RecyclerViewAdapter adapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(dragMgr.createWrappedAdapter(adapter));

        // Set up FireBase
        DatabaseReference fb = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String uid = user.getUid();

        DatabaseReference tasksRef = fb.child("usrs").child(uid).child("tasks");
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot data) {
                List<TaskModel> taskList = new ArrayList<>();
                for (DataSnapshot child: data.getChildren()) {
                    TaskModel task = child.getValue(TaskModel.class);
                    taskList.add(task);
                }
                Collections.reverse(taskList);
                adapter.updateItems(taskList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Set add item listener
        // Set up EditText add-task listener
        EditText toDo = ((EditText)v.findViewById(R.id.add_todo));
        toDo.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, @NonNull KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event == null ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            String title = toDo.getText().toString();

                            if (!TextUtils.isEmpty(title)) {
                                // Get reference to "fb/tasks/new_task"
                                DatabaseReference ref = fb.child("tasks").push();
                                String taskKey = ref.getKey();

                                // Create TaskModel
                                long id = longHash(taskKey);
                                TaskModel task = new TaskModel(title, "", id, taskKey, uid);

                                // Add task to FireBase
                                ref.setValue(task);
                                fb.child("usrs").child(uid).child("tasks").child(taskKey).setValue(task);

                                // Get project ref
                                DatabaseReference projectRef = fb.child("projects").child(uid);

                                // Retrieve current project, add task to project, and update FireBase
                                projectRef.child("tasks").child(taskKey).setValue(task);
                                // fb.child("usrs").child(uid).child("projects").child(projectFireBaseID).child("tasks").child(taskKey).setValue(task);

                                toDo.setText("");
                            }
                            return false; // consume.
                        }
                        return false; // pass on to other listeners.
                    }
                });


        // NOTE: need to disable change animations to ripple effect work properly
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        dragMgr.attachRecyclerView(recyclerView);

        return v;
    }

    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        ImageView signOutBtn = getActivity().findViewById(R.id.sign_out_list);
        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), PersonalInfoActivity.class));
            }
        });
    }

    // Helper function to create RecyclerView ID from task FireBase ID
    private static long longHash(String string) {
        long h = 98764321261L;
        int l = string.length();
        char[] chars = string.toCharArray();

        for (int i = 0; i < l; i++) {
            h = 31 * h + chars[i];
        }
        return h;
    }
}
