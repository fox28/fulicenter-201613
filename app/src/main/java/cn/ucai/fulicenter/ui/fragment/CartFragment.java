package cn.ucai.fulicenter.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.fulicenter.R;
import cn.ucai.fulicenter.application.FuLiCenterApplication;
import cn.ucai.fulicenter.application.I;
import cn.ucai.fulicenter.model.bean.CartBean;
import cn.ucai.fulicenter.model.bean.GoodsDetailsBean;
import cn.ucai.fulicenter.model.bean.MessageBean;
import cn.ucai.fulicenter.model.bean.User;
import cn.ucai.fulicenter.model.net.CartModel;
import cn.ucai.fulicenter.model.net.ICartModel;
import cn.ucai.fulicenter.model.net.OnCompleteListener;
import cn.ucai.fulicenter.model.utils.CommonUtils;
import cn.ucai.fulicenter.model.utils.L;
import cn.ucai.fulicenter.model.utils.ResultUtils;
import cn.ucai.fulicenter.ui.adapter.CartAdapter;
import cn.ucai.fulicenter.ui.view.MFGT;
import cn.ucai.fulicenter.ui.view.SpaceItemDecoration;

/**
 * Created by clawpo on 2017/3/24.
 */

public class CartFragment extends Fragment {
    private static final String TAG = "CartFragment";

    ICartModel model;
    @BindView(R.id.tv_cart_sum_price)
    TextView mTvCartSumPrice;
    @BindView(R.id.tv_cart_save_price)
    TextView mTvCartSavePrice;
    @BindView(R.id.tv_refresh)
    TextView mTvRefresh;
    @BindView(R.id.tv_nothing)
    TextView mTvNothing;
    @BindView(R.id.rv)
    RecyclerView mRv;
    @BindView(R.id.srl)
    SwipeRefreshLayout mSrl;
    User user;
    ArrayList<CartBean> cartList = new ArrayList<>();
    LinearLayoutManager gm;
    CartAdapter adapter;
    @BindView(R.id.layout_cart)
    RelativeLayout mLayoutCart;
    UpdateReceiver mUpdateReceiver;
    int sumPrice = 0;
    int rankPrice = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        model = new CartModel();
        initView();
        initData();
        setListener();
    }

    private void setListener() {
        setPullDownListener();
        adapter.setListener(mOnCheckedChangeListener);
        adapter.setUpdateListener(updateListener);
        mUpdateReceiver = new UpdateReceiver();
        IntentFilter filter = new IntentFilter(I.BROADCAST_UPDATA_CART);
        getContext().registerReceiver(mUpdateReceiver,filter);
    }

    View.OnClickListener updateListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (int) view.getTag();
            int count =0;
            if (view.getTag(R.id.action_add_cart)!=null){
                count = (int) view.getTag(R.id.action_add_cart);
            }else if(view.getTag(R.id.action_del_cart)!=null){
                count = (int) view.getTag(R.id.action_del_cart);
            }
            L.e(TAG,"updateListener,position="+position+",count="+count);
            updateCart(position,count);
        }
    };

    private void updateCart(final int position, final int count) {
        CartBean bean = cartList.get(position);
        GoodsDetailsBean goods = bean.getGoods();
        int action = bean.getCount()+count==0?I.ACTION_CART_DEL:I.ACTION_CART_UPDATA;
        if (bean!=null && goods!=null) {
            model.cartAction(getContext(), action, String.valueOf(bean.getId()),
                    String.valueOf(goods.getGoodsId()),
                    FuLiCenterApplication.getCurrentUser().getMuserName(),
                    bean.getCount() + count,
                    new OnCompleteListener<MessageBean>() {
                        @Override
                        public void onSuccess(MessageBean result) {
                            if (result!=null && result.isSuccess()){
                                updateCartListView(position,count);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            L.e(TAG,"error="+error);
                        }
                    });
        }
    }

    private void updateCartListView(int position,int count) {
        L.e(TAG,"updateCartListView,position="+position+",count="+count);
        if (cartList.get(position).getCount()+count==0) {
            cartList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position,cartList.size()-position-1);
        }else{
            cartList.get(position).setCount(cartList.get(position).getCount() + count);
            adapter.notifyItemChanged(position);
        }
        L.e(TAG,"updateCartListView,notifyDataSetChanged....");
        setCartListLayut(!cartList.isEmpty());
        setPriceText();
    }

    CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            int position = (int) compoundButton.getTag();
            L.e(TAG,"onCheckedChanged....checked="+checked+",position="+position);
            cartList.get(position).setChecked(checked);
            setPriceText();
        }
    };

    private void setRefresh(boolean refresh) {
        mSrl.setRefreshing(refresh);
        mTvRefresh.setVisibility(refresh ? View.VISIBLE : View.GONE);
    }

    private void setCartListLayut(boolean isShow) {
        mTvNothing.setVisibility(isShow ? View.GONE : View.VISIBLE);
        mLayoutCart.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    private void setPullDownListener() {
        mSrl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setRefresh(true);
                initData();
            }
        });
    }

    private void initView() {
        mSrl.setColorSchemeColors(
                getResources().getColor(R.color.google_blue),
                getResources().getColor(R.color.google_green),
                getResources().getColor(R.color.google_red),
                getResources().getColor(R.color.google_yellow));
        gm = new LinearLayoutManager(getContext());
        mRv.setLayoutManager(gm);
        mRv.setHasFixedSize(true);
        adapter = new CartAdapter(getContext(), cartList);
        mRv.setAdapter(adapter);
        mRv.addItemDecoration(new SpaceItemDecoration(12));
        setCartListLayut(false);
        setPriceText();
    }

    private void initData() {
        user = FuLiCenterApplication.getCurrentUser();
        if (user != null) {
            showCartList();
        }
    }

    private void showCartList() {
        model.loadData(getContext(), user.getMuserName(),
                new OnCompleteListener<CartBean[]>() {
                    @Override
                    public void onSuccess(CartBean[] result) {
                        setRefresh(false);
                        setCartListLayut(true);
                        cartList.clear();
                        setPriceText();
                        if (result != null) {
                            if (result.length > 0) {
                                ArrayList<CartBean> list = ResultUtils.array2List(result);
                                cartList.addAll(list);
                                adapter.notifyDataSetChanged();
                            }else{
                                setCartListLayut(false);
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        setRefresh(false);
                        Log.e(TAG, "onError,error=" + error);
                    }
                });
    }

    private void setPriceText(){
        sumPrice = 0;
        rankPrice = 0;
        for (CartBean cart:cartList){
            if (cart.isChecked()){
                GoodsDetailsBean goods = cart.getGoods();
                if (goods!=null){
                    sumPrice += getPrice(goods.getCurrencyPrice())*cart.getCount();
                    rankPrice += getPrice(goods.getRankPrice())*cart.getCount();
                }
            }
        }
        mTvCartSumPrice.setText("合计：￥"+sumPrice);
        mTvCartSavePrice.setText("节省：￥"+(sumPrice-rankPrice));
    }

    private int getPrice(String p){
        String pStr = p.substring(p.indexOf("￥")+1);
        return Integer.valueOf(pStr);
    }

    class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            initData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUpdateReceiver!=null){
            getContext().unregisterReceiver(mUpdateReceiver);
        }
    }

    @OnClick(R.id.tv_cart_buy)
    public void buy(){
        if (sumPrice>0){
            MFGT.gotoOrder(getActivity(),rankPrice);
        }else{
            CommonUtils.showShortToast(R.string.order_nothing);
        }
    }
}
