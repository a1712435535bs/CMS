package com.example.dell.afinal.Adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.dell.afinal.Activity.PostInfoActivity;
import com.example.dell.afinal.R;
import com.example.dell.afinal.Utils.ToastUtil;
import com.example.dell.afinal.View.CircleImageView;
import com.example.dell.afinal.bean.Post;
import com.example.dell.afinal.bean.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.datatype.BmobRelation;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListener;
import cn.bmob.v3.listener.UpdateListener;

// 讨论区主题帖的适配器
public class PostListAdapter extends RecyclerView.Adapter<PostListAdapter.ViewHolder> {
    private List<Post> postList = new ArrayList<>();
    private Context mContext;

    public PostListAdapter(List<Post> list) {
        postList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.post_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.postView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPostViewClicked(viewHolder);
            }
        });

        viewHolder.zan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onZanIconClicked(viewHolder);
            }
        });
        return viewHolder;
    }

    // 点击帖子跳转
    private void onPostViewClicked(ViewHolder viewHolder) {
        Intent intent = new Intent(mContext, PostInfoActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("postTitle", viewHolder.postTitle.getText().toString());
        bundle.putString("postContent", viewHolder.postContent.getText().toString());
        bundle.putString("date", viewHolder.date.getText().toString());
        bundle.putString("authorId", viewHolder.authorId);
        bundle.putString("postId", viewHolder.postId);
        intent.putExtras(bundle);
        mContext.startActivity(intent);
    }

    // 点击点赞图标
    private void onZanIconClicked(final ViewHolder viewHolder) {
        BmobQuery<User> query = new BmobQuery<>();
        final Post post = new Post();
        post.setObjectId(viewHolder.postId);
        query.addWhereRelatedTo("zan", new BmobPointer(post));
        query.findObjects(new FindListener<User>() {
            @Override
            public void done(List<User> list, BmobException e) {
                if (e == null) {
                    loadZanDataSuccess(list, viewHolder);
                } else {
                    Log.e("读取点赞数失败:", e.toString());
                }
            }
        });
    }

    // 读取点赞数成功, 判断点击图标是点赞还是取消点赞
    private void loadZanDataSuccess(List<User> list, ViewHolder viewHolder) {
        User user = BmobUser.getCurrentUser(User.class);
        List<String> idList = new ArrayList<>();
        for (User u : list) idList.add(u.getObjectId());
        if (!idList.contains(user.getObjectId())) {
            addZan(viewHolder);
        } else {
            cancelZan(viewHolder);
        }
    }

    // 增加赞
    private void addZan(final ViewHolder viewHolder) {
        User user = BmobUser.getCurrentUser(User.class);
        Post post = new Post();
        post.setObjectId(viewHolder.postId);
        BmobRelation relation = new BmobRelation();
        relation.add(user);
        post.setZan(relation);
        post.update(new UpdateListener() {
            @Override
            public void done(BmobException e) {
                if (e == null) {
                    int zanNum = Integer.parseInt(viewHolder.zanNum.getText().toString());
                    zanNum += 1;
                    viewHolder.zanNum.setText(String.valueOf(zanNum));
                } else {
                    ToastUtil.toast(mContext, "操作失败，请检查你的网络或稍后再试");
                    Log.e("操作失败:", e.toString());
                }
            }
        });
    }

    // 取消赞
    private void cancelZan(final ViewHolder viewHolder) {
        User user = BmobUser.getCurrentUser(User.class);
        Post post = new Post();
        post.setObjectId(viewHolder.postId);
        BmobRelation relation = new BmobRelation();
        relation.remove(user);
        post.setZan(relation);
        post.update(new UpdateListener() {
            @Override
            public void done(BmobException e) {
                if (e == null) {
                    int zanNum = Integer.parseInt(viewHolder.zanNum.getText().toString());
                    zanNum -= 1;
                    viewHolder.zanNum.setText(String.valueOf(zanNum));
                } else {
                    ToastUtil.toast(mContext, "操作失败，请检查你的网络或稍后再试");
                    Log.e("操作失败:", e.toString());
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Post curPost = postList.get(position);
        holder.postTitle.setText(curPost.getTitle());
        holder.postContent.setText(curPost.getContent());
        holder.date.setText(curPost.getCreatedAt());             // 直接用Bmob SDK获取数据的创建时间
        holder.postId = curPost.getObjectId();                   // 获取帖子的id
        holder.authorId = curPost.getAuthor().getObjectId();     // 获取作者的id
        if(curPost.getImage() != null) {
            BmobFile pros = curPost.getImage();
            Picasso.with(mContext).load(pros.getFileUrl()).into(holder.postImg);
        } else {
            Picasso.with(mContext).load(R.mipmap.blank_holder).into(holder.postImg);
        }
        getAndSetZanNum(holder);
        getAuthorData(holder.authorId, holder);
    }

    // 获取并设置点赞数
    private void getAndSetZanNum(final ViewHolder holder) {
        BmobQuery<User> query = new BmobQuery<>();
        Post post = new Post();
        post.setObjectId(holder.postId);
        query.addWhereRelatedTo("zan", new BmobPointer(post));
        query.findObjects(new FindListener<User>() {
            @Override
            public void done(List<User> list, BmobException e) {
                if (e == null) {
                    holder.zanNum.setText(String.valueOf(list.size()));
                } else {
                    Log.e("读取点赞数失败:", e.toString());
                }
            }
        });
    }

    // 根据作者id从服务器读取作者头像及用户名(直接getAuthor获取失败 原因尚未明确)
    private void getAuthorData(String id, final ViewHolder holder) {
        BmobQuery<User> query = new BmobQuery<>();
        query.getObject(id, new QueryListener<User>() {
            @Override
            public void done(User user, BmobException e) {
                if (e == null) {
                    onLoadUserSuccess(user, holder);
                } else {
                    onLoadUserFailed(holder);
                    Log.e("读取用户资料失败:", e.toString());
                }
            }
        });
    }

    // 成功读取用户数据
    private void onLoadUserSuccess(User user, ViewHolder holder) {
        if(user.getNickName() != null) {
            holder.userName.setText(user.getNickName());
        } else {
            holder.userName.setText(user.getUsername());
        }
        if (user.getHeadFile() != null) {
            Picasso.with(mContext).load(user.getHeadFile().getFileUrl())
                    .into(holder.userImage);
        } else {
            Picasso.with(mContext).load(R.mipmap.ic_head).into(holder.userImage);
        }
        if (user.getSex() == 0) {
            Picasso.with(mContext).load(R.mipmap.male).into(holder.ivSex);
        } else {
            Picasso.with(mContext).load(R.mipmap.female).into(holder.ivSex);
        }
    }

    // 读取出现异常
    private void onLoadUserFailed(ViewHolder holder) {
        holder.userName.setText("匿名用户");
        Picasso.with(mContext).load(R.mipmap.ic_head).into(holder.userImage);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View postView;               // 帖子item
        CircleImageView userImage;   // 用户头像
        TextView userName;           // 用户名
        TextView date;               // 日期
        TextView postTitle;          // 帖子标题
        TextView postContent;        // 帖子正文
        String authorId;             // 作者
        String postId;               // 帖子的唯一id, 用于加载评论
        ImageView ivSex;             // 性别
        ImageView postImg;           // 帖子图片图片
        ImageView zan;               // 点赞图标
        TextView zanNum;             // 点赞数

        ViewHolder(View itemView) {
            super(itemView);
            postView = itemView.findViewById(R.id.item);
            userImage = itemView.findViewById(R.id.user_head_image);
            userName = itemView.findViewById(R.id.user_name);
            date = itemView.findViewById(R.id.date);
            postTitle = itemView.findViewById(R.id.post_title);
            postContent = itemView.findViewById(R.id.post_text);
            ivSex = itemView.findViewById(R.id.find_item_iv_sex);
            postImg = itemView.findViewById(R.id.pro);
            zan = itemView.findViewById(R.id.zan);
            zanNum = itemView.findViewById(R.id.zan_num);
        }
    }
}