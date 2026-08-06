export const baseURL = "http://192.168.1.86:9000";
export type Result<T> = { ok: true; data: T } | { ok: false; error: string };

export type Session = {
    refreshToken: string;
    accessToken: string;
    expiration: number;
    userId: number;
};

// Helper function to get authenticated headers
function getAuthHeaders(session: Session): HeadersInit {
    console.log("Using Session: " + JSON.stringify(session));
    const headers: HeadersInit = {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${session.accessToken}`,
        "User": String(session.userId),
    };
    
    return headers;
}



export type loginResponse = {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    userId: number;
}

export type errorResponse = {
    error: string;
}

export async function login(username: string, password: string): Promise<Result<loginResponse>> {  
    const response = await fetch(baseURL + "/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            username, 
            password 
        }),
    });

    if (!response.ok) {
        try {
            const json = await response.json();
            const error = (json && json.error) ? String(json.error) : `Login failed (${response.status})`;
            return { ok: false, error };
        } catch (_e) {
            return { ok: false, error: `Login failed (${response.status})` };
        }
    }
    try {
        const json = await response.json();
        return { ok: true, data: json as loginResponse };
    } catch (_e) {
        return { ok: false, error: 'Invalid response from server' };
    }
}

export type registerResponse = {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    userId: number;
}

export async function register(username: string, password: string, biography: string, communityId: number): Promise<Result<registerResponse>>{
    const response = await fetch(baseURL + "/auth/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            username,
            password,
            biography,
            communityId
        }),
    });

    if (!response.ok) {
        try {
            const json = await response.json();
            const error = (json && json.error) ? String(json.error) : `Registration failed (${response.status})`;
            return { ok: false, error };
        } catch (_e) {
            return { ok: false, error: `Registration failed (${response.status})` };
        }
    }
    
    try {
        const json = await response.json();
        return { ok: true, data: json as registerResponse };
    } catch (_e) {
        return { ok: false, error: 'Invalid response from server' };
    }
}

export type refreshResponse = {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
}

export async function refreshToken(userid: number, refreshToken: string): Promise<Result<refreshResponse>> {
    const response = await fetch(baseURL + "/auth/refresh", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            userid,
            refreshToken
        }),

    });
    if (!response.ok) {
        try {
            const json = await response.json();
            const error = (json && json.error) ? String(json.error) : `Token refresh failed (${response.status})`;
            return { ok: false, error };
        } catch (_e) {
            return { ok: false, error: `Token refresh failed (${response.status})` };
        }
    }
    try {
        const json = await response.json();
        return { ok: true, data: json as refreshResponse };
    } catch (_e) {
        return { ok: false, error: 'Invalid response from server' };
    }
}

// Cluster API Types
export type ClusterBlob = {
    id: number;
    image: string;
    title: string;
    biography: string;
    tags: string[];
    community: {
        id: number;
        name: string;
        image: string;
    };
};

export type ClusterProfile = {
    id: number;
    image: string;
    title: string;
    biography: string;
    tags: string[];
    community: {
        id: number;
        name: string;
        image: string;
    };
    followers: number;
    posts: number;
};

export async function getClusters(session: Session, options: {
    format?: 'blob' | 'profile' | 'database';
    community?: number;
    followed?: number;
    parent?: number;
} = {}, userId?: number | null): Promise<Result<ClusterBlob[] | ClusterProfile[]>> {
    const params = new URLSearchParams({
        format: options.format || 'blob',
        ...(options.community && { community: String(options.community) }),
        ...(options.followed && { followed: String(options.followed) }),
        ...(options.parent && { parent: String(options.parent) })
    });
    
    const response = await fetch(`${baseURL}/clusters?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get clusters (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getCluster(session: Session, id: number, format: 'blob' | 'profile' | 'database' = 'blob', userId?: number | null): Promise<Result<ClusterBlob | ClusterProfile>> {
    const params = new URLSearchParams({ format });
    const response = await fetch(`${baseURL}/clusters/${id}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get cluster (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export type CliqueBlob = {
    id: number;
    image: string;
    name: string;
    biography: string;
    tags: string[];
};

export type CliqueProfile = {
    id: number;
    image: string;
    name: string;
    biography: string;
    tags: string[];
    members: number;
    followers: number;
};

export async function getCliques(session: Session, options: {
    format?: 'blob' | 'profile' | 'database';
    member?: number;
    followed?: number;
    hasPost?: number;
    achievement?: number;
    cluster?: number;
} = {}): Promise<Result<CliqueBlob[] | CliqueProfile[]>> {
    const params = new URLSearchParams({
        format: options.format || 'blob',
        ...(options.member && { member: String(options.member) }),
        ...(options.followed && { followed: String(options.followed) }),
        ...(options.hasPost && { hasPost: String(options.hasPost) }),
        ...(options.achievement && { achievement: String(options.achievement) }),
        ...(options.cluster && { cluster: String(options.cluster) })
    });
    
    const response = await fetch(`${baseURL}/cliques?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get cliques (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getClique(session: Session, id: number, format: 'blob' | 'profile' | 'database' = 'blob'): Promise<Result<CliqueBlob | CliqueProfile>> {
    const params = new URLSearchParams({ format });
    const response = await fetch(`${baseURL}/cliques/${id}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get clique (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export type UserBlob = {
    id: number;
    image: string;
    username: string;
    biography: string;
    community: {
        id: number;
        name: string;
        image: string;
    };
};

export type UserProfile = {
    id: number;
    image: string;
    username: string;
    biography: string;
    community: {
        id: number;
        name: string;
        image: string;
    };
    followers: number;
    following: number;
};

// User API Functions
export async function getUsers(session: Session, options: {
    format?: 'blob' | 'profile' | 'database';
    community?: number;
    followed?: number;
    following?: number;
    followingCluster?: number;
    followingClique?: number;
    memberClique?: number;
    likedPost?: number;
    viewedPost?: number;
    achievement?: number;
} = {}): Promise<Result<UserBlob[] | UserProfile[]>> {
    const params = new URLSearchParams({
        format: options.format || 'blob',
        ...(options.community && { community: String(options.community) }),
        ...(options.followed && { followed: String(options.followed) }),
        ...(options.following && { following: String(options.following) }),
        ...(options.followingCluster && { followingCluster: String(options.followingCluster) }),
        ...(options.followingClique && { followingClique: String(options.followingClique) }),
        ...(options.memberClique && { memberClique: String(options.memberClique) }),
        ...(options.likedPost && { likedPost: String(options.likedPost) }),
        ...(options.viewedPost && { viewedPost: String(options.viewedPost) }),
        ...(options.achievement && { achievement: String(options.achievement) })
    });
    
    const response = await fetch(`${baseURL}/users?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get users (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getUser(session: Session, id: number, format: 'blob' | 'profile' | 'database' = 'blob'): Promise<Result<UserBlob | UserProfile>> {
    const params = new URLSearchParams({ format });
    const response = await fetch(`${baseURL}/users/${id}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get user (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// Post API Types and Functions
// =============================================================================

export type SongBlob = {
    id: number;
    title: string;
    artist: string;
    album: string;
    image: string;
    duration: number;
    spotifyId?: string;
    appleMusicId?: string;
};

export type PostBlob = {
    id: number;
    caption: string;
    song: SongBlob;
    poster: UserBlob;
    clique: CliqueBlob;
    cluster: ClusterBlob;
    createdAt: string;
    rank: number;
    score: number;
};

export async function getPosts(session: Session, options: {
    format?: 'blob' | 'database';
    poster?: number;
    clique?: number;
    cluster?: number;
    song?: number;
    likedBy?: number;
    viewedBy?: number;
} = {}): Promise<Result<PostBlob[]>> {
    const params = new URLSearchParams({
        format: options.format || 'blob',
        ...(options.poster && { poster: String(options.poster) }),
        ...(options.clique && { clique: String(options.clique) }),
        ...(options.cluster && { cluster: String(options.cluster) }),
        ...(options.song && { song: String(options.song) }),
        ...(options.likedBy && { likedBy: String(options.likedBy) }),
        ...(options.viewedBy && { viewedBy: String(options.viewedBy) })
    });
    
    const response = await fetch(`${baseURL}/posts?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get posts (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getPost(session: Session, id: number, format: 'blob' | 'database' = 'blob'): Promise<Result<PostBlob>> {
    const params = new URLSearchParams({ format });
    const response = await fetch(`${baseURL}/posts/${id}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get post (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// Radio API Functions (Get personalized post feeds)
// =============================================================================

export async function getUserRadio(session: Session, userId: number, count: number = 10): Promise<Result<PostBlob[]>> {
    const params = new URLSearchParams({ count: String(count) });
    const response = await fetch(`${baseURL}/radio/user/${userId}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get user radio (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getCliqueRadio(session: Session, cliqueId: number, count: number = 10): Promise<Result<PostBlob[]>> {
    const params = new URLSearchParams({ count: String(count) });
    const response = await fetch(`${baseURL}/radio/clique/${cliqueId}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get clique radio (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getClusterRadio(session: Session, clusterId: number, count: number = 10): Promise<Result<PostBlob[]>> {
    const params = new URLSearchParams({ count: String(count) });
    const response = await fetch(`${baseURL}/radio/cluster/${clusterId}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get cluster radio (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getForYouRadio(session: Session, count: number = 10): Promise<Result<PostBlob[]>> {
    const response = await fetch(`${baseURL}/radio/foryou`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get for you radio (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getGlobalRadio(session: Session, count: number = 10): Promise<Result<PostBlob[]>> {
    const params = new URLSearchParams({ count: String(count) });
    const response = await fetch(`${baseURL}/radio/global?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get global radio (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getCommunityRadio(session: Session, communityId: number, count: number = 10): Promise<Result<PostBlob[]>> {
    const params = new URLSearchParams({ count: String(count) });
    const response = await fetch(`${baseURL}/radio/community/${communityId}?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get community radio (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function getFollowingRadio(session: Session, count: number = 10): Promise<Result<PostBlob[]>> {
    const params = new URLSearchParams({ count: String(count) });
    const response = await fetch(`${baseURL}/radio/following?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        return { ok: false, error: `Failed to get following radio (${response.status})` };
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// User Action API Functions
// =============================================================================

export type ActionResponse = {
    message: string;
};

export async function followUser(session: Session, userId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/follow/user/${userId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to follow user (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to follow user (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function unfollowUser(session: Session, userId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/unfollow/user/${userId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to unfollow user (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to unfollow user (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// Clique Action API Functions
// =============================================================================

export async function followClique(session: Session, cliqueId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/follow/clique/${cliqueId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to follow clique (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to follow clique (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function unfollowClique(session: Session, cliqueId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/unfollow/clique/${cliqueId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to unfollow clique (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to unfollow clique (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function joinClique(session: Session, cliqueId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/join/clique/${cliqueId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to join clique (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to join clique (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function leaveClique(session: Session, cliqueId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/leave/clique/${cliqueId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to leave clique (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to leave clique (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function createClique(session: Session, name: string, biography: string): Promise<Result<CliqueProfile>> {
    const params = new URLSearchParams({ name, biography });
    const response = await fetch(`${baseURL}/actions/create/clique?${params.toString()}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to create clique (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to create clique (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// Cluster Action API Functions
// =============================================================================

export async function followCluster(session: Session, clusterId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/follow/cluster/${clusterId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to follow cluster (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to follow cluster (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function unfollowCluster(session: Session, clusterId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/unfollow/cluster/${clusterId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to unfollow cluster (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to unfollow cluster (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// Post Action API Functions
// =============================================================================

export async function likePost(session: Session, postId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/like/post/${postId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to like post (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to like post (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function unlikePost(session: Session, postId: number): Promise<Result<ActionResponse>> {
    const response = await fetch(`${baseURL}/actions/unlike/post/${postId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to unlike post (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to unlike post (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function viewPost(session: Session, postId: number): Promise<Result<ActionResponse & { watchCount?: number }>> {
    const response = await fetch(`${baseURL}/actions/view/post/${postId}`, {
        method: "POST",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to view post (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to view post (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export type CreatePostRequest = {
    caption: string;
    songId: number;
    cliqueId: number;
    clusterId: number;
};

// =============================================================================
// Search API Types and Functions
// =============================================================================

export type SearchResultItem = {
    id: number;
    name: string;
    typ: 'user' | 'cluster' | 'clique';
    image: string;
    subtitle: string;
};

export type SearchResponse = {
    users: SearchResultItem[];
    clusters: SearchResultItem[];
    cliques: SearchResultItem[];
};

export async function search(session: Session, query: string, limit: number = 20): Promise<Result<SearchResponse>> {
    if (!query.trim()) {
        return { ok: true, data: { users: [], clusters: [], cliques: [] } };
    }
    
    const params = new URLSearchParams({
        query: query.trim(),
        limit: String(limit)
    });
    
    const response = await fetch(`${baseURL}/search?${params.toString()}`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Search failed (${response.status})` };
        } catch {
            return { ok: false, error: `Search failed (${response.status})` };
        }
    }
    
    const data = await response.json();
    return { ok: true, data };
}

export async function createPost(session: Session, postData: CreatePostRequest): Promise<Result<PostBlob>> {
    const response = await fetch(`${baseURL}/actions/create/post`, {
        method: "POST",
        headers: getAuthHeaders(session),
        body: JSON.stringify(postData)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to create post (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to create post (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// Profile API Types and Functions
// =============================================================================

export type UpdateProfileRequest = {
    displayName?: string;
    biography?: string;
    image?: string;
};

export type UpdateProfileResponse = {
    message: string;
    profile: UserProfile;
};

export async function updateProfile(session: Session, profileData: UpdateProfileRequest): Promise<Result<UpdateProfileResponse>> {
    const response = await fetch(`${baseURL}/profile/update`, {
        method: "POST",
        headers: getAuthHeaders(session),
        body: JSON.stringify(profileData)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to update profile (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to update profile (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

// =============================================================================
// Settings API Types and Functions
// =============================================================================

export type PrivacySettings = {
    profileVisible: boolean;
    showActivityStatus: boolean;
    allowTagging: boolean;
};

export type NotificationSettings = {
    push: boolean;
    mentions: boolean;
    messages: boolean;
    newFollowers: boolean;
};

export type UISettings = {
    language: string;
    fontSize: number;
};

export type PlayerSettings = {
    defaultService: 'spotify' | 'appleMusic';
};

export type LocationData = {
    latitude: number | null;
    longitude: number | null;
    city?: string | null;
    region?: string | null;
};

export type UserSettingsBlob = {
    userId: number;
    privacy: PrivacySettings;
    notifications: NotificationSettings;
    ui: UISettings;
    player: PlayerSettings;
    location: LocationData | null;
};

export type UpdateSettingsRequest = {
    privacy?: PrivacySettings;
    notifications?: NotificationSettings;
    ui?: UISettings;
    player?: PlayerSettings;
    location?: LocationData;
};

export type UpdateSettingsResponse = {
    message: string;
    settings: UserSettingsBlob;
};

export async function getSettings(session: Session): Promise<Result<UserSettingsBlob>> {
    const response = await fetch(`${baseURL}/settings`, {
        method: "GET",
        headers: getAuthHeaders(session)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to get settings (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to get settings (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}

export async function updateSettings(session: Session, settingsData: UpdateSettingsRequest): Promise<Result<UpdateSettingsResponse>> {
    const response = await fetch(`${baseURL}/settings/update`, {
        method: "POST",
        headers: getAuthHeaders(session),
        body: JSON.stringify(settingsData)
    });
    if (!response.ok) {
        try {
            const json = await response.json();
            return { ok: false, error: json.error || `Failed to update settings (${response.status})` };
        } catch {
            return { ok: false, error: `Failed to update settings (${response.status})` };
        }
    }
    const data = await response.json();
    return { ok: true, data };
}
