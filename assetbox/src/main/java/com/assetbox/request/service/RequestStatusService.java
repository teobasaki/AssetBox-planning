package com.assetbox.request.service;

import com.assetbox.common.security.AuthUser;
import com.assetbox.request.domain.RequestStatus;

public interface RequestStatusService {

    void requireValidTransition(RequestStatus from, RequestStatus to, AuthUser actor);
}
