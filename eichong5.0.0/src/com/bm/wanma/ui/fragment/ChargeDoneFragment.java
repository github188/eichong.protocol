package com.bm.wanma.ui.fragment;

import java.util.ArrayList;

import pulltorefresh.ListViewCustom;
import pulltorefresh.PullToRefreshBase;
import pulltorefresh.PullToRefreshBase.OnRefreshListener2;
import pulltorefresh.PullToRefreshScrollView;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ScrollView;
import android.widget.TextView;
import com.bm.wanma.R;
import com.bm.wanma.adapter.ChargeFinishAndDoneAdapter;
import com.bm.wanma.adapter.MyCouponDeprecateAdapter;
import com.bm.wanma.entity.ChargeFinishAndDoneBean;
import com.bm.wanma.entity.CouponBean;
import com.bm.wanma.net.GetDataPost;
import com.bm.wanma.net.Protocol;
import com.bm.wanma.ui.activity.ChargeDetailActivity;
import com.bm.wanma.ui.fragment.BaseFragment;
import com.bm.wanma.utils.PreferencesUtil;

public class ChargeDoneFragment extends BaseFragment implements OnRefreshListener2<ScrollView>{
	
	private ListViewCustom listview;
	private TextView tv_nodata;
	private PullToRefreshScrollView pScrollView;
	private final String pageNum = "20";
	private int currentPage;
	private boolean isRefresh;
	private String pkuserId;
	private ArrayList<ChargeFinishAndDoneBean> rawdata,beans;
	private ChargeFinishAndDoneAdapter maAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 默认加载第一页数据
		
		pkuserId = PreferencesUtil.getStringPreferences(getActivity(),"pkUserinfo");
		currentPage = 1;
		isRefresh = true;
	}
	 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View fragment = inflater.inflate(
				R.layout.fragment_charge_done, container, false);
//		tv_nodata = (TextView) fragment.findViewById(R.id.charge_done_nodata);
//		tv_nodata.setText("暂无数据");
		listview = (ListViewCustom) fragment.findViewById(R.id.charge_done_finish_listview);
		pScrollView = (PullToRefreshScrollView) fragment.findViewById(R.id.charge_done_finish_refresh);
		pScrollView.setOnRefreshListener(this);
		rawdata = new ArrayList<ChargeFinishAndDoneBean>();
		beans = new ArrayList<ChargeFinishAndDoneBean>();
		if(isNetConnection()){
			GetDataPost.getInstance(getActivity()).getChargeInfoList(handler, pkuserId, "1", String.valueOf(currentPage), pageNum);
		}
		return fragment;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onSuccess(String sign, Bundle bundle) {
		if(bundle != null ){
			pScrollView.onRefreshComplete();
			rawdata = (ArrayList<ChargeFinishAndDoneBean>) bundle.getSerializable(Protocol.DATA);
			if(rawdata.size()<20){
				pScrollView.pullsettins = false;
			}else {
				pScrollView.pullsettins = true;
			}
			if(isRefresh){
					beans.clear();
					beans.addAll(rawdata);
			}else {
				
				if(rawdata.size()>0){
					beans.addAll(rawdata);
				}else {
					showToast("暂无更多数据");
				}
			}
			
			if(beans.size()>0){
				if(maAdapter == null){
					maAdapter = new ChargeFinishAndDoneAdapter(getActivity(), beans);
					listview.setAdapter(maAdapter);
					listview.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent,
								View view, int position, long id) {
							Intent in = new Intent();
							in.setClass(getActivity(),ChargeDetailActivity.class);
							in.putExtra("ordernumber", beans.get(position).getPk_ChargingOrder());
							startActivity(in);
							
						}
					});
				}
//				tv_nodata.setVisibility(View.GONE);
//				listview.setVisibility(View.VISIBLE);
				maAdapter.notifyDataSetChanged();
			}
//			else {
//					tv_nodata.setVisibility(View.VISIBLE);
//					listview.setVisibility(View.GONE);
//			}
			
		}
	}

	@Override
	public void onFaile(String sign, Bundle bundle) {
		pScrollView.onRefreshComplete();
		showToast(bundle.getString(Protocol.MSG));
	}



	@Override
	public void onPullDownToRefresh(PullToRefreshBase<ScrollView> refreshView) {
		// 下拉刷新
		String label = (String) DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()); 
		refreshView.getLoadingLayoutProxy().setLastUpdatedLabel("最后更新："+label);
		isRefresh = true;
		currentPage = 1;
		if(isNetConnection()){
			GetDataPost.getInstance(getActivity()).getChargeInfoList(handler, pkuserId, "1", String.valueOf(currentPage), pageNum);
		}else {
			showToast("网络不稳，请稍后再试");
		}

	}

	@Override
	public void onPullUpToRefresh(PullToRefreshBase<ScrollView> refreshView) {
		// 上拉加载
		isRefresh = false;
		//获取下一页数据
		currentPage ++;
		if(isNetConnection()){
			GetDataPost.getInstance(getActivity()).getChargeInfoList(handler, pkuserId, "1", String.valueOf(currentPage), pageNum);
		}else {
			showToast("网络不稳，请稍后再试");
		}
		
	}
	@Override
	public void onPullUpToRefreshNoData(PullToRefreshBase<ScrollView> refreshView) {
		showToast("暂无更多数据");
	}
}
