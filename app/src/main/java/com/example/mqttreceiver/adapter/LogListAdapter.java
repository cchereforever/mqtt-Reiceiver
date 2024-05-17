package com.example.mqttreceiver.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.mqttreceiver.R;

import java.util.List;

public class LogListAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public LogListAdapter(List list) {
        super(R.layout.item_layout, list);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {

        helper.setText(R.id.textView, item);

    }
    public void addData(String data) {
        this.getData().add(data);
        notifyItemInserted(getData().size() - 1); // 确保通知RecyclerView有新的数据插入
    }
    public void clean() {
        this.getData().clear();
        notifyDataSetChanged();
    }

}
