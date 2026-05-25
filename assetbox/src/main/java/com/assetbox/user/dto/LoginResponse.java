package com.assetbox.user.dto;

public record LoginResponse(String accessToken, String tokenType, boolean profileRequired) {
}
