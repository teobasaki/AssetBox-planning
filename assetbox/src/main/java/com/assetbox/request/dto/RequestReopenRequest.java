package com.assetbox.request.dto;

import com.assetbox.request.domain.RequestStatus;

public record RequestReopenRequest(RequestStatus targetStatus) {
}
