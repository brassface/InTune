package com.zzy.intune;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView rv = findViewById(R.id.rvModes);
        GridLayoutManager grid = new GridLayoutManager(this, 2);
        rv.setLayoutManager(grid);
        // 简单的Item间距
        int spacing = (int) (getResources().getDisplayMetrics().density * 12 + 0.5f);
        rv.addItemDecoration(new androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, android.view.View view, androidx.recyclerview.widget.RecyclerView parent, androidx.recyclerview.widget.RecyclerView.State state) {
                outRect.left = spacing/2;
                outRect.right = spacing/2;
                outRect.top = spacing/2;
                outRect.bottom = spacing/2;
            }
        });
        List<ModeItem> data = createModes();
        ModeAdapter adapter = new ModeAdapter(data, new ModeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ModeItem item) {
                Intent i = new Intent(MainActivity.this, com.zzy.intune.ModeActivity.class);
                i.putExtra("mode_key", item.key);
                startActivity(i);
            }
        });
        rv.setAdapter(adapter);

        // 显示并编辑噪声门阈值
        TextView tvTh = findViewById(R.id.tvThresholdHome);
        int current = getSharedPreferences("settings", MODE_PRIVATE).getInt("rms_threshold", 400);
        tvTh.setText(String.valueOf(current));
        android.widget.ImageView ivEditTh = findViewById(R.id.ivEditThresholdHome);
        ivEditTh.setColorFilter(getResources().getColor(R.color.colorPrimary));
        findViewById(R.id.rowThreshold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.ivEditThresholdHome).performClick();
            }
        });
        findViewById(R.id.ivEditThresholdHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final android.widget.EditText et = new android.widget.EditText(MainActivity.this);
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setText(tvTh.getText().toString());
                final androidx.appcompat.app.AlertDialog d = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("设置噪声门阈值")
                        .setView(et)
                        .setPositiveButton(R.string.ok, null)
                        .setNegativeButton(R.string.cancel, null)
                        .create();
                d.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(android.content.DialogInterface dialog) {
                        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String s = et.getText().toString().trim();
                                int val;
                                try { val = Integer.parseInt(s); } catch (Exception e) {
                                    android.widget.Toast.makeText(MainActivity.this, "请输入数字", android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (val < 50 || val > 5000) {
                                    android.widget.Toast.makeText(MainActivity.this, "数值需在 50~5000 之间", android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                getSharedPreferences("settings", MODE_PRIVATE).edit().putInt("rms_threshold", val).apply();
                                tvTh.setText(String.valueOf(val));
                                d.dismiss();
                            }
                        });
                    }
                });
                d.show();
            }
        });

        // 问号提示：定制 PopupWindow，指向问号图标上方
        findViewById(R.id.ivHelpThreshold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View anchor) {
                android.view.LayoutInflater inflater = android.view.LayoutInflater.from(MainActivity.this);
                android.view.View content = inflater.inflate(R.layout.tooltip_threshold, null, false);
                final android.widget.PopupWindow pw = new android.widget.PopupWindow(content,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        false);
                pw.setOutsideTouchable(true);
                pw.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                // 计算位置：显示在锚点正上方，水平居中
                int[] loc = new int[2];
                anchor.getLocationOnScreen(loc);
                int anchorX = loc[0];
                int anchorY = loc[1];
                int anchorW = anchor.getWidth();
                int anchorH = anchor.getHeight();

                content.measure(android.view.View.MeasureSpec.UNSPECIFIED, android.view.View.MeasureSpec.UNSPECIFIED);
                int pwW = content.getMeasuredWidth();
                int pwH = content.getMeasuredHeight();

                int x = anchorX + anchorW / 2 - pwW / 2;
                int y = anchorY - pwH - dp2px(6);

                // 使用 showAtLocation 以屏幕坐标显示
                pw.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, x, y);

                anchor.postDelayed(new Runnable() {
                    @Override public void run() { if (pw.isShowing()) pw.dismiss(); }
                }, 3000);
            }
        });

        // 主题模式：默认浅色，可切换浅色/深色/跟随系统
        int themeMode = getSharedPreferences("settings", MODE_PRIVATE).getInt("theme_mode", 1); // 1=light, 2=dark, 0=system
        updateThemeToggle(themeMode);
        findViewById(R.id.btnThemeLight).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { applyTheme(1); }
        });
        findViewById(R.id.btnThemeSystem).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { applyTheme(0); }
        });
        findViewById(R.id.btnThemeDark).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { applyTheme(2); }
        });
        // 整行点击切换（循环：浅色->深色->跟随系统->浅色）
        findViewById(R.id.rowTheme).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int cur = getSharedPreferences("settings", MODE_PRIVATE).getInt("theme_mode", 1);
                int next = (cur == 1) ? 2 : (cur == 2 ? 0 : 1);
                applyTheme(next);
            }
        });

        findViewById(R.id.rowAbout).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });
    }

    private int dp2px(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (dp * d + 0.5f);
    }

    private List<ModeItem> createModes() {
        List<ModeItem> list = new ArrayList<>();
        list.add(new ModeItem("standard", getString(R.string.mode_standard)));
        list.add(new ModeItem("drop_d", getString(R.string.mode_drop_d)));
        list.add(new ModeItem("open_g", getString(R.string.mode_open_g)));
        list.add(new ModeItem("custom", getString(R.string.mode_custom)));
        // 读取用户预设
        android.content.SharedPreferences sp = getSharedPreferences("presets", MODE_PRIVATE);
        java.util.Map<String, ?> all = sp.getAll();
        for (String name : all.keySet()) {
            list.add(new ModeItem("preset:" + name, name));
        }
        return list;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从自定义模式保存返回后刷新列表，立即显示新预设
        RecyclerView rv = findViewById(R.id.rvModes);
        if (rv != null && rv.getAdapter() instanceof ModeAdapter) {
            ModeAdapter ad = (ModeAdapter) rv.getAdapter();
            ad.setData(createModes());
            ad.notifyDataSetChanged();
        }
    }

    public void refreshModes() {
        RecyclerView rv = findViewById(R.id.rvModes);
        if (rv != null && rv.getAdapter() instanceof ModeAdapter) {
            ModeAdapter ad = (ModeAdapter) rv.getAdapter();
            ad.setData(createModes());
            ad.notifyDataSetChanged();
        }
    }

    private void applyTheme(int mode) {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putInt("theme_mode", mode).apply();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                mode == 2 ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES :
                        (mode == 0 ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        );
        updateThemeToggle(mode);
    }

    private void updateThemeToggle(int mode) {
        android.widget.ImageView iconLight = findViewById(R.id.iconLight);
        android.widget.ImageView iconSystem = findViewById(R.id.iconSystem);
        android.widget.ImageView iconDark = findViewById(R.id.iconDark);
        int active = getResources().getColor(R.color.colorPrimary);
        int inactive = getResources().getColor(R.color.colorCardStroke);
        iconLight.setColorFilter(mode == 1 ? active : inactive);
        iconSystem.setColorFilter(mode == 0 ? active : inactive);
        iconDark.setColorFilter(mode == 2 ? active : inactive);
    }
}

