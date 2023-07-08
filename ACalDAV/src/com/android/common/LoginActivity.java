package com.android.common;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;


import ch.punkt.mp02.dailyview.R;

public class LoginActivity extends AppCompatActivity {

    private int mPosition;
    private String mName;
    private String mValue;

    private RecyclerView mRcContent;
    private View mCurrentView;

    private MyAdaptor myAdaptor;
    private CenterLayoutManager manager;

    private int mCurrentIndex = 1; // Activity starts from focus on 1st item (save=0)
    private int mLastPosition = 0;

    private TextView mSaveText;
    private TextView mLoginText;
    private EditText mInputText;
    private TextSwitcher mTextSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mTextSwitcher = findViewById(R.id.textSwitcher);
        mTextSwitcher.setFactory(mFactory);

        mPosition = getIntent().getIntExtra("position", 0);
        mName = getIntent().getStringExtra("name");
        mValue = getIntent().getStringExtra("value");

        if (mValue == null) {
            if ((mPosition == 0 && !getString(R.string.email_label).equals(mName)) ||
                    (mPosition == 1 && !getString(R.string.password_label).equals(mName))) {
                mValue = mName;
            } else if (mPosition == 1 && !getString(R.string.password_label).equals(mName)) {
                mValue = mName;
            } else if (mPosition == 2 && !getString(R.string.custom_server_label).equals(mName)) {
                mValue = mName;
            }

        }

