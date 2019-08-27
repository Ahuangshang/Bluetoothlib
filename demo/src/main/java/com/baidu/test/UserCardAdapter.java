package com.baidu.test;



import androidx.annotation.Nullable;


import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class UserCardAdapter extends BaseQuickAdapter<UserCardModel, BaseViewHolder> {

    public UserCardAdapter(int layoutResId, @Nullable List<UserCardModel> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, UserCardModel item) {
        helper.setText(R.id.tv_userId, item.getUserId());
    }
}
