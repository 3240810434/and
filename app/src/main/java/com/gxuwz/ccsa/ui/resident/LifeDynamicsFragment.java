package com.gxuwz.ccsa.ui.resident;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.adapter.PostAdapter;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.model.Post;
import com.gxuwz.ccsa.model.User;
import java.util.ArrayList;
import java.util.List;

public class LifeDynamicsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList = new ArrayList<>();

    private ImageView fabAdd;
    private ImageView btnRefresh;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_life_dynamics, container, false);

        // 初始化时获取最新用户信息
        updateCurrentUser();

        recyclerView = view.findViewById(R.id.recycler_view);
        fabAdd = view.findViewById(R.id.fab_add);
        btnRefresh = view.findViewById(R.id.btn_refresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(getContext(), postList, currentUser);
        recyclerView.setAdapter(adapter);

        // 点击发布
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MediaSelectActivity.class);
                if (currentUser != null) {
                    intent.putExtra("user", currentUser);
                }
                startActivity(intent);
            });
        }

        // 刷新按钮逻辑
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                ObjectAnimator rotate = ObjectAnimator.ofFloat(btnRefresh, "rotation", 0f, 360f);
                rotate.setDuration(800);
                rotate.setInterpolator(new LinearInterpolator());
                rotate.start();

                loadPosts();
                recyclerView.smoothScrollToPosition(0);
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 页面从后台回到前台（或从其他Activity返回）时刷新
        updateCurrentUser();
        loadPosts();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // 页面在 Fragment 之间切换（如底部导航栏切换）时刷新
        if (!hidden) {
            updateCurrentUser();
            loadPosts();
        }
    }

    private void updateCurrentUser() {
        if (getActivity() instanceof ResidentMainActivity) {
            currentUser = ((ResidentMainActivity) getActivity()).getUser();
            if (adapter != null) {
                adapter.setCurrentUser(currentUser);
            }
        }
    }

    private void loadPosts() {
        new Thread(() -> {
            if (getContext() == null) return;
            AppDatabase db = AppDatabase.getInstance(getContext());

            // 1. 获取所有帖子
            List<Post> posts = db.postDao().getAllPosts();

            // 2. 【核心】遍历帖子，查询最新的用户信息覆盖旧数据
            // 这步是解决改名/改头像后列表不刷新的关键
            for (Post p : posts) {
                p.mediaList = db.postDao().getMediaForPost(p.id);
                p.commentCount = db.postDao().getCommentCount(p.id);

                // 根据帖子的 userId 去 User 表查最新的 User 对象
                User latestUser = db.userDao().getUserById(p.userId);
                if (latestUser != null) {
                    // 覆盖 Post 对象中旧的 userName 和 userAvatar
                    p.userName = latestUser.getName();
                    p.userAvatar = latestUser.getAvatar();
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    postList.clear();
                    postList.addAll(posts);
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }
}