        mRcContent = findViewById(R.id.rv_content_login);
        MenuItem.init(this);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(mRcContent);
        manager = new CenterLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRcContent.setLayoutManager(manager);
        mRcContent.setOnFlingListener(snapHelper);
        mRcContent.addItemDecoration(new MyDecoration());
        myAdaptor = new MyAdaptor();
        myAdaptor.setIListItemClick(iListItemClickListener);
        mRcContent.setAdapter(myAdaptor);
    }

    private final ViewFactory mFactory = new ViewFactory() {

        @Override
        public View makeView() {
            // Create a new TextView
            TextView t = new TextView(LoginActivity.this);
            t.setTextAppearance(R.style.mp02TextViewFocusStyle);
            return t;
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (mCurrentIndex == 0) {
                        return true;
                    }
                    mCurrentIndex--;
                    manager.smoothScrollToPosition(mRcContent, null, 1, 0);
                    requestUnfocus(mLoginText);
                    requestFocus(mSaveText);
                    mInputText.setSelected(false);
                    mInputText.clearFocus();
                    mInputText.setPaddingRelative(mLoginText.getPaddingStart(), mInputText.getPaddingTop(), mInputText.getPaddingEnd(), mInputText.getPaddingBottom());
                    mSaveText.setSelected(true);
                    setTextSwitcher(MenuItem.Direction.UP);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (mCurrentIndex == 1) {
                        return true;
                    }
                    mCurrentIndex++;
                    manager.smoothScrollToPosition(mRcContent, null, 0, 1);
                    requestFocusInput();
                    mInputText.setPaddingRelative(mLoginText.getPaddingStart(), mInputText.getPaddingTop(), mInputText.getPaddingEnd(), mInputText.getPaddingBottom());
                    setTextSwitcher(MenuItem.Direction.DOWN);
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
//                    if (mCurrentView != null) mCurrentView.performClick();
                    mSaveText.performClick();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    finish();
                    return true;
            }
        }
        if (mLastPosition != mCurrentIndex) {
            mLastPosition = mCurrentIndex;
        }
        return super.dispatchKeyEvent(event);
    }

    private void setTextSwitcher(MenuItem.Direction direction) {
        switch (direction) {
            case DOWN:
                mTextSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.upandvisible));
                mTextSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.upandgone));
                mTextSwitcher.setText(mLoginText.getText());
                break;
            case UP:
                mTextSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.downandvisible));
                mTextSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.downandgone));
                mTextSwitcher.setText(mSaveText.getText());
                break;
        }
    }

    private void requestFocus(TextView textView) {
//        textView.setTextAppearance(R.style.mp02TextViewFocusStyle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 54);
        textView.setLayoutParams(lp);
        textView.setPadding(5, 0, 0, 0);
        mCurrentView = textView;
    }

    private void requestUnfocus(TextView textView) {
//        textView.setTextAppearance(R.style.mp02TextViewUnfocusStyle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 32);
        textView.setLayoutParams(lp);
        textView.setPadding(30, 2, 0, 2);
    }

    private void requestFocusInput() {
        requestFocus(mLoginText);
        requestUnfocus(mSaveText);
        mSaveText.setSelected(false);
        mInputText.setSelected(true);
        mInputText.requestFocus();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        View mView;

        MyHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    class MyAdaptor extends RecyclerView.Adapter<MyHolder> {
        private static final int ITEM_ONE = 1;
        private static final int ITEM_TWO = 2;

        IListItemClick mIListItemClick;
        SparseArray<MyHolder> mSparseArray = new SparseArray<>();

        public void setIListItemClick(IListItemClick mIListItemClick) {
            this.mIListItemClick = mIListItemClick;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return ITEM_ONE;
            } else {
                return ITEM_TWO;
            }
        }

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case ITEM_ONE:
                    View view1 = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_layout, parent, false);
                    return new MyHolder(view1);
                case ITEM_TWO:
                    View view2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.two_items_view_layout, parent, false);
                    return new MyHolder(view2);
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final MyHolder holder, int position) {
            if (position == 0) {
                mSaveText = holder.mView.findViewById(R.id.tv_item);
                mSaveText.setText(getString(R.string.save_label));
                requestFocus(mSaveText);
            } else if (position == 1) {
                mLoginText = holder.mView.findViewById(R.id.login_text);
                mInputText = holder.mView.findViewById(R.id.login_edit);
                if (mPosition == 0) {
                    mLoginText.setText(getString(R.string.email_label));
                    mInputText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                } else if (mPosition == 1) {
                    mLoginText.setText(getString(R.string.password_label));
                    mInputText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else if (mPosition == 2) {
                    mLoginText.setText(getString(R.string.custom_server_label));
                    mInputText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                }
                mInputText.setText(mValue);
                if (mValue != null && mValue.length() > 0) {
                    mInputText.setSelection(mValue.length());
                }
                manager.smoothScrollToPosition(mRcContent, null, 0, 1);
                requestFocusInput();
                mTextSwitcher.setText(mLoginText.getText());
            }

            holder.mView.setTag(position);
            mSaveText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mIListItemClick != null) {
                        mIListItemClick.itemClick(v, 0);
                    }
                }
            });

            mSparseArray.put(position, holder);

        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    static class MyDecoration extends RecyclerView.ItemDecoration {
        private int distance = 0;

        @Override
        public void getItemOffsets(Rect outRect, final View view, final RecyclerView parent, RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            if (distance <= 0) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        distance = 76;
                        View childView = parent.getChildAt(0);
                        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) childView.getLayoutParams();
                        layoutParams.setMargins(0, distance, 0, 0);
                        childView.setLayoutParams(layoutParams);
                        parent.scrollToPosition(1);
                    }
                });
            }
            if (pos == 0) {
                layoutParams.setMargins(0, distance, 0, 0);
            } else if (pos == 1) {
                layoutParams.setMargins(0, 0, 0, 73);
            }
            view.setLayoutParams(layoutParams);
            super.getItemOffsets(outRect, view, parent, state);
        }
    }

    private final IListItemClick iListItemClickListener = new IListItemClick() {
        @Override
        public void itemClick(View view, int position) {
            if (position == 0) {
                Intent intent = new Intent();

                String name = "";
                if (mPosition == 0) {
                    name = getString(R.string.email_label);
                } else if (mPosition == 1) {
                    name = getString(R.string.password_label);
                } else if (mPosition == 2) {
                    name = getString(R.string.custom_server_label);
                }

                intent.putExtra("Name", name);
                intent.putExtra("Save", mInputText.getText().toString());
                setResult(0, intent);
                finish();
            }
        }
    };
}