package com.bupt.tarecruit.mo.model;

/**
 * MO 注册请求数据传输对象。
 *
 * <p>封装注册表单字段，提供只读访问器。</p>
 */
public class MoRegisterRequest {
    private final String moId;
    private final String name;
    private final String username;
    private final String email;
    private final String phone;
    private final String password;
    private final String confirmPassword;

    public MoRegisterRequest(String moId, String name, String username, String email,
                             String phone, String password, String confirmPassword) {
        this.moId = moId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getMoId() {
        return moId;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
}
