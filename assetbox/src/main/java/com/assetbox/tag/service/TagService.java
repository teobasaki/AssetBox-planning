package com.assetbox.tag.service;

import com.assetbox.tag.domain.Tag;
import com.assetbox.tag.dto.PopularTagResponse;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TagService {

    Set<Tag> findOrCreateAll(Collection<String> names);

    List<PopularTagResponse> popularTags(int limit);
}
