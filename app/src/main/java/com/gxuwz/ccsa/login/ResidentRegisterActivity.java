package com.gxuwz.ccsa.login;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.model.User;
import com.gxuwz.ccsa.ui.resident.ResidentMainActivity;
import com.gxuwz.ccsa.util.SharedPreferencesUtil;

/**
 * 居民注册页面
 */
public class ResidentRegisterActivity extends AppCompatActivity {

    // UI控件
    private EditText etName;
    private RadioGroup rgGender;
    private EditText etPhone;
    private EditText etPassword;
    private Spinner spinnerCommunity;
    private Spinner spinnerBuilding;
    private Spinner spinnerRoom;
    private Button btnRegister;

    // 数据相关
    private AppDatabase db;
    private String gender = "男"; // 默认性别：男

    // 小区列表
    private final String[] communities = {
            "悦景小区", "梧桐小区", "阳光小区", "锦园小区", "幸福小区",
            "芳邻小区", "逸景小区", "康城小区", "沁园小区", "静安小区"
    };
    // 楼栋列表
    private final String[] buildings = {
            "1栋", "2栋", "3栋", "4栋", "5栋", "6栋", "7栋", "8栋", "9栋", "10栋"
    };
    // 房号列表
    private final String[] rooms = {
            "101", "102", "201", "202", "301", "302", "401", "402", "501", "502",
            "601", "602", "701", "702", "801", "802", "901", "902", "1001", "1002"
    };

    // 选中的地址信息
    private String selectedCommunity;
    private String selectedBuilding;
    private String selectedRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resident_register);

        // 初始化数据库
        db = AppDatabase.getInstance(this);
        // 初始化控件
        initViews();
        // 初始化下拉选择框
        setupSpinners();
        // 设置点击事件
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        rgGender = findViewById(R.id.rg_gender);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        spinnerCommunity = findViewById(R.id.spinner_community);
        spinnerBuilding = findViewById(R.id.spinner_building);
        spinnerRoom = findViewById(R.id.spinner_room);
        btnRegister = findViewById(R.id.btn_register);

        rgGender.check(R.id.rb_male);
    }

    private void setupSpinners() {
        // 小区适配器
        ArrayAdapter<String> communityAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, communities
        );
        communityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCommunity.setAdapter(communityAdapter);
        selectedCommunity = communities[0];

        // 楼栋适配器
        ArrayAdapter<String> buildingAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, buildings
        );
        buildingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBuilding.setAdapter(buildingAdapter);
        selectedBuilding = buildings[0];

        // 房号适配器
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, rooms
        );
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(roomAdapter);
        selectedRoom = rooms[0];

        // 选择监听
        spinnerCommunity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCommunity = communities[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerBuilding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBuilding = buildings[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRoom = rooms[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupListeners() {
        rgGender.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_male) {
                gender = "男";
            } else {
                gender = "女";
            }
        });

        btnRegister.setOnClickListener(v -> register());
    }

    /**
     * 注册核心逻辑
     */
    private void register() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. 基础非空校验
        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请填写所有必填信息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 校验手机号唯一性
        if (db.userDao().findByPhone(phone) != null) {
            Toast.makeText(this, "该手机号已注册，请直接登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 【新增】校验房屋地址唯一性 (小区 + 楼栋 + 房号)
        User addressOwner = db.userDao().findByAddress(selectedCommunity, selectedBuilding, selectedRoom);
        if (addressOwner != null) {
            // 如果查到了用户，说明该地址已经被注册了
            String msg = selectedCommunity + selectedBuilding + selectedRoom + " 已被注册！";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }

        // 4. 创建新用户
        User user = new User(
                name,
                gender,
                phone,
                password,
                selectedCommunity,
                selectedBuilding,
                selectedRoom
        );

        // 5. 插入数据库
        db.userDao().insert(user);

        // 6. 重新查询以获取生成的ID，并保存登录状态
        User registeredUser = db.userDao().findByPhone(phone);
        if (registeredUser != null) {
            // 保存登录状态，防止跳转后显示成别的账号
            SharedPreferencesUtil.saveUser(this, registeredUser);

            Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show();

            // 跳转主页
            Intent intent = new Intent(ResidentRegisterActivity.this, ResidentMainActivity.class);
            // 依然可以通过Bundle传递，虽然SharedPreferences已经保存了
            Bundle bundle = new Bundle();
            bundle.putSerializable("user", registeredUser);
            intent.putExtras(bundle);

            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
}