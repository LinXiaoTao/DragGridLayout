package com.leo.android.draggridlayout;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class MainActivity extends AppCompatActivity {

    private SimpleGridLayout mSimpleGridLayout;

    private EditText mEditColumn;
    private EditText mEditGap;
    private Button mBtnChange;

    private static final String TAG = "GridLayoutActivity";

    private LayoutInflater mLayoutInflater;
    private final static String[] IMG_SOURCE = {
            "http://img4.imgtn.bdimg.com/it/u=1916250222,309759298&fm=27&gp=0.jpg",
            "https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=4204913282,3820880852&fm=27&gp=0.jpg",
            "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=1160079957,1552586524&fm=27&gp=0.jpg",
            "https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=2528193201,1831047279&fm=27&gp=0.jpg",
            "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=3674324381,892248934&fm=27&gp=0.jpg",
            "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=1685223107,3467608528&fm=27&gp=0.jpg",
            "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=3381146714,2496287773&fm=27&gp=0.jpg",
            "https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=2254434047,2378609120&fm=27&gp=0.jpg",
            "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=3385842121,2880011954&fm=27&gp=0.jpg",
            "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=992967123,470419484&fm=27&gp=0.jpg",
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayoutInflater = LayoutInflater.from(this);

        mEditColumn = findViewById(R.id.edit_column);
        mEditGap = findViewById(R.id.edit_gap);
        mBtnChange = findViewById(R.id.btn_change);
        mSimpleGridLayout = findViewById(R.id.grildlayout);
        mSimpleGridLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(),"点击了 GridLayout",Toast.LENGTH_SHORT).show();
            }
        });
        mSimpleGridLayout.setGridItemClickListener(new GridItemClickListener() {
            @Override
            public void onClickItem(int position, View view) {
                Toast.makeText(view.getContext(),"点击了：" + position + " 子视图",Toast.LENGTH_SHORT).show();
            }
        });
        mBtnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int column = Integer.parseInt(mEditColumn.getText().toString());
                    int gap = Integer.parseInt(mEditGap.getText().toString());
                    mSimpleGridLayout.setColumnCount(column);
                    mSimpleGridLayout.setHGap(gap);
                    mSimpleGridLayout.setVGap(gap);
                    mSimpleGridLayout.buildAdapter();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "请输入正确的数字", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mSimpleGridLayout.setGridAdapter(new GridAdapter() {
            @Override
            public View getView(int position, ViewGroup parentView) {
                View itemView = mLayoutInflater.inflate(R.layout.item_grid, parentView, false);
                ImageView img = itemView.findViewById(R.id.img);
                Glide.with(parentView.getContext())
                        .load(IMG_SOURCE[position])
                        .apply(RequestOptions.placeholderOf(R.color.placeholder))
                        .apply(RequestOptions.errorOf(R.color.placeholder))
                        .into(img);
                return itemView;
            }

            @Override
            public int getCount() {
                return 9;
            }
        });

        mEditGap.setText(String.valueOf(mSimpleGridLayout.getHGap()));
        mEditColumn.setText(String.valueOf(mSimpleGridLayout.getColumnCount()));
    }
}
