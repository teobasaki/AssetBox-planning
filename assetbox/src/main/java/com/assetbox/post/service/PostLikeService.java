package com.assetbox.post.service;

public interface PostLikeService {

    boolean toggle(Long postId, Long userId);
}
