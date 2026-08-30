package com.example.fopostdemo;

import com.fopost.sdk.FoPost;
import com.fopost.sdk.model.Account;
import com.fopost.sdk.model.Post;
import com.fopost.sdk.param.CreatePostParams;
import com.fopost.spring.FoPostProperties;
import java.util.List;
import org.springframework.stereotype.Service;

/** The client is a bean, so inject it like anything else. */
@Service
public class PublishService {

    private final FoPost fopost;
    private final String workspaceId;

    public PublishService(FoPost fopost, FoPostProperties properties) {
        this.fopost = fopost;
        this.workspaceId = properties.getDefaultWorkspaceId();
    }

    /** Creates a post across every connected account and queues it for delivery. */
    public String publish(String text) {
        List<String> accounts =
                fopost.accounts().list(workspaceId).stream().map(Account::id).toList();

        Post post = fopost.posts()
                .create(CreatePostParams.of(workspaceId).content(text).accounts(accounts));

        fopost.posts().publish(post.id());
        return post.id();
    }
}