class ModeItem {
    public String key;
    public String name;

    public ModeItem(String key, String name) {
        this.key = key;
        this.name = name;
    }
}

class ModeAdapter extends RecyclerView.Adapter<ModeViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(ModeItem item);
    }

    private List<ModeItem> data;
    private OnItemClickListener listener;

    public ModeAdapter(List<ModeItem> data, OnItemClickListener l) {
        this.data = data;
        this.listener = l;
    }

    public void setData(List<ModeItem> newData) {
        this.data = newData;
    }

    @Override
    public ModeViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mode, parent, false);
        return new ModeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ModeViewHolder holder, int position) {
        ModeItem item = data.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }
}

class ModeViewHolder extends RecyclerView.ViewHolder {
    private TextView tvName;
    private android.widget.ImageView ivMenu;

    public ModeViewHolder(android.view.View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tvName);
        ivMenu = itemView.findViewById(R.id.ivMenu);
    }

    public void bind(final ModeItem item, final ModeAdapter.OnItemClickListener listener) {
        tvName.setText(item.name);
        if (item.key != null && item.key.startsWith("preset:")) {
            ivMenu.setVisibility(android.view.View.VISIBLE);
            ivMenu.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    showPresetMenu(v.getContext(), item);
                }
            });
        } else {
            ivMenu.setVisibility(android.view.View.GONE);
        }
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onItemClick(item);
            }
        });
    }

    private void showPresetMenu(android.content.Context ctx, final ModeItem item) {
        android.widget.PopupMenu pm = new android.widget.PopupMenu(ctx, ivMenu);
        pm.getMenu().add("复制");
        pm.getMenu().add("删除");
        pm.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem menuItem) {
                String title = menuItem.getTitle().toString();
                if ("复制".equals(title)) {
                    promptCopy(item);
                    return true;
                } else if ("删除".equals(title)) {
                    deletePreset(item);
                    return true;
                }
                return false;
            }
        });
        pm.show();
    }

    private void promptCopy(final ModeItem item) {
        final android.widget.EditText et = new android.widget.EditText(itemView.getContext());
        et.setHint("输入新预设名称");
        new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                .setTitle("复制预设")
                .setView(et)
                .setPositiveButton(R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialogInterface, int i) {
                        String name = et.getText().toString().trim();
                        if (name.length() == 0 || name.length() > 20) {
                            android.widget.Toast.makeText(itemView.getContext(), "名称需1-20字符", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        android.content.SharedPreferences sp = itemView.getContext().getSharedPreferences("presets", android.content.Context.MODE_PRIVATE);
                        if (sp.contains(name)) {
                            android.widget.Toast.makeText(itemView.getContext(), R.string.error_preset_exists, android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String src = sp.getString(item.name, null);
                        if (src != null) {
                            sp.edit().putString(name, src).apply();
                            android.widget.Toast.makeText(itemView.getContext(), "已复制", android.widget.Toast.LENGTH_SHORT).show();
                            if (itemView.getContext() instanceof MainActivity) {
                                ((MainActivity) itemView.getContext()).refreshModes();
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deletePreset(final ModeItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                .setTitle("删除预设")
                .setMessage("确定删除预设 " + item.name + " ?")
                .setPositiveButton(R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialogInterface, int i) {
                        android.content.SharedPreferences sp = itemView.getContext().getSharedPreferences("presets", android.content.Context.MODE_PRIVATE);
                        sp.edit().remove(item.name).apply();
                        if (itemView.getContext() instanceof MainActivity) {
                            ((MainActivity) itemView.getContext()).refreshModes();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}


