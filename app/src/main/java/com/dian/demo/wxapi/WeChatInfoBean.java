package com.dian.demo.wxapi;

public class WeChatInfoBean {
    //授权用户唯一标识
    public String openid;
    //性别
    public int sex;
    //昵称
    public String nickname;
    //头像地址
    public String headimgurl;
    //省份
    public String province;
    //语言
    public String language;
    //国家
    public String country;
    //当且仅当该移动应用已获得该用户的 userinfo 授权时，才会出现该字段
    public String unionid;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getHeadimgurl() {
        return headimgurl;
    }

    public void setHeadimgurl(String headimgurl) {
        this.headimgurl = headimgurl;
    }

    public String getSex() {
        return (sex == 0) ? "男" : "女";
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }


    @Override
    public String toString() {
        return "WeChatInfo{" +
                "openid='" + openid + '\'' +
                ", sex='" + getSex() + '\'' +
                ", nickname='" + nickname + '\'' +
                ", headimgurl='" + headimgurl + '\'' +
                ", province='" + province + '\'' +
                ", language='" + language + '\'' +
                ", country='" + country + '\'' +
                ", unionid='" + unionid + '\'' +
                '}';
    }
}

