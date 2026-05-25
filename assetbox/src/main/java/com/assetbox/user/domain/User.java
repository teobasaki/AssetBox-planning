package com.assetbox.user.domain;

import com.assetbox.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String realName;
    private String nickname;
    private String major;
    private String bio;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    protected User() {
    }

    public Long getId() {
        return id;
    }
}
