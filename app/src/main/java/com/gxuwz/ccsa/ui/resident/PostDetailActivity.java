package com.gxuwz.ccsa.ui.resident;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.adapter.CommentAdapter;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.model.Comment;
import com.gxuwz.ccsa.model.Post;
import com.gxuwz.ccsa.model.PostMedia;
import com.gxuwz.ccsa.model.User;
import com.gxuwz.ccsa.model.HistoryRecord;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {
    private Post post;
    private User currentUser;
    private RecyclerView rvComments;
    private CommentAdapter adapter;
    private List<Comment> commentList = new ArrayList<>();
    private EditText etComment;

    // Body Views (普通帖子用户信息)
    private TextView tvName, tvContent;
    private ImageView ivAvatar, ivBack;
    private LinearLayout llBodyUserInfo;

    // Header Views (视频帖子用户信息)
    private LinearLayout llHeaderUserInfo;
    private ImageView ivHeaderAvatar;
    private TextView tvHeaderName;

    private ViewPager2 viewPager;
    private VideoView videoView;
    private FrameLayout videoContainer;
    private ImageView btnFullScreen;
    private LinearLayout indicatorContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail_custom);

        post = (Post) getIntent().getSerializableExtra("post");
        currentUser = (User) getIntent().getSerializableExtra("user");

        initViews();
        setupPostContent(); // 先显示传递过来的基础内容
        setupComments();

        // 【核心修复】加载最新的作者信息，覆盖可能过时的 Intent 数据
        if (post != null) {
            loadLatestAuthorInfo(post.userId);
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        rvComments = findViewById(R.id.rv_comments);
        etComment = findViewById(R.id.et_comment);
        Button btnSend = findViewById(R.id.btn_send);

        // Body User Info
        llBodyUserInfo = findViewById(R.id.ll_user_info_body);
        tvName = findViewById(R.id.detail_name);
        tvContent = findViewById(R.id.detail_content);
        ivAvatar = findViewById(R.id.detail_avatar);

        // Header User Info
        llHeaderUserInfo = findViewById(R.id.ll_header_user_info);
        ivHeaderAvatar = findViewById(R.id.header_avatar);
        tvHeaderName = findViewById(R.id.header_name);

        viewPager = findViewById(R.id.view_pager_images);

        // Video Views
        videoContainer = findViewById(R.id.video_container);
        videoView = findViewById(R.id.detail_video_view);
        btnFullScreen = findViewById(R.id.btn_fullscreen);

        indicatorContainer = findViewById(R.id.indicator_container);

        ivBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String content = etComment.getText().toString();
            if (!TextUtils.isEmpty(content)) {
                sendComment(content);
            } else {
                Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 【新增方法】从数据库获取最新用户信息并刷新界面
    private void loadLatestAuthorInfo(int userId) {
        new Thread(() -> {
            User author = AppDatabase.getInstance(this).userDao().getUserById(userId);
            if (author != null) {
                runOnUiThread(() -> {
                    // 更新当前内存中的 post 对象属性，防止后续逻辑使用旧数据
                    post.userName = author.getName();
                    post.userAvatar = author.getAvatar();

                    // 刷新 UI 显示
                    RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(100));

                    if (post.type == 2) {
                        // 视频模式更新 Header
                        tvHeaderName.setText(author.getName());
                        Glide.with(PostDetailActivity.this)
                                .load(author.getAvatar())
                                .apply(options)
                                .placeholder(R.drawable.lan) // 设置占位图
                                .error(R.drawable.lan)       // 设置错误图
                                .into(ivHeaderAvatar);
                    } else {
                        // 普通模式更新 Body
                        tvName.setText(author.getName());
                        Glide.with(PostDetailActivity.this)
                                .load(author.getAvatar())
                                .apply(options)
                                .placeholder(R.drawable.lan)
                                .error(R.drawable.lan)
                                .into(ivAvatar);
                    }
                });
            }
        }).start();
    }

    private void setupPostContent() {
        if (post == null) return;

        tvContent.setText(post.content);

        // 初始加载（使用 Intent 数据作为缓冲，防止白屏，随后会被 loadLatestAuthorInfo 覆盖）
        RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(100));

        if (post.type == 2) {
            llBodyUserInfo.setVisibility(View.GONE);
            llHeaderUserInfo.setVisibility(View.VISIBLE);
            tvHeaderName.setText(post.userName);
            Glide.with(this)
                    .load(post.userAvatar)
                    .apply(options)
                    .placeholder(R.drawable.lan)
                    .error(R.drawable.lan)
                    .into(ivHeaderAvatar);
        } else {
            llHeaderUserInfo.setVisibility(View.GONE);
            llBodyUserInfo.setVisibility(View.VISIBLE);
            tvName.setText(post.userName);
            Glide.with(this)
                    .load(post.userAvatar)
                    .apply(options)
                    .placeholder(R.drawable.lan)
                    .error(R.drawable.lan)
                    .into(ivAvatar);
        }

        // 媒体处理逻辑不变
        if (post.mediaList != null && !post.mediaList.isEmpty()) {
            if (post.type == 2) {
                // ... 视频逻辑 ...
                viewPager.setVisibility(View.GONE);
                indicatorContainer.setVisibility(View.GONE);
                videoContainer.setVisibility(View.VISIBLE);

                String videoUrl = post.mediaList.get(0).url;
                videoView.setVideoPath(videoUrl);
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    videoView.start();
                });
                videoView.setOnClickListener(v -> {
                    if (videoView.isPlaying()) videoView.pause();
                    else videoView.start();
                });
                btnFullScreen.setOnClickListener(v -> {
                    Intent intent = new Intent(this, VideoFullScreenActivity.class);
                    intent.putExtra("video_url", videoUrl);
                    startActivity(intent);
                });

            } else {
                // ... 图片逻辑 ...
                videoContainer.setVisibility(View.GONE);
                viewPager.setVisibility(View.VISIBLE);
                ImagePagerAdapter imageAdapter = new ImagePagerAdapter(post.mediaList);
                viewPager.setAdapter(imageAdapter);
                setupIndicators(post.mediaList.size());
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        updateIndicators(position);
                    }
                });
            }
        } else {
            viewPager.setVisibility(View.GONE);
            videoContainer.setVisibility(View.GONE);
            indicatorContainer.setVisibility(View.GONE);
        }

        if (currentUser != null) {
            saveHistory();
        }
    }

    private void saveHistory() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.historyDao().deleteRecord(currentUser.getId(), post.id, 1);
            String cover = post.userAvatar;
            if (post.mediaList != null && !post.mediaList.isEmpty()) {
                cover = post.mediaList.get(0).url;
            }
            String title = !TextUtils.isEmpty(post.content) ? post.content : post.userName + "的动态";
            HistoryRecord record = new HistoryRecord(
                    currentUser.getId(), post.id, 1, title, cover, post.userName, System.currentTimeMillis()
            );
            db.historyDao().insert(record);
        }).start();
    }

    private void setupIndicators(int count) {
        if (count < 2) {
            indicatorContainer.setVisibility(View.GONE);
            return;
        }
        indicatorContainer.setVisibility(View.VISIBLE);
        indicatorContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            View view = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
            params.setMargins(5, 0, 5, 0);
            view.setLayoutParams(params);
            view.setBackgroundColor(0xFFE0E0E0);
            indicatorContainer.addView(view);
        }
        updateIndicators(0);
    }

    private void updateIndicators(int position) {
        int count = indicatorContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = indicatorContainer.getChildAt(i);
            view.setBackgroundColor(i == position ? 0xFF888888 : 0xFFE0E0E0);
        }
    }

    private void setupComments() {
        adapter = new CommentAdapter(this, commentList);
        if (currentUser != null) {
            adapter.setCurrentUserId(currentUser.getId());
        }
        adapter.setOnReplyListener(comment -> {
            String replyPrefix = "回复 " + comment.userName + ": ";
            etComment.setText(replyPrefix);
            etComment.setSelection(etComment.getText().length());
            etComment.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);
        loadComments();
    }

    private void loadComments() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Comment> comments = db.postDao().getCommentsForPost(post.id);
            for (Comment c : comments) {
                User u = db.userDao().getUserById(c.userId);
                if (u != null) {
                    c.userName = u.getName();
                    c.userAvatar = u.getAvatar();
                }
            }
            runOnUiThread(() -> {
                commentList.clear();
                commentList.addAll(comments);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void sendComment(String content) {
        if (post == null || currentUser == null) {
            Toast.makeText(this, currentUser == null ? "请先登录" : "数据错误", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            Comment comment = new Comment();
            comment.postId = post.id;
            comment.userId = currentUser.getId();
            comment.userName = currentUser.getName();
            comment.userAvatar = currentUser.getAvatar();
            comment.content = content;
            comment.createTime = System.currentTimeMillis();
            AppDatabase.getInstance(this).postDao().insertComment(comment);
            runOnUiThread(() -> {
                etComment.setText("");
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
                }
                Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show();
                loadComments();
            });
        }).start();
    }

    class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
        private List<PostMedia> mediaList;
        public ImagePagerAdapter(List<PostMedia> mediaList) { this.mediaList = mediaList; }

        @NonNull @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Glide.with(holder.imageView.getContext()).load(mediaList.get(position).url).into(holder.imageView);
            holder.imageView.setOnClickListener(v -> {
                ArrayList<String> urls = new ArrayList<>();
                for (PostMedia m : mediaList) urls.add(m.url);
                Intent intent = new Intent(PostDetailActivity.this, ImagePreviewActivity.class);
                intent.putStringArrayListExtra("images", urls);
                intent.putExtra("position", position);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return mediaList.size(); }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            public ImageViewHolder(@NonNull View itemView) { super(itemView); this.imageView = (ImageView) itemView; }
        }
    }
}