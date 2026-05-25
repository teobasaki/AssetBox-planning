package com.assetbox.user.dto;

public record SignupRequest(String email, String password, String realName, String nickname, String major) {
}
