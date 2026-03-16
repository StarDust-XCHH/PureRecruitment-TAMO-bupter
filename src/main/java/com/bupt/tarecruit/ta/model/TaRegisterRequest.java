package com.bupt.tarecruit.ta.model;

public class TaRegisterRequest {
    private final String taId;
    private final String name;
    private final String username;
    private final String email;
    private final String phone;
    private final String password;
    private final String confirmPassword;

    public TaRegisterRequest(String taId, String name, String username, String email, String phone, String password, String confirmPassword) {
        this.taId = taId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getTaId() {
        return taId;
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
