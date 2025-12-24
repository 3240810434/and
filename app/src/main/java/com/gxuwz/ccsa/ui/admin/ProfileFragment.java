package com.gxuwz.ccsa.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.login.LoginActivity;
import com.gxuwz.ccsa.model.Admin;

import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private TextView tvAdminAccount;
    private ImageView btnChangePassword; // 类型改为 ImageView
    // private Button btnLogout; // 已删除
    private String adminAccount;
    private AppDatabase db;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            adminAccount = getArguments().getString("adminAccount");
        }
        db = AppDatabase.getInstance(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profile, container, false);

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        tvAdminAccount = view.findViewById(R.id.tv_admin_account);
        // ID保持不变，但强转类型变为 ImageView
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        // btnLogout = view.findViewById(R.id.btn_logout); // 已移除

        if (adminAccount != null) {
            // 增加“账号：”前缀
            tvAdminAccount.setText("账号：" + adminAccount);
        }
    }

    private void setupListeners() {
        // 修改密码点击事件
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // 退出登录按钮已删除
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("修改密码");

        // 动态创建对话框布局
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        final EditText etOldPassword = new EditText(getContext());
        etOldPassword.setHint("请输入原密码");
        layout.addView(etOldPassword);

        final EditText etNewPassword = new EditText(getContext());
        etNewPassword.setHint("请输入新密码");
        etNewPassword.setPadding(0, 30, 0, 0);
        layout.addView(etNewPassword);

        final EditText etConfirmPassword = new EditText(getContext());
        etConfirmPassword.setHint("请再次输入新密码");
        etConfirmPassword.setPadding(0, 30, 0, 0);
        layout.addView(etConfirmPassword);

        builder.setView(layout);

        builder.setPositiveButton("确定", (dialog, which) -> {
            // 点击确定由单独的方法处理，避免点击后直接关闭对话框无法验证
        });

        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // 重写 PositiveButton 的点击事件，防止校验失败时对话框关闭
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(getContext(), "所有字段都不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(getContext(), "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePassword(oldPass, newPass, dialog);
        });
    }

    private void updatePassword(String oldPass, String newPass, AlertDialog dialog) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Admin admin = db.adminDao().findByAccount(adminAccount);
            if (admin != null) {
                if (admin.getPassword().equals(oldPass)) {
                    // 更新密码
                    admin.setPassword(newPass);
                    db.adminDao().update(admin);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "密码修改成功，请重新登录", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();

                            // 密码修改成功后，直接执行退出登录逻辑跳转回登录页
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "原密码错误", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "当前账户异常", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}