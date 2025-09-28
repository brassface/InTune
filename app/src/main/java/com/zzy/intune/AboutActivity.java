package com.zzy.intune;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public final class AboutActivity extends AppCompatActivity {

    private static final String GITHUB_URL = "https://github.com/yourname/Intune"; // 固定写死
    private static final String FEEDBACK_EMAIL = "support@example.com"; // 固定写死

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        String version = BuildConfig.VERSION_NAME;

        TextView tvApp = findViewById(R.id.tvAppName);
        TextView tvVer = findViewById(R.id.tvVersion);
        TextView tvMsg = findViewById(R.id.tvMsg);
        TextView tvGithub = findViewById(R.id.tvGithub);
        TextView tvEmail = findViewById(R.id.tvEmail);

        tvVer.setText("v" + version);

        // 更加活泼的文案
        String message = "感谢使用 合调 (InTune)! \n\n" +
                "如果觉得好用，欢迎到 GitHub 给我们点个 Star ✨\n" +
                "你的支持，是我们持续优化的动力 ( •̀ ω •́ )✧\n\n" +
                "有任何点子/吐槽/建议，都可以发邮件告诉我们：" + FEEDBACK_EMAIL + "\n" +
                "记得在标题写上：合调用户反馈 —— 方便我们快速定位（鞠躬）m(_ _)m\n\n" +
                "本软件为免费开源软件。如果您是通过付费渠道获取，请立即申请退款，并从官方渠道下载。";
        tvMsg.setText(message);

        tvGithub.setOnClickListener(v -> {
            try {
                android.content.Intent it = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(GITHUB_URL));
                startActivity(it);
            } catch (Exception ignored) {}
        });
        tvEmail.setOnClickListener(v -> {
            try {
                android.content.Intent email = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
                email.setData(android.net.Uri.parse("mailto:" + FEEDBACK_EMAIL));
                email.putExtra(android.content.Intent.EXTRA_SUBJECT, "合调用户反馈");
                startActivity(email);
            } catch (Exception ignored) {}
        });
    }
}


