package org.sheedon.demo;

/**
 * 管理员卡片
 *
 * @author sheedon
 * @version 1.0
 * @time 2019/5/17
 */
public class AdminCard {
    private String id;// 住户ID
    private String name;// 住户姓名
    private String phone;// 手机号码
    private String userType;// 用户类型
    private boolean isDeleteMark;// 删除标识

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean isDeleteMark() {
        return isDeleteMark;
    }

    public void setDeleteMark(boolean deleteMark) {
        isDeleteMark = deleteMark;
    }
}
